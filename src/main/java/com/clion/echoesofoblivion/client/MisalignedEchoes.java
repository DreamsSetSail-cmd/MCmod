package com.clion.echoesofoblivion.client;

import com.clion.echoesofoblivion.Config;
import com.clion.echoesofoblivion.ConfigHelper;
import com.clion.echoesofoblivion.sound.ModSounds;
import com.clion.echoesofoblivion.world.ModWorldGen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 错位的回响（v2.1.0）——**回声本该是延迟的复制品。
 * 当回声开始出错，说明承载它的介质已经在腐坏。**
 *
 * <h2>它做什么</h2>
 *
 * <p>在寂静走廊里，玩家会听到**自己刚刚听过的一段声音**被重放，但：
 *
 * <ul>
 *   <li><b>音高错</b>：永远偏低（0.55~0.90）。更高的音会显得滑稽，更低的音才显得旧。</li>
 *   <li><b>方向反</b>：来源被反射到玩家的另一侧，让人回头看不存在的来源。</li>
 *   <li><b>截断</b>：音量压到 0.45~0.70，听起来像自己衰没了。</li>
 *   <li><b>延迟错</b>：2~7 秒之后才响，久到玩家已经忘了原声。</li>
 * </ul>
 *
 * <h2>为什么不新增音效</h2>
 *
 * <p>这一版零新素材。介质腐坏不需要新声音——它需要**同一段声音坏掉**。
 * 玩家必须先认出「这个我听过」，机制才成立，因此复用是设计的一部分，
 * 不是省钱。
 *
 * <h2>为什么在客户端做</h2>
 *
 * <p>{@code PlaySoundEvent} 是客户端事件，而「玩家听到了什么」本来就是
 * 客户端的知识。放在客户端意味着不需要新增网络包、不需要为每个玩家在服务端
 * 维护一份缓冲，也不会有「服务端播了但没到客户端」的不同步。
 * 代价只是表现层判断，错的后果最多是玩家多听到一段坏掉的声音。
 *
 * <h2>四条克制的规则</h2>
 *
 * <ol>
 *   <li><b>只记录来自世界里某个坐标的声音</b>，且离玩家至少 3 格。
 *       贴着你响的脚步与挖掘声重放出来只会像卡顿。</li>
 *   <li><b>排除模组自己的环境音与心跳</b>。它们本来就是氛围，
 *       重放没有「出错」感；心跳重放则纯粹是烦人。</li>
 *   <li><b>概率压得很低</b>（每秒 1/26，且两次之间至少 20 秒）。
 *       介质在腐坏，不是音响坏了。频繁出现就退化成 bug 观感。</li>
 *   <li><b>不记录自己放出来的回声</b>。否则回声会产生回声，无限滚下去。</li>
 * </ol>
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MisalignedEchoes {

    /** 环形缓冲容量。刻意很小：太久以前的声音重放出来，玩家认不出是自己的回声。 */
    private static final int CAPACITY = 12;

    /** 记录一个声音的最小距离（格）。 */
    private static final double MIN_DISTANCE = 3.0;

    /** 每秒判定一次。 */
    private static final int CHECK_INTERVAL = 20;

    /** 每次判定的触发概率。1/26 ≈ 每 26 秒一次期望值。 */
    private static final float CHANCE = 1.0f / 26.0f;

    /** 两次回响之间的最短间隔（tick）。20 秒。 */
    private static final int MIN_GAP = 400;

    /** 延迟范围（tick）。2~7 秒。 */
    private static final int DELAY_MIN = 40;
    private static final int DELAY_SPAN = 100;

    /**
     * 一个声音记录。
     *
     * <p>存 {@link ResourceLocation} 而不是 {@code SoundEvent}：Minecraft 的
     * {@code SoundInstance} 没有公开取回 {@code SoundEvent} 的接口
     * （{@code AbstractSoundInstance} 只在构造时接收它，不保留可访问的字段）。
     * 而用资源路径重放反而更通用——它同样能重放原版与其它模组的声音，
     * 且 {@code SimpleSoundInstance} 正好有一个以路径为参数的构造器。
     */
    private record Entry(ResourceLocation location, Vec3 position) {
    }

    private static final Deque<Entry> RECENT = new ArrayDeque<>();

    private static int tickCounter;
    private static int sinceLastEcho = MIN_GAP;

    /** 「你听过的某个东西回来了，但不对」是否已经说过。整个会话只说一次。 */
    private static boolean announced;

    /**
     * 防止重放自身被记录成新的回声。
     *
     * <p>{@code SoundManager.playDelayed} 不会再次触发 {@code PlaySoundEvent}
     * （事件在 {@code SoundEngine.play} 之前触发，而 playDelayed 走的是同一条路径），
     * 所以这个标志在正常情况下是保险而不是必需——但回声机制一旦滚雪球会迅速
     * 淹没玩家，值得用一行代价换掉这个风险。
     */
    private static boolean replaying;

    private MisalignedEchoes() {
    }

    /**
     * 读取配置：开关与频率系数。
     *
     * <p>{@code ConfigHelper} 是为「配置可能尚未加载」这个坑准备的包装
     * （见技术手册 §6.1）——客户端 tick 可能早于配置加载，
     * 直接读 {@code Config.CLIENT_SPEC} 会抛异常。
     */
    private static boolean enabled() {
        return ConfigHelper.getBoolean(Config.enableMisalignedEchoes, true);
    }

    private static double rate() {
        return ConfigHelper.getDouble(Config.misalignedEchoRate, 1.0);
    }

    /** 记录玩家听到的每一个声音。这是整个机制唯一的数据来源。 */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (replaying) {
            return;
        }
        SoundInstance sound = event.getSound();
        if (sound == null || sound.isRelative()) {
            // 相对声音（贴着玩家的 UI / 自听音）没有空间感，重放它们听起来像故障
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Vec3 origin = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        if (origin.distanceTo(minecraft.player.position()) < MIN_DISTANCE) {
            return;
        }
        if (!isMeaningful(sound.getLocation().getPath(), sound.getSource())) {
            return;
        }
        if (RECENT.size() >= CAPACITY) {
            RECENT.removeFirst();
        }
        RECENT.addLast(new Entry(sound.getLocation(), origin));
    }

    /**
     * 排除不适合重放的声音。
     *
     * <p>按**音效路径**与**音源类别**判断，不引用具体注册项——
     * 这样新增音效时不需要回来改这里。
     */
    private static boolean isMeaningful(String path, SoundSource source) {
        if (path.equals(ModSounds.HEARTBEAT.getId().getPath())
            || path.equals(ModSounds.WHISPER_AMBIENT.getId().getPath())
            || path.equals(ModSounds.MEMORY_VISION.getId().getPath())
            || path.equals(ModSounds.CORRUPTION_PULSE.getId().getPath())) {
            return false;
        }
        // 音乐与唱片不属于「世界里的声音」：它们本来就带自己的空间氛围
        return source != SoundSource.MUSIC && source != SoundSource.RECORDS;
    }

    /** 每秒判定一次。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            RECENT.clear();
            return;
        }

        sinceLastEcho++;
        tickCounter++;
        if (tickCounter % CHECK_INTERVAL != 0) {
            return;
        }

        if (!enabled()) {
            return;
        }

        // 只在寂静走廊里发生。走廊的介质是坏的，主世界还不是。
        // 用维度 ResourceKey 比较而不是名字符串——后者脆弱且不符合规范。
        if (!minecraft.level.dimension().equals(ModWorldGen.SILENT_CORRIDOR)) {
            return;
        }
        // 不额外要求侵蚀阈值：进入走廊本身就是「你已经开始被改写」的证据。
        if (sinceLastEcho < MIN_GAP || RECENT.isEmpty()) {
            return;
        }

        RandomSource random = minecraft.player.getRandom();
        if (random.nextFloat() >= (float) (CHANCE * rate())) {
            return;
        }

        Entry chosen = pick(random);
        if (chosen == null) {
            return;
        }

        sinceLastEcho = 0;

        // 方向：反射到玩家的另一侧。只有一半概率真的反，让「方向反了」本身
        // 也不确定——否则玩家很快会学会「听到什么就去看反方向」。
        boolean reflect = random.nextBoolean();
        double x = reflect ? minecraft.player.getX() * 2.0 - chosen.position().x
                           : chosen.position().x;
        double z = reflect ? minecraft.player.getZ() * 2.0 - chosen.position().z
                           : chosen.position().z;

        float pitch = 0.55f + random.nextFloat() * 0.35f;    // 永远偏低：显得旧
        float volume = 0.45f + random.nextFloat() * 0.25f;   // 压低：像自己衰没了
        int delay = DELAY_MIN + random.nextInt(DELAY_SPAN);  // 2~7 秒：久到忘了原声

        playCorrupted(chosen.location(), x, chosen.position().y, z, volume, pitch, delay);

        // 第一次发生时给一句提示，此后永远不再出现。
        // 只提示「它回来了，但不对」——不解释。玩家自己会得出
        // 「承载回声的介质坏了」这个结论，那比被告知有效得多。
        if (!announced) {
            announced = true;
            minecraft.player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                    "message.echoesofoblivion.misaligned_echo")
                    .withStyle(net.minecraft.ChatFormatting.DARK_AQUA), true);
        }
    }

    /** 从缓冲里随机取一条。 */
    private static Entry pick(RandomSource random) {
        int size = RECENT.size();
        if (size == 0) {
            return null;
        }
        int index = random.nextInt(size);
        int i = 0;
        for (Entry entry : RECENT) {
            if (i++ == index) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 播放一段「坏掉」的声音。
     *
     * <p>用 {@code SimpleSoundInstance} 的路径构造器 + {@code SoundManager.playDelayed}
     * 表达「延迟」——比自己在客户端 tick 里维护一个倒计时队列更准确，
     * 也不会因为客户端卡顿而漂移。
     */
    private static void playCorrupted(ResourceLocation location, double x, double y, double z,
                                      float volume, float pitch, int delayTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSoundManager() == null) {
            return;
        }
        SimpleSoundInstance instance = new SimpleSoundInstance(
            location,
            SoundSource.AMBIENT,
            volume,
            pitch,
            RandomSource.create(),
            false,
            0,
            SoundInstance.Attenuation.LINEAR,
            x, y, z,
            false);
        replaying = true;
        try {
            minecraft.getSoundManager().playDelayed(instance, delayTicks);
        } finally {
            replaying = false;
        }
    }

    /** 供调试与测试查询：当前缓冲里有多少条。 */
    public static int bufferedCount() {
        return RECENT.size();
    }

    /** 进入世界时清空——上一个世界的回声不该跟过来。 */
    public static void reset() {
        RECENT.clear();
        sinceLastEcho = MIN_GAP;
    }
}