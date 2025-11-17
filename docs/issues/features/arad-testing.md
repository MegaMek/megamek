# ARAD Missile Testing Plan

**Document Version:** 1.0
**Date:** 2025-01-16
**Status:** In Progress - LRM Testing 25% Complete
**Related Doc:** `arad-implementation.md`

---

## Table of Contents

1. [Testing Overview](#testing-overview)
2. [Test Environment Setup](#test-environment-setup)
3. [LRM ARAD Testing (Phase 5)](#lrm-arad-testing-phase-5)
4. [SRM ARAD Testing (Phase 6)](#srm-arad-testing-phase-6)
5. [MML ARAD Testing (Phase 6)](#mml-arad-testing-phase-6)
6. [Unit Testing Requirements (Phase 7)](#unit-testing-requirements-phase-7)
7. [Pre-Review Testing Checklist](#pre-review-testing-checklist)
8. [Edge Case Testing](#edge-case-testing)
9. [Testing Progress Tracking](#testing-progress-tracking)

---

## Testing Overview

### Testing Phases

| Phase | Weapon Type | Status | Completion |
|-------|-------------|--------|------------|
| Phase 5 | LRM ARAD | In Progress | 3/12 scenarios (25%) |
| Phase 6 | SRM ARAD | Pending | 0/12 scenarios (0%) |
| Phase 6 | MML ARAD | Pending | 0/16 scenarios (0%) |
| Phase 7 | Unit Tests | Pending | 0% |
| Phase 8 | Integration | Pending | 0% |

### Report Messages to Verify

All ARAD weapons use the same report messages:

| Report | Message | When Displayed |
|--------|---------|----------------|
| **3363** | `(ARAD bonus blocked by ECM)` | Electronics detected but ECM blocks cluster bonus |
| **3364** | `(ARAD bonus - Narc override)` | Narc pod overrides ECM blocking |
| **3368** | `(ARAD penalty - no electronics detected)` | Target has no qualifying electronics |
| **3369** | `(ARAD bonus - electronics detected)` | Normal bonus case (electronics, no ECM) |

### Expected Modifier Summary

| Scenario | To-Hit Modifier | Cluster Modifier | Parent Report |
|----------|-----------------|------------------|---------------|
| Has Electronics (no ECM) | -1 | +1 | `(w/ +1 bonus)` |
| ECM Blocking | -1 | 0 | None (blocked) |
| Narc Override | -1 | +1 | `(w/ +1 bonus)` |
| No Electronics | +2 | -2 | `(w/ -2 malus)` |

---

## Test Environment Setup

### Required Test Units

**Attacker Units (Player-Controlled):**
- Archer ARC-2S (LRM 15 x2, SRM 4 x2)
- Catapult CPLT-C1 (LRM 15 x2)
- Any MML-equipped mech (for Phase 6)

**Target Units (Princess-Controlled):**
1. **Electronics Targets:**
   - Cyclops CP-11-C (C3 Master, no ECM)
   - Phoenix Hawk PXH-2 (Guardian ECM)
   - Any unit with Active Probe only
   - Mobile HQ vehicle (Heavy Comms)

2. **No Electronics Targets:**
   - Charger CGR-1A1 (no electronics)
   - UrbanMech UM-R60 (no electronics)
   - Standard vehicles (built-in comms only)

3. **Stealth Targets:**
   - Any mech with Stealth Armor (active)

4. **Non-Entity Targets:**
   - Buildings (Medium Building recommended)
   - Terrain hexes

### Test Map Requirements

- **ECM Testing:** Units must be within 6 hexes for ECM field coverage
- **Range Testing:**
  - LRM: 6-21 hexes (minimum 6, optimal 7-15)
  - SRM: 0-3 hexes
  - MML: Both ranges
- **LOS:** Clear line of sight preferred for consistent results
- **Terrain:** Light woods acceptable, avoid heavy modifiers

### ARAD Ammo Setup

**For Each Test:**
1. Load ARAD ammo in weapon bays
2. Verify ammo shows as "LRM 15 Anti-Radiation ammo" (or SRM/MML variant)
3. Confirm weapon can fire (ARAD compatible with LRM/SRM/MML only)
4. Check ammo count before/after firing

---

## LRM ARAD Testing (Phase 5)

### Core Functionality Tests (Scenarios 1-5)

**REQUIRED FOR PR: All 5 scenarios must pass**

#### Scenario 1: Normal Bonus (No ECM Interference)

**Objective:** Verify ARAD detects electronics and applies standard bonus

**Setup:**
- Target: Cyclops CP-11-C (C3 Master, no ECM field) OR any unit with Active Probe only
- **Critical:** Ensure NO ECM field present on board
- Range: 7-15 hexes (medium range recommended)
- LOS: Clear

**Test Steps:**
1. Select Archer with LRM 15 (ARAD ammo loaded)
2. Fire at Cyclops (or Active Probe target)
3. Record to-hit calculation
4. Record cluster hit report

**Expected Results:**
- ✅ To-Hit includes: `-1 (LRM 15 Anti-Radiation ammunition to-hit modifier)`
- ✅ Cluster report: `(ARAD bonus - electronics detected) (w/ +1 bonus)`
- ✅ Report 3369 displayed
- ✅ Parent cluster bonus displayed

**Pass Criteria:**
- To-hit modifier = -1
- Both reports appear
- Cluster modifier = +1 (verify damage is higher than expected for base hits)

**Status:** ⏳ Pending (retest - previous attempt had ECM interference)

**Notes:**
- If Report 3363 appears, ECM field is present - find new target
- Use `/showECM` command to visualize ECM coverage

---

#### Scenario 2: ECM Blocking

**Objective:** Verify ECM blocks ARAD cluster bonus but not to-hit

**Setup:**
- Target: Phoenix Hawk PXH-2 (Guardian ECM active)
- Attacker: Archer within 6 hexes of Phoenix Hawk (inside ECM bubble)
- Range: 7-15 hexes
- LOS: Clear

**Test Steps:**
1. Verify ECM field visible on board
2. Fire ARAD LRM at Phoenix Hawk
3. Record to-hit and cluster reports

**Expected Results:**
- ✅ To-Hit includes: `-1` (electronics still detected)
- ✅ Cluster report: `(ARAD bonus blocked by ECM)`
- ✅ Report 3363 displayed
- ✅ NO parent bonus report (cluster modifier = 0)

**Pass Criteria:**
- To-hit modifier = -1
- Report 3363 appears
- Cluster modifier = 0 (no bonus, no penalty)

**Status:** ✅ Passed (2025-01-16)

**Test Results:**
```
LRM 15 (LRM 15 Anti-Radiation ammo) at Phoenix Hawk PXH-2 (Princess);
needs 4, rolls 9: 9 missile(s) hit. (ARAD bonus blocked by ECM)
```

---

#### Scenario 3: Narc Override

**Objective:** Verify Narc pod overrides ECM blocking

**Setup:**
- Target: Phoenix Hawk PXH-2 (Guardian ECM active)
- **Prerequisite:** Narc pod attached to Phoenix Hawk (friendly team)
- Attacker: Archer within ECM field
- Range: 7-15 hexes

**Test Steps:**
1. **Setup Phase:** Fire Narc missile at Phoenix Hawk, confirm pod attachment
2. Next turn: Fire ARAD LRM at Narc-tagged Phoenix Hawk
3. Record to-hit and cluster reports

**Expected Results:**
- ✅ To-Hit: `-1` (electronics detected)
- ✅ Cluster report: `(ARAD bonus - Narc override) (w/ +1 bonus)`
- ✅ Report 3364 displayed
- ✅ Parent bonus report displayed

**Pass Criteria:**
- To-hit modifier = -1
- Report 3364 appears
- Cluster modifier = +1 (Narc overrides ECM)
- Parent bonus report confirms +1

**Status:** ⏳ Pending

**Notes:**
- Narc pod must be from friendly team (same owner as ARAD attacker)
- iNarc pods also work (any friendly Narc pod type)
- Narc attachment visible in unit status

---

#### Scenario 4: No Electronics Penalty

**Objective:** Verify ARAD penalty against targets without electronics

**Setup:**
- Target: Charger CGR-1A1 (no electronics)
- Range: 7-15 hexes
- LOS: Clear
- **Optional:** Target can be inside ECM field (penalty applies regardless)

**Test Steps:**
1. Fire ARAD LRM at Charger
2. Record to-hit and cluster reports

**Expected Results:**
- ✅ To-Hit includes: `+2 (LRM 15 Anti-Radiation ammunition to-hit modifier)`
- ✅ Cluster report: `(ARAD penalty - no electronics detected) (w/ -2 malus)`
- ✅ Report 3368 displayed
- ✅ Parent malus report displayed

**Pass Criteria:**
- To-hit modifier = +2
- Report 3368 appears
- Cluster modifier = -2 (minimum 2 hits enforced by engine)
- Parent malus report confirms -2

**Status:** ✅ Passed (2025-01-16)

**Test Results:**
```
LRM 15 (LRM 15 Anti-Radiation ammo) at Charger CGR-1A1 (Princess);
needs 6, rolls 10: 5 missile(s) hit (w/ -2 malus).
(ARAD penalty - no electronics detected)
```

---

#### Scenario 5: Building Target (Non-Entity)

**Objective:** Verify ARAD treats buildings/hexes as no electronics

**Setup:**
- Target: Medium Building (any hex)
- Range: 6-21 hexes (minimum range applies)
- LOS: Clear

**Test Steps:**
1. Fire ARAD SRM at building (SRM required for building attacks)
2. Record to-hit and cluster reports

**Expected Results:**
- ✅ To-Hit includes: `+2` penalty
- ✅ Cluster report: `(ARAD penalty - no electronics detected) (w/ -2 malus)`
- ✅ Report 3368 displayed

**Pass Criteria:**
- To-hit modifier = +2
- Report 3368 appears
- Cluster modifier = -2

**Status:** ⏳ Pending (requires SRM ARAD - Phase 6)

**Notes:**
- This scenario waits for SRM implementation
- Buildings cannot be targeted with LRM (indirect fire only)

---

### Advanced Interaction Tests (Scenarios 6-8)

**REQUIRED FOR PR: All 3 scenarios must be tested**

#### Scenario 6: Active Stealth Armor

**Objective:** Verify Stealth blocks internal electronics (C3, ECM, etc.)

**Setup:**
- Target: Mech with active Stealth Armor + C3 (internal system)
- **No Narc pod** attached
- Range: 7-15 hexes
- LOS: Clear

**Test Steps:**
1. Verify target has Stealth active (check unit status)
2. Verify target has C3 or other internal electronics
3. Fire ARAD LRM at target
4. Record results

**Expected Results:**
- ✅ To-Hit: `+2` (Stealth blocks electronics detection)
- ✅ Cluster report: `(ARAD penalty - no electronics detected) (w/ -2 malus)`
- ✅ Report 3368 displayed

**Pass Criteria:**
- To-hit modifier = +2 (Stealth blocks internal systems)
- Report 3368 appears (treated as no electronics)
- Cluster modifier = -2

**Status:** ⏳ Pending

**Notes:**
- Stealth must be **active** (powered and not destroyed)
- Internal systems (C3, ECM, Active Probe) are invisible to ARAD when Stealth active
- Only external Narc pods work through Stealth

---

#### Scenario 7: Stealth + Narc Override

**Objective:** Verify Narc pod works through Stealth Armor

**Setup:**
- Target: Mech with active Stealth Armor + internal electronics
- **Friendly Narc pod attached**
- Range: 7-15 hexes

**Test Steps:**
1. Verify Stealth active
2. Attach Narc pod to target
3. Fire ARAD LRM at Narc-tagged Stealth target
4. Record results

**Expected Results:**
- ✅ To-Hit: `-1` (Narc detected through Stealth)
- ✅ Cluster report: `(ARAD bonus - Narc override) (w/ +1 bonus)`
- ✅ Report 3364 displayed

**Pass Criteria:**
- To-hit modifier = -1 (Narc overrides Stealth)
- Report 3364 appears (Narc works as external system)
- Cluster modifier = +1

**Status:** ⏳ Pending

**Notes:**
- This verifies external Narc pods bypass Stealth blocking
- Same logic as Narc overriding ECM, but for Stealth

---

#### Scenario 8: Multiple Systems (No Stacking)

**Objective:** Verify multiple electronics don't stack bonuses

**Setup:**
- Target: Mech with C3 + ECM equipment + Active Probe (all active)
- **No ECM field** (ECM equipment present but field off or out of range)
- Range: 7-15 hexes

**Test Steps:**
1. Verify target has multiple electronics (3+)
2. Ensure no ECM field affecting attacker
3. Fire ARAD LRM
4. Record results

**Expected Results:**
- ✅ To-Hit: `-1` (NOT -3 or cumulative)
- ✅ Cluster report: `(ARAD bonus - electronics detected) (w/ +1 bonus)`
- ✅ Report 3369 displayed
- ✅ Cluster modifier: +1 (NOT +3)

**Pass Criteria:**
- To-hit modifier = -1 (single bonus)
- Report 3369 appears
- Cluster modifier = +1 (single bonus, not cumulative)

**Status:** ⏳ Pending

**Notes:**
- ARAD is binary: electronics detected (bonus) or not (penalty)
- Multiple systems don't increase bonus
- Only one qualifying system needed

---

### Edge Case Tests (Scenarios 9-12)

**RECOMMENDED BUT OPTIONAL for PR**

#### Scenario 9: Powered-Down Equipment

**Objective:** Verify powered-off electronics don't trigger ARAD

**Setup:**
- Target: Mech with C3 Master
- **C3 shut down** (powered off via heat/shutdown)
- No other electronics
- Range: 7-15 hexes

**Test Steps:**
1. Shut down target mech's C3 system
2. Verify equipment shows as inactive
3. Fire ARAD LRM
4. Record results

**Expected Results:**
- ✅ To-Hit: `+2` (powered-down = no electronics)
- ✅ Cluster report: `(ARAD penalty - no electronics detected) (w/ -2 malus)`
- ✅ Report 3368 displayed

**Pass Criteria:**
- To-hit modifier = +2
- Report 3368 appears
- Powered-off equipment not detected

**Status:** ⏳ Pending

**Notes:**
- Difficult to test (requires equipment destruction or mech shutdown)
- May require custom scenario setup

---

#### Scenario 10: Mobile Headquarters (Heavy Comms)

**Objective:** Verify vehicles with ≥3.5 tons comms trigger ARAD

**Setup:**
- Target: Mobile HQ vehicle with dedicated comms equipment
- Verify comms ≥3.5 tons (check unit details)
- Range: 7-15 hexes

**Test Steps:**
1. Confirm target has Heavy Comms equipment
2. Fire ARAD LRM
3. Record results

**Expected Results:**
- ✅ To-Hit: `-1` (Heavy Comms detected)
- ✅ Cluster report: `(ARAD bonus - electronics detected) (w/ +1 bonus)`
- ✅ Report 3369 displayed

**Pass Criteria:**
- To-hit modifier = -1
- Report 3369 appears
- Heavy Comms triggers ARAD bonus

**Status:** ⏳ Pending

**Notes:**
- Mobile HQ units typically have ≥3.5 tons dedicated comms
- Standard vehicles (1-ton built-in comms) should NOT trigger bonus

---

#### Scenario 11: Standard Vehicle (Built-in Comms Only)

**Objective:** Verify built-in comms (≤1 ton) don't trigger ARAD

**Setup:**
- Target: Vedette Medium Tank (or any standard vehicle)
- Only built-in comms (1 ton)
- No additional electronics
- Range: 7-15 hexes

**Test Steps:**
1. Verify target has only built-in comms
2. Fire ARAD LRM
3. Record results

**Expected Results:**
- ✅ To-Hit: `+2` (built-in comms don't count)
- ✅ Cluster report: `(ARAD penalty - no electronics detected) (w/ -2 malus)`
- ✅ Report 3368 displayed

**Pass Criteria:**
- To-hit modifier = +2
- Report 3368 appears
- Built-in comms ignored

**Status:** ⏳ Pending

**Notes:**
- This verifies Heavy Comms threshold (≥3.5 tons)
- Standard vehicles treated as "cold" targets

---

#### Scenario 12: Indirect Fire + ARAD

**Objective:** Verify ARAD modifiers work with indirect fire

**Setup:**
- Target: Narc-tagged mech with electronics (behind cover)
- Attacker: No LOS (indirect fire required)
- Spotter: Friendly unit with LOS to target
- Range: 6-21 hexes

**Test Steps:**
1. Confirm no direct LOS to target
2. Narc tag target with spotter
3. Fire ARAD LRM indirect at Narc-tagged target
4. Record results

**Expected Results:**
- ✅ To-Hit: Standard indirect penalties + ARAD `-1`
- ✅ Cluster report: ARAD bonus still applies
- ✅ Report 3369 or 3364 displayed

**Pass Criteria:**
- ARAD to-hit modifier applies to indirect fire
- ARAD cluster modifier applies
- Indirect fire penalties stack normally

**Status:** ⏳ Pending

**Notes:**
- Complex scenario - may require specific map setup
- Narc required for indirect ARAD (provides electronic signature)

---

## SRM ARAD Testing (Phase 6)

### Overview

SRM ARAD uses identical mechanics to LRM ARAD but with different range brackets:
- **Range:** 0-3 hexes (close range)
- **Same modifiers:** -1/+2 to-hit, +1/-2 cluster
- **Same reports:** 3363, 3364, 3368, 3369

### Core Functionality Tests (Scenarios 1-5)

**Test scenarios are identical to LRM but at short range (0-3 hexes):**

#### SRM Scenario 1: Normal Bonus (No ECM)

**Setup:**
- Range: 0-3 hexes
- Target: Cyclops or Active Probe mech
- No ECM field

**Expected:**
- To-Hit: `-1`
- Cluster: `(ARAD bonus - electronics detected) (w/ +1 bonus)`

**Status:** ⏳ Pending (Phase 6)

---

#### SRM Scenario 2: ECM Blocking

**Setup:**
- Range: 0-3 hexes (inside ECM bubble)
- Target: Phoenix Hawk with ECM active

**Expected:**
- To-Hit: `-1`
- Cluster: `(ARAD bonus blocked by ECM)`

**Status:** ⏳ Pending (Phase 6)

---

#### SRM Scenario 3: Narc Override

**Setup:**
- Range: 0-3 hexes
- Target: Narc-tagged Phoenix Hawk (ECM active)

**Expected:**
- To-Hit: `-1`
- Cluster: `(ARAD bonus - Narc override) (w/ +1 bonus)`

**Status:** ⏳ Pending (Phase 6)

---

#### SRM Scenario 4: No Electronics

**Setup:**
- Range: 0-3 hexes
- Target: Charger (no electronics)

**Expected:**
- To-Hit: `+2`
- Cluster: `(ARAD penalty - no electronics detected) (w/ -2 malus)`

**Status:** ⏳ Pending (Phase 6)

---

#### SRM Scenario 5: Building Target

**Setup:**
- Range: 0-3 hexes
- Target: Medium Building

**Expected:**
- To-Hit: `+2`
- Cluster: `(ARAD penalty - no electronics detected) (w/ -2 malus)`

**Status:** ⏳ Pending (Phase 6)

**Notes:**
- SRM can target buildings (unlike LRM)
- Verifies non-entity handling for SRM

---

### SRM Advanced Tests (Scenarios 6-8)

**Identical to LRM at 0-3 hex range:**

- **SRM Scenario 6:** Stealth Armor (penalty expected)
- **SRM Scenario 7:** Stealth + Narc (bonus expected)
- **SRM Scenario 8:** Multiple Systems (single bonus only)

**Status:** ⏳ All Pending (Phase 6)

---

### SRM Edge Cases (Scenarios 9-12)

**Identical to LRM at 0-3 hex range:**

- **SRM Scenario 9:** Powered-Down Equipment
- **SRM Scenario 10:** Mobile HQ (Heavy Comms)
- **SRM Scenario 11:** Standard Vehicle (built-in comms only)
- **SRM Scenario 12:** N/A (SRM doesn't use indirect fire)

**Status:** ⏳ All Pending (Phase 6)

**Notes:**
- SRM Scenario 12 skipped (no indirect fire for SRM)
- SRM total scenarios: 11 (vs LRM's 12)

---

## MML ARAD Testing (Phase 6)

### Overview

MML ARAD requires testing BOTH LRM and SRM modes:
- **LRM Mode:** 6-21 hexes (same as LRM ARAD)
- **SRM Mode:** 0-3 hexes (same as SRM ARAD)
- **Mode Switching:** Verify ARAD works after mode changes

### Core Functionality Tests

**MML requires 16 scenarios: 12 LRM mode + 4 SRM mode (skip duplicates)**

#### MML LRM Mode (Scenarios 1-12)

**Repeat all LRM scenarios using MML in LRM mode:**
- Range: 6-21 hexes
- MML configured for LRM ammo
- All expected results identical to LRM ARAD

**Status:** ⏳ All Pending (Phase 6)

---

#### MML SRM Mode (Scenarios 1-4)

**Test core SRM scenarios only (skip duplicates):**
1. Normal Bonus (0-3 hexes)
2. ECM Blocking (0-3 hexes)
3. Narc Override (0-3 hexes)
4. No Electronics (0-3 hexes)

**Status:** ⏳ All Pending (Phase 6)

---

#### MML Mode Switching Test

**Objective:** Verify ARAD works after switching MML modes

**Setup:**
- MML weapon loaded with ARAD ammo
- Turn 1: Fire LRM mode (6+ hexes)
- Turn 2: Fire SRM mode (0-3 hexes)

**Expected:**
- Both modes use ARAD logic correctly
- Mode switch doesn't break ARAD detection

**Status:** ⏳ Pending (Phase 6)

---

## Unit Testing Requirements (Phase 7)

### Required Unit Tests

**File:** `megamek/unittests/megamek/common/weapons/handlers/lrm/LRMARADHandlerTest.java`

#### Test 1: Equipment Detection

**Method:** `testTargetHasQualifyingElectronics()`

**Test Cases:**
```java
@Test
void testC3MasterDetection() {
    // Setup mock entity with C3 Master
    // Assert: targetHasQualifyingElectronics() returns true
}

@Test
void testECMDetection() {
    // Setup mock entity with Guardian ECM
    // Assert: targetHasQualifyingElectronics() returns true
}

@Test
void testActiveProbeDetection() {
    // Setup mock entity with Active Probe
    // Assert: targetHasQualifyingElectronics() returns true
}

@Test
void testNoElectronicsDetection() {
    // Setup mock entity with no electronics
    // Assert: targetHasQualifyingElectronics() returns false
}

@Test
void testStealthBlocksInternalSystems() {
    // Setup mock entity with Stealth + C3
    // Assert: targetHasQualifyingElectronics() returns false (no Narc)
}

@Test
void testNarcOverridesStealthBlocking() {
    // Setup mock entity with Stealth + Narc tag
    // Assert: targetHasQualifyingElectronics() returns true
}
```

---

#### Test 2: Cluster Modifier Logic

**Method:** `testGetSalvoBonus()`

**Test Cases:**
```java
@Test
void testNormalBonus() {
    // Electronics detected, no ECM
    // Assert: getSalvoBonus() returns +1
}

@Test
void testECMBlocking() {
    // Electronics detected, ECM present, no Narc
    // Assert: getSalvoBonus() returns 0
}

@Test
void testNarcOverridesECM() {
    // Electronics detected, ECM present, Narc tagged
    // Assert: getSalvoBonus() returns +1
}

@Test
void testNoElectronicsPenalty() {
    // No electronics detected
    // Assert: getSalvoBonus() returns -2
}

@Test
void testNonEntityTarget() {
    // Building/hex target
    // Assert: getSalvoBonus() returns -2
}
```

---

#### Test 3: Report Message Display

**Method:** `testCalcHits()`

**Test Cases:**
```java
@Test
void testReport3369Display() {
    // Normal bonus scenario
    // Assert: vPhaseReport contains Report(3369)
}

@Test
void testReport3363Display() {
    // ECM blocking scenario
    // Assert: vPhaseReport contains Report(3363)
}

@Test
void testReport3364Display() {
    // Narc override scenario
    // Assert: vPhaseReport contains Report(3364)
}

@Test
void testReport3368Display() {
    // No electronics scenario
    // Assert: vPhaseReport contains Report(3368)
}
```

---

#### Test 4: To-Hit Modifiers

**File:** `megamek/unittests/megamek/common/actions/WeaponAttackActionToHitTest.java`

**Test Cases:**
```java
@Test
void testARADToHitBonus() {
    // Electronics detected
    // Assert: toHit modifier = -1
}

@Test
void testARADToHitPenalty() {
    // No electronics detected
    // Assert: toHit modifier = +2
}
```

---

### SRM ARAD Unit Tests

**File:** `megamek/unittests/megamek/common/weapons/handlers/srm/SRMARADHandlerTest.java`

**Test Cases:**
- Mirror all LRM tests for SRM handler
- Same logic, different range brackets
- Verify building targeting works

**Status:** ⏳ Pending (Phase 6)

---

### MML ARAD Unit Tests

**File:** `megamek/unittests/megamek/common/weapons/handlers/mml/MMLARADHandlerTest.java`

**Test Cases:**
- Mirror all LRM tests for MML LRM mode
- Mirror all SRM tests for MML SRM mode
- Add mode-switching test

**Status:** ⏳ Pending (Phase 6)

---

## Pre-Review Testing Checklist

**Complete this checklist before requesting code review:**

### Functional Testing

- [ ] **LRM ARAD:** All 8 core/advanced scenarios pass (1-8)
- [ ] **SRM ARAD:** All 7 core/advanced scenarios pass
- [ ] **MML ARAD:** All 16 scenarios pass (12 LRM + 4 SRM)
- [ ] **Report Messages:** All 4 reports (3363, 3364, 3368, 3369) verified in-game
- [ ] **Edge Cases:** At least 2/4 edge case scenarios tested

### Unit Testing

- [ ] **LRM Tests:** All unit tests pass (100% coverage)
- [ ] **SRM Tests:** All unit tests pass (100% coverage)
- [ ] **MML Tests:** All unit tests pass (100% coverage)
- [ ] **Build:** `gradlew test` passes with no failures
- [ ] **Code Coverage:** ≥80% coverage for ARAD handlers

### Code Quality

- [ ] **Compilation:** `gradlew compileJava` succeeds
- [ ] **No Warnings:** Code analysis shows no errors/warnings
- [ ] **Style Guide:** MegaMek Java style guide followed
- [ ] **JavaDoc:** All public methods documented
- [ ] **Logging:** No debug logging in production code
- [ ] **Unicode:** No Unicode characters in code/logs

### Localization

- [ ] **English:** All 4 reports present in `report-messages.properties`
- [ ] **German:** All 4 reports present in `report-messages_de.properties`
- [ ] **Spanish:** All 4 reports present in `report-messages_es.properties`
- [ ] **Russian:** All 4 reports present in `report-messages_ru.properties`
- [ ] **Verification:** No duplicate keys, no trailing spaces

### Documentation

- [ ] **Implementation Doc:** `arad-implementation.md` updated with commits
- [ ] **Testing Doc:** `arad-testing.md` updated with results
- [ ] **CHANGELOG:** Session summary added
- [ ] **Commit Messages:** All commits reference issue number

---

## Edge Case Testing

### Rare Scenarios

#### Ghost Targets

**Setup:** TAG designator creates ghost target

**Expected:** ARAD treats as electronics present (forum ruling)

**Test:** Fire ARAD at TAG ghost target, verify bonus applies

**Status:** ⏳ Pending

---

#### Enemy vs Friendly Narc

**Setup:** Enemy Narc pod on target

**Expected:** Enemy Narc doesn't override ECM

**Test:** Fire ARAD at target with enemy Narc in ECM field

**Status:** ⏳ Pending

---

#### Artemis Incompatibility

**Setup:** Artemis IV LRM with ARAD ammo

**Expected:** ARAD ammo cannot be loaded (incompatible)

**Test:** Verify ammo selection excludes ARAD for Artemis launchers

**Status:** ⏳ Pending

---

#### Streak Incompatibility

**Setup:** Streak SRM with ARAD ammo

**Expected:** ARAD ammo cannot be loaded (incompatible)

**Test:** Verify ammo selection excludes ARAD for Streak launchers

**Status:** ⏳ Pending

---

## Testing Progress Tracking

### Overall Completion

| Category | Scenarios | Completed | Percentage |
|----------|-----------|-----------|------------|
| **LRM Core (1-5)** | 5 | 2 | 40% |
| **LRM Advanced (6-8)** | 3 | 0 | 0% |
| **LRM Edge (9-12)** | 4 | 0 | 0% |
| **SRM Core (1-5)** | 5 | 0 | 0% |
| **SRM Advanced (6-8)** | 3 | 0 | 0% |
| **SRM Edge (9-11)** | 3 | 0 | 0% |
| **MML LRM Mode** | 12 | 0 | 0% |
| **MML SRM Mode** | 4 | 0 | 0% |
| **Unit Tests** | ~20 | 0 | 0% |
| **Edge Cases** | 4 | 0 | 0% |
| **TOTAL** | 63 | 2 | 3% |

### Testing Priority

**Week 1: LRM ARAD (Current)**
- [x] Scenario 2: ECM Blocking ✅
- [x] Scenario 4: No Electronics ✅
- [ ] Scenario 1: Normal Bonus (retest)
- [ ] Scenario 3: Narc Override
- [ ] Scenarios 6-8: Advanced tests

**Week 2: SRM ARAD**
- [ ] Implement SRMARADHandler
- [ ] Test Scenarios 1-5
- [ ] Test Scenarios 6-8

**Week 3: MML ARAD**
- [ ] Implement MMLARADHandler
- [ ] Test LRM mode (1-12)
- [ ] Test SRM mode (1-4)

**Week 4: Unit Tests**
- [ ] Write LRM unit tests
- [ ] Write SRM unit tests
- [ ] Write MML unit tests
- [ ] Achieve 80%+ coverage

**Week 5: Pre-Review**
- [ ] Complete edge case testing
- [ ] Update all documentation
- [ ] Run full test suite
- [ ] Prepare PR description

---

## Session Notes

### 2025-01-16 Session

**Completed:**
- ✅ LRM Scenario 2 (ECM Blocking) - Report 3363 verified
- ✅ LRM Scenario 4 (No Electronics) - Report 3368 verified
- ⚠️ LRM Scenario 1 (Normal Bonus) - ECM interference, needs retest

**Discoveries:**
- Added Report 3369 for normal bonus clarity
- User feedback: "+1 bonus confusing without context"
- All ARAD scenarios now have explicit messages

**Next Session:**
- Retest Scenario 1 with C3-only target (no ECM)
- Test Scenario 3 (Narc Override)
- Begin Advanced scenarios (6-8)

---

**END OF TESTING DOCUMENT**
