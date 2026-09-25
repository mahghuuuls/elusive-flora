# Elusive Flora

A Minecraft 1.12.2 Forge mod that adds rare plants which grow only in specific biomes and places: ocean floors, cliff faces, tree trunks, deserts, swamps, snow, the Nether, the End, and the Aether. Some open only at night, in rain, at a full or new moon, or in a season. A journal, the Wanderer's Herbarium, says where and when each plant grows. Plants regrow where they were found and cannot be moved or farmed.

Player-facing description: [MOD-PAGE.md](MOD-PAGE.md). Release history: [CHANGELOG.md](CHANGELOG.md). License: [MIT](LICENSE). Build template notice: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Install side

- Mod id `elusiveflora`, version 1.0.0. Needed on the server and on every client.
- Requires [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli) 1.0-28 or newer (the ROFL edition) on both sides, for the journal.
- Optional: [Serene Seasons](https://www.curseforge.com/minecraft/mc-mods/serene-seasons). With it, four plants follow the seasons. Without it, they are always open.
- Optional: the Aether. One plant grows there when it is installed.
- Plants are placed when a chunk first generates. Existing chunks get none.

## Plant roster

`src/main/resources/assets/elusiveflora/plants.csv` is the single source of truth for every plant: id, dimension, biome types or biome names, ground rule, condition, chunk chance, regrow factor, glow, model kind. Blocks, items, config defaults, and journal biome lists all derive from it. The same file is kept in the design documentation of the workflow that produced this mod.

The biome rule of a plant is satisfied by any listed Forge biome dictionary type or any listed biome registry name. Eight End and Nether plants name biomes from other mods only (Better End Forge, Stygian End, Environs, BetterNether through Nether API, Nether Backport, Biomes O' Plenty), and Cloudfern needs the Aether. In a game without those mods they register but never generate, and the journal says so on their pages.

## Configuration

`config/elusiveflora.cfg`, written with comments on first start. All options need a game restart.

| Key | Scope | Default | Meaning |
| --- | --- | --- | --- |
| `regrowBaseMinutes` | general | 180 | Base time in real minutes for a picked plant to grow back (1 minute = 1200 ticks) |
| `enableCheckCommand` | general | false | Registers the operator command `/elusiveflora here`, which lists for every plant whether it can appear where you stand and the first rule that fails |
| `debugLog` | general | false | Logs one line per plant placed by world generation |
| `enabled` | per plant | true | `false` removes the plant from world generation; existing plants stay |
| `chunkChancePercent` | per plant | 1 or 3 | Percent chance, rolled once per new chunk, that the mod tries to place one plant of that kind |
| `regrowFactor` | per plant | 0.5, 1.0, or 2.0 | Multiplier of `regrowBaseMinutes` for that plant |
| `biomeTypes` | per plant | from the roster | Forge biome dictionary types where the plant may appear |
| `biomeNames` | per plant | from the roster | Biome registry names where the plant may appear; a value here replaces the roster list |

A bad value is clamped and logged once; the file is left as the pack maker wrote it.

## Scope boundaries

- No recipes and no uses for the picked items. Other mods give them a purpose.
- No farming: a plant cannot be picked up as a block, planted, or grown with bone meal. Breaking a stem removes that spot for good.
- Regrowth does not fire `CropGrowEvent`.
- Serene Seasons is used through its API and never bundled.
- Cleanroom is not tested by this project.

## Development

Standard Forge 1.12.2 project on the CleanroomMC ForgeDevEnv template: `./gradlew build` produces `build/libs/elusiveflora-1.0.0.jar`; `./gradlew runClient` and `./gradlew runServer` start the development runtimes. Unit tests run without Minecraft and cover the roster grammar, biome and ground rule matching, time arithmetic, config clamping, resources, and the journal text.

## License

MIT, see [LICENSE](LICENSE). The build scripts and project skeleton come from CleanroomMC ForgeDevEnv under MIT, see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
