# 12 — UX & GUI Patterns

This document specifies the GUI patterns, screen layouts, and UX rules for Colony. AI agents implementing screens must read this and the relevant `04-BUILDING-SYSTEM.md` / `05-CITIZEN-SYSTEM.md` sections.

## GUI framework decision

**V1.0 uses vanilla `AbstractContainerScreen` + custom widgets.** No BlockUI, no Owo, no Spectrum. Why:

- Zero external dependency. The mod runs anywhere NeoForge runs.
- No risk of BlockUI license/API breakage.
- Vanilla widgets are battle-tested and well-understood.
- Players don't expect rich UI from a colony mod — they expect functional UI.

**V2 may adopt BlockUI** as an opt-in advanced UI mode if vanilla becomes a bottleneck for UX quality. The fallback remains vanilla, always.

The vanilla path uses:

- `AbstractContainerScreen<T>` for screens with inventories.
- `Screen` for screens without inventories (info panels, configuration).
- `Button`, `EditBox`, `ImageButton` for widgets.
- Custom widgets (`ColonyListWidget`, `ColonyTabsWidget`) extending `AbstractWidget` for repeated patterns.

## Layout grid

All screens use a base size of 360×220 pixels. Larger screens (Town Hall overview) scale to 380×256. Use multiples of 4 for spacing.

```
┌────────────────────────────────────────────────────┐ 0
│ Title bar (24px high)                       [×]    │
├────────────────────────────────────────────────────┤ 24
│ Tab strip (20px high)                              │
│ [Overview] [Citizens] [Huts] [Warnings]            │
├────────────────────────────────────────────────────┤ 44
│                                                    │
│ Content area                                       │
│                                                    │
│                                                    │
├────────────────────────────────────────────────────┤ 200
│ Footer (buttons, status text)               16px   │
└────────────────────────────────────────────────────┘ 220
0                                                  360
```

Margins: 8px from edges. Padding inside widget groups: 4px.

## Color palette

Defined in `client/screen/ColonyColors.java`:

