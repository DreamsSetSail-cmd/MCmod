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

**A history you have to dig up** — The civilization's chronicle is broken into **12 lore fragments**
scattered through the ruins. They are items: carry them, collect them, re-read them. **The order is
not yours to choose**, so you assemble the cause and effect yourself rather than being told.
Four voices, deliberately distinct: *chronicle* (cold official record), *record* (lab notes),
*letter* (the only place "I" appears), *prayer* (addressed to no one).
**Fragments have no crafting recipe** — they can only be found.

**Psychological horror, no jump scares** — Corruption climbs as you learn. It thickens the fog
around you, spawns phantom glimpses in the corner of your eye, and layers whispers and a heartbeat
under the ambient audio. The higher it gets, the harder it is to see where you are going.

**Reality collapse** — Once you return to the Overworld, **Mirror Chunks** begin to spread from
wherever you stand. Inside them gravity flickers, particles run backwards, and phantoms take form
and hunt the living.

**Rites you perform yourself** — The **Resonance Fork** performs one of three rites, each consuming
real materials. *Requiem* scatters every echo within 32 blocks. **Descent** silences the world
within 48 blocks for 30 seconds — the first time you get to be the one who makes the silence,
instead of the one it happens to. *Void Passage* crosses between worlds without a portal.

**Burning your own memory** — Sneak and press the memory-attack key to **permanently burn a memory
you have witnessed** in exchange for an immediate effect. It is gone from your progress, struck
through in the codex, and it does not come back. You are spending what you know in order to keep
living.

**A conceptual boss** — The **Silent Aggregate** is immune to every weapon in the game. It cannot
be killed, only *unsettled*: each memory you have witnessed can be used exactly once to damage its
stability. Resonant memories cut twice as deep. Witnessing all five memories, standing inside an
infected chunk, and holding the Eye of Silence are the three conditions for summoning it.

The **Memory Blade** and **Shatter Staff** are deliberately effective against echoes and
**completely useless against the Aggregate**. You will suspect your damage is too low, then your
enchantments, before you understand that there is nothing there to cut.

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
| Read a lore fragment | Right-click it |
| Open the codex | Right-click the **Memory Scroll** |
| Find a crystal | Right-click the **Ruins Compass** |
| Sample a crystal | Right-click it with the **Excavation Shovel** (the crystal stays readable) |
| Open a sealed fragment | Hold the **Excavation Shovel** and right-click |
| Memory attack (boss) | `R` — the selected hotbar slot picks which memory you use |
| **Burn a memory** | **Sneak + `R`** — permanent, and it does not grow back |
| Perform a rite | Right-click with the **Resonance Fork**; sneak-right-click to switch rite |
| Summon the boss | Hold the **Eye of Silence**, stand inside an infected chunk, and have all 5 memories witnessed |

## The 42 items

Every item does something. There are no placeholders.

| Group | Items |
| --- | --- |
| **Archaeology** | Ruins Compass, Memory Scroll, Excavation Shovel, 12 lore fragments |
| **Rites** | Corridor Key, Eye of Silence, Resonance Fork, Ritual Alloy, Silence Shard |
| **Consumables** | Purification Agent, Stabilizer, Memory Vial, Corruption Essence, Echo Whisper |
| **Materials** | Shard of Truth, Void Embers, Crystal Dust, Resonant Alloy, Corrupted Fragment, Mirror Shard, Membrane, Archivist's Ink, Ossuary Ash, Thread of the Choir, Blank Plaque, Sealed Fragment |
| **Tools** | Memory Blade, Shatter Staff, Researcher's Lantern |

Three of them are worth knowing about before you craft them:

- **Memory Vial** relieves 25 corruption, and pays for it by burning **the memory you witnessed
  most recently** — you do not get to choose which. It pairs with burning as its mirror image:
  burning is a cost you pick, the vial is a cost that picks you.
- **Researcher's Lantern** halves corruption growth while carried. It is deliberately weak — it
  doubles how long you have, and changes nothing about where you are going.
- **Crystal Dust** is produced by grinding a memory crystal down. To build the tools, you first
  destroy something readable. That cost is the point.

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

The mod ships with **21 locales** (**201 keys** each):

`en_us` `zh_cn` `zh_tw` `ja_jp` `ko_kr` `ru_ru` `uk_ua` `de_de` `fr_fr` `es_es` `es_mx`
`it_it` `pt_br` `pt_pt` `nl_nl` `pl_pl` `cs_cz` `sv_se` `da_dk` `nb_no` `tr_tr`

`es_es`/`es_mx` and `pt_br`/`pt_pt` are genuine regional variants, not copies.

Run the validator after editing any of them:

```bash
python tools/check_lang.py
```

It checks that every locale has exactly the same **201 keys** as `en_us`, and that all
`%s` / `%%` placeholders, literal `\n` line breaks and the `◈` marker are preserved —
missing placeholders would otherwise crash the client at runtime.

`tools/check_lang_format.py` is the companion check for the format layer: BOM, mixed CRLF/LF
line endings, missing trailing newline, and more than one key on a line. It matters here because
this repo genuinely contains both CRLF and LF locale files — each file is internally consistent,
but rewriting a JSON file with a blanket `json.dump` will silently convert its line endings.
**Edit locale files textually, or restore the original line ending afterwards.**

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

Build output: `build/libs/echoes-of-oblivion-2.0.0-mc1.20.6-forge.jar`

Note that `src/generated/resources` is a resource source directory **and** the datagen output
target. Seven item models live there rather than in `src/main/resources`; duplicating one of them
by hand makes `processResources` fail with `duplicate but no duplicate handling strategy`.

### Regenerating assets

Both the audio and the textures are generated from code, so they are reproducible and
carry no third-party licensing risk:

```bash
# Sounds: 11 synthesized wav files -> convert to ogg (Minecraft only reads .ogg)
javac -d run/downloads/synth-classes tools/SoundSynth.java
java -cp run/downloads/synth-classes SoundSynth <output-dir>
ffmpeg -y -i in.wav -c:a libvorbis -q:a 4 -ar 44100 -ac 1 out.ogg

# Textures: 44 item / block textures, pure Python standard library, no Pillow needed
python tools/make_textures.py       # v1.x base set
python tools/make_v2_textures.py    # v2.0.0 set
```

## Project documentation

| File | Contents |
| --- | --- |
| [STORY.md](STORY.md) | The story, in the order you are meant to discover it (spoilers) |
| [ASSETS.md](ASSETS.md) | Asset inventory, regeneration steps, current status of every texture and sound |
| [CREDITS.md](CREDITS.md) | Per-file provenance and licensing for all third-party assets |
| [CHANGELOG.md](CHANGELOG.md) | Release notes |
| [PUBLISHING.md](PUBLISHING.md) | How releases are built and published |
| [docs/story-bible.md](docs/story-bible.md) | Full narrative design: chronology, characters, planned in-game expansion |
| [docs/story-expansion.md](docs/story-expansion.md) | v2.0.0 narrative expansion: the 12 lore fragments in full, and the completed timeline |
| [docs/technical-guide.md](docs/technical-guide.md) | Technical handbook: architecture, every system, pitfalls, modification guide |
| [docs/ideas-backlog.md](docs/ideas-backlog.md) | Ideas that were considered and not built, with the reasoning |
| [docs/translation-spec.md](docs/translation-spec.md) | Translation rules and terminology table |
| [achieve.md](achieve.md) | The 8-phase implementation roadmap this mod was built against |
| [tech-outline.md](tech-outline.md) | Original technical outline |
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
