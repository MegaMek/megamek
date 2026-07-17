# Trench/Fieldworks Engineers + Vehicle Fieldworks

**Branch:** `Update-Trench-and-Fieldworks-Engineers`
**Status:** Implemented (in review - PR #8347)
**Rules:** TO:AUE p.153 (Trench/Fieldworks Engineers, Vehicles and Fieldworks), TO:AR p.106 (Digging In),
TO:AR p.19 (vehicle Hull Down)
**Related:** Bridge-Building Engineers (`bridge-building-engineers.md`) - the fortify rule explicitly mirrors it.

---

## 1. Rules as written

### Trench/Fieldworks Engineers (TO:AUE p.153)

> These engineers dig trenches and set up ad-hoc field works that can be used as handy cover for any
> infantry unit. A trench/fieldworks hex (called a fortified hex) may be established in any hex except
> for those containing water, pavement or buildings, and provides a +2 modifier to any attacks (except
> for flamers and area-effect weapons) made against any infantry unit that subsequently "digs into" the
> fortified hex. (Fortified hexes made of clear terrain are also not treated as clear when determining
> damage against infantry.) Creating trenches and fieldworks takes 3 full turns to complete, during
> which time the engineering unit may take no other action. As with Bridge-Building Engineers, damage to
> a unit during a turn in which it is attempting to fortify a hex in this fashion extends the effort by 1
> turn (regardless of the number of attacks made against the unit). If a Trench/Fieldworks unit is
> destroyed before completing its task, the underlying terrain remains unchanged and provides no
> modifiers.

### Vehicles and Fieldworks (TO:AUE p.153)

> Vehicles with a bulldozer, backhoe, or any piece of equipment ruled as equivalent may be considered to
> have the fieldworks ability, allowing them to construct field fortifications in a manner identical to
> Trench/Fieldworks Engineers.

### Digging In (TO:AR p.106)

> An infantry unit may safeguard itself from attack by digging in [...] in any type of terrain except
> roads, pavement, buildings and water. The process takes a full turn [...]. If it is attacked during
> this turn, it is not considered dug in yet and so receives no bonuses. All attacks against a dug-in
> unit (regardless of munition type) add a +2 to-hit modifier, except for flamers and area-effect
> weapons. In addition, damage is not doubled against a dug-in unit in Clear terrain. [...] Specialized
> infantry can build a fortified hex that any infantry unit, including mechanized infantry, can enter and
> automatically receive the "digging in" benefit. Mechanized Infantry: Except in the case of fortified
> hexes built by appropriately specialized infantry, mechanized infantry may not use the digging-in rule.

---

## 2. Current implementation inventory (baseline)

| Concern                                                    | State                        | Location                                                                         |
|------------------------------------------------------------|------------------------------|----------------------------------------------------------------------------------|
| Dug-in state machine (NONE/WORKING/COMPLETE/FORTIFYING1-3) | Done                         | `Infantry.java:83-88`                                                            |
| Tank fortify states (NONE/FORTIFYING1-3)                   | Done                         | `Tank.java:140-144`                                                              |
| `MoveStepType.FORTIFY` / `DIG_IN`                          | Done                         | `MoveStepType.java:71-73`                                                        |
| Server sets fortify state on step                          | Done                         | `MovePathHandler.java:2900-2906 / 2930-2937`                                     |
| Counter advance per round                                  | Done                         | `Infantry.java:453-457`, `Tank.java:993-998`                                     |
| Fortify completion -> writes `FORTIFIED` terrain           | Done                         | `TWGameManager.resolveFortify()` 31543-31604                                     |
| Engineer may take no other action while building           | Done                         | `Infantry.isEligibleFor` 431-439, `Tank.isEligibleFor` 968-974                   |
| +2 to-hit vs dug-in / fortified infantry                   | Done (flamer-only exclusion) | `ComputeTerrainMods.java:158-168`                                                |
| No double-damage in fortified / dug-in clear hex           | Done                         | `ServerHelper.infantryInOpen` 85-109                                             |
| Specialization -> equipment linkage                        | Done                         | `ConvInfantry.java:433` (TRENCH_ENGINEERS -> Vibro-Shovel -> `F_TRENCH_CAPABLE`) |
| Fieldworks equipment carries `F_TRENCH_CAPABLE`            | Done                         | Bulldozer `MiscType.java:6869`, Backhoe `6768`, Vibro-Shovel `11675`             |
| Destroyed-before-complete leaves terrain unchanged         | Done (implicit)              | terrain only written at `FORTIFYING3`                                            |

### Gaps vs RAW (this work)

1. **Damage does not extend the effort.** `newRound()` advances the counter unconditionally - no damage check. (Bridge
   building already does this via `ConvInfantry.updateBridgeBuildCasualties()`.)
2. **WATER not excluded** from dig-in / fortify terrain check. `MoveStep.java:1941-1947` lists
   PAVEMENT/FORTIFIED/BUILDING/ROAD only.
3. **Equipment gate is cosmetic server-side.** `MovePathHandler` warns when `F_TRENCH_CAPABLE` is missing but still
   enters the fortify state.
4. **+2 modifier excludes flamers only**, not area-effect weapons (RAW excludes both). Affects dug-in *and* fortified.

### Confirmed NON-issues

- "No double damage in clear terrain" is correctly implemented (`ServerHelper.infantryInOpen` checks both `FORTIFIED`
  and `DUG_IN_COMPLETE`).
- Vehicle fieldworks equipment is already recognized (bulldozer/backhoe/vibro-shovel all carry `F_TRENCH_CAPABLE`).
- `dugIn` is **not** persisted to save files (`EntityListFile` has no reference), so new transient fields need no
  save-compat handling.

---

## 3. Implementation tasks

### A. Damage extends the effort by 1 turn (snapshot) - infantry + tanks ✅ DONE

- [x] New shared class `common/units/FortifyState.java` (Serializable) encapsulating the damage-snapshot detection (
  `begin` / `checkpointWasDamaged` / `reset`). Used by both `Infantry` and `Tank` - removes the duplicated logic without
  touching the per-class dug-in state machines or the public `getDugIn()`/`setDugIn(int)` API.
- [x] **Unified health signature** = `getTotalArmor() + getTotalInternal()` for **both** infantry and tanks (private
  `currentFortifyHealthSignature()` in each). For infantry, internal = trooper count, so casualties lower it; for tanks,
  any armor/internal loss lowers it. This is closer to the RAW "damage to a unit" wording than the bridge feature's
  trooper-only casualty check.
  Known limitation: pure crit/motive hits on a vehicle that reduce neither armor nor internal will not register. Revisit
  with a per-round damage flag only if playtests demand it.
- [x] `Infantry.beginDigIn(boolean convertFromDeck)` and `Infantry.beginFortify()` / `Tank.beginFortify()` set the state
  **and** seed the snapshot (mirrors `ConvInfantry.startBridgeBuild`).
- [x] `MovePathHandler` calls the new `begin*` helpers instead of raw `setDugIn(...)` (infantry dig-in/fortify + tank
  fortify).
- [x] `newRound()` (both classes): advance the counter only when not damaged; otherwise log
  `[Fortify] ... extended by 1 turn` (Infantry uses new `LOGGER`, Tank uses existing `logger`) and hold.
  `Infantry.clearGroundPostures()` resets the tracker.
- [x] Timing verified: `newRound()` (round start) compares against the prior round's snapshot, so it sees damage from
  the just-finished round; `resolveFortify()` (END prep) still completes at `FORTIFYING3`. `dugIn` is not
  save-persisted, so the new field needs no save handling.
- [x] Tests: `unittests/megamek/common/units/FortifyTest.java` - 9 tests (FortifyState logic, infantry
  advance/extend/repeated-damage/self-dig-in, vehicle advance/extend). All pass; `HitTheDeckTest` + `BridgeBuildingTest`
  still green.

### B. Exclude WATER from dig-in / fortify ✅ DONE

- [x] `MoveStep` dig-in/fortify block: added `Terrains.WATER` to the exclusion list (now
  water/pavement/building/road/already-fortified). Consolidated the chained `||` into a named `illegalFortifyTerrain`
  boolean with a null-hex guard.
- [x] `[Fortify]` debug log on each rejection path (was a silent `return`).

### C. Enforce the equipment gate server-side ✅ DONE

- [x] `MovePathHandler` infantry + tank: if no `F_TRENCH_CAPABLE`, log `[Fortify]` reason and skip entering the fortify
  state (replaced the cosmetic `sendServerChat`-then-proceed).
- [x] Mirrored the `F_TRENCH_CAPABLE` check in `MoveStep` legality (FORTIFY only; plain DIG_IN needs no equipment) so
  the path renders illegal client-side too, with its own `[Fortify]` log.
- [x] Tests added to `FortifyTest` (MovementLegality, 5 tests): dig-in clear legal, dig-in water illegal,
  fortify-with-equipment legal, fortify-without-equipment illegal, fortify-in-water illegal. All pass.

### D. Vehicles & Fieldworks (RAW audit, no expansion)

- [ ] Enumerate every `MiscType` with `F_TRENCH_CAPABLE`; confirm the set = {Bulldozer, Backhoe, Vibro-Shovel} (+ any
  genuine rules-equivalent). Record it here.
- [ ] Confirm tank path has full A-C parity after the above.

### E. Area-effect weapon exclusion on the +2 (fuller RAW)  ✅ DONE

- [x] `ComputeTerrainMods`: the cover bonus (the +2 dug-in/fortified AND the +1 hit-the-deck, since they share the
  block) is now suppressed for flamers **and** area-effect attacks via a named `excludedFromCoverBonus` boolean.
- [x] New package-private predicate `ComputeTerrainMods.isAreaEffectAgainstInfantry(weaponType, ammoType)`. **Scope (
  fuller RAW)**: artillery `F_ARTILLERY` (covers Arrow IV, Long Tom, Sniper, Thumper, cruise missiles), artillery
  cannons (`instanceof ArtilleryCannonWeapon` - verified these double infantry damage as AE in
  `ArtilleryCannonWeaponHandler.calcDamagePerHit`), bombs (`F_ALT_BOMB`/`F_DIVE_BOMB`/`F_SPACE_BOMB`), and fuel-air
  explosive munitions (`M_FAE`).
- [x] Tests: `unittests/.../ComputeTerrainModsTest.java` (5 tests, Mockito) - artillery / artillery-cannon / bomb /
  fuel-air are AE; standard direct-fire weapon is not. All pass.
- Note: predicate left package-private so it can be reused if the same RAW exclusion is needed elsewhere; promote to a
  shared helper only when a second caller appears.

### F. Toasts

- [x] **Typed server->client toast packet infrastructure** (see Section 4) - DONE and compiling/tested.
  `PacketCommand.SEND_TOAST`, `GameToastEvent` (+ nested `Level`), `GameListener.gameToast` default, `AbstractClient`
  dispatch, `ClientGUI.gameToast` + `toastLevelFor` mapping, `TWGameManager.sendToast(...)`. `GameToastEventTest` (2
  tests) green.
- [x] **Client-side rejections** (immediate): `MovementDisplay` DIG_IN / FORTIFY handlers now show
  `addToast(ToastLevel.WARNING, msg, unit)` when the current hex is not fortifiable, via the shared
  `MoveStep.isFortifiableTerrain(hex)` helper (dedups the terrain rule between `MoveStep` legality and the UI). New
  `messages.properties` keys `MovementDisplay.digInIllegalTerrain.toast` / `fortifyIllegalTerrain.toast`. (No-equipment
  is already prevented by the disabled button + server gate.)
- [x] **Progress/extension** (server): new report `5306` ("fortification work set back a turn by damage") plus an INFO
  toast (`Fortify.delayedToast`), raised from `resolveFortify` via `reportFortificationDelayed(...)` when
  `isFortifyExtendedThisRound()` is set. (END-phase reports are not auto-toasted, so the toast is sent explicitly.)
- [x] **Completion = SUCCESS toast with icon**: `completeFortification(...)` calls
  `sendToast(GameToastEvent.Level.SUCCESS, Fortify.completeToast, builder)` for infantry and vehicles.

### G. Optional modernization (Java 21, human-readable) - scoped to touched code

- [x] Extracted the damage-snapshot logic into the focused `FortifyState` class shared by `Infantry` and `Tank`.
- [x] Refactored `resolveFortify` to remove the infantry/tank duplication: extracted `completeFortification(...)`,
  `reportFortificationDelayed(...)`, `addSimpleFortifyReport(...)`; extracted `MoveStep.isFortifiableTerrain(...)`
  shared by legality + UI; named-boolean / `instanceof` pattern binding used in the touched branches.
- [N/A] Kept the loose `int` dug-in constants (did not convert to an `enum DugInState`): the call-site churn across
  `ComputeTerrainMods`, `ServerHelper`, `EntitySprite`, `MovementDisplay`, `MoveStep`, `resolveFortify` would sprawl
  well beyond this feature's scope. The `FortifyState` extraction captured the duplication that mattered; the enum
  conversion is left as a separate future cleanup.

### H. Tests ✅ (across `FortifyTest`, `ComputeTerrainModsTest`, `GameToastEventTest`)

- [x] Infantry fortify completes in 3 turns, no damage (baseline).
- [x] Trooper loss during a fortify turn -> +1 turn; cumulative; extended-this-round flag.
- [x] Tank fortify extension parity (armor loss).
- [x] `DIG_IN`/`FORTIFY` in WATER -> step illegal; fortify-without-equipment illegal; `isFortifiableTerrain` unit-tested
  for clear/water/pavement/building/road/fortified/null.
- [x] Self dig-in completes in one clean turn but holds on a damaged turn.
- [x] +2 area-effect classification (artillery / artillery cannon / bomb / fuel-air AE, standard weapon not).
- [x] Toast event contract (accessors + dispatch).
- [x] Existing `HitTheDeckTest` still passes. (`BridgeBuildingTest` not present on this branch.)
- `FortifyTest` 19, `ComputeTerrainModsTest` 5, `GameToastEventTest` 2 - all green.

### I. i18n ✅ DONE

- [x] `messages.properties`: `MovementDisplay.digInIllegalTerrain.toast`, `MovementDisplay.fortifyIllegalTerrain.toast`,
  `Fortify.completeToast`, `Fortify.delayedToast` (English).
- [x] `report-messages.properties`: `5306` delayed-fortification report (English; other locales fall back).

---

## 4. New infrastructure: typed server->client toast

**Goal:** let the server raise a single high-signal toast (e.g. `SUCCESS` "Fortification complete" with the
unit icon) instead of relying on the always-`INFO`, text-parsed report path.

**Design:**

- New `PacketCommand.SEND_TOAST` (server -> client) carrying `level` (`ToastLevel`), `message` (already
  i18n-resolved server-side), and `entityId` (or -1).
- Server: a `TWGameManager` helper (e.g. `sendToast(ToastLevel, String, Entity)`) builds and sends the
  packet to the relevant players. Call it from `resolveFortify()` on completion.
- Client: handle the packet in the client packet dispatcher and surface it via `ClientGUI.addToast(level,
  text, entity)`. Route through a `GameListener` callback (consistent with how other server events reach
  `ClientGUI`) rather than reaching into the GUI from the network layer.
- This is general-purpose - any future server event can raise a typed toast. Keep it generic; do not
  special-case fortify in the packet.

**Toast usage map for this feature:**
| Event | Side | Mechanism | Level |
|---|---|---|---|
| Rejected: no equipment / illegal terrain / already fortified | Client | `addToast` direct | WARNING |
| Effort delayed by damage | Server | report `5306` -> auto toast | INFO |
| Fortification complete | Server | `SEND_TOAST` packet | SUCCESS (+ icon) |

---

## 5. Files to touch

- `common/units/Infantry.java`, `common/units/Tank.java` - snapshot, helpers, `newRound`, logging (+ optional
  `FortifyState`).
- `common/moves/MoveStep.java` - WATER exclusion (+ optional equipment legality), logging.
- `server/totalWarfare/MovePathHandler.java` - equipment gate enforcement, call new helpers, logging.
- `server/totalWarfare/TWGameManager.java` - `sendToast` + SUCCESS toast at `resolveFortify()` completion.
- `common/net/enums/PacketCommand.java` + client packet dispatcher + a `GameListener` hook - `SEND_TOAST`.
- `common/actions/compute/ComputeTerrainMods.java` - area-effect exclusion on +2.
- `common/equipment/WeaponType.java` or `weapons/handlers/AreaEffectHelper.java` - AE-against-infantry predicate.
- `client/ui/panels/phaseDisplay/MovementDisplay.java` (+ `MoveCommand`) - rejection toasts.
- `resources/.../messages.properties`, `resources/.../report-messages.properties` - i18n.
- `unittests/.../FortifyTest.java` - new tests.

---

## 6. Open decisions (resolved)

1. Area-effect scope - **fuller RAW** (artillery + bombs/Arrow IV + AE munitions). [resolved]
2. Completion toast - **SUCCESS via new typed packet** to avoid INFO spam. [resolved]
3. Tracking doc - this file. [resolved]

## 6a. Player feedback for in-progress fortification ✅ DONE

- [x] **Entity sprite badge**: shows fortify progress as **"Fortify N/3"** (i18n `BoardView1.fortifyProgress`) for
  infantry and vehicles, replacing the generic "Working" badge for the FORTIFYING stages (plain 1-turn self dig-in still
  shows "Working"/"D"). `EntitySprite.java`.
- [x] **Bridge-style ghost hex**: `FortifyBuildSprite` (HexSprite) draws the sandbags/fortified graphic (
  `boring/sandbags.gif`) as a ghost whose opacity = stage/3 (0.18 floor), plus "N/3" text low in the hex - modeled on PR
  #8343's `BridgeBuildSprite`. New `FortifyBuildSpriteHandler` rebuilds the sprites from entity state on
  phase/entity/board changes, registered in `ClientGUI.initializeSpriteHandlers`. It's a self-contained fortify-specific
  sprite (no dependency on bridge code, which isn't on this branch).
- [x] Single source of truth: `Infantry`/`Tank` expose `isFortifying()` / `getFortifyStage()` /
  `getFortifyTotalStages()`, used by both the badge and the ghost sprite/handler. Stage accessor unit-tested in
  `FortifyTest`.

### Ribbon button helper text

- [x] Added hover tooltips explaining the difference for infantry: `MovementDisplay.moveDigIn.tooltip` (1-turn personal
  cover, any non-mech infantry, ends on move) and `MovementDisplay.moveFortify.tooltip` (3-turn engineer/vehicle build
  of a permanent fortified hex that benefits any occupant). Resource-only - `StatusBarPhaseDisplay.createToolTip`
  already wires `MovementDisplay.<cmd>.tooltip` keys.

### Visual split: trenches for dug-in, sandbags for fortified

- [x] **Trench overlay for dug-in infantry**: new asset `boring/trenches.png` (84x72, cropped/centered from the user's
  `saxarba/Trenches.png`). New `DugInSprite` (HexSprite) draws it on the hex of infantry that are digging in
  (`DUG_IN_WORKING`, faint 0.4 alpha) or dug in (`DUG_IN_COMPLETE`, full alpha). `DugInSpriteHandler` rebuilds these on
  phase/entity changes, registered in `ClientGUI`. Infantry only (tanks don't self dig in).
- [x] **Status text** on the trench overlay ("Digging in" / "Dug in", i18n `BoardView1.diggingIn` / `BoardView1.dugIn`),
  mirroring the fortify sprite's progress text. (Dig-in is a 1-turn action so there is no N/3-style counter; the label
  conveys the state.)
- [x] **Z-order**: `DugInSprite.isBehindTerrain()` returns `true` so the trench draws on top of the base terrain but
  before the entity sprites - the platoon's icon sits on top of its trench, not under it. (FortifyBuildSprite still
  draws
  over the unit; left as-is unless the same treatment is wanted there.)
- [x] **Sandbags stays for fortify build / FORTIFIED terrain**: `FortifyBuildSprite` (FORTIFYING1-3) and the completed
  `FORTIFIED` tileset hex keep the sandbags graphic. No overlap: `DugInSprite` shows only for WORKING/COMPLETE,
  `FortifyBuildSprite` only for the FORTIFYING stages.

## 6b. Pre-placed fortified hexes (mapmaker + scenario/player setup)

Goal: let mapmakers and players start a game with fortified hexes already on the board.

- **Mapmaker: already supported (no code).** `FORTIFIED` (type 37) is not in `Terrains.AUTOMATIC`, so it is in the
  board-editor terrain palette, and `.board` files round-trip `fortified:1` (existing boards use it, e.g. Gothic
  Trenches, Tukayyid pack). Nothing to plumb.
- **Player allotment + place in the Minefields deployment phase** (mirrors minefields; a fortified hex is just terrain,
  so no Minefield object / hidden-info needed). Decisions: deployment-zone-only placement, visible to all, own "
  Fortifications" settings section.
  - [x] `Player`: `numFortifiedHexes` + `getNbrFortifiedHexes()`/`setNbrFortifiedHexes()`; added to `hasMinefields()` so
    the deploy phase triggers (and a player gets a turn) on fortifications alone - independent of the minefields game
    option, since the phase transition (`TWPhaseEndManager`) is gated by `hasMinefields()`, not the option. Serializes
    automatically via `PLAYER_UPDATE`.
  - [x] `PlayerSettingsDialog`: new "Fortifications" section (`fortificationSection()`) with a Fortified Hexes field,
    shown unconditionally; read/write like the minefield fields.
  - [x] `PacketCommand.DEPLOY_FORTIFICATIONS` + `Client.sendDeployFortifications(Vector<BoardLocation>)`.
  - [x] `DeployMinefieldDisplay`: new "Fortify(N)" tool. Click a hex -> validate (fortifiable terrain via shared
    `MoveStep.isFortifiableTerrain` + within deployment zone via `Board.isLegalDeployment`), preview locally (
    `board.setHex` repaints), decrement count; the Remove tool undoes a fortification placed this turn (refund). Sends
    placed `BoardLocation`s on Done.
  - [x] `TWGameManager.receiveDeployFortifications` / `processDeployFortifications` / `isLegalFortificationPlacement`:
    re-validates each hex (on board, fortifiable terrain, in the player's deployment zone, within allotment), writes
    `FORTIFIED` terrain, `sendChangedHex` to all clients (visible to everyone). `[Fortify]` logging on
    rejects/placement.
  - [x] i18n: `DeployMinefieldDisplay.deployFortification`/`.tooltip`/`DuplicateFortification`/`fortifyIllegalTerrain`/
    `fortifyOutsideZone`/`undeployedFortifications`/`undeployedItems`; `PlayerSettingsDialog.header.fortifications`/
    `labFortifiedHexes`/`fortifiedHexesTT`.
  - Reuses the existing FORTIFIED terrain rendering (sandbags tileset) and the combat rules already implemented (A-E).
    Compiles; `FortifyTest`/`ComputeTerrainModsTest` still green. Remaining: manual playtest of the deploy-phase tool.

## 6c. Playtest fixes

- [x] **Deploy phase never appeared for fortifications.** Root cause: `Server.receivePlayerInfo` applies a
  `PLAYER_UPDATE` by copying individual fields (it does NOT replace the Player wholesale), and it copied the minefield
  counts but not `numFortifiedHexes` - so the client's allotment was dropped server-side and `hasMinefields()` stayed
  false. Fixed by adding `gamePlayer.setNbrFortifiedHexes(player.getNbrFortifiedHexes())` alongside the minefield copies
  in `Server.java`.

## 6d. Player feedback / awareness additions (playtest-driven)

- [x] **To-hit wording split**: cover bonus now reads "infantry in fortified hex" (`WeaponAttackAction.FortifiedHexInf`)
  when sourced from terrain vs "infantry dug in" (`DugInInf`) from the unit's own posture.
- [x] **To-hit diagnostics**: `[Fortify]` DEBUG logging in `ComputeTerrainMods` (applied / suppressed-area-effect /
  not-yet-WORKING) + a ready-to-uncomment "Trench/Fieldworks diagnostics" logger block in `mmconf/log4j2.xml` (the
  `megamek` logger is info by default, so these are opt-in).
- [x] **Unit display General tab indicator**: `UnitToolTip.getFortificationStatus(entity)` adds a line between Facing
  and Sensors - "Dug in" / "Digging in" / "Fortify N/3" (building) / "In fortified hex (cover)" (infantry) / "In
  fortified hex" (other). i18n `BoardView1.inFortifiedHexCover` / `inFortifiedHex` (+ reused
  dugIn/diggingIn/fortifyProgress).
- [x] **Deploy-dug-in lobby option**: `CustomMekDialog` Deployment tab gets a "Dug In" checkbox for **non-mechanized
  infantry** when the dig-in option is on; sets `DUG_IN_COMPLETE` on the entity at OK so it deploys already dug in (
  `CustomMekDialog.labDeployDugIn`). RAW note: mechanized excluded (can't dig in); terrain legality of the eventual
  deploy hex isn't re-validated at config time (setup convenience).

## 6e. Hidden-unit interaction

- [x] **Dug in + hidden allowed**: the deploy-dug-in checkbox and the Hidden checkbox are independent, so a
  non-mechanized infantry can deploy both dug in and hidden (in concealing terrain). No change needed.
- [x] **Hidden onto a fortified hex blocked**: `DeploymentDisplay.validateDeploymentBoard` now returns a new
  `HIDDEN_IN_FORTIFIED` result when a hidden unit is placed on a FORTIFIED hex (the visible fortification would reveal
  the position), surfaced via a WARNING toast (`DeploymentDisplay.hiddenInFortified`). Terrain-based, so it applies to
  any hidden unit regardless of dug-in posture. Client-side guard; server-side enforcement could be added if needed.

## 7. Progress log

- 2026-06-13: Rules compared to code, gaps identified, plan approved, this doc created. Implementation pending.
- 2026-06-14: Task A implemented (damage-extension for infantry + vehicles via new shared `FortifyState`; unified
  armor+internal signature; `begin*` helpers; `MovePathHandler` wired). `FortifyTest` (9 tests) passing; no regressions.
- 2026-06-14: Tasks B + C implemented (WATER excluded from dig-in/fortify; `F_TRENCH_CAPABLE` gate enforced server-side
  in `MovePathHandler` and mirrored in `MoveStep` legality; per-condition `[Fortify]` logging). `FortifyTest` now 14
  tests, all passing. Note: `BridgeBuildingTest` is not present on this branch (bridge source is).
- 2026-06-14: Task E implemented (area-effect exclusion on the cover bonus; `isAreaEffectAgainstInfantry` predicate
  covering artillery, artillery cannons, bombs, fuel-air). `ComputeTerrainModsTest` (5 tests) passing.
- 2026-06-14: Task F part 1 - typed server->client toast packet infrastructure built end-to-end (`SEND_TOAST` packet,
  `GameToastEvent`+`Level`, `GameListener.gameToast`, `AbstractClient` dispatch, `ClientGUI.gameToast`/`toastLevelFor`,
  `TWGameManager.sendToast`). Layer-clean: common event carries its own `Level`, mapped to UI `ToastLevel` only in
  `ClientGUI`. Main compile green; `GameToastEventTest` (2 tests) passing.
- 2026-06-14: Task F part 2 - wired the three fortify events: completion SUCCESS toast (`completeFortification`),
  delayed-by-damage report 5306 + INFO toast (`reportFortificationDelayed`), client-side WARNING rejection toasts on
  illegal terrain (via shared `MoveStep.isFortifiableTerrain`). `resolveFortify` refactored to dedupe infantry/tank.
  i18n keys added. All tests green (`FortifyTest` 19, `ComputeTerrainModsTest` 5, `GameToastEventTest` 2,
  `HitTheDeckTest` ok). Tasks A-F + G + H + I complete. The `int`->`enum DugInState` cleanup is intentionally deferred.
- 2026-06-14: Player-feedback pass (poor in-game feedback reported). Added "Fortify N/3" entity-sprite badge AND a
  bridge-style ghost-hex indicator (`FortifyBuildSprite` + `FortifyBuildSpriteHandler`, registered in `ClientGUI`)
  drawing the sandbags graphic at opacity = stage/3 with "N/3" text. Backed by new `isFortifying()` /
  `getFortifyStage()` / `getFortifyTotalStages()` on Infantry/Tank (single source of truth for badge + sprite).
  Modeled on PR #8343 (`Implement-Bridge-Laying-Engineers`) `BridgeBuildSprite`. `FortifyTest` now 20, all green;
  compiles. Remaining: manual visual playtest of the ghost sprite.
