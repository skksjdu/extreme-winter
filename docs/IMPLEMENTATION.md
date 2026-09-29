# Extreme Winter — implementation notes

Target: Minecraft Java Edition **1.21.6** only. User clarified that this is a
singleplayer mod; release acceptance uses an integrated server, not dedicated
server/multiplayer testing.

## Verified dependencies (2026-09-29)

| Component | Pin | Evidence |
| --- | --- | --- |
| Minecraft | 1.21.6 | User requirement |
| Game / bytecode | Java 21 | Fabric setup documentation; local Temurin 21 |
| Fabric Loader | 0.19.5 | `meta.fabricmc.net/v2/versions/loader/1.21.6`, stable |
| Fabric API | 0.128.2+1.21.6 | Official Maven metadata, latest exact 1.21.6 suffix |
| Yarn | 1.21.6+build.1 | Official Meta API for 1.21.6 (Yarn stable flag is false) |
| Loom | 1.18.2 | Official Maven stable release; remap plugin supports obfuscated MC |
| Gradle | 9.8.0 | Official current stable endpoint; Loom requires >=9.7 |

Loom 1.18.2 runs on Java 25+; Gradle uses the already installed Java 26 on this
machine. Minecraft, compilation, and tests use a separate Java 21 toolchain.
Neither system PATH nor system JAVA_HOME is changed. Wrapper and distribution
SHA-256 values are pinned and verified.

Sources:
- https://fabricmc.net/2025/06/15/1216.html
- https://docs.fabricmc.net/develop/loom/
- https://meta.fabricmc.net/v2/versions/loader/1.21.6
- https://meta.fabricmc.net/v2/versions/yarn/1.21.6
- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml
- https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.18.2/fabric-loom-1.18.2.module
- https://services.gradle.org/versions/current

## Architectural decisions

- Keep common gameplay and client code in separate Loom source sets.
- Use Fabric persistent data attachments for per-player temperature; no player
  NBT mixin. Death resets temperature; logging out does not.
- Use server tick events with bounded scheduled work. No client gameplay authority.
- Use normal status effects with short lifetimes. Never remove another mod's
  effects when temperature recovers.
- Use vanilla blocks, placement checks, weather and structure templates.
- Use `HudElementRegistry`, the API introduced for 1.21.6. No renderer replacement.
- Use Fabric's public biome modification API to set vanilla Overworld biomes to
  a cold, precipitation-enabled climate. This changes data, not biome classes or
  renderer code, and lets vanilla and shader packs render normal snow. Modded
  biomes retain their original climate. `coldVanillaBiomes=false` disables this
  change for climate/world-generation modpacks; warm biomes may then show rain.
  Vanilla light-based snow/ice melting is retained.

## Development log

Each phase must compile before the next phase starts. Runtime assertions and
screenshots are produced by a separate Fabric client test mod, excluded from the
release JAR. Test outcomes are recorded in `TESTING.md` after execution.

| Phase | Implementation and relevant API | Validation performed |
| --- | --- | --- |
| 1 | Split Loom main/client source sets; exact 1.21.6 dependencies | Empty project built, real client started |
| 2 | `AttachmentRegistry` + persistent `Codec.DOUBLE`; default on death | Built; real singleplayer save/rejoin preserved value |
| 3 | `ServerTickEvents.END_SERVER_TICK`, heightmap cover, weather/time/water inputs | Built; exploration timing, cover, bounds and stage unit tests |
| 4 | Finite sphere of precomputed offsets, loaded-chunk checks, vanilla collision raycast | Built; real lit/unlit campfire, obstruction and recovery assertions |
| 5 | Vanilla status effects and `DamageSources.freeze()` | Built; actual effect application/expiry and periodic health loss |
| 6 | Typed S2C `CustomPayload`, `ServerPlayNetworking`, `HudElementRegistry` | Built; owner HUD sync, reconnect, disconnect, rendered screenshots |
| 7 | Fabric biome data modification, world tick events, vanilla snow layer placement | Built; layer cap, crop/block-entity/roof protection and unloaded-column assertions |
| 8 | Loaded surface sampling, source-water checks, vanilla ice | Built; outdoor freezing and glass-roof protection assertions |
| 9 | Vanilla `StructureTemplateManager`, NBT blueprint, `PersistentStateType` + codec | Built; new-world generation, first arrival, heat/farm/chest, persistent marker and no loot refill |
| 10 | Gson already supplied by Minecraft; readable JSON, validated defaults | Built; four config tests including preserving malformed user files |
| 11 | Independent test mod, then Loom `ClientProductionRunTask` | Actual remapped release JAR tested in A–E singleplayer matrix |

Configuration defaults were introduced alongside the temperature model so systems
share one set of values; JSON loading/validation was added in phase 10.

## Work bounds and compatibility tradeoffs

- Temperature work is staggered by entity ID, once per 20 ticks per active player.
  A radius-4 heat sphere contains 257 offsets; maximum allowed radius 6 contains
  925. Offsets are constructed once. Only actual heat-source candidates need rays.
- Environmental sampling has one shared per-world budget, not a full scan per
  player. Defaults are 16 columns each 20 ticks; freezing shares the sampled
  columns every 40 ticks. Sampling never loads a missing chunk.
- Shelter generation is startup-only. At most two bounded searches of 121
  footprints use already loaded chunks. Blocks with block entities are rejected;
  only air, snow and ordinary grass/ferns can be cleared above the chosen surface.
- A small corner foundation and entrance stairs adapt the NBT template to ground
  height; room geometry and loot are in the editable JSON/NBT, not Java placements.
- `POST_PROCESSING` is Fabric's documented phase for changing biome properties.
  Climate/weather settings are explicit switches because another climate mod may
  wish to own those same properties. No rendering classes are replaced.
- Heat line-of-sight uses collision shapes, so windows and walls obstruct it, while
  gaps or an open doorway can admit heat. There is no room-volume simulation.
- Snow and ice obey ordinary block light and vanilla melting. Natural vanilla
  weather ticks remain active in addition to this mod's small sampling budget.
- Effects refresh for 40 ticks and expire naturally, preserving stronger/longer
  vanilla or mod effects. Client networking has no C2S temperature channel.

## Test-launch correction

The initial development-mode D run failed before loading a world because Loom's
local mod remapping strips nested JARs from Iris, and the file dependency had no
Maven metadata to restore its `jcpp` library. The test stack reported
`NoClassDefFoundError: org/anarres/cpp/PreprocessorListener`.

The acceptance matrix therefore uses **production-mode** launches with the intact
published optional mod JARs and the actual `remapJar` output. This also tests the
deliverable's Minecraft name remapping. The test mod is remapped into
`build/testmods`, and it is never included in `build/libs` or the distributed mod.
No Iris internals or mod gameplay were changed to work around the test launcher.
