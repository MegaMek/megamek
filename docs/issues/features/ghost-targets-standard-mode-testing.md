# Ghost Targets Standard Mode - In-Game Testing Checklist

**Feature**: Standard (TO:AR) Ghost Targets implementation
**Date**: 2026-03-22
**Status**: Ready for testing

---

## Test Setup

**Required Game Options** (Game Options > Advanced Rules):

- [x] TacOps Ghost Targets: **Enabled**
- [x] Ghost Targets Mode: **Standard (Targeted)**
- [x] TacOps ECCM: **Enabled** (needed for ECCM sub-rule tests)
- [x] TacOps Angel ECM: **Enabled** (needed for Angel ECM tests)

**Recommended Map**: Small map (16x17), flat terrain, no special conditions

---

## Section 1: Game Option UI

- [ ] 1.1 - Ghost Targets Mode dropdown appears in Game Options under Advanced Rules
- [ ] 1.2 - Dropdown has two choices: "Legacy (Area Effect)" and "Standard (Targeted)"
- [ ] 1.3 - Default value is "Legacy (Area Effect)"
- [ ] 1.4 - Selecting "Standard (Targeted)" persists when reopening Game Options
- [ ] 1.5 - "Ghost Targets Maximum Penalty (Legacy only)" label shows for the max penalty option
- [ ] 1.6 - Ghost Targets Mode dropdown is only editable when TacOps Ghost Targets is checked

---

## Section 2: Equipment Mode Selection

### 2.1 Standard ECM Suite

- [ ] 2.1.1 - Unit with Guardian ECM can switch to "Ghost Targets" mode in End Phase
- [ ] 2.1.2 - ECM loses normal ECM function when in Ghost Targets mode
- [ ] 2.1.3 - Mode change takes effect next turn (not instant)

### 2.2 Angel ECM Suite

- [ ] 2.2.1 - Angel ECM shows "ECM & Ghost Targets" combined mode option
- [ ] 2.2.2 - Angel ECM shows "ECCM & Ghost Targets" combined mode option (when ECCM enabled)
- [ ] 2.2.3 - Angel ECM in combined mode retains ECM/ECCM function AND generates ghost targets

### 2.3 Communications Equipment (7+ tons)

- [ ] 2.3.1 - Comms Equipment (7+ tons) shows "Ghost Targets" mode option
- [ ] 2.3.2 - Comms Equipment with less than 7 tons does NOT show Ghost Targets mode
- [ ] 2.3.3 - Comms Equipment cannot be in both "ECCM" and "Ghost Targets" simultaneously

### 2.4 Vehicle Cockpit Command Console

- [ ] 2.4.1 - Vehicle CCC shows "Default" and "Ghost Targets" mode options
- [ ] 2.4.2 - Mode change takes effect next turn

---

## Section 3: PRE_FIRING Phase UI (Human Player)

**Setup**: Deploy a unit with ECM in Ghost Targets mode. Start a game, advance to PRE_FIRING phase.

### 3.1 Ghost Target Button

- [ ] 3.1.1 - "Ghost Target" button appears in PRE_FIRING phase button panel
- [ ] 3.1.2 - Button is enabled when the selected unit has ghost-target-capable equipment in GT mode
- [ ] 3.1.3 - Button is NOT visible when Ghost Targets option is disabled
- [ ] 3.1.4 - Button is NOT visible when Legacy mode is selected
- [ ] 3.1.5 - Button is NOT visible in PRE_MOVEMENT phase
- [ ] 3.1.6 - Button is disabled/hidden when unit has no qualifying equipment in GT mode

### 3.2 Target Selection

- [ ] 3.2.1 - Clicking "Ghost Target" shows "Select a target for Ghost Targets..." in status bar
- [ ] 3.2.2 - Clicking a friendly unit assigns as defensive (status bar shows "protect")
- [ ] 3.2.3 - Clicking an enemy unit assigns as offensive (status bar shows "jam")
- [ ] 3.2.4 - After assignment, ghost target mode exits (button becomes clickable again for additional equipment)

