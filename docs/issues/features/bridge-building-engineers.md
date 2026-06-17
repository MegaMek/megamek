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

1. **Placement** (revised 2026-06-13 - origin-from-engineer, 2 clicks): the bridge always **originates from the
   engineer's hex**. The player clicks the **hex the bridge will occupy** (must be adjacent to the engineer; stage 1
   highlights only these <=6 hexes), then clicks the **far end the bridge reaches to** (stage 2 highlights the valid
   far ends). The near bank is fixed to the deck side facing the engineer, so the far end alone uniquely determines a
   straight or curved span - no third click. The two banks may be opposite (straight bridge) or non-opposite (curved
   bridge); the connected-hexside bitmask both validates the site and selects the tileset image
   (`bridge_<exits>.gif`, e.g. `bridge_17.gif` for a curve connecting sides 0 and 4). (Earlier iterations led with a
   start bank, then with the bridge hex + auto-straight pairing; the auto-straight prevented aiming curves and ignored
   the engineer's side, so the origin is now fixed at the engineer.)
2. **Type choice**: Light/Medium prompt when declaring the build; per-scenario budget of 2 points
   (Light = 1, Medium = 2) tracked on the unit and persisted in saves. Points are spent when the build starts and
   are not refunded on abandonment.
3. **Action phase**: declared as a movement-phase stance (like dig in / fortify), not a physical-phase attack.
4. **Damage extension trigger**: only actual trooper losses extend the build (detected via a per-turn strength
   snapshot); a hit that kills no troopers does not extend.
5. **Placement constraints**: the TM p.242 Bridge-Layer constraints apply: a water hex is a legal site only if it
   connects to at least one land hex or another bridge; the two banks may differ by at most 1 level. Bridges may
   be straight or curved (any two distinct hexsides), revising the earlier "straight only" decision after
   playtesting curved crossings.
6. **Bridge elevation**: the bridge surface sits level with the LOWER of the two connected banks (a bridge hex has
   a single `BRIDGE_ELEV`; two ends cannot differ in height).

## Mechanics

- **Declaration** (movement phase): `Build Bridge` button -> Light/Medium dialog -> click the bridge hex (highlighted
  hexes next to the engineer) -> click the far end the bridge reaches to -> move committed. The bridge originates from
  the engineer's side; the declaring turn counts as the first full turn of work.
- **Eligibility while busy**: a platoon actively building or dismantling stays **Movement-phase eligible only**
  (`ConvInfantry.isEligibleFor()` returns true only for Movement, false for firing/physical), so the player can select
  it each turn to keep working or to pause/cancel/abandon; it takes no other action. A **paused** platoon is *not*
  busy - it is freed and eligible in all phases. `newRound()` no longer advances progress; it only abandons an active
  build/dismantle whose platoon was displaced from its site.
- **Progress is banked in the END phase** (`TWGameManager.checkBuildBridges()`, called from
  `TWPhasePreparationManager`): each round of active work banks one completed turn (`ConvInfantry.bankBridgeBuildTurn()`
  / `bankBridgeDismantleTurn()`) so the count reflects turns actually completed. It applies the casualty extension,
  abandons builds whose platoon is no longer adjacent or was destroyed (clearing paused builds too, with a report),
  advances dismantling (refunding the budget on completion), and completes finished bridges: terrain added, the bridge
  registered as an `IBuilding` (so it can take damage and collapse), clients updated via `CHANGE_HEX` + `BLDG_ADD`
  packets.

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
- `common/moves/MoveStep.java` - bridge-deck exit constraint (`isRealBridgeSpan()`): a unit on a bridge deck
  may only enter and leave through the bridge's connected hexsides (its exits) - you board a bridge at its
  ends, not over its sides (TO:AR p.115). The pre-existing engine check only covered climbing onto a bridge
  from below; this adds the same constraint for decks flush with the bank level (engineer bridges over water,
  whose `BRIDGE_ELEV` is 0). Scoped to "real" spans (over water, or deck raised above the hex) so flush dry
  bridges that act as road segments keep their existing free movement.

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

## Cancel / dismantle (added 2026-06-13)

A build in progress has several exits, offered through a **chooser dialog** (added 2026-06-13) when the bridge
button is clicked. The button label is state-based — **Build Bridge** (idle), **Cancel Bridge** (building),
**Resume Building** (paused/dismantling) — and clicking it (when busy) opens a dialog listing the valid actions:

- **Build Bridge** (idle platoon): opens the type/site selection flow.
- While **actively building** → **Pause**, **Dismantle**, or **Abandon**:
    - **Dismantle** (`CANCEL_BRIDGE`): dismantles the partial structure over as many turns as were **banked** building
      (at least one); the spent point(s) are **refunded** once it finishes.
    - **Abandon** (`ABANDON_BRIDGE`): instant — the partial work is lost this turn and the points are **forfeit** (no
      refund).
    - **Pause** (`PAUSE_BRIDGE`): the progress is held on the platoon and the platoon is **freed** to move and fight;
      the spent points stay committed. Persists indefinitely.
- While **paused** → **Resume** (only when standing adjacent to the site again) or **Abandon**.
- While **dismantling** → **Resume** (`RESUME_BRIDGE`, reverses the dismantle from the structure still standing) or
  **Abandon**.

Each declaration ends the platoon's turn. Pausing frees the platoon from the **next** turn (it can't be unlocked
mid-movement-phase because the client doesn't know it's freed until the server round-trips). A platoon can have only
**one** bridge in progress at a time (building, paused, or dismantling). A paused platoon that is destroyed loses the
work (no bridge, no refund).

