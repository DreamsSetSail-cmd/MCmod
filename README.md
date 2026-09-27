# Echoes of Oblivion

> You are not the hero. You are the contamination.

A psychological horror / exploration mod for **Minecraft 1.20.6** on **Forge**.

*[中文说明见 README.zh_cn.md](README.zh_cn.md)*

---

## What it is

Step through an unstable portal into the **Silent Corridor** — an endless, lifeless dimension
strewn with the ruins of a dead civilization. There are no monsters there, no animals, no weather.
Only silence, and the faint glow of **Memory Crystals**.

Touch a crystal and you witness a fragment of that civilization's final moments. Piece the
fragments together and you will learn what they did to themselves.

That knowledge does not stay in the corridor. You bring it home.

## Core mechanics

**Memory archaeology** — Five fixed memories, each with its own clue. Collect clues and connected
memories begin to *resonate*, revealing more than any single memory could. Progress is per-player
and persists in the world save.

**Psychological horror, no jump scares** — Corruption climbs as you learn. It thickens the fog
around you, spawns phantom glimpses in the corner of your eye, and layers whispers and a heartbeat
under the ambient audio. The higher it gets, the harder it is to see where you are going.

**Reality collapse** — Once you return to the Overworld, **Mirror Chunks** begin to spread from
wherever you stand. Inside them gravity flickers, particles run backwards, and phantoms take form
and hunt the living.

**A conceptual boss** — The **Silent Aggregate** is immune to every weapon in the game. It cannot
be killed, only *unsettled*: each memory you have witnessed can be used exactly once to damage its
stability. Resonant memories cut twice as deep. Witnessing all five memories, standing inside an
infected chunk, and holding the Eye of Silence are the three conditions for summoning it.

## Installation

1. Install **Minecraft Forge 50.2.10 or newer for 1.20.6**.
2. Drop the jar into your `mods/` folder.

Requires **Java 21**. Works on both client and dedicated server.

## Playing it

| Action | How |
| --- | --- |
| Open a portal | Build a 2×3 frame of obsidian / crying obsidian / amethyst, then right-click its interior with the **Corridor Key** |
| Enter the corridor | Walk into the portal |
| Witness a memory | Right-click a **Memory Crystal** |
| Escape the vision | `ESC` or click |
| Memory attack (boss) | `R` — the selected hotbar slot picks which memory you use |
| Summon the boss | Hold the **Eye of Silence**, stand inside an infected chunk, and have all 5 memories witnessed |

## Configuration

Two config files are generated on first run: `echoesofoblivion-common.toml` (server-side) and
`echoesofoblivion-client.toml` (client-side). Notable options:

| Key | Default | Meaning |
| --- | --- | --- |
| `corruptionGainRate` | 1 | Natural corruption gain per second |
| `mirrorExpandInterval` | 300 | Seconds between Mirror Chunk spreads |
| `enableMirrorChunks` | true | Toggle the reality-collapse mechanic |
| `overlayIntensity` | 1.0 | Strength of the corruption fog |
| `enableShadowEffects` | true | Toggle phantom glimpses |

## Languages

The mod ships with **21 locales** (59 strings each):

`en_us` `zh_cn` `zh_tw` `ja_jp` `ko_kr` `ru_ru` `uk_ua` `de_de` `fr_fr` `es_es` `es_mx`
`it_it` `pt_br` `pt_pt` `nl_nl` `pl_pl` `cs_cz` `sv_se` `da_dk` `nb_no` `tr_tr`

`es_es`/`es_mx` and `pt_br`/`pt_pt` are genuine regional variants, not copies.

Run the validator after editing any of them:

```bash
python tools/check_lang.py
```

It checks that every locale has exactly the same 59 keys as `en_us`, and that all
`%s` / `%%` placeholders, literal `\n` line breaks and the `◈` marker are preserved —
missing placeholders would otherwise crash the client at runtime.

Adding a locale: copy `en_us.json` and follow
[docs/translation-spec.md](docs/translation-spec.md) for the terminology table and rules.

## Building from source

```bash
./gradlew build               # compile and package -> build/libs/
./gradlew runClient           # launch a dev client
./gradlew runServer           # launch a dev server
./gradlew runData             # regenerate blockstates and item models
./gradlew runGameTestServer   # run the automated server-side tests
```

Build output: `build/libs/echoes-of-oblivion-1.0.0-mc1.20.6-forge.jar`

### Regenerating assets

Both the audio and the textures are generated from code, so they are reproducible and
carry no third-party licensing risk:

```bash
# Sounds: 8 synthesized wav files -> convert to ogg (Minecraft only reads .ogg)
javac -d run/downloads/synth-classes tools/SoundSynth.java
java -cp run/downloads/synth-classes SoundSynth <output-dir>
ffmpeg -y -i in.wav -c:a libvorbis -q:a 4 -ar 44100 -ac 1 out.ogg

# Textures: 9 pixel-art PNGs, pure Python standard library, no Pillow needed
python tools/make_textures.py
```

## Project documentation

| File | Contents |
| --- | --- |
| [ASSETS.md](ASSETS.md) | Asset inventory, regeneration steps, current status of every texture and sound |
| [CREDITS.md](CREDITS.md) | Per-file provenance and licensing for all third-party assets |
| [achieve.md](achieve.md) | The 8-phase implementation roadmap this mod was built against |
| [tech-outline.md](tech-outline.md) | Technical architecture: packages, networking, persistence, rendering |
| [tools/README.md](tools/README.md) | Developer tooling and the verification commands |

## Technical notes worth knowing

**Forge 1.20.6 removed the entire GUI overlay system.** `IGuiOverlay`, `ForgeGui`,
`RegisterGuiOverlaysEvent` and `RenderGuiEvent` are absent from the Forge 50.2.10 jar
(verified class-by-class). This mod therefore implements full-screen fades with
`RenderLevelStageEvent` + `RenderSystem`, and corruption pressure with `ViewportEvent` fog —
no custom shaders, which also keeps it usable on older GPUs.

**Entities are drawn as geometry, not models.** `client/ShapeRenderer.java` emits coloured
boxes directly through a `VertexConsumer`, so the Phantom and the Silent Aggregate are visible
without any model files or textures. Upgrading them to proper `MobRenderer` + `LayerDefinition`
models is a self-contained change when art becomes available.

**GameTest has an environment limit.** In Forge 50.2.10's test server, *any moving
`LivingEntity` throws on tick*, because `ForgeHooks.isLivingOnLadder` reads
`ForgeConfig.SERVER` which the test server never loads. This is unrelated to the mod; the
automated tests avoid `entity.tick()` accordingly. See `tools/README.md`.

## License

**MIT** — see [LICENSE](LICENSE).

Third-party assets bundled with this mod are **CC0 1.0 Universal** (public domain, no rights
reserved) and are documented per file in [CREDITS.md](CREDITS.md).

Minecraft is a trademark of Mojang Studios. This is an unaffiliated third-party mod.
