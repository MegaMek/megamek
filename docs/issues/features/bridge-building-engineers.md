# Feature: Bridge-Building Engineers (TO:AUE)

**Branch**: `Implement-Bridge-Laying-Engineers`
**Status**: Implemented + tested (2026-06-12)
**Rules**: TO:AUE Specialized Infantry Types (Bridge-Building Engineers); TM p.242 (Bridge-Layer placement rules,
adopted for placement validation)

## Summary

Implements the Bridge-Building Engineers infantry specialization: a platoon with the `BRIDGE_ENGINEERS`
specialization can spend 6 full turns adjacent to a target hex - engaging in no other actions - to raise a
single-hex Light Bridge (CF 15, two per scenario) or Medium Bridge (CF 40, one per scenario). CF is doubled when
the bridge is built over water (depth 1+). If the platoon takes casualties during a build turn, the work is
extended by 1 turn (once per turn regardless of the number of attacks). A platoon displaced, transported or
destroyed mid-build abandons the work; the bridge only comes into existence on completion.

The feature is gated behind a new dedicated game option: **TO Bridge-Building Engineers (Infantry)**
(`bridge_building_engineers`, advanced rules, default off).

## Rule interpretations (user decisions)

1. **Placement**: the player picks the adjacent target hex AND the orientation (which two opposite hexsides the
   bridge connects), via two board clicks.
2. **Type choice**: Light/Medium prompt when declaring the build; per-scenario budget of 2 points
   (Light = 1, Medium = 2) tracked on the unit and persisted in saves. Points are spent when the build starts and
   are not refunded on abandonment.
3. **Action phase**: declared as a movement-phase stance (like dig in / fortify), not a physical-phase attack.
4. **Damage extension trigger**: only actual trooper losses extend the build (detected via a per-turn strength
   snapshot); a hit that kills no troopers does not extend.
5. **Placement constraints**: the TM p.242 Bridge-Layer constraints apply: a water hex is a legal site only if it
   connects to at least one land hex or another bridge; the two banks may differ by at most 1 level.
6. **Bridge elevation**: the bridge surface sits level with the LOWER of the two connected banks (a bridge hex has
   a single `BRIDGE_ELEV`; two ends cannot differ in height).

## Mechanics

- **Declaration** (movement phase): `Build Bridge` button -> Light/Medium dialog -> click target hex (highlighted
  valid sites) -> click a hex adjacent to the target to set the axis -> move committed. The declaring turn counts
  as the first full turn of work.
- **Progress**: `ConvInfantry.newRound()` increments the completed-turn counter; `isEligibleFor()` returns false
  for all phases while building, so the platoon takes no turns.
- **END phase** (`TWGameManager.checkBuildBridges()`, called from `TWPhasePreparationManager`): reports progress,
  applies the casualty extension, abandons builds whose platoon is no longer adjacent (with a report), and
  completes finished bridges: terrain added, the bridge registered as an `IBuilding` (so it can take damage and
  collapse), clients updated via `CHANGE_HEX` + `BLDG_ADD` packets.

## Files changed

### Common

- `common/units/ConvInfantry.java` - bridge build state machine (turns, required turns with casualty extension,
  target/exits/type, 2-point budget, trooper snapshot), `newRound()`/`isEligibleFor()` overrides, CF helper
  (`getBuiltBridgeCF`), consolidated engineer tool auto-equip (`updateEngineerEquipment`) now also equipping the
  Infantry Bridge Kit for `BRIDGE_ENGINEERS`.
- `common/board/BridgeConstruction.java` (NEW) - shared site validation (TM p.242) and bridge placement
  (terrains, elevation = lower bank, `IBuilding` registration). Designed for reuse by future vehicle Bridge-Layer
  equipment (TM p.242), which passes its own CF values (8/20/45, no water doubling).
- `common/enums/MoveStepType.java` - new `BUILD_BRIDGE` step type.
- `common/moves/MoveStep.java` - additional-data keys + getters for target/exits/type; `compileIllegal` branch
  (first step only, full validation via `isValidBridgeBuildStep`).
- `common/options/OptionsConstants.java`, `common/options/GameOptions.java` - new game option.
- `common/equipment/EquipmentTypeLookup.java`, `common/equipment/MiscType.java` - `INFANTRY_BRIDGE_KIT` lookup
  constant (the Infantry Bridge Kit MiscType already existed with `S_BRIDGE_KIT`).

### Server

- `server/totalWarfare/MovePathHandler.java` - processes the `BUILD_BRIDGE` step (`processBuildBridgeStep`):
  starts the build, spends budget, reports (4274).
- `server/totalWarfare/TWGameManager.java` - `checkBuildBridges()` + `finishBridgeBuild()` (reports 4276-4279).
- `server/totalWarfare/TWPhasePreparationManager.java` - END phase call.

