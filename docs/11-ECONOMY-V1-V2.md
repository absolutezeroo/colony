# 11 — Economy: V1 minimal + V2 frozen design

This document covers two scopes:
1. **What economy exists in V1** (intentionally minimal but coherent).
2. **What V2 economy will look like, frozen now** to avoid re-debating decisions in 12 months.

V2 sections are not implementation specs. They are design commitments. Implementation specs come when V2 work begins.

---

## Part 1 — V1 economy

### Principle

V1 has **no money, no taxes, no wages**. The economy is item-based: citizens consume food and tools, produce goods, and the player provides inputs.

This is intentional. Adding currency in V1 doubles scope. We ship without it and validate that the colony loop is fun on its own merits.

### What V1 has

**Item-based resource flow.** Citizens take items from typed storage chests, perform work, deposit outputs to other typed chests. The player provides initial inputs (seeds, tools) and consumes outputs (crops, lumber, ore).

**Tool durability matters.** Tools degrade as citizens use them. When a tool breaks, the citizen returns to its hut and either picks up a replacement from the `tools` storage slot or stops working until the player provides one.

**Hunger consumes food.** When a citizen eats, the food item is consumed from the source (Cookhouse output chest or Town Hall fallback). Hunger is the only "currency" that flows naturally in V1.

**Upkeep is symbolic only.** The tier system mentions `upkeep_cost_per_day` but in V1 this field is logged for future use only. No actual deduction happens. The number is parsed and stored, ready for V2 when treasury exists.

### What V1 does NOT have

- No wallets on citizens or colony.
- No transactions or transaction history.
- No wages paid.
- No taxes.
- No treasury balance.
- No marketplaces.
- No currency item.
- No trading with other colonies.
- No upkeep deduction (despite the field existing).

### Why the upkeep field is parsed anyway

The `upkeep_cost_per_day` field is read and stored at JSON load time so that:

- Building tier JSONs are forward-compatible. When V2 lands, no JSON changes needed.
- Players can see "Daily upkeep: 22 (V2)" in the Building GUI, making future mechanics visible.
- Modpack authors can tune the numbers now, validate they're sensible, before V2.

The field is **never deducted from anything in V1**. The validation in `01-ARCHITECTURE.md` and `06-DATA-DRIVEN.md` accepts it without action.

### Food economy details

Citizens eat when hunger drops below `HUNGER_HUNGRY_THRESHOLD`. Priority order:

1. Cookhouse with prepared meal in `output` chest — consumes the highest-quality meal available.
2. Town Hall fallback — provides a basic, low-quality "ration" item. The ration is generated server-side (no actual recipe), single-use. This represents the colony's communal pantry. Slow regeneration: 1 ration every 5 in-game minutes per citizen housed at Town Hall.
3. Personal carry (if the citizen happens to hold food for any reason).

If none available, hunger continues dropping. At `starving` threshold the citizen receives the `colony:mood/starving` modifier and productivity halves.

### Tool economy details

Each job declares its tool requirements via `JobType.requiredTools()`. Example: Farmer requires `#minecraft:hoes` (any hoe).

- The citizen takes a tool from the `tools` storage slot when starting work.
- The tool stays in the citizen's hand inventory (not stored back).
- When it breaks, the citizen's hand inventory loses the item, citizen returns to hut to fetch another.
- If no replacement, the citizen reports "needs tool" status and idles.

The player is responsible for providing tools. There is no auto-crafting, no auto-repair in V1.

### Initial colony provisioning

When a colony is founded, the Town Hall starts with:

- 32 starter rations (generic food item).
- 4 basic hoes.
- 1 basic pickaxe.
- 1 basic axe.

These represent "what the founders brought." Configurable via server config. After consumption, the player must produce more.

### V1 economy hooks for V2

The architecture exposes these extension points, dormant in V1, ready in V2:

- `EconomyService` interface exists in `:api` with a no-op implementation in V1. V2 swaps in the real implementation.
- `WagePaidEvent` and `TaxAssessedEvent` exist in `:api` but are never fired in V1.
- `Wallet` interface exists with a placeholder `EmptyWallet` returning balance 0 always.
- `Treasury` interface exists with a placeholder `NullTreasury` rejecting all transactions.

Addons developed against V1's `:api` will see these interfaces. Documentation tags them clearly as "V2: not yet implemented." Addon devs can prepare V2-aware features now.

---

## Part 2 — V2 economy (frozen design)

This is the commitment for V2. Implementation details come when V2 work begins, but the design decisions below are locked in to prevent re-debating.

### Currency

**Single base currency**, internally called `colony:currency/coin`. Players see "coins" in UI.

- Stored as `long` (no decimals, no fractions). One transaction = one integer movement.
- Codec-serialized as a record `Currency(long amount)`.
- No multi-currency in V1 or V2. A third party can add their own currency type via the `CurrencyType` registry, but the base mod uses one.

### Wallets

Every citizen has a `Wallet`. Every colony has multiple **Treasuries** (one general, one military reserved for V3+, one public works, etc.).

