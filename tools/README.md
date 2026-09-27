# tools/

存放**开发期辅助工具**。这些文件不参与模组打包（`build.gradle` 只把
`src/main/java` 与 `src/main/resources`、`src/generated/resources` 纳入构建），
因此不会进入发布产物。

---

## SoundSynth.java

音效合成器。用 JDK 自带的 `javax.sound.sampled` 生成 8 个核心音效的
16-bit 单声道 WAV，**不依赖任何第三方库或素材**。

```bash
javac -d run/downloads/synth-classes tools/SoundSynth.java
java -cp run/downloads/synth-classes SoundSynth <输出目录>
```

输出：`memory_collect / memory_vision / heartbeat / corruption_pulse /
boss_summon / boss_hit / portal_open / portal_travel` 共 8 个 wav。

> **后续步骤不可省略**：Minecraft 的 `SoundManager` 把音效事件名硬编码为 `.ogg` 路径，
> wav 文件不会被识别（已实测，会产生 `File ... does not exist` 警告）。
> 必须转码：
>
> ```bash
> ffmpeg -y -i in.wav -c:a libvorbis -q:a 4 -ar 44100 -ac 1 out.ogg
> ```

各音效的设计意图与合成方式见 `../ASSETS.md` 与 `../CREDITS.md`。

---

## make_empty_structure.py

生成 gameTest 需要的空结构模板
（`src/main/resources/data/echoesofoblivion/structure/empty.nbt`）。

```bash
python tools/make_empty_structure.py src/main/resources/data/echoesofoblivion/structure
```

**为什么需要它**：`@GameTest(template = "...")` 会让测试框架去数据包里找一个
**结构 NBT 文件**。原版并没有名为 `minecraft:empty` 的结构，直接引用它会在启动
测试时抛：

```
IllegalStateException: Missing test structure: minecraft:empty
```

本脚本用**纯 Python 标准库**（`gzip` + `struct`）写出该文件——NBT 是 Mojang 的
公开二进制格式，Minecraft 侧由 `NbtIo.readCompressed` 读取（会自动解 gzip），
因此不需要任何第三方库，也不需要动 Java 类路径。

产物约 109 字节（未压缩 101 字节），内容为 5×5×5 的空区域、palette 只含
`minecraft:air`、`DataVersion=3953`（与 1.20.6 一致）。

---

## make_textures.py

生成 9 张像素画贴图（8 个物品图标 + 1 个方块六面贴图）。

```bash
python tools/make_textures.py
```

输出到 `src/main/resources/assets/echoesofoblivion/textures/`。

**用纯 Python 标准库手写 PNG**（`zlib` + `struct`），不依赖 Pillow 或任何图像库，
因此：无需安装依赖、版权完全属于本项目、像素级设计可版本控制。

提供的基础能力（`Canvas` 类）：字符网格绘制（`rows`）、填充多边形（`polygon`）、
圆/圆环（`circle`/`ring`）、矩形（`rect`）、外缘描边（`outline`）、
欧氏距离柔光（`glow`）、垂直明暗（`shade_vertical`）。

> **经验记录**：第一版全部用字符网格手绘，结果走廊之钥的匙杆只有 1 像素宽、
> 真理碎片糊成一条线、虚空余烬被画布边缘裁切。改用几何图元（圆环 + 矩形 +
> 多边形）重画这三张后才有干净的轮廓。16×16 下手绘网格的精度不够，
> 对称图形应当用图元生成。

---

## cc0-assets/

已验证授权的 CC0 第三方素材留档（含 `SOURCE.txt` 记录来源、作者、授权与用途）。

目前包含 `chests/`（Treasure Chests，CC0 1.0）——因语义不符未接入模组，
保留作为备用素材。详见 `../CREDITS.md` 第 3 节。

---

## check_lang.py

校验全部语言文件与 `en_us.json` 是否一致。

```bash
python tools/check_lang.py
```

检查项：

1. JSON 是否合法（UTF-8）
2. 键集合是否与基准**完全一致**（不缺、不多）
3. 占位符 `%s` 与 `%%` 的出现次数是否与基准一致
4. 记忆正文里的字面量换行 `\n` 数量是否一致
   **（少了它幻境屏幕就不会分句）**
5. 共鸣标记 `◈` 是否存在

退出码 0 表示全部通过。**修改任何语言文件后都应运行一次**——
翻译时漏掉或改坏占位符（例如把 `%s` 译成了 `%d`）会让客户端在运行时崩溃。

新增语言的方法：复制 `en_us.json` 为 `<locale>.json`，
按 `../docs/translation-spec.md` 的术语表与规则翻译，然后跑这个脚本验证。

---

## 验证方式

本项目的自动化验证手段（按成本从低到高）：

| 命令 | 覆盖范围 | 耗时 |
| --- | --- | --- |
| `gradlew compileJava` | 编译正确性 | ~20s |
| `gradlew build` | 编译 + 打包（含 datagen 产物） | ~1min |
| `gradlew runGameTestServer` | 服务端逻辑（Boss 免疫、稳定性阶段、记忆库一致性、属性注册） | ~3min |
| `gradlew runData` | datagen provider 输出 | ~2min |
| `gradlew runServer` | 数据包加载（维度 / 群系 / 地物 / 配方）、服务端启动 | ~2min |
| `gradlew runClient` | 客户端启动、资源解析、渲染路径 | ~5min |

`runClient` 是最慢但覆盖面最广的一环：它会真实加载全部资源，
因此**音效是否缺失、模型是否可解析、渲染器是否抛异常**都在这一步暴露。

### 已知的、可忽略的日志噪声

- `Missing sound for event: minecraft:item.goat_horn.play` 等 —— 原版 1.20.6
  自身缺少的乐器音效文件，与本模组无关。
- `Assets URL ... uses unexpected schema` —— ForgeGradle 开发环境的类路径提示。
- `Ignoring duplicate module on SecureModuleFinder: ...` —— LWJGL 多平台原生库提示。
- `Configuration conflict: there is more than one oshi.properties` —— 依赖重复告警。

### gameTest 的环境限制（重要）

在 Forge 50.2.10 的 gameTest 环境下，**任何会移动的 `LivingEntity` 在 tick 时都会抛异常**：

```
ForgeConfigSpec$ConfigValue.get
  <- ForgeHooks.isLivingOnLadder      （Forge 自己的代码）
  <- LivingEntity.onClimbable
  <- LivingEntity.tick
```

原因是 `ForgeHooks.isLivingOnLadder` 读取 `ForgeConfig.SERVER`，而**游戏测试服务器
不加载服务端配置**。这是环境限制，与被测模组无关；真机上不会发生。

因此 `gametest/BossGameTests.java` 刻意**不调用 `entity.tick()`**，也不使用
`helper.spawn()`（那会把实体注册进 `EntityTickList` 接受 tick 驱动）。
它只验证不依赖 tick 的逻辑：稳定性阶段推进、武器免疫、记忆库一致性、
方块放置、属性注册。

需要实体真正跑 tick 才能验证的行为（恐惧光环累积、亡魂追击、Boss 阶段表现），
只能靠 `runClient` 实机验证。