| Token | Hex | Use |
|---|---|---|
| `BG_PRIMARY` | `0xC6C6C6` | Vanilla container background (don't override) |
| `TEXT_PRIMARY` | `0x404040` | Body text |
| `TEXT_TITLE` | `0x202020` | Section headers |
| `TEXT_DIM` | `0x707070` | Secondary info, hints |
| `TEXT_WARNING` | `0xB08000` | Warnings (non-blocking) |
| `TEXT_ERROR` | `0xC04030` | Errors (blocking) |
| `TEXT_SUCCESS` | `0x308040` | Validation passes, success states |
| `BORDER_DEFAULT` | `0x8B8B8B` | Widget borders |
| `BORDER_FOCUS` | `0x4080C0` | Active/focused widget |
| `OVERLAY_TIER_BASIC` | `0xCCAA66` | Tier 1 indicator |
| `OVERLAY_TIER_DEVELOPED` | `0x88BB88` | Tier 2 indicator |
| `OVERLAY_TIER_ESTABLISHED` | `0xCC88CC` | Tier 3 indicator |

Don't inline hex values in widget code. Always reference tokens.

## Tabs pattern

Multi-tab screens (Building, Town Hall, Citizen Detail) use a horizontal tab strip below the title.

```java
private final ColonyTabsWidget tabs = ColonyTabsWidget.builder()
    .tab("overview", Component.translatable("colony.gui.tab.overview"))
    .tab("structure", Component.translatable("colony.gui.tab.structure"))
    .tab("storage", Component.translatable("colony.gui.tab.storage"))
    .tab("workzones", Component.translatable("colony.gui.tab.workzones"))
    .tab("citizens", Component.translatable("colony.gui.tab.citizens"))
    .onChange(this::setActiveTab)
    .build();
```

Active tab renders in `TEXT_TITLE`. Inactive in `TEXT_DIM`. The body switches via a `SwitchView`-style group system: each tab has its own body group, only one visible at a time. No manual `show()`/`hide()`.

## List pattern

Repeating lists (citizens, huts, rooms) use `ColonyListWidget`:

```java
ColonyListWidget<HutRowEntry> hutList = ColonyListWidget.<HutRowEntry>builder()
    .rowHeight(18)
    .visibleRows(7)
    .scrollable(true)
    .rowBinder((entry, pane) -> {
        pane.label("name", entry.displayName());
        pane.label("tier", entry.tierLabel());
        pane.button("inspect", b -> b.onClick(() -> openHut(entry.id())));
    })
    .build();

hutList.setRows(snapshot.huts());
```

Each row has a fixed layout (label + label + button). Rows are not interactive beyond their buttons. No drag-drop, no inline editing in V1.

## Required screens (V1)

The following screens must exist by end of Phase 4 (month 12):

### Colony Tool HUD

Not a full screen — an overlay rendered when holding the Colony Tool.

- Top-right corner of the screen.
- Shows current mode (Zone / Storage / Link / Inspect).
- Shows context (e.g. "Designating: office for Farmer Hut #2").
- Shows hints (key bindings: Enter confirm, Esc cancel, Shift+Scroll mode, etc.).

Implemented in `client/hud/ColonyToolHud.java`, registered via `RenderGuiOverlayEvent`.

### Pending Placement HUD

Shown when the player is in `PendingPlacement` state for a Hut block.

- Bottom-center of the screen.
- Shows "Designate this hut's zone, then press Enter."
- Shows live validation status (✓ Volume / ✗ Encloses hut block / ✓ No overlap).
- Shows expected tier ("Tier 1 — Basic") when valid.

### Town Hall Screen

Tabs: Overview, Citizens, Huts, Warnings.

- **Overview**: colony name, founded date, citizen count, hut count, treasury balance (V2 — V1 shows "Economy disabled").
- **Citizens**: list of citizens with name, job, home, status. Click "Inspect" → opens Citizen Detail.
- **Huts**: list of Buildings with name, type, tier. Click "Inspect" → opens Building Screen.
- **Warnings**: aggregated warnings (homeless citizens, overcrowded rooms, missing tools, etc.).

### Building Screen (generic, parameterized by HutType)

Tabs: Structure, Storage, Work Zones, Citizens, Info.

- **Structure**: list of Room slots with status (empty / assigned / invalid). Click "Designate" → enter Room painting mode.
- **Storage**: list of Storage slots with assigned chest count. Click "Designate chests" → enter Storage typing mode.
- **Work Zones**: list of Anchor slots with linked anchors. Click "Link" → enter Link mode. Click "Get new {AnchorType}" → receive item.
- **Citizens**: list of citizens assigned to this Building (workers and residents).
- **Info**: current tier, structural score, next-tier requirements, daily upkeep (V2).

### Citizen Detail Screen

Tabs: Overview, Traits, Skills, Schedule.

- **Overview**: name, home, job, mood, hunger, fatigue.
- **Traits**: list with descriptions.
- **Skills**: per-job XP and level.
- **Schedule**: current daily schedule (read-only in V1).

### Anchor Configuration Screen

For scarecrows, quarry pillars, etc.

- Top: current zone dimensions (N/S/W/E/U/D) with +/- arrows and direct numeric input.
- Middle: anchor type-specific config (crop type for scarecrow, depth for quarry).
- Bottom: live volume + valid tile count + buttons (Preview, Cancel, Confirm).
- Preview button toggles in-world wireframe overlay while screen is open.

## Server-authoritative pattern

Every interactive GUI follows this pattern:

1. Player clicks a button.
2. Client sends a typed `CustomPacketPayload` to the server.
3. Server validates, mutates state, pushes back a delta payload.
4. Client receives delta, updates its `ColonyView` mirror, re-renders.

**Never** mutate client state directly and expect the server to catch up. If the network is lossy or out of sync, the client must remain consistent with the server, not the other way around.

## Visual feedback patterns

### Real-time zone painting

When painting an outer zone or room zone:

- Wireframe of the in-progress zone in 3D, color-coded:
  - `GREEN` — valid zone (passes all constraints).
  - `RED` — invalid (fails a constraint).
  - `YELLOW` — warning (valid but suboptimal, e.g. too large).
- Painted blocks highlighted with a subtle particle effect.
- Volume counter floating above the player's view ("96 / 128 max").

Implemented via custom `LevelRenderer`-hooked overlay in `client/render/ZonePaintOverlay.java`.

### Typed chest indicators

Chests typed for storage roles emit subtle particles per role:

- `BLUE` for input.
- `GREEN` for output.
- `YELLOW` for tools.
- `ORANGE` for materials.
- `WHITE` for general.

Particles spawn at low frequency (1 every 20 ticks per chest) to avoid spam. Visible from ~24 blocks.

On hover (player looking at chest within 8 blocks), a floating label appears: "Seeds (input) — Farmer Hut #3 — 12/27 slots used".

### Anchor visualization

Anchors (scarecrows, quarry pillars) emit no particles by default but show a wireframe of their work zone when:

- The player holds a Colony Tool in Inspect mode.
- The player is within 32 blocks of the anchor.
- The player has the parent Building's GUI open.

Color matches the anchor role (green for farming, gray for mining, brown for lumber).

## Internationalization (i18n)

All user-facing strings use `Component.translatable(key)`. No `Component.literal` in production code paths.

Translation key conventions:

```
colony.gui.tab.<tab_name>
colony.gui.button.<action>
colony.gui.label.<context>.<field>
colony.gui.tooltip.<context>
colony.gui.error.<context>
colony.gui.warning.<context>

colony.entity.citizen.<aspect>
colony.block.hut.<hut_type>.<aspect>
colony.item.colony_tool.mode.<mode>

colony.message.<event>
colony.command.<cmd>.<feedback>
```

Examples:

- `colony.gui.tab.overview` → "Overview"
- `colony.gui.button.assign_worker` → "Assign worker"
- `colony.gui.label.building.tier` → "Tier"
- `colony.gui.error.zone_too_small` → "Zone is too small (%s blocks, minimum %s)"

The CI validates that every key referenced in Java exists in `en_us.json`. Missing keys fail the build.

## UX rules

**One screen does one thing.** No "super-screen" with 12 tabs. Split into focused screens if a screen exceeds 5 tabs.

**Show, don't tell.** When the player makes a mistake, show what's wrong (red wireframe, error label with specific reason). Don't show a generic "Error" message.

**Cancellation is free.** Every interactive state has Esc-to-cancel. Nothing is committed until the player confirms.

**Feedback is immediate.** A button click should produce visible feedback within 1 frame. If the action takes longer, show a loading indicator and don't block input.

**No modal pop-ups.** Mojang's vanilla style doesn't use them. Inline validation messages instead.

**Numbers are formatted.** Volumes ("936 blocks"), durations ("3 days"), percentages ("65%") all use `String.format` consistently. No raw integers in user-facing labels.

**Names are translated.** Citizens, Buildings, items all have `displayName()` that returns a localized `Component`.

## Forbidden patterns

- **No client-only Gui that mutates state.** GUIs send payloads, period.
- **No `ItemStack` manipulation client-side.** Inventory contents are server-mirrored only.
- **No screen that requires the player to remember context across openings.** Each screen is self-contained or explicitly linked.
- **No timed auto-advance.** Players control progression.
- **No "Are you sure?" dialogs for actions that can be undone.** Trust the player. For destructive irreversible actions (delete colony), require typing the colony name.

## Testing GUIs

GUI tests are end-to-end:

- Open the screen, render once, take a screenshot, compare to baseline.
- Click each button, verify payload sent.
- Trigger each error case, verify error label appears.

Baselines stored in `:neoforge/src/test/resources/gui-baselines/`. Updated manually on intentional layout changes. CI fails on mismatch unless `-Dcolony.gui.update-baselines=true` is set.

GameTests for in-world behavior (anchor configuration → in-world wireframe appears) live in `:neoforge/src/test/java/.../gametest/`.
