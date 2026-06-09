# Disposable Infantry Weapons (TO:AR p.106)

**Status**: Planning — not yet implemented
**Rules Level**: Advanced (Tactical Operations: Advanced Rules)
**Available To**: Conventional Infantry (CI), Battle Armor (BA)
**Scope**: MegaMek (engine + UI), MegaMekLab (design), MekHQ (campaign)

---

## 1. Rule Summary

**Construction**

- Any Conventional Infantry unit may carry a single one-shot Disposable Weapon **per active trooper**.
- Battle Armor suits may carry Disposable Weapons only if they also have an **anti-personnel weapon mount** or **two
  armored gloves**.
- All troopers in a platoon / BA squad must carry the **same** Disposable Weapon.
- Qualifying weapons are those listed on the TechManual infantry weapons table with an
  Ammo (Shots) value of **"(1-D)"**.

**Game play**

- Once per scenario, the platoon/squad may make a **single** attack with the Disposable Weapons,
  used **instead of** the platoon's standard weapon attack, and only if the unit is **not** engaged
  in an anti-'Mech (leg/swarm) attack.
- Resolved as a standard direct-fire attack using the **Disposable Weapon's range brackets**.
- On a hit, damage is resolved per a standard conventional-infantry ranged attack:

  > **Total damage = 3 x (disposable weapon damage-each) x (troopers who hit on the Cluster Hits Table)**, rounded
  normally.

  Applied to the target as a normal conventional-infantry weapon attack.

---

## 2. Qualifying Weapons (TechManual pp. 349-352, "(1-D)")

All 11 already exist as `InfantryWeapon` subclasses; none currently model the one-shot/disposable
behavior. The implementation flags these existing classes — no new weapon classes are required.

| TechManual weapon                     | Damage (each) | Range     | Existing class                                         |
|---------------------------------------|---------------|-----------|--------------------------------------------------------|
| Laser Pistol (Hold-Out [White Dwarf]) | 0.02          | 0         | `InfantryPistolWhiteDwarfLaserPistolWeapon`            |
| Grenade (Non-Inferno)                 | 0.48          | 0         | `InfantryGrenadeStandardWeapon`                        |
| Grenade (Inferno)                     | 0.19          | 0         | `InfantryGrenadeInfernoWeapon`                         |
| Grenade (Micro)                       | 0.16          | 0         | `InfantryGrenadeMicroWeapon`                           |
| Grenade (Mini) (Non-Inferno)          | 0.27          | 0         | `InfantryGrenadeMiniWeapon`                            |
| Grenade (Mini) (Inferno)              | 0.11          | 0         | `InfantryGrenadeMiniInfernoWeapon`                     |
| Grenade (Rocket-Assisted)             | 0.30          | (1+)      | `InfantryGrenadeRAGWeapon`                             |
| AA Weapon (Mk. 1 Light AA)            | 0.23          | (support) | `InfantrySupportMk1LightAAWeapon`                      |
| Pulse Laser (Dragonsbane Disp.)       | 0.16 / 0.49*  | 3         | `InfantrySupportDragonsbaneDisposablePulseLaserWeapon` |
| Rocket Launcher (LAW)                 | 0.53          | 2         | `InfantrySupportRocketLauncherLAWWeapon`               |
| Rocket Launcher (V-LAW)               | 0.48          | (support) | `InfantrySupportRocketLauncherVLAWWeapon`              |

\* The Dragonsbane's in-code `infantryDamage` (0.49) differs from the TM 6th-printing table (0.16).
IN SCOPE: correct it to 0.16 as part of this feature.

---

## 3. Current-State Findings (verified)

- **No disposable mechanic exists.** `DisposableDisplayContainer` is an unrelated UI helper; the
  "Dragonsbane Disposable" weapon is disposable in name only.
- **One-shot infrastructure** exists: `WeaponType.F_ONE_SHOT`, `Mounted.fired`/`setFired`/`isFired`,
  `canFire()` (blocks fired one-shots), `WeaponHandler.useAmmo` sets `fired=true`. `fired` persists
  for the whole game and never resets — this naturally gives us "once per scenario."
- **Infantry flag vocabulary** lives in `equipment/WeaponTypeFlag.java` (`F_INF_AA`, `F_INF_BURST`,
  `F_INF_SUPPORT`, ...) and is re-exposed on `WeaponType` as
  `public static final WeaponTypeFlag F_INF_SUPPORT = WeaponTypeFlag.F_INF_SUPPORT;` (line 620).
