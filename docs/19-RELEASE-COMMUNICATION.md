# 19 — Release & Communication Strategy

This document covers how Colony releases are versioned, published, announced, and supported. The strategy is designed for a solo maintainer with assistance from AI tooling.

## Versioning

Mod jar versions follow Semantic Versioning (semver) `MAJOR.MINOR.PATCH`:

- **PATCH** (1.0.1 → 1.0.2): bugfixes only. No new features. No save format changes. Players upgrade transparently.
- **MINOR** (1.0.x → 1.1.0): new features, content additions, performance improvements. Save format additive-only. Players upgrade with confidence.
- **MAJOR** (1.x.y → 2.0.0): breaking changes (save format restructuring, removed features, API breaks). Player communication required.

The `:api` artifact has its **own** semver, independent of the mod jar. See `01-ARCHITECTURE.md` and `docs 15-17` for the API versioning contract.

## Pre-release tags

Before V1.0 stable, releases use pre-release suffixes:

- `0.1.0-alpha.1`, `0.1.0-alpha.2`, ... — alphas, expected to break
- `0.5.0-beta.1`, `0.5.0-beta.2`, ... — betas, save-stable but feature-incomplete
- `0.9.0-rc.1`, `0.9.0-rc.2`, ... — release candidates, considered V1.0 unless blocking bugs found
- `1.0.0` — V1.0 stable

The pre-V1 jump from 0.x to 1.0 is the public commitment that the mod is feature-stable and the save format is locked.

## Release cadence

Targets, not commitments:

| Phase | Cadence |
|---|---|
| Alpha (months 4-9) | Monthly releases, expected breakage |
| Beta (months 9-12) | Bi-weekly releases, save-stable, polish-focused |
| RC (months 12-14) | Weekly releases, bug-bash period |
| Post-V1.0 | Monthly minor releases, weekly patches as needed |

If the maintainer is unavailable (life, work, illness), releases pause. Communicate the pause on Discord and in the latest release notes. Don't release rushed code to maintain a cadence.

## Release channels

Colony is published on:

1. **Modrinth** — primary channel. Best metadata, modpack-friendly, no ads.
2. **CurseForge** — secondary channel. Required for some modpack platforms (FTB, ATLauncher).
3. **GitHub Releases** — source of truth. Tagged releases with `colony-{version}.jar` and `colony-api-{version}.jar` attached.

The same jar is uploaded to all three channels. No platform-specific builds.

The maintainer Maven repository (GitHub Packages or self-hosted) publishes `colony-api` for addon developers.

## Release checklist

Before tagging a release:

1. Run `./gradlew check` locally and in CI. All tests pass.
2. Run `./gradlew :neoforge:runGameTestServer`. All GameTests pass.
3. Manual smoke test: load a save from the previous version, verify no errors. Save and reload, verify no errors.
4. Update `CHANGELOG.md` with the release notes (see format below).
5. Update version in `gradle.properties`: `mod_version=X.Y.Z`.
6. Commit and tag: `git tag -a v{X.Y.Z} -m "Release X.Y.Z"`, `git push origin v{X.Y.Z}`.
7. CI publishes automatically (see `13-TESTING.md` workflow).
8. After CI succeeds, manually edit the GitHub Release description with player-facing release notes (Modrinth/CurseForge get the same notes via the publishing action).
9. Announce on Discord and Reddit (see below).

## Changelog format

`CHANGELOG.md` follows Keep a Changelog style. Each release section has:

```markdown
## [1.2.0] - 2026-08-15

### Added
- New `Lumberjack` job. See `docs/05-CITIZEN-SYSTEM.md` for behavior details.
- Decoration adjacency bonus when bedrooms are adjacent to libraries.

### Changed
- Citizen pathfinding cache is now invalidated incrementally instead of fully on block change. ~30% performance improvement at 50+ citizens.

### Deprecated
- `ColonyApi.getOldMethod()` is marked deprecated; will be removed in V2.0. Migrate to `getNewMethod()`.

### Removed
- Nothing.

### Fixed
- Citizens no longer stall when their assigned bedroom is destroyed mid-tick.
- Storage chest typing now correctly persists when a chunk unloads and reloads.

### Security
- Nothing.

### Save format
- No changes. V1.0+ saves load seamlessly.
```

Categories: Added / Changed / Deprecated / Removed / Fixed / Security / Save format. Omit categories with no content for that release.

Each entry is one sentence, present tense, player-facing language. Don't write "refactored the FooService class"; write "improved colony performance at large citizen counts."

## Communication channels

### Discord

The official Colony Discord is the **primary community hub** post-alpha. Setup:

- Channels: `#announcements`, `#general`, `#bug-reports`, `#feature-requests`, `#screenshots`, `#modpack-discussion`, `#dev-talk`, `#addon-development`.
- Roles: `@Maintainer` (you), `@Contributor` (PR merged), `@Tester` (active in beta), `@Modpack-Author` (verified), `@Addon-Developer` (verified).
- Bot: a basic announcement bot that pings `@everyone` on release tags.

The Discord is created at the start of beta (around month 9), not at project start. Empty Discords are sadder than no Discord.

### Reddit

Post release announcements on:

- `r/feedthebeast` for major releases (V1.0, V2.0). Don't spam minor patches.
- `r/Minecraft` for V1.0 only (one-shot announcement).
- `r/Minecolonies` is **not** appropriate — we are a competing mod and posting there is bad form unless explicitly invited.
- `r/NeoForge` for technical updates (multi-module architecture, API releases).

