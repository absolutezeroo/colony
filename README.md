# Colony

> A Minecraft NeoForge 1.21.1 mod for freeform player-built colonies. Custom citizens live, work, eat, sleep, and complain inside structures **you build yourself** — no schematics imposed, no level locks, no bullshit.

**Status:** pre-alpha. Not yet playable.
**Loader:** NeoForge 1.21.1 (no Fabric, no other versions planned).
**Java:** 21.
**License:** LGPL-3.0-or-later (see `LICENSE`).
**Namespace:** `com.akikazu.colony`.

## Vision in one sentence

A colony simulation where every building you place has a reason to exist, every citizen has a personality, and every economic decision has consequences — without forcing you to copy someone else's schematic.

## What makes Colony different

| Axis | MineColonies | Hearthbound | **Colony** |
|---|---|---|---|
| Building model | Schematic-locked, levels 1-5 | Hut-marker + AABB zone | **Hut-marker + freeform footprint (L/T/U shapes)** |
| Room shapes | Rectangle only | Rectangle only | **Freeform 2D footprint + uniform height** |
| Anchor configuration | Right-click scarecrow with seed | GUI-based | **Right-click pattern generalized to all anchor types** |
| Reputation | Single happiness bar | Complaints system | **Four-axis reputation (V2)** |
| Economy | Token gold for requests | Not implemented | **Full economy: treasuries, 6 tax types, evasion (V2)** |
| Module architecture | Mono-repo, package separation | Mono-module, package separation | **Multi-module Gradle, compile-time enforced** |
| API for addons | Limited, internal-leaking | Not published | **`:api` published separately, ServiceLoader SPI** |

## Documents

Read in this order if you're new:

1. [00 — Vision](docs/00-VISION.md) — what this project is, what it isn't, why it exists
2. [01 — Architecture](docs/01-ARCHITECTURE.md) — Gradle multi-module, layers, registries, persistence, networking
3. [02 — Design Pillars](docs/02-DESIGN-PILLARS.md) — non-negotiable design rules and explicit non-goals
4. [03 — Code Style](docs/03-CODE-STYLE.md) — Allman, blank lines, no inline comments, naming conventions
5. [04 — Building System](docs/04-BUILDING-SYSTEM.md) — huts, zones, rooms, tiers, anchors, storage typing
6. [05 — Citizen System](docs/05-CITIZEN-SYSTEM.md) — custom entity, traits, jobs, schedules, pathfinding
7. [06 — Data-Driven Content](docs/06-DATA-DRIVEN.md) — JSON schemas, codecs, tag conventions
8. [07 — Networking & Server Authority](docs/07-NETWORKING.md) — payloads, sync, server-authoritative rules
9. [08 — Roadmap & MVP Scope](docs/08-ROADMAP.md) — V1 scope, V2 deferred items, release tempo
10. [09 — Glossary](docs/09-GLOSSARY.md) — terms, naming rules, ID conventions
11. [10 — Tech Debt Register](docs/10-TECH-DEBT.md) — known limitations, deferred work, accepted constraints
12. [11 — Economy V1 + V2](docs/11-ECONOMY-V1-V2.md) — V1 minimal item-based, V2 frozen design (treasuries, taxes)
13. [12 — UX & GUI](docs/12-UX-GUI.md) — vanilla `AbstractContainerScreen`, layout grid, tabs, lists, color palette
14. [13 — Testing](docs/13-TESTING.md) — JUnit + GameTest + JMH layers, CI gates, manual checklist
15. [14 — Datagen](docs/14-DATAGEN.md) — tag providers, profile providers, lang provider, recipe provider
16. [15 — API: Reputation](docs/15-API-REPUTATION.md) — V2 frozen API for 4-axis reputation system
17. [16 — API: Room Adjacency](docs/16-API-ROOM-ADJACENCY.md) — V2 frozen API for adjacency bonuses/penalties
18. [17 — API: Schematic Import](docs/17-API-SCHEMATIC-IMPORT.md) — V2 frozen API for optional .nbt import
19. [18 — Save Versioning](docs/18-SAVE-VERSIONING.md) — migration chain, backward compatibility, corruption recovery
20. [19 — Release & Communication](docs/19-RELEASE-COMMUNICATION.md) — versioning, release channels, communication policy

Operational documents at the repo root:

- [`CLAUDE.md`](CLAUDE.md) — AI agent briefing for Claude Code sessions
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — contribution guide for PRs and issues
- [`LICENSE`](LICENSE) — LGPL-3.0-or-later full text

## Non-negotiable rules (short version)

- Server-authoritative everything. Client requests, server validates.
- Custom citizens, not vanilla villagers.
- Custom pathfinding (tested vanilla, insufficient for colony semantics).
- Multi-module Gradle from commit zero. `:api` must be compileable against vanilla MC without `:common` or `:neoforge`.
- Zero hardcode for content. Registry + Codec dispatch + JSON datapack.
- LGPL-3.0 license. Contributors welcome.
- Public repo from day one.

## License

LGPL-3.0-or-later. You can:
- Use this mod in any modpack, commercial or free.
- Develop addons against `:api` without your addon being LGPL-contaminated (dynamic linking exception).

You cannot:
- Fork and close the source.
- Distribute modified `Colony` without source.

See `LICENSE` for the full text.

## How to contribute

Read [03 — Code Style](docs/03-CODE-STYLE.md) and [01 — Architecture](docs/01-ARCHITECTURE.md) first. Open an issue before starting non-trivial work. PRs without prior discussion may be closed.

---

**Maintainer:** absolutezeroo (akikazu)
**Repo:** public, contributors welcome
**Discord:** TBD (after first alpha release)