An in-progress bridge (building, paused, or dismantling) **reserves its hex**: no terrain is placed until 6/6, so
`ConvInfantry.isBridgeTargetClaimed(game, boardId, target, excluded)` stops a second platoon from raising another
bridge in the same hex. It's enforced both in the client's site list (the hex isn't offered, with a "claimed" reason)
and defensively on the server in `processBuildBridgeStep`.

Rules / behavior:

- A **finished** bridge cannot be dismantled (the button only enters cancel mode while `isBuildingBridge()`).
- A building or dismantling platoon is **movement-phase eligible only** (so the player can keep working, or cancel),
  and may take **no other action whatsoever** - it cannot move and cannot even turn in place (TO:AUE p.152, "no
  other actions"). Enforced in `MoveStep` (busy-with-bridge guard blocks every step except `CANCEL_BRIDGE` /
  `RESUME_BRIDGE`) and in the ribbon, which is locked down to **Next Unit**, **Done**, and the bridge button
  (**Cancel Bridge** while building, **Resume Building** while dismantling). Starting a build also clears any dug-in
  / hit-the-deck posture (`clearGroundPostures`).
- Displacement or destruction **during dismantling** abandons the work with **no refund** (refund is only on
  successful completion).
- The hex indicator, per-turn toast and hex tooltip all show dismantling progress; the ghost bridge image fades
  out as the structure is removed.

**Dismantle countback (2026-06-13):** the dismantling indicator counts the **standing structure** back down on the
**same `N / build-required` scale** as the build, rather than starting a fresh `1 / dismantle-turns` scale. Cancel at
4/6 -> the indicator reads 4/6, 3/6, 2/6, 1/6, then gone (the ghost fades from where it was, no jump). Implemented
via `ConvInfantry.getBridgeDismantleRemaining()` with the build's required-turn denominator preserved through the
dismantle; the sprite/tooltip/toast/report 4282 all read "N of 6 still standing".

**Turn counting (banked semantics, revised 2026-06-13):** a turn of work is counted **once, at the END phase**
(`TWGameManager.checkBuildBridges` -> `ConvInfantry.bankBridgeBuildTurn`/`bankBridgeDismantleTurn`), not at
`newRound` (INITIATIVE). So the counter, the on-board indicator (`N/6`), and the dismantle length all reflect turns
**actually completed**: the indicator advances at END (not at the start of the round), and cancelling at "3/6"
dismantles 3 turns and shows "3/6 -> 0/3" (3 built, none dismantled yet, full ghost) rather than jumping. `newRound`
only performs the displacement-abandon check now. The build still completes in 6 game rounds and the END-phase
reports are unchanged. The build ghost has a faint minimum opacity so the planned bridge is visible at "0/6".

Key additions: `ConvInfantry` dismantle/resume state (`bridgeDismantleTurns`/`-RequiredTurns`,
`isDismantlingBridge()`, `isBusyWithBridge()`, `startBridgeDismantle()`, `resumeBridgeBuild()`,
`getBridgeDismantleRemaining()`, `refundBridgeBuildPoints()`); `MoveStepType.CANCEL_BRIDGE` / `RESUME_BRIDGE`;
`MovePathHandler.processCancelBridgeStep`/`processResumeBridgeStep`;
`TWGameManager.progressBridgeBuild`/`progressBridgeDismantle`; three-mode button in
`MovementDisplay.updateBridgeBuildButton` (Build / Cancel / Resume); reports 4281-4284; messages
`MovementDisplay.moveCancelBridge*` / `moveResumeBridge*`, `ClientGUI.bridgeDismantleProgress.toast`,
`BoardView1.Tooltip.BridgeDismantling`. Tests added for the dismantle lifecycle, countback, resume,
displacement-no-refund, movement-only eligibility and serialization.

## Bridge section repair (unofficial, added 2026-06-17)

An **unofficial** extension (not part of any published ruleset) lets a Bridge-Building Engineer platoon rebuild a
single **destroyed section** (a missing hex) of an existing bridge. It reuses the entire build lifecycle; only the
finished section's deck placement differs.

**Game option**: `OptionsConstants.UNOFFICIAL_BRIDGE_REPAIR_ENGINEERS` (`"unofficial_bridge_repair_engineers"`),
registered in `advancedRules`, default off. Its displayable name contains "(Unofficial)" so the options dialog hides
it behind the *Unofficial Opts* toggle. It is **only effective when the RAW `bridge_building_engineers` option is also
on** - every gate (`MoveStep.isValidBridgeBuildStep`, `MovementDisplay.canSelectBridgeBuild`, the step processor)
checks the base option first, so repair cannot be declared without it.

**User decisions (2026-06-17)**:

- *Footing*: the engineer may work while standing on an **adjacent surviving bridge deck** (enables mid-span repairs
  over water, where the only adjacent footing is the span itself). Adjacency is the existing distance-1 check.
- *Cost*: a repair **spends one bridge-building point** (Light 1 / Medium 2), refunded on dismantle, exactly like a
  fresh build.
- *Strength*: the player picks **Light (CF 15) or Medium (CF 40)**, doubled over water - the kit's own values, which
  may be weaker than the original section. CF is not matched to the destroyed section.
- *Toggle*: its own unofficial option that requires the TacOps bridge-building option (above).

**What is a repairable gap** (`BridgeConstruction.bridgeRepairIssue`): the target hex holds no structure, and at least
one connected neighbor is a **surviving span pointing into the hex** (that span also fixes the repaired deck height).
The far side must reconnect the run - either a second surviving span (mid-span gap, orientation fully fixed) or, with
one span, the **straight hexside opposite** the span onto a bank a unit can use (end-of-run gap). A bent repair to an
arbitrary side bank is rejected so the section stays in the bridge's line; an originally **curved end span** is the
one case this cannot repair (rejected as `FAR_SIDE_UNANCHORED`, logged). Underlying rubble/water left by the collapse
is preserved (only bridge terrain is added).

**Deck matching**: `BridgeConstruction.placeRepairedBridge` sets the section's `BRIDGE_ELEV` to the **surviving span's
deck** (not the lowest bank, as a fresh build does), so the repaired hex lines up with the rest of the run. The two
placement methods share a private `addBridgeStructure` helper.

**State / flow**: `ConvInfantry.bridgeBuildIsRepair` (serialized, non-transient) records the intent;
`startBridgeRepair()` sets it. `MovePathHandler.processBuildBridgeStep` derives repair-vs-build from the option plus
`isBridgeRepairSite` (no new `MoveStep` field) and routes to `startBridgeRepair`; `TWGameManager.finishBridgeBuild`
re-validates with the matching predicate and calls `placeRepairedBridge`. UI: `MovementDisplay.addRepairPlansForGap`
surfaces gap hexes (orientation fixed by the span, not the engineer's facing) when the option is on. Reports 4288
(repair started) / 4289 (section repaired); `[BridgeRepair]`-tagged diagnostic logging throughout.

**Player wording (repair vs new)**: the same engineer button reads "Build / Repair Bridge" when the repair option is
on; the declaration toast, per-round progress toast and the in-progress hex tooltip (building / paused / dismantling
variants) all say "repair / section" when `isBridgeBuildRepair()`; END-phase reports 4288/4289 read "repairing /
repairs a destroyed bridge section". Clicking a non-repairable gap explains why (`bridgeRepairClickReason` maps the
`BridgeRepairIssue` to a toast, e.g. "the far side is open water or out of line").

**Repair badge (distinct graphic)**: a finished repair carries a persistent marker terrain
`Terrains.BRIDGE_REPAIRED` (id 60, in `AUTOMATIC` so it is hidden from the board editor and not written to `.board`
files, but rides the hex through save games and `sendChangedHex` sync). `placeRepairedBridge` adds it;
`Board.removeBuilding` strips it when the section is destroyed. The board view draws a small amber hazard-stripe
badge on marked hexes via `BridgeRepairedSprite` + `BridgeRepairedSpriteHandler` (registered in `ClientGUI`,
scans boards on phase/board-change events) - no new tileset art, survives save/reload, shows on every client.

**Tests** (`BridgeBuildingTest`): end-gap and mid-span gaps validate; no-span and still-a-bridge hexes reject; the
bent-repair rejection; the repaired deck matches the elevated span rather than the lower bank; a full 6-turn repair
from bridge-deck footing places the section at the run's deck/CF, marks it BRIDGE_REPAIRED, and registers it; a fresh
build is not marked; the repair flag survives serialization.

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

## Considered and declined

- **Showing a bridge's absolute deck level in the hex tooltip** (2026-06-13). A playtest read a bridge as
  "level 4" while its bank hex was "level 3" and questioned the 1-MP entry. Investigation: no bug - the bridge hex
  was level -1, so `BRIDGE_ELEV 4` puts the deck at absolute level 3, **at grade** with the level-3 bank (1 MP is
  correct). The "level 4" was the tooltip's relative **Height** (`BRIDGE_ELEV`). Declined to change it: the bridge
  tooltip line (`BoardView1.Tooltip.Bridge` = "Height: {bridge_elev}, CF: ...") is **shared by all bridges** -
  board-authored, engineer-built, and the future vehicle bridge-layers all use it, and a completed engineer bridge
  has no special tooltip. An engineer-only tweak would give players two ways to read bridges; changing the shared
  line would alter every bridge on every board and diverge from the long-standing MM/Board Editor convention (height
  is relative to the hex). Keeping engineer bridges consistent with core MM was judged more important than removing
  the relative-vs-absolute reading, which applies uniformly to every bridge in the game.