### 3.3 Validation

- [ ] 3.3.1 - Clicking a unit beyond 6 hexes shows "out of ghost target range" error
- [ ] 3.3.2 - Clicking conventional infantry shows "immune to ghost targets" error
- [ ] 3.3.3 - If unit has no more qualifying equipment, shows "no ghost target equipment available"

### 3.4 Multiple Equipment

- [ ] 3.4.1 - Unit with 2+ qualifying equipment (e.g., ECM + Comms): can click "Ghost Target" again after first
  assignment to use second equipment
- [ ] 3.4.2 - Same equipment cannot be assigned twice in one turn

---

## Section 4: Ghost Target Resolution (Firing Phase Reports)

**Setup**: Assign ghost targets in PRE_FIRING, then advance to FIRING phase.

### 4.1 Roll and Reports

- [ ] 4.1.1 - "--- Ghost Target Assignments ---" header appears in firing phase report
- [ ] 4.1.2 - Each assignment shows: source unit, target unit, target number (Piloting+3), roll result
- [ ] 4.1.3 - Successful roll shows "success! +1 to-hit modifier applied."
- [ ] 4.1.4 - Failed roll shows "No ghost targets."
- [ ] 4.1.5 - Target number is Piloting Skill + 3 (no other modifiers)

### 4.2 ProtoMek Roll

- [ ] 4.2.1 - ProtoMek uses Gunnery + 3 instead of Piloting + 3

---

## Section 5: To-Hit Modifier Application

### 5.1 Defensive Ghost Target (Friendly Target)

- [ ] 5.1.1 - Successful friendly ghost target adds +1 to attacks AGAINST that unit
- [ ] 5.1.2 - Modifier shows as "ghost targets (defensive)" in to-hit breakdown
- [ ] 5.1.3 - Multiple successful friendly GT on same target stack (verify +2 with 2 successes)
- [ ] 5.1.4 - Cap at +3: third successful friendly GT on same target caps at +3, fourth has no additional effect

### 5.2 Offensive Ghost Target (Enemy Target)

- [ ] 5.2.1 - Successful enemy ghost target adds +1 to attacks BY that unit
- [ ] 5.2.2 - Modifier shows as "ghost targets (offensive)" in to-hit breakdown
- [ ] 5.2.3 - Multiple successful enemy GT on same target stack (verify +2 with 2 successes)
- [ ] 5.2.4 - Cap at +3: third successful enemy GT on same target caps at +3

### 5.3 Combined Effects

- [ ] 5.3.1 - A unit can have both defensive (+N to attacks against it) and offensive (+N to its attacks) simultaneously
- [ ] 5.3.2 - Each capped independently at +3

---

## Section 6: Active Probe Immunity

**Setup**: Attacker has a functioning Active Probe (BAP/Beagle/Bloodhound).

- [ ] 6.1 - Attacker with active probe ignores ALL ghost target modifiers (both defensive and offensive)
- [ ] 6.2 - No "ghost targets" line appears in to-hit breakdown for attacker with active probe
- [ ] 6.3 - Active probe immunity works regardless of probe range (even 0-hex infantry probe)
- [ ] 6.4 - Active probe that is jammed by ECM does NOT provide immunity (modifier applies)
- [ ] 6.5 - Attacker with active stealth armor + active probe does NOT get immunity (modifier applies)

---

## Section 7: Conventional Infantry Immunity

- [ ] 7.1 - Conventional infantry attacker ignores all ghost target to-hit modifiers
- [ ] 7.2 - Cannot select conventional infantry as a ghost target in PRE_FIRING (error message shown)

---

## Section 8: ECCM Suppression

**Setup**: Unit with ECM in Ghost Targets mode is in a hex affected by enemy ECM that exceeds friendly ECCM.

- [ ] 8.1 - Ghost target generation fails with "ECCM suppressed by enemy ECM" report message
- [ ] 8.2 - No roll is made when suppressed
- [ ] 8.3 - Unit NOT under enemy ECM can generate ghost targets normally

---

