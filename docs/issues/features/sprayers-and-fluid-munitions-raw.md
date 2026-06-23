# Functional Sprayers + Full Fluid Munitions (RAW)

**Branch:** `Implement-Sprayers`
**Scope (confirmed):** Both — make Sprayers function as fluid-dispensing weapons AND implement
the full set of fluid munitions/effects across all fluid-dispensing weapons.
**Decisions (locked):** ALL SEVEN fluids including **Inferno Fuel**; built **phase-by-phase with
review at each gate**.
**Primary refs:** TM p.218 (Vehicle Flamer), TM p.248 (Sprayer equipment),
TO:AUE p.124 (Heavy Flamer), TO:AUE p.125 (Fluid Gun), TO pp.361-363 (fluid munition effects).

---

## Current state (as found)

| Weapon | Class | Status |
|--------|-------|--------|
| Vehicle Flamer | `flamers/VehicleFlamerWeapon` -> `ISVehicleFlamer` | Works. Std + Coolant ammo only. |
| Heavy Flamer | `ISHeavyFlamer` / `CLHeavyFlamer` (extend VehicleFlamerWeapon) | Works. Std + Coolant ammo only. |
| Fluid Gun | `other/FluidGunWeapon` -> `ISFluidGun` / `CLFluidGun` | Works. Coolant handler only; BF damage TODO. |
| Sprayer (Mek/Vee) | `MiscType` `createMekSprayer()` / `createTankSprayer()` (`F_SPRAYER`) | **Inert** - no firing, no handler, no ammo. |

**Munition flags already declared** (`AmmoType.Munitions`): `M_WATER`, `M_PAINT_OBSCURANT`,
`M_OIL_SLICK`, `M_ANTI_FLAME_FOAM`, `M_CORROSIVE`, `M_COOLANT`.
Only `M_COOLANT` is wired up (mutator + handler). The rest are flag-only:
- `AmmoType.java:2094` `// TODO: implement all of these except coolant`
- `AmmoType.java:3730` `// TODO: Need Corrosive, Flame-Retardant, Oil Slick, Paint and Water Ammo's`

**Helpful existing infrastructure:**
- `ISFireExtinguisher extends Weapon` - precedent for a range-1, damage-0 utility weapon (`F_SOLO_ATTACK`,
  hex-extinguish targeting). Good template for the Sprayer weapon.
- `VehicleFlamerCoolHandler` / `FluidGunCoolHandler` - precedent for fluid-effect handlers.
- Princess `FireControl.getHardTargetAmmo()` already **excludes** all five fluid munitions from
  damage-ammo selection - bot will not fire them as damage. No regression risk there.
- `Terrains`: FIRE, SMOKE exist; **no OIL_SLICK terrain** exists yet.

---

## Confirmed RAW - General fluid ammunition rules (from rulebook, per user)

- **Eligible weapons:** Vehicle Flamer (TM p.218), Heavy Flamer (TO:AUE p.124), Fluid Gun
  (TO:AUE p.125), Sprayer (TM pp.248-249). Only these may use special fluid ammunitions.
- **Compatibility:** every fluid type is available to **ALL four** weapons **by default**.
  Restrictions exist **only** where an ammo type is **bracketed** to specific weapons.
  (Corrects earlier guess - flamers are NOT limited to water+coolant.)
- **Sprayer ammo:** the Sprayer uses **Fluid Gun ammunition**, but a full ton yields **half the
  shots** it would for a Fluid Gun.
- **Loadout:** all Fluid Gun / Flamer ammunition installed in **full-ton lots**.
- **Pre-game designation:** fluid types chosen before play begins.
- **Defaults when unspecified:** Fluid Gun / Sprayer -> **Water Ammo**; Flamer -> **standard
  flamer ammo**.

### Implementation implications / still-open
- Apply each fluid `MunitionMutator` to **all** eligible ammo bases (vehicle flamer, heavy flamer,
  fluid gun) unless a bracket restricts it. Sprayer shares the Fluid Gun ammo type.
- "Half shots for Sprayer" - model as the Sprayer consuming ammo at 2x, or via a sprayer-specific
  shot count. TBD which mechanism fits MegaMek's per-bin ammo model.
- Default = Water: confirm whether the existing base "Fluid Gun Ammo" should BE water, or water is
  a separate munition that becomes the default selection.

## Confirmed RAW - per-weapon rules (from rulebook, per user)

### Fluid Gun (Advanced; BM/IM/CV/SV/AF/CF/SC/DS/MS; Both B/B-B-B)
- Ballistic, short-range; sprays liquid ammo. May hold multiple fluid bins (1-ton each) but only
  **one type may be fired per turn**.
- **Ammo crit (any fluid except water)** -> 2-point ammo explosion (regardless of shots remaining)
  **plus** the special effect of a single hit by that fluid in the affected location.
  **CASE II** reduces the explosion to 1 point but does **not** negate the special effect.
- **Cannot** fire underwater or in vacuum.
- **Cannot** target airborne units (fighters, Small Craft, DropShips, VTOLs, Airship/Fixed-Wing/
  Satellite Support Vehicles).
- Ammo full-ton lots, no mixing within a ton. Default if unspecified = **Water**.

### Heavy Flamer (Advanced; BA/PM/BM/IM/CV/SV/MS; Both C/X-X-E)
- **Not usable in vacuum (including space).**
- vs non-infantry: **2x the damage and heat** of a standard flamer.
- vs infantry: **6D6** burst (**4D6** for the BA-grade Heavy Flamer).
- Still functions if the **vehicle engine is critically hit**.
- **Ammo crit -> 5 points per unfired shot** (volatile fuel + high pressure).
- (Code already does 6D6 infantry burst + double damage; verify vacuum gate + ammo-crit value.)

### Sprayer (IS & Clan)
- "Fires" as a weapon **but does NOT count as a weapon and needs no gunner** (construction/crew).
- **No targeting computer / no FCS benefit** -> all attacks at **+2 to-hit** (as if no fire control).
- Direct damage to **conventional infantry only**, equal to a **BA flamer at range 1** (3D6, TW p.217).
- Uses **Fluid Gun ammunition** at **half shots/ton**; requires **dedicated liquid cargo storage**
  (Transport Bay) to function.
