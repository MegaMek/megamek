# Nova CEWS Manual Testing Scenarios

**Purpose**: Comprehensive testing guide for Nova CEWS implementation
**Status**: Ready for execution
**Related**: Issue #6754, `nova-cews-implementation.md`

---

## Test Environment Setup

**Required Units**: 3-4 ground units with Nova CEWS (e.g., Pariah (Septicemia) A-Z)
**Map**: Any ground map
**Game Mode**: Hotseat (local) recommended for initial testing

---

## PART 1: LOBBY CONFIGURATION TESTS

### Test 1.1: Create 2-Unit Network in Lobby ✅
**Objective**: Verify minimum network size works

**Steps**:
1. Start lobby, load 2 units with Nova CEWS
2. Select both units → Configure C3 → Select Nova Network
3. Link Unit 2 to Unit 1
4. Verify connection line appears in lobby
5. Click "I'm Done"
6. **Expected**: Connection line visible after deployment

**Pass Criteria**:
- ✅ Line appears in lobby
- ✅ Line persists through Initiative_Report
- ✅ Line visible in Movement phase
- ✅ Nova Network GUI shows both units linked

---

### Test 1.2: Create 3-Unit Network in Lobby ✅
**Objective**: Verify maximum network size works

**Steps**:
1. Start lobby, load 3 units with Nova CEWS
2. Link Unit 2 → Unit 1, Unit 3 → Unit 1
3. Verify 2 connection lines appear (star pattern with Unit 1 at center)
4. Click "I'm Done"
5. **Expected**: Both lines visible after deployment

**Pass Criteria**:
- ✅ 2 lines appear in lobby (1-2, 1-3)
- ✅ Lines persist through all phases
- ✅ Nova Network GUI shows all 3 units on same network

---

### Test 1.3: Attempt 4-Unit Network (Should Fail) ⏳
**Objective**: Verify 3-unit maximum is enforced

**Steps**:
1. Start lobby, load 4 units with Nova CEWS
2. Link Units 1, 2, 3 successfully
3. Attempt to add Unit 4 to the network
4. **Expected**: Error message or prevented from linking

**Pass Criteria**:
- ❌ 4th unit cannot be added to network
- ✅ Error message displayed (if applicable)
- ✅ Existing 3-unit network remains intact

---

### Test 1.4: Reconfigure Network in Lobby ⏳
**Objective**: Verify networks can be modified before game start

**Steps**:
1. Create network: Unit 1 + Unit 2
2. Unlink Unit 2
3. Link Unit 2 → Unit 3 instead
4. **Expected**: Old connection removed, new connection appears

**Pass Criteria**:
- ✅ Old line (1-2) disappears when unlinked
- ✅ New line (2-3) appears when linked
- ✅ Changes persist when game starts

---

## PART 2: IN-GAME NETWORK MANAGEMENT TESTS

### Test 2.1: Link Units During End Phase ✅
**Objective**: Verify in-game network configuration works

**Steps**:
1. Start game with 3 unlinked Nova CEWS units
2. Proceed to End Phase
3. Click "Nova Networks" button
4. Select Unit 1 and Unit 2 → Click "Link"
5. Click "Apply Changes"
6. **Expected**: Connection line appears immediately

**Pass Criteria**:
- ✅ Nova Networks dialog opens in End Phase
- ✅ Units show as [Unlinked] initially
- ✅ Link button creates connection
- ✅ Connection line appears when dialog closes
- ✅ Next turn, units share targeting data

---

### Test 2.2: Unlink Units During End Phase ⏳
**Objective**: Verify units can be removed from network

**Steps**:
1. Start with 3-unit network (from lobby or previous test)
2. End Phase → Open Nova Networks dialog
3. Select Unit 3 → Click "Unlink"
4. Click "Apply Changes"
5. **Expected**: Unit 3 shows as [Unlinked], line disappears