- **CI loadout** (`units/ConvInfantry.java`): `primaryWeapon`, `secondaryWeapon`,
  `secondaryWeaponsPerSquad`. No third "disposable" slot. `getDamagePerTrooper()` caps primary at
  `MMConstants.INFANTRY_PRIMARY_WEAPON_DAMAGE_CAP` (0.6) — the disposable formula must NOT use this cap.
- **Damage resolution** (`weapons/infantry/InfantryWeaponHandler.calcHits`, lines 108-256):
  `troopersHit = Compute.missilesHit(getShootingStrength(), mod)` (Cluster Hits Table);
  `damageDealt = round(damagePerTrooper x troopersHit)`. This is exactly the shape the disposable
  rule needs, with a different per-trooper base (`3 x weapon.getInfantryDamage()`) and the disposable
  weapon's own range.
- **BA** (`battleArmor/BattleArmor.java`): AP weapons are per-trooper mounts identified by
  `F_INFANTRY`; `getNumAllowedAntiPersonnelWeapons(loc, trooper)` (lines ~1677-1705) and the
  `MANIPULATOR_ARMORED_GLOVE` constant give the eligibility hooks.
- **Rule gating**: `game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_*)`.

---

## 4. Design

### 4.1 Weapon flag (data marker)

- Add `F_INF_DISPOSABLE` to `WeaponTypeFlag` enum and re-expose on `WeaponType` (mirrors `F_INF_SUPPORT`).
- Add `.or(F_INF_DISPOSABLE)` to the 11 classes in section 2. Optionally set `shots = 1` for display
  consistency with the "(1-D)" listing.

### 4.2 Loadout model

**Conventional Infantry** — model the disposable weapon as a real, one-shot `Mounted` weapon on the
unit so it appears in the firing UI and reuses existing fired/canFire machinery.

- `ConvInfantry`: add `@Nullable InfantryWeapon disposableWeapon` + `String disposableName` (for
  serialization), getters/setters, and a `hasDisposableWeapon()` helper.
- When present, add a corresponding `WeaponMounted` (or `InfantryWeaponMounted`) at `LOC_INFANTRY`
  during unit load, flagged so it behaves one-shot.

**Battle Armor** — disposable weapon is an AP-mount/armored-glove weapon already supported by the BA
per-trooper mount model. The disposable flag rides on the weapon type, so once the weapon classes are
flagged the BA mount round-trips automatically. Add an **eligibility validator**: a BA suit may mount
a disposable weapon only if it has an AP mount or two armored gloves (use
`getNumAllowedAntiPersonnelWeapons` / manipulator inspection).

### 4.3 Attack declaration / firing

- **Game option**: add `OptionsConstants.ADVANCED_DISPOSABLE_INFANTRY` (advanced-combat group),
  register in `GameOptions.initialize()`, add i18n label/tooltip. All new behavior gated on this.
- **Eligibility to fire the disposable** (checked in weapon selection + server validation):
    1. option enabled, 2. unit has a disposable weapon, 3. disposable not already used this game
       (`isFired()`), 4. unit is not declaring a leg/swarm anti-'Mech attack this turn.
- **"Instead of standard attack"**: conventional infantry already resolve a single platoon weapon
  attack. Firing the disposable must consume that single attack — the player chooses the normal weapon
  OR the disposable, never both. Enforce in `FiringDisplay`/weapon-selection logic and re-validate
  server-side in the weapon-attack handler path.

### 4.4 Resolution / damage

- New handler `InfantryDisposableWeaponHandler extends InfantryWeaponHandler` (or a guarded branch in
  `InfantryWeaponHandler.calcHits` keyed on `weaponType.hasFlag(F_INF_DISPOSABLE)`).
- `troopersHit = Compute.missilesHit(getShootingStrength(), mods)` (unchanged Cluster Hits path).
- `perTrooper = 3.0 x disposableWeapon.getInfantryDamage()` (NO primary-weapon cap; no
  secondary blending; NO range-0 prosthetic/TSM/beast-mount bonus stacking — strict formula only).
- `damageDealt = (int) Math.round(perTrooper x troopersHit)`, applied as a normal conventional
  infantry weapon attack (mechanized halving preserved; inferno disposable grenades still apply
  inferno/heat effects on hit).