### Client

- `client/ui/panels/phaseDisplay/commands/MoveCommand.java` - `MOVE_BUILD_BRIDGE` (hidden when the game option
  is off, like VTOL strafing).
- `client/ui/panels/phaseDisplay/MovementDisplay.java` - button enable logic (per-condition DEBUG logging of why
  the button is disabled), type dialog, two-stage hex selection (target, then orientation) with board
  highlighting, step submission, declaration toast.
- `client/ui/clientGUI/boardview/sprite/BridgeBuildSprite.java` (NEW) - in-hex indicator: bridge deck/pylon icon
  with "2/6" progress text on the target hex (mirrors SawClearingSprite).
- `client/ui/clientGUI/boardview/spriteHandler/BridgeBuildSpriteHandler.java` (NEW) - rebuilds the indicators
  from synced ConvInfantry state on phase/entity/board change events (mirrors SawClearingSpriteHandler).
- `client/ui/clientGUI/ClientGUI.java` - handler registration; per-round INFO toast at the start of each
  movement phase for the local player's building platoons (they are ineligible for all phases, so no phase
  display ever selects them).
- `client/ui/clientGUI/tooltip/HexTooltip.java` - "Bridge under construction (turn X of Y)" hex tooltip line
  (mirrors the woods-clearing entry).

### Crossing fix (engine-level)

- `common/moves/MoveStep.java` - `isOnBridgeDeck()` exemption at both terrain-prohibition gates in
  `isMovementPossible`: a unit at bridge deck elevation stands ON the bridge, not in the prohibited terrain
  below it (TO:AR p.115). Without this, bridges with no approach road (all engineer bridges, and some
  map-authored ones) could not be mounted, stood on, or left by units prohibited in the underlying terrain -
  the pavement-step exemption only applies when arriving FROM a road/pavement hex. Crossing notes: climb mode
  "Climb Up" is needed for units that could instead enter the underlying terrain (existing engine behavior,
  documented in the README); foot infantry (1 MP) needs two turns for a bank-bridge-bank crossing.

### Player feedback

- `BridgeBuildSprite` draws the finished bridge's tileset graphic (`bridge/bridge_NN.gif` by exits) as a ghost
  image whose opacity equals the build progress (turn N of 6 = N/6 opacity, fully solid on the final turn),
  plus the "2/6" progress text in the lower hex.
- Free facing change toward the site when the build starts (`MovePathHandler.processBuildBridgeStep`).
- Mid-build state survives saves and entity sync: all state lives in non-transient fields on ConvInfantry
  (regression-tested via a serialization round trip).

### Diagnostics

- `[BuildBridge]`-tagged logging across the whole chain: button gate failures (MovementDisplay, DEBUG),
  step rejection (MoveStep, DEBUG), state transitions (ConvInfantry, DEBUG), declaration/completion/abandonment
  (MovePathHandler/TWGameManager/BridgeConstruction, INFO).
- `mmconf/log4j2.xml` - active DEBUG loggers for MovementDisplay/ConvInfantry/MoveStep (comment out when not
  diagnosing).

### Resources

- `common/options/messages.properties` - game option name/description.
- `common/messages.properties` - specialization tooltip (was "(Unimplemented)").
- `common/report-messages.properties` - reports 4274, 4276-4279.
- `client/messages.properties` - button, tooltip, dialog and status bar strings.

### Tests

- `unittests/megamek/server/totalWarfare/BridgeBuildingTest.java` (NEW) - 20 tests: exits/site validation (water,
  isolated water, bridge-connected water, bank difference, occupied hex, off-board bank), placement (terrain, CF,
  elevation at lower bank, structure registration), lifecycle (eligibility lock, progress, displacement abandon,
  casualty extension, budget, CF values, kit auto-equip), END phase (completion after 6 turns with water-doubled
  CF, displacement abandon, casualty delay).

## Future work (out of scope)

- **Vehicle Bridge-Layer equipment (TM p.242)**: Light/Medium/Heavy bridge layers already exist as `MiscType`
  equipment. Their implementation should reuse `BridgeConstruction` with their own trigger (End Phase declaration,
  1 stationary turn, placed in the front hex along the unit's facing) and CF values (8/20/45, no doubling), plus
  the carried-bridge-as-ablative-armor rules. See memory/plan notes.
- BA version of the Infantry Bridge Kit (TODO already in `MiscType.createBridgeKit`).
- Princess (bot) does not use the ability.

## Open items

- Exact TO:AUE page number for the citation (rules text supplied without page); update Javadoc citations from
  "TO:AUE" to "TO:AUE p.XXX" when confirmed.