```java
public interface Wallet
{
    UUID owner();

    Currency balance();

    TransactionResult debit(Currency amount, Identifier purpose);

    TransactionResult credit(Currency amount, Identifier source);

    Stream<Transaction> history(int limit);
}
```

Wallets persist as part of citizen/colony NBT. Transaction history capped (default 100 entries per wallet, configurable).

### Treasuries

Colonies have multiple treasuries by role:

- `colony:treasury/general` — default destination for tax revenue, default source for upkeep payments.
- `colony:treasury/public_works` — funds Builder Hut activities (paying citizens hired to build).
- `colony:treasury/military` — V3+ for guards/soldiers.

A colony can have multiple instances of each type (e.g. two general treasuries if the player wants segregated bookkeeping). Each Treasury is a separate `Wallet` with explicit role tag.

### Wages

Each `JobType` declares a `baseWage` in V2 JSON:

```json
{
  "type": "colony:harvesting",
  "base_wage": 5,
  "wage_cadence": "daily"
}
```

The wage is paid daily/weekly per cadence. Source: `colony:treasury/general` of the colony, or a specific treasury declared by the Building.

`WagePaidEvent` fires on each payment. The amount can be modified by:

- Skill multiplier (high-skill workers earn more).
- Mood multiplier (loyal citizens accept lower pay, in mood modifier form).
- Trait multiplier (some traits demand premium).

If the treasury can't pay, the citizen receives `colony:mood/unpaid` modifier. Repeated non-payment leads to desertion (citizen leaves the colony).

### Taxes

Six tax types registered in V2:

| ID | Basis | Cadence |
|---|---|---|
| `colony:tax/head` | Per citizen, flat | Monthly (in-game) |
| `colony:tax/income` | % of wages paid | On wage payment |
| `colony:tax/property` | Per Building, based on tier | Monthly |
| `colony:tax/sales` | % of trade transactions | On transaction |
| `colony:tax/wealth` | % of wallet balance | Monthly |
| `colony:tax/tariff` | On imports from other colonies | On transaction (V3+ inter-colony) |

Each tax is configured by a `TaxPolicy` JSON:

```json
{
  "id": "colony:policies/standard_income_tax",
  "type": "colony:tax/income",
  "cadence": "on_wage",
  "brackets": [
    { "up_to": 100, "rate": 0.05 },
    { "up_to": 500, "rate": 0.10 },
    { "up_to": null, "rate": 0.20 }
  ],
  "exemptions": [
    { "type": "colony:trait_exempt", "trait": "colony:trait/veteran", "rate_multiplier": 0.5 }
  ],
  "treasury": "colony:treasury/general"
}
```

Brackets are progressive by default. Custom curves can be added via registered `TaxBracketCurveType`.

### Tax evasion

Citizens with low loyalty (V2 reputation) may evade taxes stochastically. Compliance probability:

```
compliance = sigmoid(loyalty / 50 - tax_burden_ratio * 2 + trait_modifier)
```

Where:

- `loyalty` is the citizen's loyalty (V2 axis 1).
- `tax_burden_ratio` is current_tax_paid / citizen_income.
- `trait_modifier` is +/- from traits (`lawful` +1, `greedy` -1).

Failed evasion goes undetected unless a tax collector investigates. Detected evasion triggers fine + reputation loss.

### Treasury budgeting (V2.x or V3)

Players can configure auto-allocations:

- Tax revenue split between treasuries by configurable percentages.
- Auto-transfer rules between treasuries (overflow general → public works).
- Upkeep budgets per Building (cap how much each Building can draw monthly).

Deferred to V2.x once V2 base economy proves stable.

### Trade between colonies (V3)

Not in V2 either. V3+ topic. Mentioned here only to clarify scope.

---

## Part 3 — Implementation order when V2 starts

When V2 work begins (post-V1.0), the implementation order is:

1. `Currency`, `Wallet`, `Treasury` interfaces fully implemented. Persistence working.
2. Wages: `WagePaidEvent` fires, treasury debited, wallet credited.
3. First tax: `colony:tax/head` (simplest, flat per citizen). Validates the pipeline.
4. Income tax with brackets.
5. Property tax.
6. Tax evasion mechanic.
7. Sales tax (requires trade transactions, which we don't have yet).
8. Wealth tax.
9. Tariff (requires inter-colony, deferred).

This sequence keeps each step independently shippable.

---

## Part 4 — Why this matters for V1

You might ask: "why freeze V2 design now if we won't implement for 14+ months?"

Three reasons:

1. **It prevents drift.** In 12 months we will have forgotten today's reasoning. Frozen text prevents debates from re-opening.
2. **It informs V1 architecture.** Knowing V2 has wallets means V1's `Citizen` interface should have a `Wallet wallet()` method returning `EmptyWallet` for now. Forgetting this in V1 means a breaking API change in V2.
3. **It enables addon planning.** Addon devs can read this and build V2-ready features against V1.

The interfaces exist in `:api` from V1.0. They no-op. They become real in V2. No breaking changes.
