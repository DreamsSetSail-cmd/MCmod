import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 * Echoes of Oblivion —— 音效合成器（阶段 7）。
 *
 * <p>为什么自己合成：本环境没有 ffmpeg，无法把现成素材转码成 Minecraft 需要的格式；
 * 而 Minecraft 原生支持 16-bit PCM WAV。用 JDK 自带的 {@code javax.sound.sampled}
 * 直接生成 WAV，音色、时长、包络都完全可控，也不需要任何外部依赖或授权。
 *
 * <p>输出目录由第一个参数指定。
 *
 * <p>设计目标：全部服务于「心理恐怖」——没有跳杀音，只有低频、缓慢、不稳定的东西。
 */
public final class SoundSynth {

    private static final int RATE = 44100;

    public static void main(String[] args) throws IOException {
        File outDir = new File(args.length > 0 ? args[0] : ".");
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IOException("无法创建输出目录: " + outDir);
        }

        write(outDir, "memory_collect", SoundSynth::memoryCollect, 1.6);
        write(outDir, "memory_vision", SoundSynth::memoryVision, 3.0);
        write(outDir, "heartbeat", SoundSynth::heartbeat, 1.4);
        write(outDir, "corruption_pulse", SoundSynth::corruptionPulse, 2.6);
        write(outDir, "boss_summon", SoundSynth::bossSummon, 4.5);
        write(outDir, "boss_hit", SoundSynth::bossHit, 1.1);
        write(outDir, "portal_open", SoundSynth::portalOpen, 2.6);
        write(outDir, "portal_travel", SoundSynth::portalTravel, 1.5);