- To-hit uses the **disposable weapon's** `infantryRange` brackets.
- Route the handler via the weapon's `getCorrectHandler` (branch on the disposable flag).

### 4.5 Once-per-scenario enforcement

- After the disposable attack resolves, set the disposable `Mounted` `fired = true`; `canFire()` then
  blocks any further use for the rest of the game. Verify the infantry firing path actually invokes
  the one-shot `setFired` (it may not go through `WeaponHandler.useAmmo`'s one-shot branch — if not,
  set it explicitly in the disposable handler's `setDone`/`useAmmo`).

### 4.6 BV and cost

- Disposable weapons carry their own BV/cost already. Fold the disposable weapon into the unit BV and
  C-bill calculations. May be staged as a follow-up if the combat path is prioritized first.

---

## 5. Serialization (shared, BLK)

- **CI**: `BLKInfantryFile` read/write — add a `disposableweapon` block tag round-tripping the weapon
  internal name; mount it on load like primary/secondary.
- **BA**: disposable weapons mount by internal name through the existing BA weapon-mount path; the
  disposable flag is intrinsic to the weapon type, so no new BLK tag is strictly required, but the
  eligibility validator must run on load and in the editors.
- Confirm round-trip with MML save and MekHQ campaign save (equipment stored by internal name).

---

## 6. Cross-Repo Work Breakdown

### MegaMek (engine + client)

1. `F_INF_DISPOSABLE` flag (enum + WeaponType constant).
2. Flag the 11 weapon classes (+ optional `shots = 1`).
3. `OptionsConstants.ADVANCED_DISPOSABLE_INFANTRY` + GameOptions registration + i18n.
4. `ConvInfantry` disposable slot + helpers + BLK round-trip.
5. BA disposable eligibility validator.
6. Firing/selection logic: choose disposable instead of standard attack; gate by eligibility.
7. `InfantryDisposableWeaponHandler` (3x damage formula, disposable range, once-per-game `setFired`).
8. BV/cost integration.
9. Unit tests (damage formula, eligibility, once-per-game, anti-'Mech exclusion).

### MegaMekLab (design)

1. CI build panel: add a "Disposable Weapon" selector (filter to `F_INF_DISPOSABLE`), beside
   primary/secondary; write to BLK.
2. BA build: allow mounting disposable weapons into AP-mount/armored-glove slots with eligibility
   validation and error reporting.
3. Summary/printout: show the disposable weapon and its once-per-scenario nature.

### MekHQ (campaign)

1. Ensure CI/BA units carrying disposable weapons load and resolve in campaign play.
2. Treat the disposable as a one-shot consumable: spent after use, requires reload/resupply between
   scenarios with appropriate cost/availability.
3. BV/cost reflected in force valuation and acquisition.

---

## 7. Resolved Decisions

1. **Handler strategy**: DECIDED — dedicated `InfantryDisposableWeaponHandler` subclass (isolation/testability).
2. **Range-0 bonus stacking**: DECIDED — strict formula only, `3 x damage x troopersHit`. No
   prosthetic/TSM/beast-mount range-0 bonuses; these are fired weapons, not melee/point-blank bonuses.
3. **Inferno grenades**: DECIDED — inferno disposable grenades DO apply inferno/heat effects on hit,
   in addition to the disposable damage formula.
4. **Dragonsbane damage discrepancy**: DECIDED — IN SCOPE. Correct `infantryDamage` 0.49 -> 0.16 in
   `InfantrySupportDragonsbaneDisposablePulseLaserWeapon` to match TM 6th printing.

## 7a. Still Open

- **BA "all troopers same disposable"**: how strictly to enforce in the BA editor vs warn-only.
- **Phasing**: confirmed MM combat path first (Phase 1), then loadout/UI, then MML, then MekHQ.

---

## 8. Suggested Phasing

- **Phase 1 (MM core)**: items MM-1..MM-3, MM-7 + a minimal hand-edited test unit; prove the damage
  formula and once-per-game behavior with unit tests.
- **Phase 2 (MM loadout + UI)**: MM-4..MM-6, MM-8.
- **Phase 3 (MML)**: design-tool support so units can be built.
- **Phase 4 (MekHQ)**: campaign consumable/resupply + valuation.

## 9. Progress Log

### Phase 1 (MM core) — DONE (branch `Implement-disposable-infantry-weapons`)

- Added `F_INF_DISPOSABLE` to `WeaponTypeFlag` + `WeaponType` constant.
- Flagged all 11 `(1-D)` weapon classes with `.or(F_INF_DISPOSABLE)`.
  (Did NOT set `shots = 1` — would wrongly deplete these weapons when mounted on support vehicles
  via `InfantryWeaponHandler.useAmmo`; the disposable behavior is driven by the flag + handler.)
- Fixed Dragonsbane `infantryDamage` 0.49 -> 0.16.
- Added `OptionsConstants.ADVANCED_COMBAT_DISPOSABLE_INFANTRY_WEAPONS` + GameOptions registration
  (default off) + i18n displayableName/description in `options/messages.properties`.
- Added `InfantryDisposableWeaponHandler` (strict `3 x damage x troopersHit` formula via overridden
  `calcHits`; `useAmmo` sets the weapon `fired` for once-per-scenario).
- Unit test `InfantryDisposableWeaponHandlerTest` (4 tests, all passing): rounds-up, normal rounding,
  troopers-who-hit scaling, once-per-scenario expend.

### Phase 2 (MM loadout + server rules) — DONE (server side)

- `WeaponMounted.isDisposableWeapon()/setDisposableWeapon()` per-mount marker.
- `ConvInfantry` disposable slot: `disposableWeapon` field + `get/set/hasDisposableWeapon()`.
- BLK round-trip: `BLKInfantryFile` reads `disposableWeapon` block -> sets slot + adds a marked
  fireable `WeaponMounted` at `LOC_INFANTRY` (rejects non-infantry / non-`F_INF_DISPOSABLE` weapons);
  `BLKFile.getBlock` writes the `disposableWeapon` block.
- `InfantryWeapon.getCorrectHandler` routes a disposable mount to `InfantryDisposableWeaponHandler`
  (before the heat/inferno checks), so the same weapon type fired normally is unaffected.
- To-hit gating in `ComputeToHitIsImpossible`: disposable attack is impossible when the game option
  is off, while engaged in an anti-Mek (swarm) attack, or when it is not the unit's only attack
  (used INSTEAD of the standard attack). New `WeaponAttackAction.Disposable*` messages.
- Tests: `BLKInfantryDisposableWeaponTest` (4) green — load, default-absent, non-disposable rejected,
  writer round-trip.

### Phase 2 — Battle Armor — DONE

- `BattleArmor.canCarryDisposableWeapons()`: eligible if a dedicated anti-personnel weapon mount
  (`F_AP_MOUNT` and NOT `F_ARMORED_GLOVE`) is present, or two armored gloves
  (`countWorkingMisc(F_ARMORED_GLOVE) >= 2`).
- `BLKBattleArmorFile` marks any mounted `F_INF_DISPOSABLE` weapon as disposable on load.
- `TestBattleArmor.hasIllegalEquipmentCombinations` rejects a disposable weapon on an ineligible suit
  (used by MML construction validation).
- Test `BattleArmorDisposableWeaponTest` (4) green — AP-mount eligible, two-gloves eligible,
  single-glove NOT eligible (and not mistaken for an AP mount), none not eligible.
- Note: `BattleArmor extends Infantry`, so `InfantryDisposableWeaponHandler` (which scales by
  `getShootingStrength()`) already covers BA squads.

### Phase 2 — Client firing UX + BV — DONE

- Firing UX: the disposable mount already appears as its own weapon-list entry (decided UX). Added a
  `(Disposable)` suffix in `Mounted.getDesc()` so it is clearly distinguished from the same weapon
  fired normally; the existing one-shot `fired` marker (`- ` prefix + `canFire()`) shows it as spent
  after use. Server rules enforce instead-of-standard / once-per-scenario / not-anti-Mek.
- BV: `InfantryBVCalculator.processWeapons` now adds the Disposable Weapon to offensive BV at full
  per-trooper BV (scaled by `originalTroopers`, then by the surviving-trooper ratio), consistent with
  how Battle Armor already counts AP-mounted infantry weapons via its squad weapon filter. BA needs
  no change (the disposable is `F_INFANTRY` at `LOC_SQUAD`, already counted).
    - NOTE/approximation: the one-shot half-BV convention is intentionally NOT applied (matches BA's
      existing full-BV treatment of AP-mounted infantry weapons). If rules require the 0.5 reduction it
      is a one-line change (halve the disposable delta). Flagged for rules confirmation.
- Test `InfantryDisposableWeaponBVTest` (1): a platoon with a Disposable Weapon has higher BV than
  one without.

### Phase 2 — Lobby Configure UI (CI) — DONE

- `ConvInfantry.equipDisposableWeapon(@Nullable InfantryWeapon)`: in-place loadout change that syncs the
  `disposableWeapon` field AND the fireable `WeaponMounted` (removes any old marked mount from
  `equipmentList`/`weaponList`/`totalWeaponList`, then adds the new marked mount). The BLK loader now
  reuses this method (removed the duplicated mount-creation).
- `InfantryArmorPanel` (lobby Configure dialog) shows a **Disposable Weapon** chooser when the
  `ADVANCED_COMBAT_DISPOSABLE_INFANTRY_WEAPONS` option is enabled: "None" + every legal `(1-D)` weapon
  (filtered by year/tech level/faction/extinct), pre-selecting the current one; `applyChoice` calls
  `equipDisposableWeapon`. i18n keys added.
- Test `ConvInfantryDisposableWeaponTest` (3): add / replace / remove keep field + mount in sync.

### Phase 2 — Lobby Configure UI (BA) — DONE

- `EquipChoicePanel`: disposable `(1-D)` weapons are offered in the BA AP-mount and armored-glove weapon
  dropdowns **only when the option is on** (both lists gated consistently; normal AP weapons unaffected).
  A **safety net** re-adds any currently-mounted AP/glove weapon the gating would otherwise hide, so opening
  Configure with the rule off never silently drops an existing weapon.
- Disposable entries are **labeled `"<name> (Disposable)"`** in both dropdowns via
  `EquipChoicePanel.weaponChoiceLabel` (i18n `CustomMekDialog.disposableWeaponLabel`), so they are
  distinguishable from ordinary AP weapons.
- `APWeaponChoice.applyChoice` (AP mounts) and `BaManipulatorChoice.applyApWeapon` (armored gloves) mark a
  mounted `F_INF_DISPOSABLE` weapon as disposable. These selectors only show when the suit has an AP mount
  / armored gloves, which matches `canCarryDisposableWeapons()`.

### Robustness fix — option-gate the *behavior*, not the marking

A weapon can be `F_INF_DISPOSABLE` yet a legitimate normal weapon (e.g. White Dwarf laser pistol as a BA
AP weapon). The disposable mount is a construction property, but disposable *behavior* is gated on the game
option:

- `InfantryWeapon.getCorrectHandler` routes to `InfantryDisposableWeaponHandler` only when the option is on;
  otherwise the mount fires through the normal `InfantryWeaponHandler`.
- `ComputeToHitIsImpossible` applies the disposable to-hit gating only when the option is on (removed the
  now-redundant `DisposableRuleOff` message).

### MegaMek status: COMPLETE for CI + BA (engine + client, incl. lobby Configure for both). 19 disposable tests green.

Lobby choosers (CI dedicated dropdown; BA AP-mount/glove lists) surface disposables only when the **TacOps
Disposable Infantry Weapons** option is on. BA lobby UI and the handler-routing gate are verified manually
(GUI / integration); the core rules logic is unit-tested.

---

## Official Rulings Reconciliation

Pasted (TO:AR p.106) text is authoritative and overrides the forum; forum rulings fill gaps the pasted text
does not cover. Sources: forum topics 74598, 66887, 56461, 51899 (Xotl, Dominus Erratorum).

**Confirmed already-correct (no change):**

- **3x damage** = three times the disposable weapon's damage-each x troopers who hit; the 3x compensates for
  the normal single-shot /3 conversion. (74598) Matches `InfantryDisposableWeaponHandler`.
- **One single attack per game**, a single volley of all disposables, instead of the standard attack. (74598)
  Enforced by the one-shot `fired` flag + `isOnlyAttack`.
- **Cluster Hits uses the platoon/firing column** (e.g. full 28), and ~40 dmg for 28x VLAW is correct. (56461)
  Matches `Compute.missilesHit(getShootingStrength(), ...)`.
- **All troopers carry the same disposable**, one per trooper, an exception to the 2-support-weapon limit. (74598)
- **Range uses the disposable weapon's own brackets** — already correct because the fired weapon's type is the
  range-determining weapon at `Compute.getInfantryRangeMods` (`Compute.java` ~1730).

**Implemented from rulings (not covered by pasted text):**

- **A 1E disposable weapon encumbers the platoon** like a secondary support weapon, even when firing the normal
  weapon. (51899) -> `Compute.getInfantryRangeMods` now also checks the platoon's disposable weapon for the
  point-blank support-weapon penalty (new `disposableWeapon` param threaded through callers: Compute,
  FireControl, InfantryTROView). Test `InfantryDisposableEncumbranceTest`.
- **Mk. 1 Light AA's anti-air applies only when it makes its single attack**, not permanently. (66887) TW
  ground-to-air already keys off the fired weapon (`ComputeToHitIsImpossible` ~1610); also made the unit-wide
  `Compute` AA-eligibility check ignore a spent (`isFired`) disposable. No Alpha Strike AA is derived from
  `F_INF_AA`, so nothing permanent to remove there.

**Verified — BA AP-attack limits (51899):** no code change needed; MM already enforces all three parts and the
disposable is covered:

- One weapon per AP mount / glove (construction): each mount/glove links exactly one weapon via
  `BaConstructionUtil.mountOnApm`; `TestBattleArmor.isMountLegal` + `getNumAllowedAntiPersonnelWeapons` bound it.
- One AP attack per turn (firing): `ComputeToHitIsImpossible` ~1209 `OnlyOneBAAPAttack` blocks a second
  `F_INFANTRY` weapon attack — i.e. only one AP mount used per turn, gloves included (glove AP weapons are
  `F_INFANTRY`).
- The disposable is `F_INFANTRY` (`InfantryWeapon` base ctor sets it), so it counts as the single AP attack.
- Consistency: the disposable `isOnlyAttack` gate (pasted "instead of the standard attack") is stricter than
  `OnlyOneBAAPAttack` and governs; no conflict. Nuance: `isOnlyAttack` also blocks field-gun + disposable in the
  same turn — defensible under "single attack", easy to relax if a future ruling separates field guns.

**Noted for later phases:**

- **Construction exception** (one disposable per trooper, bypassing the 2-support-weapon cap) is primarily a
  MegaMekLab (Phase 3) validation concern.

---

## Phase 3 — MegaMekLab — DONE (build CI + BA units)

MegaMekLab uses a Gradle composite build (`includeBuild('../megamek')`), so the engine changes are visible
directly. Branch `Implement-disposable-infantry-weapons` in both repos.

**Conventional Infantry build view** (mirrors the Primary/Secondary weapon style):

- `CIWeaponView`: a read-only **Disposable:** field in the left panel (clickable -> opens the weapon table, like
  Primary/Secondary), showing the current disposable or "None".
- `CIEquipmentView`: a **Disposable Weapons** filter category plus **Add Disposable** / **Remove Disposable**
  buttons next to Add Primary/Add Secondary. Add Disposable enables only for a tech-legal `F_INF_DISPOSABLE`
  selection; it calls `ConvInfantry.equipDisposableWeapon(weapon)` (shared field+mount sync) and `refreshAll`,
  which repopulates the left field. Remove Disposable clears it. (`actionPerformed` refactored to extract
  `selectedEquipment`/`addMainWeapon`/`addDisposableWeapon` helpers.)
- The `disposableWeapon` BLK block round-trips via MegaMek's `BLKFile`.
- i18n: `InfantryWeaponView.txtDisposable.text/.tooltip` in `Views.properties`.

**Battle Armor build view:**

- Marking moved to the shared `BaConstructionUtil.mountOnApm` (MegaMek): any AP-mounted/gloved
  `F_INF_DISPOSABLE` weapon is marked disposable, so BOTH the MM lobby and MML's `BABuildView` mounting path
  mark it. Removed the now-redundant marking from the MM-lobby `APWeaponChoice`/`BaManipulatorChoice`.
- `BABuildView`: relaxed the "support weapons only on gloves" restriction for disposables, so disposable
  support weapons (LAW, grenades) may be mounted in a standard AP mount (the construction exception).
- `MiscType` disposable eligibility on the suit is already enforced by `TestBattleArmor`
  (`canCarryDisposableWeapons`), which MML uses for validation.

**Tests:** `BaConstructionUtilDisposableTest` (2) — AP-mounting a `(1-D)` weapon marks it disposable; a normal
AP weapon is not. MML UI is verified manually (GUI). MegaMek disposable suite: 21 tests green.

### MegaMek + MegaMekLab status: COMPLETE for CI + BA. Remaining: Phase 4 (MekHQ campaign).
