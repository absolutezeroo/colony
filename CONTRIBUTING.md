# Contributing to Colony

Thanks for considering a contribution. This document covers what you need to know before opening an issue or pull request.

## Before you start

Read `docs/02-DESIGN-PILLARS.md` and `docs/03-CODE-STYLE.md`. Both are non-negotiable. If your proposed change conflicts with them, open an issue first to discuss whether the pillars need amending.

## Issues

### Bug reports

A good bug report contains:

- **What you did**: exact steps to reproduce, in numbered list.
- **What you expected**: described behavior.
- **What happened**: actual behavior, with screenshots or video if visual.
- **Environment**: Minecraft version, NeoForge version, Colony version, other mods loaded.
- **Logs**: attach `latest.log` and `debug.log` from your `.minecraft/logs/`. Don't paste 5000 lines into the issue body; use a file attachment.

Bugs reproducible only with non-trivial modpack combinations will be marked `needs-isolation` and require you to confirm whether the bug reproduces without the other mods. We can't debug ten mods at once.

### Feature requests

A good feature request contains:

- **Problem**: what gameplay or UX friction you experience today.
- **Proposed solution**: your suggested approach.
- **Alternatives considered**: other approaches and why they're worse.
- **Scope**: is this V1, V2, V3?
- **Pillar check**: does this respect the design pillars?

Feature requests that violate the design pillars without arguing why the pillar should change will be closed.

## Pull requests

### Before writing code

Open an issue describing the change. **Do not submit a PR without prior discussion** unless the change is trivial (typo fix, lang file entry, one-line bugfix).

The reason: the architecture has invariants the compiler can't fully check. Multi-module boundaries, server authority, naming conventions, codec versioning. A PR that breaks one of these wastes both our time. A 30-second issue prevents 2 hours of rework.

### Writing the code

- Read the relevant system docs (`docs/04-*` through `docs/17-*`) for the area you're touching.
- Match the existing code style. CI enforces this; running `./gradlew spotlessApply checkstyleMain` locally catches violations early.
- Add tests for new logic. See `docs/13-TESTING.md` for the layered strategy.
- Update documentation when you change observable behavior. Either edit existing docs or add a new section.
- Update `docs/10-TECH-DEBT.md` if you took a shortcut you intend to revisit.

### Commit messages

Conventional Commits. Examples:

```
feat(building): add freeform footprint validation
fix(citizen): correct pathfinding stall when home zone is unloaded
refactor(persistence): extract migration step interface
docs(architecture): clarify :api module boundary
test(building): cover overcrowded room edge cases
chore(deps): bump Parchment to 2025.06.15
```

The subject line is imperative mood, lowercase, no trailing period, under 72 characters. The body (optional) explains *why* the change was made, not *what*; the diff already shows what.

### Submitting

- One PR per logical change. Don't bundle "refactor + new feature + bugfix" into one PR.
- Branch name: `feature/short-description`, `fix/short-description`, `refactor/short-description`, `docs/short-description`.
- Link the related issue in the PR description (`Fixes #42`, `Refs #17`).
- Don't merge your own PR without review. Solo maintainer self-reviews are OK in the early project phase but should not become a habit.

### CI requirements

PRs must pass:

- `./gradlew spotlessCheck checkstyleMain` — code style.
- `./gradlew build` — compilation and unit tests.
- `./gradlew :neoforge:runGameTestServer` — in-world tests.

PRs failing CI will not be merged. If you believe CI is broken (not your code), open a separate issue.

### Review expectations

Solo maintainer means review can take time. Realistic timeline:

- Trivial PRs (typo, lang fix): 1-2 days.
- Small PRs (single-file feature, bugfix): 3-7 days.
- Medium PRs (new system component, refactor): 1-3 weeks.
- Large PRs: please don't. Split into multiple smaller PRs first.

Reviews focus on:

- Does it match the design pillars and the relevant system doc?
- Is the code style compliant?
- Are the tests sufficient?
- Are the public API changes (if any) intentional and minor-version-compatible?
- Is the documentation updated?

Style nitpicks are addressed by Spotless/Checkstyle. We don't waste review cycles on indentation.

## Specific contribution types

### Content (new rooms, jobs, buildings, traits)

V1 has a deliberately small content set. Before adding new content, ask: does this fit V1 scope (`docs/08-ROADMAP.md`) or is it V2+?

V1 content additions:

- New `RoomType`: needs JSON profile + datagen entry + lang entries + tests.
- New `JobType`: needs JSON profile + Java `JobBehavior` implementation + datagen + lang + GameTest.
- New `BuildingType`: needs JSON profile + Hut block registration + datagen + lang + screenshots.
- New `CitizenTrait`: needs JSON profile + lang + scoring math validated by tests.

Content for V2+: open a discussion issue first, do not submit code.

### Datapack (modpack authors)

If you're a modpack author tweaking Colony for your pack, you don't need to PR to the main repo. Datapack overrides are designed for this. See `docs/06-DATA-DRIVEN.md` for the override conventions.

If your modpack needs a feature that requires Java changes (a new tier requirement type, a new mood modifier), open a feature request issue.

### Addon developers

You're building a separate mod consuming `colony-api`. You don't contribute to the Colony repo; you have your own.

For your addon, follow the V2 API contracts in docs 15-17. The interfaces are committed and won't break across V1 patch versions. V2 may add (not remove) capabilities.

If you discover an API gap (something your addon needs but the API doesn't expose), open an issue. We will consider adding an extension point. We will not break existing API to add new features.

### Translations

The base mod ships `en_us.json`. Other languages are community-contributed.

- Open a PR adding `assets/colony/lang/{lang_code}.json`.
- Translate every key present in `en_us.json`. Missing keys fall back to English.
- Don't translate developer-facing strings (log messages, debug output). Only user-facing strings.
- Test in-game before submitting (`/reload` after dropping the file in `resources/`).

## Code of conduct

Brief because we trust adults:

- Treat others with respect. Disagree with ideas, not people.
- No harassment, slurs, or personal attacks. Will result in a ban from the project.
- Keep discussions on-topic. The repo issues are for Colony, not general Minecraft modding philosophy.
- If a discussion escalates, walk away. Take 24 hours before responding. The maintainer reserves the right to lock threads that derail.

## Maintainer prerogative

The maintainer (akikazu / absolutezeroo) has final say on:

- What features are accepted.
- What architectural patterns are used.
- What gets merged or rejected.

If you disagree fundamentally with a direction, you can fork. The LGPL-3.0 license explicitly permits this. We will not be offended.

## Recognition

Contributors are listed in a `CONTRIBUTORS.md` file (created when the first external PR merges). Significant contributions are mentioned in release notes.

There is no Patreon, no monetization, no paid tiers. Colony is and will remain free software. If you contribute time, you contribute time. We don't promise compensation beyond credit.

## Questions

Open a discussion thread (GitHub Discussions, when enabled) for general questions. Open an issue for bug reports and feature requests.

For private inquiries (security disclosures, sensitive matters): see `SECURITY.md` when it exists, or contact the maintainer via the email listed in the repo profile.