        System.out.println("完成，输出目录: " + outDir.getAbsolutePath());
    }

    // ------------------------------------------------------------------ 合成定义

    /** 记忆收集：清亮的钟声，带轻微失谐与长尾，像水晶被唤醒。 */
    private static void memoryCollect(float[] buf) {
        int n = buf.length;
        double[] partials = {1.0, 2.01, 3.03, 4.77, 6.4};
        double[] gains = {1.0, 0.5, 0.28, 0.16, 0.09};
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            // 钟体的指数衰减 + 前 8ms 的极短起音，避免爆音
            double env = Math.exp(-t * 2.4) * attack(t, 0.008);
            double s = 0.0;
            for (int p = 0; p < partials.length; p++) {
                // 每个分音略有失谐，制造「不纯」的水晶感
                double detune = 1.0 + (p * 0.0017);
                s += gains[p] * Math.sin(2 * Math.PI * 660.0 * partials[p] * detune * t);
            }
            // 起始处叠一层上滑的高频"闪光"
            if (t < 0.25) {
                double sweep = 1400.0 + t * 4000.0;
                s += 0.22 * Math.exp(-t * 9.0) * Math.sin(2 * Math.PI * sweep * t);
            }
            buf[i] = (float) (s * env * 0.30);
        }
    }

    /** 幻境开启：低频涌入 + 反向混响感，空间像被抽走。 */
    private static void memoryVision(float[] buf) {
        int n = buf.length;
        double prev = 0.0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double total = n / (double) RATE;
            // 缓慢升起的包络（前 60% 上升，后 40% 维持），制造"被吸入"
            double env = Math.min(1.0, t / (total * 0.6));
            // 三个低频失谐正弦，形成拍频颤动
            double drone = Math.sin(2 * Math.PI * 55.0 * t)
                + 0.7 * Math.sin(2 * Math.PI * 57.3 * t)
                + 0.5 * Math.sin(2 * Math.PI * 82.5 * t);
            // 一阶低通噪声，作为"空气"层
            double noise = Math.random() * 2 - 1;
            prev = prev * 0.995 + noise * 0.005;
            double shimmer = 0.35 * Math.sin(2 * Math.PI * (600.0 + 200.0 * Math.sin(t * 1.3)) * t);
            buf[i] = (float) ((drone * 0.22 + prev * 6.0 + shimmer * 0.10) * env * 0.9);
        }
    }

    /** 心跳：一次 lub-dub，两拍之间有明显间隔。 */
    private static void heartbeat(float[] buf) {
        addThump(buf, 0.00, 62.0, 0.16, 0.85);
        addThump(buf, 0.26, 54.0, 0.22, 0.62);
    }

    /** 腐蚀脉冲：非常慢的呼吸式低频，带轻微失真，重复两轮。 */
    private static void corruptionPulse(float[] buf) {
        int n = buf.length;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            // 两轮 1.3s 的呼吸
            double phase = (t % 1.3) / 1.3;
            double env = Math.sin(Math.PI * phase);
            env *= env;
            // 基频缓慢下潜，像什么东西在沉下去
            double freq = 48.0 - t * 4.0;
            double s = Math.sin(2 * Math.PI * freq * t);
            // 加入三倍频做一点"脏"
            s += 0.22 * Math.sin(2 * Math.PI * freq * 3.0 * t);
            s = Math.tanh(s * 1.4);
            buf[i] = (float) (s * env * 0.26);
        }
    }

    /** Boss 降临：长时间上行的低频轰鸣 + 颤音，压迫感逐步建立。 */
    private static void bossSummon(float[] buf) {
        int n = buf.length;
        double total = n / (double) RATE;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double progress = t / total;
            // 频率从 30Hz 升到 90Hz，像巨大存在正在靠近
            double freq = 30.0 + progress * 60.0;
            // 颤音随进度加快
            double tremolo = 1.0 + 0.25 * Math.sin(2 * Math.PI * (3.0 + progress * 9.0) * t);
            double s = Math.sin(2 * Math.PI * freq * t);
            s += 0.5 * Math.sin(2 * Math.PI * freq * 1.5 * t);
            s += 0.3 * Math.sin(2 * Math.PI * freq * 2.02 * t);
            // 越到后面越"脏"
            s = Math.tanh(s * (1.0 + progress * 1.8));
            // 渐强，末尾稍收
            double env = Math.pow(progress, 1.6) * (progress > 0.92 ? (1.0 - progress) / 0.08 : 1.0);
            buf[i] = (float) (s * env * tremolo * 0.28);
        }
    }

    /** 武器无效化：沉闷的"被吞掉"，不是金属撞击。 */
    private static void bossHit(float[] buf) {
        int n = buf.length;
        double prev = 0.0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            // 极快的下潜音
            double freq = 220.0 * Math.exp(-t * 9.0);
            double bodyEnv = Math.exp(-t * 7.0) * attack(t, 0.003);
            double body = Math.sin(2 * Math.PI * freq * t) * bodyEnv;
            // 被吸走的噪声尾巴
            double noise = Math.random() * 2 - 1;
            prev = prev * 0.97 + noise * 0.03;
            double tail = prev * Math.exp(-t * 3.0) * 2.2;
            buf[i] = (float) ((body * 0.55 + tail * 0.35) * 0.8);
        }
    }

    // ------------------------------------------------------------------ 工具

    /** 传送门点燃：空间被撕开的上升噪声 + 低频共振，末尾趋于稳定。 */
    private static void portalOpen(float[] buf) {
        int n = buf.length;
        double total = n / (double) RATE;
        // 简单的单极点带通状态，用来给噪声塑形
        double lp = 0.0;
        double hp = 0.0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double progress = t / total;
            double noise = Math.random() * 2 - 1;

            // 噪声的"撕裂"层：带通中心频率随时间上下扫
            double cutoff = 900.0 + 2600.0 * Math.sin(Math.PI * progress);
            double alpha = Math.min(0.9, cutoff / RATE * 6.283);
            lp += alpha * (noise - lp);
            hp = noise - lp;
            double tear = hp * (1.0 - progress * 0.6);

            // 低频共振：从 40Hz 升到 70Hz，给出"门被撑开"的体量
            double bass = Math.sin(2 * Math.PI * (40.0 + progress * 30.0) * t);
            bass = Math.tanh(bass * 1.6);

            // 包络：快速起音，中段维持，末尾收束
            double env = attack(t, 0.05)
                * (progress > 0.85 ? (1.0 - progress) / 0.15 : 1.0);

            buf[i] = (float) ((tear * 0.16 + bass * 0.26) * env);
        }
    }

    /** 传送瞬间：短促的空间错位——音高骤降 + 一声闷响。 */
    private static void portalTravel(float[] buf) {
        int n = buf.length;
        double prev = 0.0;
        double phase = 0.0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            // 频率从 900Hz 掉到 90Hz，制造"被拽走"的下坠感
            double freq = 900.0 * Math.exp(-t * 2.6) + 90.0;
            phase += 2 * Math.PI * freq / RATE;
            double tone = Math.sin(phase) * Math.exp(-t * 3.2);

            // 一层被吸走的噪声
            double noise = Math.random() * 2 - 1;
            prev = prev * 0.94 + noise * 0.06;
            double whoosh = prev * Math.exp(-t * 4.0) * 1.8;

            double env = attack(t, 0.004);
            buf[i] = (float) ((tone * 0.32 + whoosh * 0.30) * env);
        }
    }

    /** 在指定时间点叠加一次低频"砰"。 */
    private static void addThump(float[] buf, double atSeconds, double freq,
                                 double decay, double gain) {
        int start = (int) (atSeconds * RATE);
        for (int i = start; i < buf.length; i++) {
            double t = (i - start) / (double) RATE;
            double env = Math.exp(-t / decay) * attack(t, 0.004);
            // 频率随时间下滑，模拟胸腔的闷响
            double f = freq * (1.0 - Math.min(0.35, t * 1.2));
            double s = Math.sin(2 * Math.PI * f * t);
            buf[i] += (float) (s * env * gain * 0.5);
        }
    }

    /** 消除起始爆音的极短淡入。 */
    private static double attack(double t, double milliseconds) {
        double a = milliseconds / 1000.0;
        return t >= a ? 1.0 : t / a;
    }

    private static void write(File dir, String name, Synth synth, double seconds)
        throws IOException {
        int n = (int) (seconds * RATE);
        float[] buf = new float[n];
        synth.render(buf);

        // 全局限幅，防止叠加后削波
        float peak = 0.0f;
        for (float v : buf) {
            peak = Math.max(peak, Math.abs(v));
        }
        float norm = peak > 0.95f ? 0.95f / peak : 1.0f;

        byte[] bytes = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            int s = Math.round(buf[i] * norm * 32767.0f);
            s = Math.max(-32768, Math.min(32767, s));
            bytes[i * 2] = (byte) (s & 0xFF);
            bytes[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }

        AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
        File file = new File(dir, name + ".wav");
        try (AudioInputStream stream = new AudioInputStream(
            new ByteArrayInputStream(bytes), format, n)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);
        }
        System.out.printf("  %-20s %6.2fs  %7d bytes%n", name + ".wav", seconds, file.length());
    }

    @FunctionalInterface
    private interface Synth {
        void render(float[] buffer);
    }
}