## Section 9: Stealth Armor + Angel ECM

**Setup**: Unit with active stealth armor and Angel ECM in ghost targets mode.

- [ ] 9.1 - Unit CAN generate ghost targets (not blocked)
- [ ] 9.2 - On successful ghost target, report shows "+1 to own attacks" self-penalty message
- [ ] 9.3 - The generating unit's own ranged attacks have +1 penalty applied
- [ ] 9.4 - Self-penalty stacks with enemy ghost targets applied to the same unit

---

## Section 10: Princess Bot Behavior

**Setup**: Play against a Princess bot that has units with ECM in Ghost Targets mode.

- [ ] 10.1 - Bot assigns ghost targets during PRE_FIRING phase (doesn't hang/crash)
- [ ] 10.2 - Bot targets nearby enemies when available (offensive ghost targets)
- [ ] 10.3 - Bot protects self when no enemies in range (defensive ghost targets)
- [ ] 10.4 - Bot handles multiple qualifying equipment on one unit

---

## Section 11: Cockpit Command Console (Mek)

**Setup**: Heavy or Assault Mek with Cockpit Command Console and active second pilot.

- [ ] 11.1 - "Ghost Target" button is available for Mek CCC units in PRE_FIRING
- [ ] 11.2 - CCC ghost target is independent of any ECM ghost targets on the same unit
- [ ] 11.3 - CCC ghost target has 6 hex range
- [ ] 11.4 - CCC ghost target rolls Piloting + 3

---

## Section 12: Legacy Mode Compatibility

**Setup**: Change Ghost Targets Mode back to "Legacy (Area Effect)".

- [ ] 12.1 - No "Ghost Target" button appears in PRE_FIRING phase
- [ ] 12.2 - Ghost target rolls happen automatically at start of firing phase (old behavior)
- [ ] 12.3 - To-hit modifier is calculated via hex-path tracing (old area effect behavior)
- [ ] 12.4 - Ghost Targets Maximum Penalty option is respected
- [ ] 12.5 - BAP gives -1 reduction (not full immunity)
- [ ] 12.6 - Targeting Computer gives -2 reduction

---

## Section 13: Round Boundary / Persistence

- [ ] 13.1 - Ghost target bonuses reset each round (do not carry over)
- [ ] 13.2 - New assignments must be made each PRE_FIRING phase
- [ ] 13.3 - Destroyed unit that was a ghost target no longer has bonus after destruction
- [ ] 13.4 - Unit that loses ECM equipment mid-game can no longer generate ghost targets

---

## Section 14: Edge Cases

- [ ] 14.1 - Self-targeting: unit can assign ghost target to itself (defensive)
- [ ] 14.2 - Indirect fire is NOT affected by ghost targets (per existing rule)
- [ ] 14.3 - Artillery is NOT affected by ghost targets
- [ ] 14.4 - Save/load game preserves ghost target game options correctly
- [ ] 14.5 - No crash when switching between Legacy and Standard modes mid-lobby

---

## Test Results Summary

| Section               | Pass | Fail | Skip | Notes |
|-----------------------|------|------|------|-------|
| 1. Game Option UI     |      |      |      |       |
| 2. Equipment Modes    |      |      |      |       |
| 3. PRE_FIRING UI      |      |      |      |       |
| 4. Resolution Reports |      |      |      |       |
| 5. To-Hit Modifiers   |      |      |      |       |
| 6. Active Probe       |      |      |      |       |
| 7. Infantry Immunity  |      |      |      |       |
| 8. ECCM Suppression   |      |      |      |       |
| 9. Stealth + Angel    |      |      |      |       |
| 10. Princess Bot      |      |      |      |       |
| 11. CCC (Mek)         |      |      |      |       |
| 12. Legacy Compat     |      |      |      |       |
| 13. Round Boundary    |      |      |      |       |
| 14. Edge Cases        |      |      |      |       |

**Overall Result**: [ ] PASS / [ ] FAIL

**Tester**: _______________
**Date Completed**: _______________
**Notes**: _______________
