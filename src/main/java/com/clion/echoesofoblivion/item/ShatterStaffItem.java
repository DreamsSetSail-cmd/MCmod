package com.clion.echoesofoblivion.item;

import com.clion.echoesofoblivion.entity.PhantomEntity;
import com.clion.echoesofoblivion.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 碎晶法杖（v2.0.0）——**远程把回响打散**。
 *
 * <h2>为什么需要远程</h2>
 *
 * <p>记忆之刃很强，但它要求你贴身。而感染区里的亡魂是**成群**出现的，
 * 贴身清场意味着你会在雾里被围住——雾会随侵蚀收近，你看不见援军。
 *
 * <p>法杖给一条远程通路：射出一束反粒子，落点半径 6 内的亡魂全部消散。
 * 它不改动聚合体（同刀刃），因此不破坏「武器无用」的核心设定——
 * 它只是让你在去往 Boss 的路上不那么容易死。
 *
 * <h2>实现</h2>
 *
 * <p>不做真实弹射物实体（那需要额外的 EntityType、渲染器与网络同步），
 * 而是**在服务端做一次射线检测**：沿视线找到第一个方块或实体命中点，
 * 在那里结算范围效果，并把轨迹用粒子画出来。
 *
 * <p>这样做的代价是「没有飞行时间」，但收益是不引入新的同步面——
 * 对于一个以氛围而非手感为核心的模组，这是正确的取舍。
 */
public class ShatterStaffItem extends Item {

    /** 最大射程（格）。 */
    private static final double RANGE = 32.0;

    /** 落点效果半径。 */
    private static final double BLAST_RADIUS = 6.0;

    /** 冷却（tick）。2 秒。 */
    private static final int COOLDOWN = 40;

    public ShatterStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));

        // 1. 先找方块阻挡点
        BlockHitResult blockHit = level.clip(new ClipContext(
            eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 impact = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        // 2. 再看这条线上有没有实体更近（取更近的那个作为落点）
        AABB sweep = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            level, player, eye, end, sweep,
            e -> e instanceof PhantomEntity && e.isAlive() && !e.isSpectator());
        if (entityHit != null && entityHit.getLocation().distanceToSqr(eye) < impact.distanceToSqr(eye)) {
            impact = entityHit.getLocation();
        }

        // 3. 轨迹：一束向前的反粒子
        for (double t = 0.0; t < eye.distanceTo(impact); t += 0.5) {
            Vec3 p = eye.add(look.scale(t));
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                p.x, p.y, p.z, 2, 0.02, 0.02, 0.02, 0.0);
        }

        // 4. 落点结算：半径内的亡魂消散
        List<PhantomEntity> hit = serverLevel.getEntitiesOfClass(PhantomEntity.class,
            new AABB(impact, impact).inflate(BLAST_RADIUS));
        for (PhantomEntity phantom : hit) {
            serverLevel.sendParticles(ParticleTypes.SOUL,
                phantom.getX(), phantom.getY() + 1.0, phantom.getZ(),
                20, 0.3, 0.5, 0.3, 0.03);
            phantom.discard();
        }

        serverLevel.sendParticles(ParticleTypes.FLASH, impact.x, impact.y, impact.z, 2, 0.0, 0.0, 0.0, 0.0);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            impact.x, impact.y, impact.z, 50, 1.2, 1.2, 1.2, 0.05);
        serverLevel.playSound(null, impact.x, impact.y, impact.z, ModSounds.CORRUPTION_PULSE.get(),
            SoundSource.PLAYERS, 1.0f, 1.5f);

        if (!hit.isEmpty()) {
            serverPlayer.displayClientMessage(
                Component.translatable("item.echoesofoblivion.shatter_staff.hit", hit.size())
                    .withStyle(ChatFormatting.AQUA), true);
        }

        // 耐久与冷却
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, slotFor(hand));
        }
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echoesofoblivion.shatter_staff.desc")
            .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.echoesofoblivion.shatter_staff.useless")
            .withStyle(ChatFormatting.RED));
    }

    /** 交互手 → 装备槽。{@code ItemStack#hurtAndBreak} 需要槽位而不是手。 */
    private static net.minecraft.world.entity.EquipmentSlot slotFor(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND
            ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
            : net.minecraft.world.entity.EquipmentSlot.OFFHAND;
    }
}