**Pass Criteria**:
- ✅ Unit 3 shows [Unlinked] in dialog
- ✅ Connection line to Unit 3 disappears
- ✅ Units 1 and 2 remain linked

---

### Test 2.3: Move Unit Between Networks ⏳
**Objective**: Verify units can switch networks

**Steps**:
1. Create Network A: Unit 1 + Unit 2
2. Create Network B: Unit 3 + Unit 4
3. End Phase → Unlink Unit 2 from Network A
4. Link Unit 2 to Network B (with Units 3 and 4)
5. **Expected**: Unit 2 shows on Network B

**Pass Criteria**:
- ✅ Unit 2 unlinks from Network A
- ✅ Unit 2 links to Network B successfully
- ✅ Connection lines update correctly
- ✅ Both networks remain functional

---

### Test 2.4: Network Survives Phase Transitions ✅
**Objective**: Verify lines persist through all phases

**Steps**:
1. Create 3-unit network in lobby
2. Proceed through game phases, checking each:
   - Initiative_Report
   - Movement
   - Movement_Report
   - Firing
   - Firing_Report
   - Physical
   - Physical_Report
   - End
   - End_Report

**Pass Criteria**:
- ✅ Lines visible in ALL phases
- ✅ No disappearing/reappearing behavior
- ✅ Nova Network GUI shows connections in all phases

---

## PART 3: BV CALCULATION TESTS

### Test 3.1: 2-Unit BV Bonus ⏳
**Objective**: Verify 5% bonus applies with 2 Nova CEWS units

**Steps**:
1. Load 2 identical units with Nova CEWS (e.g., 1000 BV each)
2. Check total force BV in lobby
3. **Expected**: Each unit gets +50 BV (5% of 1000 total)
4. **Expected**: Total force BV = 2100 (2000 base + 2x50 bonus)

**Pass Criteria**:
- ✅ Individual unit BV shows +50 bonus
- ✅ Total force BV = 2100
- ✅ BV display updates when units are added/removed

---

### Test 3.2: 35% BV Cap Enforcement ⏳
**Objective**: Verify bonus is capped at 35% of base BV

**Steps**:
1. Load 21 units @ 1000 BV each with Nova CEWS
2. Raw bonus = 21000 * 0.05 = 1050 BV per unit
3. Cap = 1000 * 0.35 = 350 BV per unit
4. **Expected**: Each unit capped at +350 BV bonus

**Pass Criteria**:
- ✅ Each unit shows +350 BV (not +1050)
- ✅ Total force BV = 28350 (21000 base + 21x350 capped bonus)

---

### Test 3.3: BV Updates When Network Changes ⏳
**Objective**: Verify BV recalculates when units are linked/unlinked

**Steps**:
1. Load 3 units @ 1000 BV each
2. Note total BV with all unlinked
3. Link all 3 units → Check BV increase
4. Unlink 1 unit → Check BV decrease
5. **Expected**: BV updates reflect network changes

**Pass Criteria**:
- ✅ BV increases when units link
- ✅ BV decreases when units unlink
- ✅ Lobby display updates in real-time

---

## PART 4: COMBAT FUNCTIONALITY TESTS

### Test 4.1: Targeting Data Sharing ⏳
**Objective**: Verify networked units share targeting data (C3i functionality)

**Setup**: 3 networked Nova CEWS units, 1 enemy target

**Steps**:
1. Unit 1 has clear LOS to enemy
2. Units 2 and 3 do NOT have LOS to enemy
3. Unit 2 attempts to fire at enemy
4. **Expected**: Unit 2 can target enemy using Unit 1's LOS
5. **Expected**: To-hit modifier shows C3 bonus

**Pass Criteria**:
- ✅ Unit 2 can target enemy without direct LOS
- ✅ To-hit number shows C3 bonus
- ✅ Attack resolves successfully

---

### Test 4.2: Network Bonus To-Hit Modifier ⏳
**Objective**: Verify C3 to-hit bonus is applied