- Won't function if **engine shut down or crew/pilot incapacitated**.
- **Cannot** be mounted on ProtoMeks or aerospace units. IndustrialMech sprayer weighs **0.5 t** (vs 15 kg).

### Implementation notes raised by these rules
- **Sprayer is the tricky one:** it must be fireable in the weapon phase yet not count as a weapon
  (no gunner, ignored by weapon-count construction checks) and must validate liquid cargo + arc/location.
  Likely a `Weapon` subclass with special flags + a construction/crew gate, plus the +2 no-FCS modifier.
- **New to-hit-impossible gates:** Fluid Gun (underwater, vacuum, airborne target); Heavy Flamer (vacuum).
- **Ammo-crit damage differs per weapon:** Fluid Gun 2 pts (1 with CASE II) + fluid effect, water = no
  explosion; Heavy Flamer 5 pts/unfired shot. Verify/centralize where ammo-explosion damage is computed.
- Sprayer engine-off / crew-incapacitated functional gate.

## Confirmed RAW - fluid ammunition table (TO:AUE 6th printing, pp.172-175)

Bracket = which weapons may load it. Default ammo: Fluid Gun/Sprayer = **Water**; Flamer = standard fuel.

### Weapon -> allowed fluids (derived from brackets)
| Weapon | Coolant | Water | Inferno Fuel | Corrosive | Flame-Ret. Foam | Oil Slick | Paint/Obscurant |
|--------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Vehicle Flamer | Y | Y | Y | - | - | - | - |
| Heavy Flamer | Y | Y | Y | - | - | - | - |
| Fluid Gun | Y | Y | Y | Y | Y | Y | Y |
| Sprayer | Y | Y | **N** | Y | Y | Y | Y |

> **Inferno Fuel is a NEW 7th fluid** not in the original flag list - no `M_INFERNO_FUEL` exists yet
> (there is `M_INFERNO` for SRMs). Bracket is [Flamers/Fluid Guns] - **Sprayers excluded.**

### COOLANT [Flamers/Fluid Guns/Sprayers] (C / B-B-B) - already implemented, verify vs RAW
- Burning hex/structure/unit: douses **non-Inferno** fires on **2D6 4+**; Inferno fires on **12**.
- Heat-tracking target: **-3 heat/hit** (**-4** from Heavy Flamer), max **9** cooling/turn.
- vs conv infantry: 1-pt direct-fire ballistic.
- Won't cook off from overheating; crit explosion: **-3 heat + 2 internal**.
- NOTE: current `VehicleFlamerCoolHandler` cools Meks by `(nDamPerHit*hits)+1` - looks non-RAW; verify.

### WATER [Flamers/Fluid Guns/Sprayers] (A / A-A-A) - default for Fluid Gun/Sprayer
- Burning hex/structure/unit: douses **non-Inferno** fires on **2D6 3+**; Inferno on **12**.
- vs conv infantry: **1D6/2 (round up)** burst-fire; casualties **knocked out, not killed**.
- Heat-tracking target: **-1 heat/hit**, max **6** cooling/turn.
- Can **wash off Paint/Obscurant** on **2D6 9+** after a successful hit on a painted unit.
- **Never explodes** (overheating or crit); ammo-slot crit does not "flood".

### CORROSIVE [Fluid Guns/Sprayers] (C / C-D-D)
- vs any non-conv-infantry target: **1D6** in Weapon Phase + **1D6/2 (round up)** in **End Phase same turn**;
  resolved in **1-pt clusters**.
- vs conv infantry: **1D6 burst-fire**.
- Won't cook off; crit explosion: **1D6 to internal structure + 2 internal**, then **End Phase of the
  FOLLOWING turn**: another **1D6/2 (round up)** internal.

### FLAME-RETARDANT FOAM [Fluid Guns/Sprayers] (B / B-B-B) = `M_ANTI_FLAME_FOAM`
- Burning hex/structure/unit: douses **ALL** fires immediately, **including Inferno**.
- Ignition rolls vs a foam-struck hex/unit get **+4 TN** (base TN 0 even for auto-igniters).
- Won't cook off; crit explosion: **-2 heat + 2 internal**.

### OIL SLICK [Fluid Guns/Sprayers] (B / B-B-B)
- No damage. **-2 to ignition TN** for a doused unit/hex (oil is flammable -> easier to ignite).
- On a **clear hex**: ground units other than infantry/hovercraft/WiGE must make **PSR at +1** to avoid
  skid when passing through (regardless of walk/run MP). **Paved/bridge hex: +1 more** (total +2).
- Cooks off from overheating **as normal**; crit explosion: **1 pt per remaining shot + 2 internal**.

### PAINT/OBSCURANT [Fluid Guns/Sprayers] (B / B-B-B)
- Successful hit vs **non-infantry**: roll **2D6**; on **9+**, +1 to-hit to **all weapon attacks by the
  target unit** for the **rest of the scenario** (unless washed off by water). Stacks to **max +3**.
- vs infantry: **no effect**.
- Won't cook off; crit explosion: **1 pt per remaining shot + 2 internal**.

### INFERNO FUEL [Flamers/Fluid Guns] (D / D-E-D) - NEW; NOT for Sprayers
- Each hit from std Vehicle Flamer / Fluid Gun = **1 Inferno SRM**; Heavy Flamer = **2 Inferno SRM**
  (same location).
- Heat-tracking carriers check heat-induced cook-off **as if carrying Inferno SRMs**.
- Support Vehicles without Armored Chassis / Environmental Sealing: external heat damage or transport-bay
  crit -> Inferno Fuel explosion on **2D6 10+**.

### Cross-cutting crit/explosion model (Fluid Gun chassis)
- Base Fluid Gun ammo explosion = **2 pts** (CASE II -> **1 pt**), water exempt, **plus** each fluid's
  own crit effect above. Heavy Flamer ammo = **5 pts/unfired shot** (separate chassis rule).

