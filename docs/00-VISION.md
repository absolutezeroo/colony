# 00 — Vision

## Why Colony exists

Two colony mods dominate the Minecraft ecosystem:

- **MineColonies** is mature, content-rich, and locked into schematic-driven construction. Players cannot architect freely; they pick a style pack and let a Builder citizen reconstruct prefab huts. The schematic model exists for historical reasons (legacy codebase from 2013, fragile pathfinding, Patreon ecosystem of style packs) rather than design preference.
- **Hearthbound** explores free-build but ships nothing publicly, suffers from mono-module architecture leaking `api → common`, and over-documents to the point of yak-shaving (55 internal MVPs, zero public alpha).

Colony is positioned in the gap: **free-build that ships, with serious modular architecture, and economic depth neither competitor offers**.

## What "free-build" means here

The player builds structures using vanilla Minecraft blocks, with no constraints from the mod. Once built, the player **designates** the structure as functional by:

1. Placing a Hut block (Town Hall, Residence Hut, Farmer Hut, etc.) inside the structure.
2. Painting an outer zone (rectangular or freeform footprint) that includes the Hut block.
3. Painting room sub-zones inside the outer zone.
4. Typing nearby chests by right-clicking them with the Colony Tool.
5. Linking outdoor anchors (scarecrows, quarry pillars) by right-clicking with an item that defines their function.

No schematic. No builder reconstruction. No "level 1 → level 5 prefab upgrade." Quality emerges from what the player actually built, scored by the mod.

## Design pillars (one-line each)

1. **Server-authoritative.** Client renders snapshots, never owns truth. Dedicated server is a first-class target from MVP-1.
2. **Zero hardcode.** Every content type is a registered identifier with a Codec. Tags for compatibility. JSON for tuning.
3. **Modular by compiler, not by discipline.** Multi-module Gradle from commit zero. `:api` extractable as a Maven artifact for addons.
4. **Ship early, ship ugly.** Public alpha at MVP-15 maximum. Feedback loop from real players beats internal polish.
5. **Free-build first, schematic optional.** The default experience is "build it yourself." Schematic import via vanilla `.nbt` is an optional convenience, not the gameplay loop.
6. **Custom citizens with personality.** Not vanilla villagers. Traits, skills, peer relationships, mood — depth that justifies playing past the first hour.

## Non-goals (explicit)

We will **not** do the following, even if players ask:

- **Multi-loader support.** NeoForge 1.21.1 only. Architectury, Fabric, multi-version porting are explicit non-goals. If the project survives V2, a Fabric port may be considered as a separate fork.
- **Pre-built schematics.** No style packs, no prefab buildings shipped with the mod. Players who want prefabs can import vanilla `.nbt` files (V2 feature).
- **Vanilla villager integration.** Citizens are a custom entity. Vanilla villagers exist in parallel, untouched.
- **Combat/military system in V1.** No raids, no soldiers, no defense huts. V2 territory.
- **Diplomatic/inter-colony systems in V1.** Single-colony focus. Trade, war, alliances are V2+ territory.
- **3D freeform interior layouts.** Rooms are 2D footprint + uniform height. No overhangs, no balconies counted as room interior. Workable for 95% of buildings.
- **Auto-detection of rooms.** Player paints rooms manually. Auto-detection is fragile, opaque, and frustrates more than it helps (lesson from RimWorld discussions on Reddit).

## Differentiators vs MineColonies and Hearthbound

| Feature | MineColonies | Hearthbound | Colony |
|---|---|---|---|
| **Free-build** | No (schematics) | Yes (AABB only) | **Yes (AABB + freeform footprint)** |
| **Published** | Yes (10M+ DLs) | No | **Yes (alpha target month 4)** |
| **Multi-module Gradle** | No | No | **Yes** |
| **Public `:api` artifact** | No | No | **Yes (Maven)** |
| **Custom pathfinding** | Yes (buggy) | Yes (in progress) | **Yes (designed clean)** |
| **Reputation depth** | Happiness bar | Complaints | **4 axes (V2)** |
| **Economy** | Token gold | None | **Multi-treasury + 6 tax types (V2)** |
| **Anchor right-click** | Scarecrow only | GUI-only | **Generalized to all anchor types** |
| **License** | Custom restrictive | TBD | **LGPL-3.0** |

## Target audience

- **Primary:** Players who love MineColonies' colony fantasy but resent schematic constraints. They want to build their own castle/village/town and have it function.
- **Secondary:** Modpack authors who want a colony mod that integrates cleanly with Create, Farmer's Delight, Mekanism, etc. without conflicting on chests/storage.
- **Tertiary:** Mod developers who want to build addons (custom huts, jobs, anchors) without forking the core.

We are **not** targeting players who want a 5-minute setup experience. The free-build approach demands creative investment.

## Success criteria

This project succeeds if, by the end of V1 (12-15 months):

- 1000+ unique downloads on Modrinth/CurseForge.
- At least 5 modpack inclusions.
- At least 1 community-developed addon using `:api`.
- A working core loop: place Town Hall → 4 citizens spawn → build residence → assign rooms → build farmer hut → place scarecrow → citizens eat, sleep, farm autonomously.

If V1 ships and gets traction, V2 adds the differentiators (reputation, economy, taxes, marketplaces). If V1 ships and gets ignored, post-mortem and pivot.

## Project status as of this document

- 0 lines of code written.
- 11 design documents being authored (this file is one of them).
- Repository public from day one at `github.com/absolutezeroo/colony` (TBD URL).
- Discord, wiki, and community channels deferred until first alpha release.
