# Changelog

All notable changes to Elusive Flora. The format follows Keep a Changelog; versions follow Semantic Versioning.

## [1.0.0] - 2026-09-24

First release.

### Added

- Thirty-two plants across the Overworld, Nether, End, and Aether, each with its own biomes and its own kind of spot: ground of a given kind, cliff faces, tree trunks, sea and river beds, lava shores, swamp water edges, desert and mesa sand, spooky woods, mountain tops.
- Conditions: some plants open only at night, in rain, at a full moon, at a new moon, or in a Serene Seasons season; closed plants show a dormant look.
- Picking: hitting a plant drops one of itself and leaves a stem; the stem regrows after the base regrow time times the plant's factor; breaking the stem removes the spot.
- Two 3D showcase plants, Ledgebloom on cliff faces and Pearlfrond on the deep sea bed; the others are flat crosses with their own textures.
- Glow on the plants that carry light.
- The journal, Wanderer's Herbarium, crafted from a book and a dandelion: one entry per plant with a hint, its condition, its regrow time, and the biomes of the running game. Season entries appear only with Serene Seasons; the Aether category only with the Aether.
- Config: `regrowBaseMinutes`, `enableCheckCommand`, `debugLog`; per plant `enabled`, `chunkChancePercent`, `regrowFactor`, `biomeTypes`, `biomeNames`.
- The operator check command `/elusiveflora here`, off by default.

### Compatibility

- Requires Patchouli 1.0-28 or newer (ROFL edition) on client and server.
- Optional Serene Seasons 1.2.18 (tested) through its API; a mismatched or failing season API turns the season feature off with one warning instead of a crash.
- Optional Aether Legacy 1.5.4.1 (tested): Cloudfern grows in the Aether.
- Seven End and Nether plants name biomes from Better End Forge, Stygian End, BetterNether, Nether Backport, and Biomes O' Plenty; they never generate without those mods.
- Fluidlogged API 3.3.3: water plants place, pick, and leave water as without it (tested).
- Dedicated servers: the mod is needed on both sides; tested on a Forge 14.23.5.2847 server.

### Known Issues

- A plant whose spot rule needs water or lava beside it (Mournlily, Emberroot) disappears at its next random tick after that water or lava is removed, not at once.