## Open design decisions (need confirmation before coding)

1. **Sprayer as a weapon** - keep the `MiscType` (construction/unit-file compat via internal names
   `MechSprayer` / `Tank Sprayer`) and add a parallel `SprayerWeapon`? Or convert outright?
   Proposed: **add a weapon, keep MiscType compat** (mirror the FireExtinguisher pattern).
   Sprayer range = 1 (adjacent).
3. **Per-fluid effects** (proposed RAW, confirm each):
   - **Water** - firefighting; extinguishes fires/infernos like coolant but does NOT cool Meks.
   - **Corrosive (acid)** - armor damage (confirm: amount, over-time vs one-shot, vs internal).
   - **Paint/Obscurant** - marks target unit; to-hit penalty on the painted unit's own attacks
     and/or easier to hit; persists until cleaned. Confirm magnitude/duration.
   - **Oil Slick** - lays slick terrain in target hex; entering units roll PSR (skid/fall). New terrain type.
   - **Anti-Flame Foam (flame-retardant)** - coats hex/unit; prevents ignition / extinguishes;
     confirm duration and whether it targets hex, unit, or both.

---

## Proposed implementation phases

**Phase 1 - Munition definitions.** Add `MunitionMutator`s for water/corrosive/paint/oil-slick/
anti-flame-foam to the correct ammo bases (per matrix above); wire into the `createMunitions(...)`
calls. Set BV/cost/availability per RAW. Replace the two TODO comments.

**Phase 2 - Sprayer weapon.** New `SprayerWeapon` (+ Mek/Vee internal-name variants) following the
FireExtinguisher template; keep the `MiscType` for construction. Range 1, uses fluid ammo.
Wire `getCorrectHandler` to fluid-effect handlers.

**Phase 3 - Effect handlers.** One focused handler per fluid effect (extending `AmmoWeaponHandler`),
reusing the cool-handler precedents. New `OIL_SLICK` terrain + skid/PSR hook. Obscurant status on
`Entity`. Anti-flame-foam fire-suppression hook. Keep `TWGameManager` thin - any server-side
resolution goes in an `AbstractTWRuleHandler` helper.

**Phase 4 - Targeting/UI/Princess.** Firing-display + `ComputeToHit`/`ComputeToHitIsImpossible`
support for hex-target fluids (oil slick, foam) and unit-target fluids (paint). Confirm Princess
does not mis-fire utility fluids.

**Phase 5 - Tests + i18n + logging.** Serialization round-trip for any new `Entity`/mount state
(obscurant status); rule tests per fluid effect; `messages.properties` for all new strings;
`[Sprayer]`/`[Fluid]` diagnostic logging on each gate.

---

## PHASE 3 - Fluid effect handlers (sub-plan)

Each weapon's `getCorrectHandler` dispatches by munition type to a shared fluid-effect handler.
Proposed sub-steps (each compiled + tested + gated):
- **3a - Fire-suppression + heat/infantry (no persistent state):** central dispatch routing for all
  three weapon families; Water (extinguish 3+/12, conv-inf 1D6/2 round-up KO, heat -1 max 6); Coolant
  brought to RAW (-3/-4 heat, max 9; extinguish 4+/12); Flame-Retardant Foam *extinguish-all* part;
  generalize fire-suppression eligibility (`flamerInCoolMode` -> "fire-suppressant fluid": water,
  coolant, foam).
- **3b - Corrosive:** weapon-phase 1D6 + End-Phase 1D6/2 round-up (1-pt clusters); conv-inf 1D6 burst.
  Needs End-Phase scheduling.
- **3c - Inferno Fuel:** each hit = 1 Inferno SRM (Heavy Flamer = 2); heat cook-off; SV explosion.
- **3d - Persistent-state effects:** Paint/Obscurant (NEW serializable Entity state, +1..+3 to-hit,
  wash off by water); Oil Slick (slick hex state/terrain, skid PSR, ignition -2); Foam +4 ignition-TN
  persistent; Water wash-off-paint (depends on Paint state).
- **3e - Ammo-crit effects:** Fluid Gun 2 pts (1 with CASE II) + per-fluid crit; Heavy Flamer 5/unfired
  shot; water never explodes.

### RAW resolutions (CONFIRMED)
- **Ammo governs infantry damage.** Each fluid's own conv-infantry rule applies for all three weapon
  families; the Sprayer's BA-flamer 3D6 is just its base class and is overridden by the loaded fluid.
  (Phase 2 `SprayerHandler` 3D6 will be reworked to defer to the fluid handler.)
- **"1D6 / 2 (round up)"** = `ceil(1D6 / 2)` (1-3). Applies to Water vs infantry and Corrosive End-Phase.
- **Water KO tracking (NEW REQUIREMENT):** infantry casualties from Water Ammo are "knocked out, not
  killed" and must be tracked so MekHQ can recover those troopers. Needs a KO/non-lethal-casualty
  mechanism in MegaMek (investigate infantry casualty tracking).

## REFACTORS (2026-06-22) - keeping TWGameManager thin
- **Cook-off knowledge -> `AmmoType.isHeatStableFluid()`**: the ~10-line fluid-munition test in the
  TWGameManager heat-cook-off loop is now a one-line call to a domain method on `AmmoType`.
- **Ignition modifier -> `Board.getFluidIgnitionModifier(Coords)`**: the foam +4 / oil -2 block in
  `tryIgniteHex` now delegates to `Board` (co-located with the foam/oil hex state).
- `resolveCorrosiveDamage()` is already a thin delegator to `CorrosiveDamageHandler`
  (`AbstractTWRuleHandler`) - no change needed.
- **Dispatch centralized + coolant bug fixed**: new `FluidMunitionHandlers.forFluidGunOrSprayer(...)`
  maps the shared fluids (Coolant->FluidGunCoolHandler, Water, Corrosive, Foam, Oil Slick,
  Paint/Obscurant) once; `FluidGunWeapon` and `SprayerWeapon` both call it (FG adds Inferno Fuel +
  default; Sprayer falls back to SprayerHandler). This removed the duplicated if-chains AND fixed the
  Sprayer-Coolant gap (a Sprayer firing Coolant now routes to `FluidGunCoolHandler`). The flamer keeps
  its own dispatch (different coolant handler `VehicleFlamerCoolHandler`, no corrosive/foam/oil/paint).
  Weapons + equipment + board + units + server suites green; dispatch is behavior-preserving (existing
  fluid tests cover the handlers). NOTE: handler-selection isn't unit-tested (handler ctors need a fully
  wired attack context); verified by regression + inspection.