**Steps**:
1. 3-unit network, all have LOS to same target
2. Note to-hit numbers for each unit
3. **Expected**: To-hit improved by -1 or -2 (per C3i rules)
4. Compare to unit firing without network

**Pass Criteria**:
- ✅ To-hit modifier shows C3 bonus
- ✅ Bonus applies to all networked units
- ✅ Bonus does NOT apply to non-networked units

---

### Test 4.3: ECM Mode Does Not Affect Network ⏳
**Objective**: Verify Nova CEWS network works regardless of ECM mode

**Steps**:
1. Create 3-unit network with ECM mode = "ECM"
2. Verify network functions (targeting, bonuses)
3. Switch one unit to ECM mode = "Off"
4. **Expected**: Network still functions for all units

**Pass Criteria**:
- ✅ Network works with ECM = "ECM"
- ✅ Network works with ECM = "Off"
- ✅ ECM effects (jamming) work independently of network

---

### Test 4.4: Multiple Units Can Have ECM Active ✅
**Objective**: Verify multiple ECMs allowed (per TT rules)

**Steps**:
1. 3-unit network, all with ECM mode = "ECM"
2. Verify all 3 ECMs are active
3. **Expected**: No "only one ECM" error message

**Pass Criteria**:
- ✅ All 3 units can activate ECM simultaneously
- ✅ No error messages
- ✅ ECM effects stack/overlap correctly

---

### Test 4.5: Network Survives Unit Destruction ⏳
**Objective**: Verify network persists when member is destroyed

**Setup**: 3-unit network (Units 1, 2, 3)

**Steps**:
1. Unit 3 is destroyed in combat
2. **Expected**: Units 1 and 2 remain networked
3. Verify targeting data still shared between Units 1 and 2

**Pass Criteria**:
- ✅ Connection line between Units 1 and 2 remains
- ✅ Units 1 and 2 continue to share targeting data
- ✅ No network errors or crashes

---

### Test 4.6: Nova CEWS Immune to Nova ECM ⏳
**Objective**: Verify Nova CEWS links not affected by Nova ECM (per TT rules)

**Setup**: 2 networked Nova CEWS units, 1 enemy Nova CEWS using ECM

**Steps**:
1. Enemy Nova CEWS activates ECM jamming
2. **Expected**: Friendly Nova CEWS network unaffected
3. Verify friendly units still share targeting data

**Pass Criteria**:
- ✅ Enemy ECM does NOT break friendly Nova CEWS link
- ✅ Targeting data sharing continues
- ✅ To-hit bonuses still apply

---

## PART 5: EDGE CASES AND ERROR CONDITIONS

### Test 5.1: Save and Load Game with Networks ⏳
**Objective**: Verify networks persist through save/load

**Steps**:
1. Create 3-unit network in game
2. Save game
3. Load saved game
4. **Expected**: Network restored correctly

**Pass Criteria**:
- ✅ Connection lines appear after load
- ✅ Nova Network GUI shows correct configuration
- ✅ Network functionality intact

---

### Test 5.2: MUL File Import with Nova Networks ⏳
**Objective**: Verify MUL files with Nova CEWS networks load correctly

**Steps**:
1. Configure 3-unit network in lobby
2. Save as MUL file
3. Load MUL file in new game
4. **Expected**: Network configuration restored

**Pass Criteria**:
- ✅ MUL file saves network data
- ✅ MUL file loads network correctly
- ✅ Connection lines appear after load

---

### Test 5.3: Mixed C3 Systems (Nova + Naval + C3i) ⏳
**Objective**: Verify multiple C3 types coexist without interference

**Setup**: 3 Nova CEWS units, 3 Naval C3 units, 3 C3i units (separate networks)

**Steps**:
1. Create separate networks for each C3 type
2. Verify all 3 network types function independently
3. **Expected**: No cross-network interference

