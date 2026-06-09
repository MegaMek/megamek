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

### MegaMek status: COMPLETE for CI + BA (engine + client, incl. lobby Configure). 16 disposable tests green.

Note: the lobby chooser only appears when the **TacOps Disposable Infantry Weapons** game option is on.
BA disposable assignment in the lobby still goes through the existing AP-mount/armored-glove weapon
selectors (the disposable flag rides on the weapon type) — a dedicated BA lobby affordance is not yet added.