## COOLANT HANDLER AUDIT (2026-06-22) - done + fixed
Audited `VehicleFlamerCoolHandler` and `FluidGunCoolHandler` vs RAW (TO:AUE p.173). Findings & fixes:
- **BUG FIXED - Heavy Flamer cooling**: `VehicleFlamerCoolHandler` cooled by `(nDamPerHit*hits)+1`
  (= 5 for a Heavy Flamer, scaled wrong with hits). Now a flat **3** (Vehicle Flamer) / **4** (Heavy
  Flamer) via testable static `coolingPointsFor(WeaponType)`. (FluidGunCoolHandler was already a correct
  flat 3 - a Fluid Gun/Sprayer is never a Heavy Flamer.)
- **BUG FIXED - "any heat-tracking target"**: both handlers cooled only `instanceof Mek`; RAW says any
  heat-tracking target. Now use `entityTarget.tracksHeat()`, so aerospace are cooled too. Verified
  `HeatResolver.resolveAeroHeat` honours `coolFromExternal` (so this actually takes effect).
- **CONFIRMED CORRECT (no change)**: conv-infantry 1-pt direct-fire; non-Inferno fire 4+, Inferno 12;
  the **max-9-cooling/turn cap is already enforced** by `HeatResolver` (`Math.min(9, coolFromExternal)`),
  not the handler's job.
- Modernized fragile `((Tank) target)` casts to `instanceof` pattern binding; removed a redundant
  extinguish loop. `VehicleFlamerCoolHandlerTest` (cooling 3/4) added; also fixed latent ordering flake
  in `InfernoFuelHandlerTest` (added `@BeforeAll initializeTypes`). weapons + server suites green.
- STILL DEFERRED (not a coolant-specific bug): hex/structure fire extinguishing (the shared empty-hex
  targeting item).

## EMPTY-HEX TARGETING (in progress) - decisions
- **To-hit (per user):** Foam = **instant** (auto-extinguish, no roll). Water/Coolant hex-extinguish
  use the **Fire Extinguisher mechanics (fixed TN 8)**, then the RAW extinguish roll (Water 3+, Coolant
  4+, Inferno 12). Oil Slick coating of a hex = a normal hit (no water-based firefighting rule found).
- **Scope (per user):** includes oil/foam coating of **empty (non-burning) hexes** -> needs a new
  hex target type + firing-display action. Sequenced: **Step 1 = hex fire-extinguish** (reuse existing
  Fire Extinguish button, generalized to fluid weapons); **Step 2 = empty-hex oil/foam coating** (new
  target type + UI).

## EMPTY-HEX TARGETING - Step 1 (hex fire-extinguish): DONE + tested (2026-06-22)
- New `AmmoType.isFireSuppressantFluid()` (Water/Coolant/Foam); refactored the inline
  `fireSuppressantFluidAmmo` check in `ComputeToHitIsImpossible` to use it.
- `ComputeToHit`: the fire-extinguisher TN-8 special-resolution branch now also fires for a
  fire-suppressant fluid targeting `TYPE_HEX_EXTINGUISH` (water/coolant/foam use the same firefighting
  to-hit; foam then auto-extinguishes, water/coolant roll).
- New shared `FluidFireSuppression.extinguishHex(...)` (roll 2D6, douse ordinary fire on Water 3+/
  Coolant 4+, Inferno 12; remove FIRE terrain + inferno/flamer markers + broadcast). `WaterHandler` and
  both coolant handlers override `specialResolution` for `TYPE_HEX_EXTINGUISH`; Foam already auto-douses.
- `FiringDisplay`: the **Fire Extinguish** command is generalized - `findFireSuppressantWeapon` picks a
  Fire Extinguisher (preferred) or a ready Flamer/Fluid Gun/Sprayer loaded with water/coolant/foam; the
  reach now uses the weapon's range (Fluid Gun up to 3, Sprayer/Extinguisher 1). Report 3389 added.
- `FluidAmmoPredicatesTest` (isFireSuppressantFluid / isHeatStableFluid) passes; actions/weapons/
  equipment/server suites green. GUI verified by compile; full firefight flow needs manual playtest.
## EMPTY-HEX TARGETING - Step 2 (oil/foam on a bare hex): DONE (2026-06-22)
- New `Targetable.TYPE_HEX_FLUID = 21`.
- `FiringDisplay.chooseTarget`: reused the existing "auto-target hexes for certain ammo" block (no new
  button) - **Oil Slick** always targets the clicked hex; **Foam** targets the hex only when no enemy
  unit is there (so foam-on-unit still works) -> `HexTarget(TYPE_HEX_FLUID)`.
- Handlers resolve `TYPE_HEX_FLUID` via `specialResolution` (short-circuits the cluster loop, which has
  no HEX_FLUID case): `OilSlickHandler` coats the hex; `FoamHandler` douses any fire there + coats it
  (its `extinguishHex` now no-ops on a non-burning hex instead of mis-reporting).
- To-hit uses the **normal attack path** (mirrors `TYPE_HEX_CLEAR`, which smoke/mine LRMs already use),
  so oil "applied through a successful attack" gets real range/movement mods. No special to-hit added.