**Pass Criteria**:
- ✅ Nova CEWS network functions
- ✅ Naval C3 network functions
- ✅ C3i network functions
- ✅ No errors or conflicts between types

---

### Test 5.4: Zero Units with Nova CEWS ⏳
**Objective**: Verify no errors when no Nova CEWS units present

**Steps**:
1. Load game with NO Nova CEWS units
2. Proceed through all phases
3. **Expected**: No errors, no Nova Network button

**Pass Criteria**:
- ✅ Game runs normally
- ✅ No Nova Network dialog button appears
- ✅ No errors in logs

---

### Test 5.5: Single Unit with Nova CEWS ⏳
**Objective**: Verify single Nova unit doesn't cause errors

**Steps**:
1. Load game with 1 Nova CEWS unit
2. Verify no BV bonus (requires 2+ units)
3. **Expected**: No errors, unit functions normally

**Pass Criteria**:
- ✅ Unit operates normally
- ✅ No BV bonus applied (requires 2+ units per TT rules)
- ✅ No network errors

---

## PART 6: STRESS TESTS

### Test 6.1: Maximum Networks (Multiple 3-Unit Networks) ⏳
**Objective**: Verify performance with many concurrent networks

**Setup**: 12 Nova CEWS units (4 networks of 3 units each)

**Steps**:
1. Create 4 separate 3-unit networks
2. Verify all networks function simultaneously
3. Run several turns of combat

**Pass Criteria**:
- ✅ All 4 networks visible and functional
- ✅ No performance degradation
- ✅ Targeting data sharing works for all networks

---

### Test 6.2: Rapid Network Reconfiguration ⏳
**Objective**: Verify stability when networks change frequently

**Steps**:
1. Create network, unlink, recreate in different configuration
2. Repeat 5 times in 5 consecutive End Phases
3. **Expected**: No errors or memory leaks

**Pass Criteria**:
- ✅ All reconfigurations succeed
- ✅ No error messages
- ✅ Network display always correct

---

## TESTING CHECKLIST SUMMARY

### Priority 1 (Critical - Test First)
- [✅] Test 1.1: 2-Unit Network in Lobby
- [✅] Test 1.2: 3-Unit Network in Lobby
- [✅] Test 2.1: Link Units During End Phase
- [✅] Test 2.4: Network Survives Phase Transitions
- [✅] Test 4.4: Multiple ECMs Active

### Priority 2 (Important - Test Soon)
- [⏳] Test 1.3: 4-Unit Network (Should Fail)
- [⏳] Test 2.2: Unlink Units During End Phase
- [⏳] Test 3.1: 2-Unit BV Bonus
- [⏳] Test 3.2: 35% BV Cap
- [⏳] Test 4.1: Targeting Data Sharing
- [⏳] Test 4.2: Network To-Hit Bonus

### Priority 3 (Nice to Have - Test When Available)
- [⏳] Test 1.4: Reconfigure Network in Lobby
- [⏳] Test 2.3: Move Unit Between Networks
- [⏳] Test 3.3: BV Updates
- [⏳] Test 4.3: ECM Mode Independence
- [⏳] Test 4.5: Network Survives Destruction
- [⏳] Test 4.6: Nova ECM Immunity
- [⏳] Test 5.1-5.5: Edge Cases
- [⏳] Test 6.1-6.2: Stress Tests

---

## TEST RESULTS TRACKING

**Date Started**: [Fill in when testing begins]
**Tester**: [Your name]
**Build Version**: [Git commit SHA or build date]

**Overall Status**: 5/30 tests completed (16.7%)

**Blockers**: None identified
**Issues Found**: [Document any bugs discovered]

---

## NOTES

- Tests marked ✅ = Completed and passing
- Tests marked ⏳ = Pending execution
- Tests marked ❌ = Failed (requires fix)
- Update this document as tests are completed
- Report any unexpected behavior to development team

---

**Last Updated**: 2025-01-15
**Document Version**: 1.0