Format: short title (`Colony 1.0 Released — Free-build colony mod for NeoForge 1.21.1`), descriptive body with screenshots, link to Modrinth and GitHub.

### Blog / website

No dedicated website at V1. The GitHub repo README is the de-facto website.

Post-V1.0, a simple GitHub Pages site can be added with:

- Landing page summarizing the mod.
- Link to Modrinth/CurseForge downloads.
- Link to the Discord.
- API documentation (auto-generated JavaDoc).

No blog. Release notes go in CHANGELOG and Discord. No essays.

### Twitter/Mastodon/Bluesky

Optional. Solo maintainer can pick one platform and announce there for visibility, or none. The Minecraft modding community is heavily on Discord and Reddit; social media is bonus.

If maintained, post:

- Major releases only.
- Occasional development screenshots (1-2 per month max).
- Replies to community questions if relevant.

Do NOT post:

- Hot takes about other mods.
- Personal politics.
- Daily updates ("today I refactored X").

## Bug report triage

Triage process:

1. **Triage label applied within 48 hours** of issue open.
   - `bug` if reproducible.
   - `needs-info` if the report is incomplete.
   - `cannot-reproduce` if we cannot reproduce.
   - `wontfix` if the behavior is intentional.
   - `duplicate` with link to original.
2. **Priority assigned**:
   - `p0-critical`: crashes server, corrupts saves, data loss. Fix within days.
   - `p1-high`: major gameplay broken. Fix within weeks.
   - `p2-normal`: minor gameplay issues. Fix in normal cadence.
   - `p3-low`: cosmetic, edge case. Fix when convenient.
3. **Milestone assigned**: which release will include the fix.
4. **Investigation**: only after triage. Don't dive into fixes for unverified bugs.

Issues without reproduction steps that the maintainer cannot reproduce are closed `cannot-reproduce` after 30 days of no response from the reporter.

## Feature request triage

Feature requests:

- Reviewed against `02-DESIGN-PILLARS.md` and the roadmap (`08-ROADMAP.md`).
- Labeled by scope: `v1-scope`, `v2-scope`, `v3-scope`, `out-of-scope`.
- `out-of-scope` requests are closed with explanation; nothing personal.
- `v2-scope` and `v3-scope` requests stay open as long as they fit the roadmap.

Community votes (👍 reactions) inform priority but don't dictate it. The maintainer has final say.

## Modpack integration

Modpack authors integrating Colony should:

- Use the latest stable release, not alpha/beta.
- Test with their full mod set before publishing.
- Report integration issues with a minimal reproduction (Colony + the specific conflicting mod, not 200 mods).

For known modpack-friendly mods, we maintain compat profiles in datapack format. See `06-DATA-DRIVEN.md` for the `conditions` syntax. If a modpack author wants Colony to integrate with mod X, they can:

- Submit a PR adding a default compat profile.
- Or maintain their own datapack override in their pack.

We accept the first reasonable PR for each major mod (Create, Farmer's Delight, Mekanism, etc.).

## Security and safety

Vulnerabilities (e.g. server-side code injection via crafted save files, payload exploits) should be reported privately:

- Email the maintainer (address in repo profile).
- Do NOT open a public issue with the vulnerability details.
- Allow 30 days for fix before public disclosure.

If you find a serious bug that could lead to grief or duping in multiplayer, follow the same process.

A `SECURITY.md` file at the repo root describes this formally; it's added before V1.0 stable.

## End-of-life policy

If Colony is abandoned:

- The repo stays public on GitHub.
- The LGPL-3.0 license permits forking.
- Released jars remain on Modrinth/CurseForge indefinitely.
- The `:api` Maven artifact stays available.
- A final commit `EOL.md` describes the state and recommends successors if any.

If a successor maintainer steps up:

- They fork the repo, create their own release channel.
- They may continue using the Colony name with a notice ("Colony, maintained by X since 2027").
- The original maintainer retains namespace authority on the GitHub URL unless explicitly transferred.

We commit to giving 60 days notice before truly abandoning the project, so the community has time to organize a fork.

## Donation and monetization stance

Colony is and will remain free software (LGPL-3.0).

- No paid features.
- No premium tiers.
- No ads in the mod jar.
- No telemetry without explicit opt-in.
- No "Patreon-exclusive content" of any kind (we are deliberately distinguishing from MineColonies here).

Donations are accepted (Ko-fi, GitHub Sponsors) but never incentivized in-game. A donor gets:

- Thank you in `CONTRIBUTORS.md`.
- Optional Discord role.
- Nothing else.

If the project ever earns enough to justify a maintainer salary, that's documented transparently. Until then, this is a hobby project published for love of modding.

## Burnout protection

Solo dev sustainability:

- Take breaks. 2 weeks every 4 months. Announce on Discord.
- Don't promise specific release dates. Targets only.
- Reject scope creep. The roadmap is the contract.
- Triage with confidence: not every feature request becomes a feature.
- If burning out, communicate. The community generally accepts a delayed release over a rushed one.

If the project is going to die, prefer a slow public death (clear EOL notice) over silence. The community can fork; silence makes that harder.

## Long-term archive

For each major release (V1.0, V2.0, etc.), maintain:

- Source code tag on GitHub.
- Jar artifacts on Modrinth, CurseForge, GitHub Releases.
- API artifact in Maven repository.
- A `releases/{version}/` directory in the repo with the changelog, known issues, migration notes.

This ensures even if the modding platforms go down, anyone can rebuild a working version of Colony from the GitHub repo plus a public NeoForge release.