- Compiles; actions/weapons/board/equipment suites green + checkstyle. GUI + to-hit need a manual
  playtest (not unit-testable here). MINOR gap: foaming a bare hex that DOES have an enemy unit on it
  is not directly reachable (you'd target the unit); negligible.

## 3e EXPLOSION SIDE-EFFECTS: DONE + tested (2026-06-22)
- New `FluidAmmoExplosion.applyCriticalEffects(Entity, AmmoType, baseDamage)` (keeps `explodeEquipment`
  thin): **Coolant** -> -3 heat, explode for 2; **Foam** -> -2 heat, explode for 2; **Corrosive** ->
  base + **1D6** internal now and queues **1D6/2** (round up) for the FOLLOWING turn; other fluids keep
  base damage. Replaced the old inline coolant block in `TWGameManager.explodeEquipment` (which only
  covered flamer coolant) with a one-line call - now also covers Fluid Gun coolant + foam + corrosive.
- New serializable `Entity.nextTurnCorrosiveDamage` (+accessors + `promoteNextTurnCorrosiveDamage()`).
  `CorrosiveDamageHandler` now applies this-turn pending corrosive, then **promotes** next-turn corrosive
  into pending so it lands one End Phase later (the "following turn" rule).
- `FluidAmmoExplosionEffectsTest` (5: coolant/foam/corrosive amounts + promote) passes; server/units/
  equipment suites green.

## 3e INFERNO FUEL COOK-OFF + SV EXPLOSION: DONE + tested (2026-06-23)
- **Heat-induced cook-off "as if carrying Inferno SRMs" (TO:AUE p.173):** new predicate
  `AmmoType.isInfernoFuel()` (fluid-gun/vehicle-flamer/heavy-flamer + `M_INFERNO_FUEL`).
  `Entity.hasInfernoAmmo()` now returns true when a loaded Inferno Fuel bin is aboard, so the carrier
  uses the lower safe-heat thresholds (9 vs 13) and rolls the inferno heat-explosion (heat >= 10, 2D6 vs
  escalating TN) in `HeatResolver` exactly like Inferno SRM ammo. `explodeInfernoAmmoFromHeat`'s
  `isInfernoType` gate also accepts `isInfernoFuel()` so the cook-off picks the Inferno Fuel bin.
  Inferno Fuel still also cooks off via the normal `explodeAmmoFromHeat` (it is NOT heat-stable) - same
  dual-path behavior as real Inferno SRMs.
- **Support Vehicle explosion (TO:AUE p.173):** new `SupportVehicleFluidExplosionHandler extends
  AbstractTWRuleHandler`. An **unsealed** Support Vehicle (lacks BOTH Armored Chassis AND Environmental
  Sealing) carrying loaded Inferno Fuel rolls **2D6; on 10+** the fuel detonates (via
  `gameManager.explodeEquipment`). `TWGameManager.checkSupportVehicleInfernoFuelCookOff(entity, trigger)`
  is a one-line delegator; wired at the two RAW triggers: the inferno-vs-tank/SV **external heat damage**
  path and the **`Tank.CRIT_CARGO`** (transport-bay) critical. Report 3381 (reuses 5041/5042
  avoid/fail sub-messages). Per-condition `[InfernoFuel]` diagnostic logging on the protected /
  detonate paths.
- Tests: `FluidAmmoPredicatesTest` +`onlyInfernoFuelIsTreatedAsInfernoForCookOff`;
  `SupportVehicleFluidExplosionHandlerTest` (4: non-SV skipped, no-fuel skipped, sealed-SV protected,
  unsealed+fuel always rolls). Handler/sprayer/equipment/server suites all green; `compileJava` clean.
- This completes all of Phase 3e. Only remaining work on the feature is end-to-end **manual playtest**.

## OIL SLICK / FOAM HEX RENDERING (tileset-native): DONE + tested (2026-06-23)
- User supplied two black-background splatter photos; refined to **84x72 RGBA, transparent background**
  (border-connected flood-fill removal preserves interior dark detail), cropped/centered to hex size,
  saved as `mm-data/data/images/hexes/transparent/oil_slick.png` and `.../fire_foam.png`
  (alongside inferno/ice/smoke). Refine script kept in scratchpad (`refine_hex.py`).
- **Decision (user): tileset-native rendering via real terrain types** (not BoardView sprites). Added
  `Terrains.OIL_SLICK = 61` and `Terrains.FLAME_RETARDANT_FOAM = 62` (names "oil_slick" /
  "flame_retardant_foam"), both in `Terrains.AUTOMATIC` (editor-hidden, excluded from .board text, but
  still Java-serialized with saves/network) - same pattern as `BRIDGE_REPAIRED`. Added `getDisplayName`
  cases ("Oil Slick" / "Flame-Retardant Foam") so hex tooltips label them.
- **Board storage migrated from `Set<Coords>` to hex terrain.** `Board.markOilSlick/removeOilSlick/
  isOilSlick` (+ foam equivalents) now add/remove/check `Terrains.OIL_SLICK` / `FLAME_RETARDANT_FOAM` on
  the hex; the two old `Set<Coords>` fields are gone. Public API unchanged so all callers (`Entity`
  skid check, `getFluidIgnitionModifier`) are unaffected. **Bonus:** state now syncs to clients and
  survives saves for free (the Set approach did neither).
- Handlers `OilSlickHandler.applyOilSlick` / `FoamHandler.applyFoam` now call
  `gameManager.sendChangedHex(coords, boardId)` so clients render the new overlay.
- **Tileset:** new shared include `StandardIncludes/StandardFluidCoatings.tileinc` with
  `super * "oil_slick:1" "" "transparent/oil_slick.png"` + the foam line; `include`d into all 9 ground
  tilesets (after their StandardHazardousLiquid include). HexTileset super-match is generic by terrain
  type/level, so the overlay draws whenever a hex has the terrain.
- Tests: `OilSlickBoardTest` / `FlameRetardantFoamBoardTest` rebuilt on hex-backed boards (now assert
  the terrain is present/cleared + the terrain name matches the tileset entry). Board/server/handler/
  equipment suites green; `compileJava` clean.
- NOTE: the user's original oversized source PNGs (`hexes/OIl Slick.png`, `hexes/Fire Foam.png`) are
  left in place; they are not used at runtime and can be deleted.

## UNIT-LEVEL FLUID COATINGS (final RAW nuance): DONE + tested (2026-06-23)
Closes the last open RAW item - oil/foam "-2/+4 ignition vs a doused **unit**" (TO:AUE pp.173-174),
distinct from the hex coating which was already wired.
- **Investigation first:** MM has no unit-*ignition* roll (units catch fire via auto-applying infernos /
  `doFlamingDamage` survival rolls, not TN ignition). The only ignition-roll mechanic is `tryIgniteHex`.
  So the RAW "ignition vs a coated unit" maps to: a coated unit modifies ignition rolls against whatever
  hex it stands in. That is a real consumer (not dead code), so the coating is implemented as unit state.
- New `megamek.common.units.FluidCoating` enum (NONE / OIL_SLICK -2 / FLAME_RETARDANT_FOAM +4) with
  `ignitionModifier()`. New serializable `Entity.fluidCoating` (+ `getFluidCoating`/`setFluidCoating`/
  `getFluidCoatingIgnitionModifier`, null-guarded for old saves).
- **Targeting split to match RAW:** firing oil/foam at a **unit** now coats the *unit*
  (`OilSlickHandler.handleEntityDamage` / `FoamHandler` entity path -> `setFluidCoating`, reports 3382 /
  3404), so the modifier travels with it; firing at a **hex** still coats the hex (skid + ignition). Foam
  at a unit still douses its fires first. (Previously firing at a unit oiled/foamed the *hex* - the new
  split is more faithful and avoids hex+unit double-counting.)
- `TWGameManager.tryIgniteHex` now calls a new private helper `fluidIgnitionModifier(c, boardId)` that
  combines the hex coating + any coated unit in the hex, **foam (+4) dominant** over oil (-2), no
  stacking (replaces the old inline hex-only block - keeps the method from growing).
- Tests: `FluidCoatingTest` (enum values, default NONE, replace-last, serialization round-trip).
  Handler/sprayer/server/units/board/equipment suites all green; `compileJava` clean.

## FEATURE COMPLETE (2026-06-23)
All RAW is implemented. The three previously-flagged nuances are resolved:
1. **Heavy Flamer Inferno "2 missiles same location"** - already faithful: `InfernoFuelHandler` delivers 2
   inferno missiles; MM applies inferno heat unit-wide so "same location" is moot (no work needed).
2. **Burning-oil consumption** - NOT a RAW rule (oil only gives -2 ignition; nothing about being consumed
   when it burns), so deliberately NOT implemented (RAW-only).
3. **Unit-level ignition coatings** - implemented above.
Only remaining work is end-to-end **manual playtest** + the mm-data classpath check for the new overlays.

## DECISIONS (locked)
- **Sprayer: convert MiscType -> WeaponType** (`SprayerWeapon` extends `AmmoWeapon`, fluid-gun ammo),
  keep internal names `MechSprayer` / `Tank Sprayer`. No canonical units mount a sprayer, so compat
  risk is low; possible minor MML categorization follow-up. Remove the old `MiscType` sprayers.

## STATUS
- **Phase 1 (Munition definitions): DONE + tested** (2026-06-22). `AmmoType.java` updated; new
  `FluidMunitionTest` (4 tests) passes; full `compileJava` clean. Effects are NOT wired yet (Phase 3).
- **Phase 3a (Water + KO tracking): DONE + tested** (2026-06-22). New `WaterHandler` (conv-inf
  ceil(1D6/2) burst KO damage, unit-fire douse 3+/12, heat -1); `M_WATER` routing in Fluid Gun /
  Vehicle Flamer / Sprayer `getCorrectHandler` (Sprayer default water now governs infantry damage,
  per "ammo governs"); generalized fire-suppression eligibility (`flamerInCoolMode` ->
  `fireSuppressantFluidAmmo` = coolant/water/foam). New serializable `Infantry.knockedOutTroopers`
  (+accessors) for recoverable casualties; `InfantryKnockedOutTroopersTest` (accessor + serialization
  round-trip) passes. DEFERRED in 3a: hex-terrain fire extinguishing for fluids (murky path, even
  coolant only douses unit infernos today - own step); Water wash-off-paint (needs Paint state, 3d);
  handler-level KO-via-real-damage integration test (mechanism in place; needs playtest/integration
  harness). Sprayer/equipment/firefighting/flamer suites still green.
- **Phase 3b (Corrosive): DONE + tested** (2026-06-22). New `CorrosiveHandler` (non-infantry: 1D6
  immediate in 1-point clusters via calcHits=d6/nCluster=1/dmg=1, + queues ceil(1D6/2) End-Phase
  damage on a hit; conv-inf: 1D6 burst). New serializable `Entity.pendingCorrosiveDamage` (+accessors).
  End-Phase pass `CorrosiveDamageHandler extends AbstractTWRuleHandler` applies queued damage in 1-pt
  clusters (random hit locations) and clears it; `TWGameManager.resolveCorrosiveDamage()` thin delegator;
  hooked in `TWPhaseEndManager` END case before dead-entity removal. Routing `M_CORROSIVE` ->
  `CorrosiveHandler` in Fluid Gun + Sprayer (not flamers). Reports 3391/3392 added. `CorrosivePendingDamageTest`
  (accessor + serialization round-trip) passes; server/sprayer/KO suites green. NOTE: ammo-crit corrosive
  (crit explosion 1D6 internal + following-turn 1D6/2) is part of Phase 3e. End-Phase damage uses
  SIDE_FRONT for the incidental hit; integration test of the full fire-to-end-phase flow is manual.
- **Phase 3c (Inferno Fuel): DONE + tested** (2026-06-22). New `InfernoFuelHandler` delivers
  `gameManager.deliverInfernoMissiles(...)` per hit - **1** missile for a Vehicle Flamer / Fluid Gun,
  **2** for a Heavy Flamer (`infernoMissilesFor(WeaponType)`); works for entity and hex-ignite targets.
  Routing `M_INFERNO_FUEL` -> `InfernoFuelHandler` in Vehicle Flamer + Heavy Flamer (VehicleFlamerWeapon)
  and Fluid Gun; **Sprayers gated** out via a to-hit-impossible check (they share Fluid Gun ammo but
  RAW forbids Inferno Fuel) + message `SprayerNoInfernoFuel`. `InfernoFuelHandlerTest` (count rule)
  passes; actions/sprayer/server suites green. NOTE deferred to 3e (ammo state/crits): heat-induced
  cook-off "as if carrying Inferno SRMs" and the Support-Vehicle (no Armored Chassis / Env. Sealing)
  external-heat explosion on 10+. "2 missiles to the same location" is approximated (inferno heat is
  unit-wide); full fired-attack integration is manual.
- **Phase 3d - Paint/Obscurant: DONE + tested** (2026-06-22). New `PaintObscurantHandler` (vs
  non-infantry: 2D6 9+ fouls sensors; no damage; no effect vs infantry). New serializable
  `Entity.obscurantToHitPenalty` (0-3, `MAX_OBSCURANT_PENALTY`, +accessors). A fouled unit's own attacks
  take +penalty via `ComputeAttackerToHitMods` (message `Obscurant`). **Water wash-off implemented**
  (closes the 3a-deferred item): `WaterHandler` clears the penalty on a 2D6 9+. Routing
  `M_PAINT_OBSCURANT` -> `PaintObscurantHandler` in Fluid Gun + Sprayer. Reports 3393/3394/3396/3398/3399.
  `ObscurantToHitPenaltyTest` (cap + serialization round-trip) passes; actions/sprayer suites green.
  REMAINING in 3d: **Oil Slick** (new slick terrain + skid PSR + ignition -2) and **Flame-Retardant Foam**
  (extinguish-all incl. Inferno + persistent +4 ignition TN). Foam's eligibility is already wired (3a);
  its handler is not.
- **Phase 3d - Oil Slick: core DONE + tested** (2026-06-22). New board state
  `Board.oilSlickHexes` (+ `markOilSlick`/`removeOilSlick`/`isOilSlick`, mirrors `flamerStartedFires`).
  New `OilSlickHandler` (0 damage; douses the struck hex). Skid integrated in `Entity.checkSkid`: a
  ground unit (not infantry/hover/WiGE) entering an oiled hex makes a skid PSR at **+1** (**+2** on
  pavement/road/bridge), regardless of speed (new `entersOilSlick` helper). Routing `M_OIL_SLICK` ->
  `OilSlickHandler` in Fluid Gun + Sprayer. Report 3387. `OilSlickBoardTest` passes; movement/skid/units
  suites green. SCOPED/deferred: oil lands on the **impact hex** (firing at a unit oils its hex) because
  there is no "apply-to-empty-hex" target type yet (shared with the deferred hex-fire-suppression
  targeting); **ignition -2** for oiled hex/unit not yet wired (needs a `tryIgniteHex` hook + unit-oil
  state); `checkSkid` oil branch verified by regression + review (full move-path integration is manual).
- **Phase 3d - Flame-Retardant Foam: DONE + tested** (2026-06-22). New `FoamHandler` overrides
  `specialResolution` (mirrors FireExtinguisher): douses ALL fires immediately incl. Inferno - hex
  (remove FIRE terrain + inferno + flamer-fire markers) and unit (clear infernos + Tank extinguish) -
  then coats the struck hex. New board state `Board.flameRetardantFoamHexes` (+mark/remove/is). New
  ignition hook in `TWGameManager.tryIgniteHex`: foamed hex **+4** to ignite, oiled hex **-2** - this
  ALSO completes Oil Slick's ignition -2. Routing `M_ANTI_FLAME_FOAM` -> `FoamHandler` in Fluid Gun +
  Sprayer. Report 3388. `FlameRetardantFoamBoardTest` passes; server/weapons suites green.
  DEFERRED: unit-level +4 ignition (only hex +4 wired; unit ignition is the inferno path); hex
  foam/extinguish via UI needs the empty-hex-targeting work (shared deferred item); tryIgniteHex
  modifier verified by regression + review.
- **Phase 3e - Ammo-crit (core): DONE + tested** (2026-06-22). `AmmoMounted.getExplosionDamage`:
  **Water -> 0** (never explodes), **Fluid Gun -> flat 2** (Oil Slick / Paint add +1 per unfired shot),
  **Heavy Flamer -> 5 x unfired shots** (replaces the wrong damagePerShot*rack*shots ~40). Heat cook-off
  exemption in `TWGameManager` generalized: Coolant/Water/Corrosive/Foam/Paint fluid ammo do NOT cook off
  from a unit's own heat; Oil Slick and Inferno Fuel still do. `FluidAmmoExplosionTest` (4) passes;
  server/equipment suites green. (Fluid variants stay explosive via default; water base is non-explosive.)
  DEFERRED in 3e: per-fluid explosion SIDE-effects (Coolant -3 heat / Foam -2 heat on crit; Corrosive
  crit 1D6 internal + FOLLOWING-turn 1D6/2; the CASE II 2->1 reduction verify); Inferno Fuel heat
  cook-off "as Inferno SRMs" + Support-Vehicle (no Armored Chassis/Env Sealing) 10+ explosion.
- **Phase 2 (Sprayer weapon): core DONE + tested** (2026-06-22). Sprayer converted MiscType->WeaponType.
  New `weapons/sprayers/{SprayerWeapon, MekSprayer, VehicularSprayer}`, `handlers/SprayerHandler`
  (conv-infantry 3D6 only), `F_SPRAYER` weapon flag, +2 no-fire-control to-hit modifier
  (`ComputeAttackerToHitMods` + message), old MiscType sprayers removed. `SprayerWeaponTest` (3) passes;
  compile + checkstyle clean. ProtoMek/aero exclusion falls out of the F_MEK/F_TANK weapon flags
  (no F_PROTO/F_AERO). REMAINING Phase-2 construction/capacity items (see below).

### Phase 2 - construction/crew/capacity items: DONE + tested (2026-06-22)
1. **Half shots/ton: DONE** - `SprayerHandler.useAmmo()` consumes 2 Fluid Gun shots per fire, so a
   ton yields half the shots (TO:AUE p.172). Localized to MegaMek (no MML change); the bin still
   *displays* full capacity but depletes twice as fast. Covered by code; manual playtest recommended.
2. **Requires liquid cargo bay: DONE** - `TestEntity.hasIllegalEquipmentCombinations` now flags a
   Sprayer without liquid storage (Liquid Cargo bay or `F_LIQUID_CARGO` equipment). Tested.
3. **No gunner: DONE** - `Compute.getSupportVehicleGunnerNeeds` excludes `F_SPRAYER` weapons in all
   weight-class branches (small/medium/large). Combat-tank crew is weight-derived, so unaffected.
   SmallCraft/Jumpship already skip range-1 weapons. Tested.
4. **Location/arc: DONE** - removed the now-dead MiscType `F_SPRAYER` body/rotor checks from
   `TestTank`; the Sprayer follows standard weapon-arc placement. (`MiscTypeFlag.F_SPRAYER` enum value
   left in place but orphaned - harmless.)
5. **BV = 0** - kept (no offensive BV), matching the old industrial MiscType.

Tests: `SprayerWeaponTest` (5) all pass; verifier + equipment suites pass (no regressions).

## PHASE 1 - Munition definitions (detailed design)

**Goal:** every eligible weapon can be loaded with every fluid the brackets allow; no effects yet
(Phase 3), just the `AmmoType`s existing, named, teched, and selectable.

**Findings that shape Phase 1:**
- `fluidGunAmmos` / `clanFluidGunAmmos` lists are **commented out** (`AmmoType.java:2651-2652`,
  `:3403`, `:3406`) -> Fluid Guns currently receive **no** munition variants at all (coolant included).
  `FluidGunCoolHandler` is effectively unreachable today.
- Vehicle/Heavy Flamer ammo lists already exist and already get Coolant; they need **Water** and
  **Inferno Fuel** added.
- `Munitions` enum has `M_WATER/M_PAINT_OBSCURANT/M_OIL_SLICK/M_ANTI_FLAME_FOAM/M_CORROSIVE/M_COOLANT`
  but **no `M_INFERNO_FUEL`** - must add it.

**Steps:**
1. Add `M_INFERNO_FUEL` to `Munitions` enum (fluid-gun group, ~`:2102`).
2. Re-enable `fluidGunAmmos` + `clanFluidGunAmmos` lists; add the IS/CL fluid-gun bases to them.
3. New `MunitionMutator` constants (IS + Clan where the tech base differs), weightRatio 1:
   | Fluid | Flag | Tech (R / A-A-A-A) | Applies to |
   |-------|------|--------------------|------------|
   | Water | M_WATER | A / A-A-A | flamers + fluid guns (sprayer via FG ammo) |
   | Coolant | M_COOLANT *(exists)* | C / B-B-B | all |
   | Corrosive | M_CORROSIVE | C / C-D-D | fluid guns |
   | Flame-Ret. Foam | M_ANTI_FLAME_FOAM | B / B-B-B | fluid guns |
   | Oil Slick | M_OIL_SLICK | B / B-B-B | fluid guns |
   | Paint/Obscurant | M_PAINT_OBSCURANT | B / B-B-B | fluid guns |
   | Inferno Fuel | M_INFERNO_FUEL *(new)* | D / D-E-D | flamers + fluid guns (NOT sprayer) |
4. Wire `createMunitions(...)`: add Water + Inferno Fuel to the vehicle/heavy flamer munition lists;
   build the full fluid set for fluidGunAmmos / clanFluidGunAmmos.
5. Delete the two stale TODO comments (`:2094`, `:3730`).

**Open call for Phase 1 - "default = Water" representation (recommend option A):**
- (A) Treat the existing base Fluid Gun ammo as the Water/default round (tag it `M_WATER`, keep the
  internal name `ISFluidGun Ammo` / `CLFluidGun Ammo` for unit-file compat) and add the other six
  fluids as named variants. No duplicate "Water" entry. Flamer base stays standard-flamer-fuel, with
  Water + Inferno Fuel as named variants.
- (B) Keep base as a generic "standard" round and add a separate named Water variant for fluid guns
  too (two water-ish entries; default handled in code).

**Tech progression (CONFIRMED from official Ammo Tech table):**
| Ammo | Tech/Rating | Avail (4 eras) | Proto/Prod/Common | Ref |
|------|-------------|----------------|-------------------|-----|
| Coolant | All/C | B,B,B,B | ES/ES/ES | 173 |
| Corrosive | All/C | C,D,D,D | PS/ES/ES | 173 |
| Flame-Ret. Foam | All/B | B,B,B,B | PS/PS/PS | 173 |
| Inferno Fuel | All/D | D,E,D,C | ~2390/2400/2425 (TH, approx proto) | 173 |
| Oil Slick | All/B | B,B,B,B | PS/PS/PS | 174 |
| Paint/Obscurant | All/B | B,B,B,B | ES/ES/ES | 174 |
| Water | All/A | A,A,A,A | PS/PS/PS | 174 |

Table tech base is **All**, but mirror the existing Coolant pattern: IS mutator -> IS ammo lists,
Clan mutator -> Clan lists (avoids shared-base name collisions). Same dates/avail both.
Note: table also lists **Flamer Fuel [Flamers/Fluid Guns]** (All/C, BBBA, 218 TM) = standard flamer
fuel, also loadable in Fluid Guns - optional extra variant, not core to the 7 fluids.

**BV/cost (CONFIRMED):** the only BV modifiers are **Corrosive x2.0** and **Inferno Fuel x2.0**
(applied to base ammo BV); every other fluid uses base ammo BV (x1.0). No special cost modifiers
given -> keep base ammo cost for new fluids (retain existing coolant cost=3000 special-case at
`:16628`). Implement the x2.0 BV alongside the existing munition-BV adjustments (~`:16624-16638`).

**Default-Water (CONFIRMED = option A):** base Fluid Gun ammo becomes the Water/default round,
tagged `M_WATER`, internal names `ISFluidGun Ammo` / `CLFluidGun Ammo` kept for unit-file compat.

## Notes
- Any new `Entity`/mount state (e.g. obscurant-painted status) MUST be `Serializable` + round-trip test.
- RAW only; no house rules unless explicitly requested.
