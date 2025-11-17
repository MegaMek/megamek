# ARAD (Anti-Radiation) Missile Implementation Design Document

**Document Version:** 1.1
**Date:** 2025-01-16
**Last Updated:** 2025-01-16 (Added Consolidated Rules Reference, resolved TAG/Ghost Targets conflicts, clarified Nemesis pod handling)
**Status:** Design Phase - Rules Research Complete
**Tech Level:** Experimental (E/X-X-E)
**Reference:** Tactical Operations: Advanced Units & Equipment, p.180

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Rules Reference](#rules-reference)
3. [Official Forum Rulings](#official-forum-rulings)
4. [Technical Architecture](#technical-architecture)
5. [Equipment Detection Logic](#equipment-detection-logic)
6. [Implementation Plan](#implementation-plan)
7. [Testing Strategy](#testing-strategy)
8. [Edge Cases and Special Handling](#edge-cases-and-special-handling)
9. [Code Reference Guide](#code-reference-guide)
10. [Open Questions](#open-questions)

---

## Executive Summary

### What Are ARAD Missiles?

Anti-Radiation (ARAD) missiles are experimental munitions that home in on electronic emissions from enemy units. They are available for LRM, SRM, and MML launchers and provide variable bonuses/penalties based on the target's electronic signature.

### Key Characteristics

- **Variable Performance:** Bonuses against "hot" targets, penalties against "cold" targets
- **Electronics-Dependent:** Performance based on target's active electronic warfare systems
- **Narc Synergy:** Works with Narc pods but doesn't stack bonuses
- **ECM Interaction:** Narc-tagged targets allow ARAD to ignore hostile ECM
- **Incompatibilities:** Cannot be used with Artemis or Streak systems

### Tech Availability

| Faction | Prototype Date | Production | Status |
|---------|---------------|------------|--------|
| Inner Sphere | 3066 (Free Worlds League) | Never | Experimental |
| Clan | 3057 (Clan Smoke Jaguar) | Never | Experimental |

**Availability Ratings:** XXEF (Experimental across all eras)

**Historical Context:**
- **3057**: Clan Smoke Jaguar develops first ARAD prototype
- **3066**: Free Worlds League develops Inner Sphere variant (9 years later)
- **Status**: Never entered full production; remains experimental as of current timeline
- **Launcher Compatibility**: LRM, SRM, and MML systems
- **Technology Rating**: E (Experimental)
- **Source Reference**: Tactical Operations: Advanced Units & Equipment, p.180

**Design Philosophy:**
ARAD missiles represent an experimental approach to anti-electronic warfare, designed to exploit the extensive sensor and communications systems common in advanced BattleTech units. The technology remains experimental due to complexity in reliably homing on electronic emissions in the chaotic battlefield environment.

---

## Rules Reference

### Core Mechanics (TO:AUE p.180)

#### To-Hit Modifiers

| Target Type | To-Hit Modifier |
|-------------|----------------|
| Target WITH qualifying electronics | -1 |
| Target WITHOUT qualifying electronics | +2 |

#### Cluster Hit Table Modifiers

| Target Type | Cluster Modifier | Minimum Result |
|-------------|-----------------|----------------|
| Target WITH qualifying electronics | +1 | N/A |
| Target WITHOUT qualifying electronics | -2 | 2 |

#### Qualifying Electronic Systems

ARAD missiles receive bonuses against targets using at least ONE of:
- Active Probes (any kind)
- Artemis fire-control systems
- Blue Shield system
- C3 systems (any kind: Master, Slave, i, Master-Master)
- Communications equipment (3.5 tons or more)
- ECM suites (any kind)

**Important:** Modifiers are NOT cumulative even if target uses multiple systems.

#### Special Rules

1. **ECM Interaction:** ARAD missiles ignore hostile ECM effects when targeting a unit tagged by a friendly Narc pod
2. **Narc Interaction:** ARAD receives standard ARAD bonus (-1/+1) when targeting Narc-tagged units, but does NOT receive additional Narc-specific bonuses
3. **Artemis Incompatibility:** ARAD missiles are incompatible with Artemis fire-control systems
4. **Streak Incompatibility:** ARAD missiles are incompatible with Streak systems
5. **Launcher Compatibility:** ARAD works with LRM, SRM, and MML launchers

---

## Official Forum Rulings

### Authority Hierarchy

**Primary Authorities:**
- **Xotl** - Official BattleTech Rules Arbiter
- **Hammer** - Official BattleTech Rules Arbiter

**Secondary Sources:**
- Catalyst Game Labs freelance writers (Welshman, etc.)

**Precedence Rules:**
- Xotl/Hammer rulings take priority over other sources
- Newer rulings supersede older rulings in case of conflicts

### Ruling Summary by Topic

#### 1. Equipment That Triggers ARAD Bonuses

**Confirmed Qualifying Systems:**
- Active Probes (all types) - MUST be powered on
- Artemis fire-control systems - MUST be functional and powered on
- Blue Shield system - MUST be powered on
- C3 systems (all types) - MUST be powered on (does NOT need to be networked)
- Communications equipment ≥3.5 tons - MUST be dedicated equipment, NOT built-in cockpit comms
- ECM suites (all types) - MUST be powered on
- TAG (Target Acquisition Gear) - When activated earlier in turn **[Forum ruling: Xotl]**
- Ghost Target generation - Units generating Ghost Targets count as emitting **[Forum ruling]**
- Friendly Narc/iNarc homing pods (external equipment)
- Friendly iNarc Nemesis pods (external equipment) **[Forum ruling: friendly Nemesis triggers bonuses]**

**Source:** TO:AUE p.180, Xotl rulings (2017-2018)

#### 2. Equipment That Does NOT Trigger ARAD Bonuses

**Explicitly Ruled Out:**
- Streak missile systems
- Chaff deployments
- EMI (Electromagnetic Interference) terrain
- Tasers/TSEMPs
- Charged weapons (PPCs, Gauss, Bombast Laser, PPC Capacitor, ProtoMech EDA)
- Viral Jammers (Homing and Decoy variants)
- Homing Jammer **[Added from forum research]**
- Decoy Jammer **[Added from forum research]**
- Enemy iNarc Nemesis pods (cannot bypass redirection)
- iNarc Haywire pods
- Powered-down/shut-down equipment
- Built-in cockpit communications (standard 1-ton comms)

**Note:** TAG and Ghost Targets were previously listed here but forum rulings indicate they DO trigger ARAD bonuses (see Consolidated Rules Reference section).

**Source:** Xotl ruling, November 11, 2018
**Forum:** https://battletech.com/forums/index.php?topic=63179.msg1452217#msg1452217

#### 3. Stealth Armor Interaction

**Ruling:** **Active** Stealth Armor makes a unit "completely non-emitting"

**Effects:**
- **Active** Stealth Armor blocks ALL internal emitting systems from triggering ARAD bonuses
- ARAD treats Stealth-equipped units as having NO electronics
- Target receives unfavorable modifiers: +2 to-hit, -2 cluster

**Critical Exception:**
- External Narc pods are NOT blocked by Stealth Armor
- Narc-tagged Stealth units still grant ARAD bonuses

**Active State Requirement:**
- Stealth Armor must be in **"On" mode** to suppress emissions
- **Inactive** Stealth Armor does NOT block internal systems
- Check equipment mode: `equipment.curMode().equals("On")`
- Stealth requires ECM to function; if ECM destroyed/off, Stealth is inactive

**Additional Note:**
- Active Stealth Armor also blocks the unit's own Artemis IV targeting system
- Design intent: Stealth creates ECM environment that traps transmissions

**Source:** Xotl ruling
**Forum:** https://battletech.com/forums/index.php?topic=78845.msg1866412#msg1866412

#### 4. Powered-Down Equipment

**Ruling:** Players may power down weapons/equipment during **End Phase**

**Effects on ARAD:**
- Powered-down electronics do NOT function
- Powered-down equipment is NOT a valid target for ARAD missiles
- Equipment must be actively powered on to trigger ARAD bonuses

**Timing:**
- Power-down declarations occur during **End Phase**
- Equipment state checked at attack resolution time
- Equipment destroyed or powered down mid-turn won't trigger ARAD

**Equipment Restrictions:**
- **Cannot be powered down**: Ammunition, fuel, cargo (will always explode if critically hit)
- **Can be powered down**: Weapons, electronics, systems
- **Powered-down equipment**: Still destroyed by crits, just won't explode

**Example:**
- C3 Slave powered ON (even if not networked): Triggers ARAD bonus
- C3 Slave powered DOWN: Does NOT trigger ARAD bonus

**Source:** Xotl ruling, November 2017, Errata to TO p.99
**Forum:** https://battletech.com/forums/index.php?topic=31896.msg1369654#msg1369654

#### 5. Communications Equipment Clarification

**Ruling:** Only dedicated communications equipment qualifies

**Details:**
- Built-in 1-ton communications (standard in all cockpits/controls): Does NOT count
- Dedicated communications equipment in critical slots: Counts if ≥3.5 tons
- Must be actual mounted equipment, not built-in systems

**Design Intent:**
- Prevent ARAD from being "far too good" against all units
- Restrict effectiveness to specialized communication-heavy units

**Examples:**
- Vedette (standard vehicle): No ARAD bonus (only built-in comms)
- Mobile Headquarters: ARAD bonus (dedicated heavy comms)
- Swift Wind: ARAD bonus (dedicated heavy comms)

**Source:** Welshman ruling (Catalyst Freelancer)
**Forum:** https://battletech.com/forums/index.php?topic=17456.msg396440#msg396440

#### 6. Narc/iNarc Pod Interactions

**Standard Narc/iNarc Homing Pods:**
- Target with friendly Narc/iNarc pod counts as "emitting"
- ARAD gets standard bonus: -1 to-hit, +1 cluster
- NO additional Narc-specific bonus (bonuses don't stack)
- ARAD ignores hostile ECM when target is Narc-tagged

**iNarc Nemesis Pods (Updated per forum rulings):**
- **Friendly Nemesis Pods:** Qualify as emitting sources; ARAD gets standard bonus (-1/+1)
- **Enemy Nemesis Pods:** Do NOT qualify; no ARAD bonuses
- ARAD **cannot bypass Nemesis redirection effects** (regardless of team)
- **Important:** Must distinguish friendly vs enemy pod ownership in code

**iNarc Haywire Pods:**
- Cannot trigger ARAD bonuses

**ECM Pods (attached to target):**
- Tagged targets grant emitting bonus
- No Narc-specific bonuses apply
- ECCM counters ECM pod effects

**Key Clarification:**
> "ARAD Missiles are more accurate against any unit that actively emits electronic signals, and receive a -1 to-hit modifier and a +1 Cluster Hits Table roll modifier if the target is using at least one of the following systems... This effect also occurs if the target has been tagged by a friendly Narc or iNarc homing pod. However, the ARAD missile does not receive any further to-hit bonus from the pod."

**No Bonus Stacking (Critical Rule):**
- ARAD modifiers are **NEVER cumulative**
- **Always either +2/-2 (non-emitting) OR -1/+1 (emitting)**
- Narc-tagged target with electronics: Gets -1/+1 (ARAD bonus), NOT -2/+2 (ARAD + Narc stacked)
- Target with multiple emitting systems: Gets -1/+1 (one system sufficient), NOT cumulative bonuses
- **Implementation**: ARAD lacks `M_NARC_CAPABLE` flag to prevent automatic Narc stacking

**Source:** Xotl rulings (2018), TO:AUE errata
**Forum:** https://battletech.com/forums/index.php?topic=26824.msg609067#msg609067

#### 7. Indirect Fire Support

**Ruling:** ARAD can work with indirect LRM fire if target has friendly Narc pod

**Details:**
- Follows standard indirect fire rules
- Narc beacon allows indirect fire as normal
- ARAD bonuses apply as usual

**Source:** Xotl ruling (2018)
**Forum:** https://battletech.com/forums/index.php?topic=63179.msg1452217#msg1452217

#### 8. Streak Systems Clarification

**Ruling:** Streak systems do NOT count as emitting systems for ARAD purposes

**Details:**
- Despite Streak SRM/LRM using telemetry updates from launcher
- Not treated as valid ARAD targets
- Target with only Streak systems: ARAD gets penalty (+2/-2)

**Source:** Xotl ruling
**Forum:** https://battletech.com/forums/index.php?topic=78845.msg1866412#msg1866412

---

## Consolidated Rules Reference

This section consolidates all official rules from:
- **Rules As Written (RAW)**: Tactical Operations: Advanced Units & Equipment, p.180
- **Official Forum Rulings**: Xotl and Hammer rulings (precedence: RAW → oldest to newest)
- **Developer Clarifications**: Catalyst Game Labs freelancers

### Complete Equipment Trigger Lists

#### Equipment That TRIGGERS ARAD Bonuses (-1 to-hit, +1 cluster)

Target must have **at least one** of the following active systems:

**Always Emitting (when powered on):**
- Active Probe (any type) - Must be powered on
- Artemis IV / Artemis V Fire Control System - Must be functional and powered on
- Blue Shield Particle Field Dampener - Must be powered on
- C3 Computer (Master, Slave, i, Boosted, Nova CEWS) - Must be powered on (does NOT need network link)
- Communications Equipment (≥3.5 tons dedicated equipment only) - Excludes built-in cockpit comms
- ECM Suite (any type) - Must be powered on
- TAG (Target Acquisition Gear) - When activated earlier in turn **[Forum ruling: Xotl]**
- Ghost Target generation - Units generating Ghost Targets count as emitting **[Forum ruling: URL #5]**

**External Attachments (Not blocked by Stealth Armor):**
- Narc Missile Beacon (friendly team attachment only)
- iNarc Homing Pod (friendly team attachment only)
- iNarc Nemesis Pod (friendly team attachment only) **[Forum ruling: friendly Nemesis triggers bonuses]**

**Important Notes:**
- Equipment must be **powered on** (not shut down)
- Equipment must be **functional** (not destroyed, missing, or breached)
- **Only one system needed** - modifiers are NOT cumulative
- **Stealth Armor blocks internal systems** but NOT external attachments

---

#### Equipment That DOES NOT Trigger ARAD Bonuses

**Explicitly Excluded Systems:**
- Streak missile guidance systems
- Chaff Pods/Grenades
- Electromagnetic Interference (EMI) terrain
- Tasers and TSEMPs
- Charged weapons (PPCs, Gauss, Bombast Laser, PPC Capacitor, ProtoMech EDA)
- Homing Jammer **[Added from forum research]**
- Decoy Jammer (Viral Jammer variant) **[Added from forum research]**
- iNarc Haywire Pods
- iNarc Nemesis Pod (enemy team attachment) **[Cannot bypass redirection]**
- Powered-down/shut-down equipment
- Destroyed/missing/breached equipment
- Built-in cockpit communications (standard 1-ton comms equivalent)

**Special Cases:**
- **Stealth Armor**: When active, suppresses ALL internal emissions (target treated as non-emitting)
- **ECM Pods**: Generate emissions but ECCM counters them, preventing ARAD bonus
- **C3 Networked vs Isolated**: Network status irrelevant; only powered-on state matters

---

### Modifier Summary Table

| Target Status | To-Hit | Cluster | Minimum | Notes |
|---------------|--------|---------|---------|-------|
| **Emitting** (has qualifying electronics) | -1 | +1 | N/A | Must be powered and active |
| **Narc/iNarc Tagged** (friendly pod) | -1 | +1 | N/A | Ignores hostile ECM; no additional Narc bonus |
| **Non-Emitting** (no qualifying electronics) | +2 | -2 | 2 hits | Minimum cluster result enforced |
| **Stealth Armor Active** | +2 | -2 | 2 hits | Suppresses all internal emissions |
| **Powered-Down Equipment** | +2 | -2 | 2 hits | Inactive electronics don't emit |

---

### Special Rules Summary

#### 1. Narc/iNarc Interaction
- ARAD receives **standard ARAD bonus only** (-1/+1) when targeting Narc-tagged units
- **No stacking**: Does NOT receive additional Narc-specific bonuses
- **Always either +2/-2 OR -1/+1** - never both
- ARAD **ignores hostile ECM** when target is Narc-tagged

#### 2. ECM Interaction
- Hostile ECM blocks ARAD bonuses against non-Narc targets
- **Exception**: Narc-tagged targets allow ARAD to ignore ECM
- iNarc ECM Pods generate emissions, but ECCM counters them

#### 3. Stealth Armor Override
- Active Stealth Armor **suppresses all internal emissions**
- Treats unit as completely non-emitting (+2/-2 applies)
- **Exception**: External Narc pods are NOT suppressed
- Stealth also blocks unit's own Artemis IV targeting system

#### 4. Powered-Down Equipment
- Players may power down equipment during **End Phase**
- Powered-down electronics **do not emit**
- Not valid targets for ARAD bonuses
- Equipment must be actively powered on to trigger bonuses
- **Exception**: Ammunition, fuel, cargo cannot be powered down

#### 5. Communications Equipment Threshold
- Requires **≥3.5 tons of dedicated communications equipment**
- Built-in "equivalent to 1 ton" comms (standard cockpit) **does NOT count**
- Must be installed as separate critical slots or vehicle slots
- Design intent: Restrict to specialized communication-heavy units

#### 6. iNarc Pod Types
- **Homing Pods**: Trigger ARAD bonus (friendly team only)
- **Nemesis Pods**: Trigger ARAD bonus (friendly team only) **[Updated per forum]**
  - Enemy Nemesis pods do NOT trigger bonuses
  - ARAD cannot bypass Nemesis redirection effects
- **Haywire Pods**: Do NOT trigger ARAD bonuses
- **ECM Pods**: Trigger bonus, but ECCM can counter

#### 7. Artemis Incompatibility
- ARAD ammunition **cannot use Artemis fire-control bonuses**
- ARAD lacks M_ARTEMIS_CAPABLE flag
- However, **targets with Artemis FCS DO trigger ARAD bonuses**

#### 8. Launcher Compatibility
- ARAD works with **LRM, SRM, and MML launchers**
- Incompatible with Streak systems

---

### Implementation Requirements

**Equipment Detection Must Check:**
1. Equipment is **installed** (critical slot or vehicle slot)
2. Equipment is **powered** (not shut down) - check via `isShutDown()`
3. Equipment is **active** (C3 linked, ECM enabled, TAG activated, etc.)
4. Equipment is **functional** (not destroyed, missing, or breached)
5. **Stealth Armor priority**: Check Stealth first; if active, skip internal systems

**Edge Cases Requiring Code Support:**
- Stealth Armor active state check (`curMode().equals("On")`)
- C3 network linkage status (powered on but not networked still counts)
- Communications equipment tonnage calculation (dedicated only, exclude built-in)
- ECM/ECCM interaction for iNarc ECM Pods
- Powered-down equipment state tracking (`isShutDown()`)
- iNarc pod type distinction (Homing vs Nemesis vs Haywire)
- iNarc pod team ownership (friendly vs enemy)
- TAG activation timing (earlier in turn)
- Ghost Target generation detection

---

## Technical Architecture

### Reference Implementation: Follow the Leader Missiles

ARAD implementation follows the pattern established by Follow the Leader (FTL) missiles:

**Pattern Structure:**
- Custom handler class extends base missile handler (LRMHandler, SRMHandler, etc.)
- Overrides `getSalvoBonus()` for cluster hit table modifications
- May override other methods for special behavior
- Weapon class selects handler based on munition type

**Key Differences from FTL:**
- **FTL:** Fixed bonuses (always +1 cluster unless ECM-affected)
- **ARAD:** Conditional bonuses based on target equipment detection
- **FTL:** No to-hit modifications
- **ARAD:** To-hit modifiers based on target electronics
- **FTL:** Simple ECM check
- **ARAD:** Complex ECM interaction (Narc overrides ECM)

### Architecture Overview

```
AmmoType.java
    └─ Defines M_ARAD munition type
    └─ Tech advancement data
    └─ Adds to LRM/SRM/MML munition lists

LRMWeapon.java / SRMWeapon.java / MMLWeapon.java
    └─ getCorrectHandler() checks for M_ARAD
    └─ Returns appropriate ARAD handler

LRMARADHandler.java
    ├─ extends LRMHandler
    ├─ Equipment Detection Methods
    │   ├─ targetHasQualifyingElectronics()
    │   ├─ hasActiveStealthArmor()
    │   ├─ hasActiveProbe()
    │   ├─ hasArtemis()
    │   ├─ hasBlueShield()
    │   ├─ hasC3()
    │   ├─ hasHeavyComms()
    │   ├─ hasECM()
    │   └─ isNarcTagged()
    ├─ getSalvoBonus() override
    │   └─ Returns +1, 0, or -2 based on target
    └─ To-hit modifier integration
        └─ Via ComputeToHit.java modification

ComputeToHit.java
    └─ Add ARAD to-hit modifier logic
    └─ Check for M_ARAD munition
    └─ Apply -1 or +2 based on target electronics
```

### File Locations

**Files to Modify:**
1. `megamek/src/megamek/common/equipment/AmmoType.java`
2. `megamek/src/megamek/common/weapons/lrms/LRMWeapon.java`
3. `megamek/src/megamek/common/weapons/srms/SRMWeapon.java`
4. `megamek/src/megamek/common/weapons/mml/MMLWeapon.java`
5. `megamek/src/megamek/common/actions/compute/ComputeToHit.java`

**Files to Create:**
1. `megamek/src/megamek/common/weapons/handlers/lrm/LRMARADHandler.java`
2. `megamek/src/megamek/common/weapons/handlers/srm/SRMARADHandler.java`
3. `megamek/src/megamek/common/weapons/handlers/mml/MMLARADHandler.java`
4. `megamek/unittests/megamek/common/weapons/handlers/lrm/LRMARADHandlerTest.java`

---

## Equipment Detection Logic

### Detection Priority Flow

```
1. Check for Active Stealth Armor
   ├─ YES → Check for external Narc pods only
   │         └─ Return: Narc status only
   └─ NO → Check all internal systems
             ├─ Active Probes
             ├─ Artemis FCS
             ├─ Blue Shield
             ├─ C3 Systems
             ├─ Heavy Comms (≥3.5 tons)
             ├─ ECM Suites
             └─ Narc Pods

2. Validate Equipment State
   ├─ Not destroyed
   ├─ Not missing
   ├─ Not breached
   └─ Powered ON (not shut down)

3. Return Boolean
   └─ TRUE: Target has qualifying electronics
   └─ FALSE: Target has no qualifying electronics
```

### Equipment Validation Checklist

**For ALL Equipment (except external Narc):**
```java
boolean isValid = !equipment.isDestroyed() &&
                  !equipment.isMissing() &&
                  !equipment.isBreached() &&
                  !equipment.isShutDown();  // Powered on check
```

### Specific Equipment Checks

#### Active Probes
**Detection Method:**
```java
Entity.hasActiveProbe() && isEquipmentPoweredOn(target, MiscType.F_BAP)
```

**MiscType Flags:**
- `F_BAP` - Beagle Active Probe / Active Probe

#### Artemis Fire-Control
**Detection Method:**
```java
for (Mounted<?> equipment : target.getEquipment()) {
    if ((equipment.getType().hasFlag(MiscType.F_ARTEMIS) ||
         equipment.getType().hasFlag(MiscType.F_ARTEMIS_V)) &&
        isValidEquipment(equipment)) {
        return true;
    }
}
```

**MiscType Flags:**
- `F_ARTEMIS` - Artemis IV
- `F_ARTEMIS_V` - Artemis V

#### Blue Shield
**Detection Method:**
```java
for (Mounted<?> equipment : target.getEquipment()) {
    if (equipment.getType().hasFlag(MiscType.F_BLUE_SHIELD) &&
        isValidEquipment(equipment)) {
        return true;
    }
}
```

**MiscType Flags:**
- `F_BLUE_SHIELD` - Blue Shield system

#### C3 Systems
**Detection Method:**
```java
// Use built-in Entity methods
boolean hasC3System = target.hasC3() || target.hasC3i();

// Verify at least one is powered on
for (Mounted<?> equipment : target.getEquipment()) {
    if ((equipment.getType().hasFlag(MiscType.F_C3S) ||
         equipment.getType().hasFlag(MiscType.F_C3I) ||
         equipment.getType().hasFlag(MiscType.F_C3M) ||
         equipment.getType().hasFlag(MiscType.F_C3SBS) ||
         equipment.getType().hasFlag(MiscType.F_NOVA)) &&
        isValidEquipment(equipment)) {
        return true;
    }
}
```

**MiscType Flags:**
- `F_C3S` - C3 Slave
- `F_C3I` - C3i
- `F_C3M` - C3 Master
- `F_C3SBS` - C3 Small Boosted System
- `F_NOVA` - Nova CEWS

**Important:** System must be powered on but does NOT need to be networked.

#### Communications Equipment
**Detection Method:**
```java
for (Mounted<?> equipment : target.getEquipment()) {
    if (equipment.getType().hasFlag(MiscType.F_COMMUNICATIONS) &&
        equipment.getTonnage() >= 3.5 &&
        isValidEquipment(equipment)) {
        return true;
    }
}
```

**MiscType Flags:**
- `F_COMMUNICATIONS` - Communications equipment

**Critical Requirements:**
- Must be dedicated equipment (not built-in cockpit comms)
- Must be ≥3.5 tons
- Only equipment in getEquipment() list qualifies (built-in comms not included)

#### ECM Suites
**Detection Method:**
```java
Entity.hasECM() && isEquipmentPoweredOn(target, MiscType.F_ECM)
```

**MiscType Flags:**
- `F_ECM` - ECM suite

#### Narc/iNarc Pods
**Detection Method:**
```java
// Standard Narc
target.isNarcedBy(attackingEntity.getOwner().getTeam())

// iNarc Homing (NOT Nemesis or Haywire)
target.isINarcedBy(attackingEntity.getOwner().getTeam())
```

**Important:**
- Only friendly team Narc pods count
- Nemesis pods do NOT qualify (need type verification)
- Haywire pods do NOT qualify
- External equipment, not blocked by Stealth Armor

#### Stealth Armor
**Detection Method:**
```java
for (Mounted<?> equipment : target.getEquipment()) {
    if (equipment.getType().hasFlag(MiscType.F_STEALTH) &&
        !equipment.isDestroyed() && !equipment.isMissing() &&
        equipment.curMode().equals("On")) {
        return true;
    }
}
```

**MiscType Flags:**
- `F_STEALTH` - Stealth Armor

**Effects:**
- Blocks ALL internal systems (C3, ECM, Active Probe, etc.)
- Does NOT block external Narc pods
- Must be in "On" mode to be active

---

## Implementation Plan

### Phase 1: AmmoType Definition

**File:** `AmmoType.java`

**Tasks:**
1. Add `M_ARAD` to `Munitions` enum (around line 1960)
   ```java
   M_ARAD("Anti-Radiation"),
   ```

2. Create `ARAD_MUNITION_MUTATOR` (around line 680)
   ```java
   private static final MunitionMutator ARAD_MUNITION_MUTATOR = new MunitionMutator(
       "Anti-Radiation",
       2,  // Cost multiplier (experimental ammo)
       Munitions.M_ARAD,
       new TechAdvancement(TechBase.BOTH).setTechRating(TechRating.E)
           .setAvailability(AvailabilityValue.X, AvailabilityValue.X,
                           AvailabilityValue.E, AvailabilityValue.F)
           .setISAdvancement(3066, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
           .setISApproximate(false, false, false, false, false)
           .setClanAdvancement(3057, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
           .setClanApproximate(false, false, false, false, false)
           .setPrototypeFactions(F_FW)  // Free Worlds League
           .setProductionFactions(F_CSJ)  // Clan Smoke Jaguar
           .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
       "180, TO:AUE");
   ```

3. Add to munition lists (around line 3416)
   - LRM munition list: `addMunition(lrm, ARAD_MUNITION_MUTATOR);`
   - SRM munition list: `addMunition(srm, ARAD_MUNITION_MUTATOR);`
   - MML munition list: `addMunition(mml, ARAD_MUNITION_MUTATOR);`

**Important:** Do NOT add `M_ARTEMIS_CAPABLE` or `M_NARC_CAPABLE` flags

**Verification:**
- Code compiles without errors
- ARAD appears in ammo selection
- Tech dates correct (IS 3066, Clan 3057)
- Marked as Experimental

### Phase 2: Create LRMARADHandler

**File:** `megamek/src/megamek/common/weapons/handlers/lrm/LRMARADHandler.java`

**Class Structure:**
```java
package megamek.common.weapons.handlers.lrm;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.ComputeECM;
import megamek.common.equipment.MiscType;
import megamek.server.GameManager;

public class LRMARADHandler extends LRMHandler {

    public LRMARADHandler(ToHitData toHit, WeaponAttackAction waa,
                          Game game, GameManager manager) {
        super(toHit, waa, game, manager);
    }

    /**
     * Main equipment detection method.
     * Checks if target has qualifying electronics for ARAD bonus.
     */
    private boolean targetHasQualifyingElectronics(Entity target) {
        // Priority 1: Check for active Stealth Armor
        if (hasActiveStealthArmor(target)) {
            // Stealth blocks internal systems, only Narc counts
            return isNarcTagged(target);
        }

        // Priority 2: Check all qualifying systems
        return hasActiveProbe(target) ||
               hasArtemis(target) ||
               hasBlueShield(target) ||
               hasC3(target) ||
               hasHeavyComms(target) ||
               hasECM(target) ||
               isNarcTagged(target);
    }

    // Individual equipment detection methods
    private boolean hasActiveStealthArmor(Entity target) { /* ... */ }
    private boolean hasActiveProbe(Entity target) { /* ... */ }
    private boolean hasArtemis(Entity target) { /* ... */ }
    private boolean hasBlueShield(Entity target) { /* ... */ }
    private boolean hasC3(Entity target) { /* ... */ }
    private boolean hasHeavyComms(Entity target) { /* ... */ }
    private boolean hasECM(Entity target) { /* ... */ }
    private boolean isNarcTagged(Entity target) { /* ... */ }
    private boolean isValidEquipment(Mounted<?> equipment) { /* ... */ }

    /**
     * Override cluster hit modifier.
     */
    @Override
    public int getSalvoBonus() {
        if (!(target instanceof Entity)) {
            return 0;
        }

        Entity entityTarget = (Entity) target;
        boolean hasElectronics = targetHasQualifyingElectronics(entityTarget);

        if (hasElectronics) {
            // Target has qualifying electronics
            // Check if Narc-tagged (overrides ECM)
            boolean isNarced = entityTarget.isNarcedBy(
                attackingEntity.getOwner().getTeam()) ||
                entityTarget.isINarcedBy(attackingEntity.getOwner().getTeam());

            if (isNarced) {
                return 1;  // +1 cluster, Narc overrides ECM
            }

            // Check for ECM interference
            boolean isECMAffected = ComputeECM.isAffectedByECM(
                attackingEntity,
                attackingEntity.getPosition(),
                target.getPosition());

            if (isECMAffected) {
                return 0;  // ECM blocks ARAD bonus
            }

            return 1;  // +1 cluster bonus
        } else {
            // Target lacks qualifying electronics
            return -2;  // -2 cluster penalty (minimum result 2)
        }
    }
}
```

### Phase 3: Add To-Hit Modifiers

**File:** `ComputeToHit.java`

**Location:** Around line 1540 (after other munition checks)

**Code to Add:**
```java
// ARAD missile to-hit modifiers
if (munition.contains(AmmoType.Munitions.M_ARAD) && (entityTarget != null)) {
    // Create handler instance to access equipment detection
    // Or duplicate detection logic here
    // Simplified version:
    boolean hasElectronics = targetHasQualifyingElectronics(entityTarget);

    if (hasElectronics) {
        toHit.addModifier(-1, "ARAD vs. emitting target");
    } else {
        toHit.addModifier(2, "ARAD vs. non-emitting target");
    }
}
```

**Challenge:** Equipment detection logic is in handler, not ComputeToHit
**Solution Options:**
1. Duplicate equipment detection in ComputeToHit
2. Create shared utility class for equipment detection
3. Make handler methods static and call from ComputeToHit

**Recommendation:** Option 2 - Create `ARADEquipmentDetector` utility class

### Phase 4: Integrate with Weapon Classes

**Files:**
- `LRMWeapon.java` (line 166+)
- `SRMWeapon.java`
- `MMLWeapon.java`

**Code to Add:**
```java
// In getCorrectHandler() method, BEFORE standard handler return
if (atype.getMunitionType().contains(AmmoType.Munitions.M_ARAD)) {
    return new LRMARADHandler(toHit, waa, game, manager);
}
```

**Pattern Reference:** See `LRMFollowTheLeaderHandler` selection (line 166-168)

### Phase 5: Create SRM and MML Handlers

**Files to Create:**
- `SRMARADHandler.java` (extends `SRMHandler`)
- `MMLARADHandler.java` (extends appropriate MML handler)

**Implementation:**
- Copy structure from `LRMARADHandler`
- Adjust parent class (SRMHandler vs. LRMHandler)
- Same equipment detection logic
- Same cluster and to-hit modifiers

### Phase 6: Testing

**Create Test File:**
- `LRMARADHandlerTest.java`

**Test Cases:**
See [Testing Strategy](#testing-strategy) section for detailed test cases.

---

## Testing Strategy

### Unit Test Framework

**Test File:** `megamek/unittests/megamek/common/weapons/handlers/lrm/LRMARADHandlerTest.java`

**Framework:** JUnit 5
**Pattern:** Mockito for complex entity setup (see `WeaponAttackActionToHitTest.java`)

### Required Test Cases

#### Category 1: Basic To-Hit Modifiers

**Test 1.1: ARAD vs. ECM-Equipped Target**
```java
@Test
void testARADToHitBonusAgainstECMTarget() {
    // Setup: Target with ECM, no hostile ECM field
    // Expected: -1 to-hit modifier
    // Verification: toHit.getValue() reflects -1 bonus
}
```

**Test 1.2: ARAD vs. Non-Emitting Target**
```java
@Test
void testARADToHitPenaltyAgainstCleanTarget() {
    // Setup: Target with no electronics
    // Expected: +2 to-hit modifier
    // Verification: toHit.getValue() reflects +2 penalty
}
```

**Test 1.3: ARAD vs. C3-Equipped Target**
```java
@Test
void testARADToHitBonusAgainstC3Target() {
    // Setup: Target with active C3
    // Expected: -1 to-hit modifier
    // Verification: toHit.getValue() reflects -1 bonus
}
```

#### Category 2: Cluster Modifiers

**Test 2.1: Cluster Bonus vs. Electronics**
```java
@Test
void testARADClusterBonusAgainstActiveProbe() {
    // Setup: Target with Active Probe
    // Expected: +1 cluster modifier
    // Verification: getSalvoBonus() returns 1
}
```

**Test 2.2: Cluster Penalty vs. Clean Target**
```java
@Test
void testARADClusterPenaltyAgainstCleanTarget() {
    // Setup: Target with no electronics
    // Expected: -2 cluster modifier
    // Verification: getSalvoBonus() returns -2
    // Additional: Verify minimum cluster result is 2
}
```

#### Category 3: Stealth Armor Interaction

**Test 3.1: Stealth Blocks Internal Systems**
```java
@Test
void testStealthArmorBlocksInternalSystems() {
    // Setup: Target with active Stealth Armor + C3 + ECM
    // Expected: +2 to-hit, -2 cluster (treated as non-emitting)
    // Verification: Stealth overrides internal systems
}
```

**Test 3.2: Narc Overrides Stealth**
```java
@Test
void testNarcOverridesStealthArmor() {
    // Setup: Target with active Stealth + Narc pod
    // Expected: -1 to-hit, +1 cluster (Narc not blocked)
    // Verification: External Narc works through Stealth
}
```

**Test 3.3: Inactive Stealth Doesn't Block**
```java
@Test
void testInactiveStealthDoesntBlock() {
    // Setup: Target with inactive Stealth + C3
    // Expected: -1 to-hit, +1 cluster (C3 works)
    // Verification: Only active Stealth blocks
}
```

#### Category 4: Narc + ECM Interaction

**Test 4.1: Narc Overrides Hostile ECM**
```java
@Test
void testNarcOverridesHostileECM() {
    // Setup: Narc-tagged target in hostile ECM field
    // Expected: -1 to-hit, +1 cluster (Narc overrides ECM)
    // Verification: "ARAD ignores ECM when Narc-tagged" rule
}
```

**Test 4.2: ECM Blocks Non-Narced Electronics**
```java
@Test
void testECMBlocksNonNarcedTarget() {
    // Setup: Target with C3 in hostile ECM field, no Narc
    // Expected: 0 bonus/penalty (ECM blocks ARAD)
    // Verification: ECM prevents ARAD from working
}
```

**Test 4.3: ARAD Doesn't Stack with Narc Bonus**
```java
@Test
void testARADDoesntStackWithNarcBonus() {
    // Setup: Narc-tagged target with C3
    // Expected: -1 to-hit (ARAD bonus only, not -2)
    // Verification: ARAD lacks M_NARC_CAPABLE flag
}
```

#### Category 5: Powered-Down Equipment

**Test 5.1: Powered-Down C3**
```java
@Test
void testPoweredDownC3DoesntTrigger() {
    // Setup: Target with C3 shut down
    // Expected: +2 to-hit, -2 cluster (no electronics)
    // Verification: isShutDown() check works
}
```

**Test 5.2: Mixed Power States**
```java
@Test
void testMixedPowerStates() {
    // Setup: Target with C3 on, ECM off
    // Expected: -1 to-hit, +1 cluster (C3 qualifies)
    // Verification: Only one system needs to be on
}
```

#### Category 6: Communications Equipment

**Test 6.1: Built-In Comms Don't Qualify**
```java
@Test
void testBuiltInCommsDoNotTrigger() {
    // Setup: Standard Mech with only cockpit comms
    // Expected: +2 to-hit, -2 cluster (no qualifying comms)
    // Verification: 1-ton built-in comms ignored
}
```

**Test 6.2: Heavy Dedicated Comms Qualify**
```java
@Test
void testHeavyCommsQualify() {
    // Setup: Mobile HQ with 4-ton dedicated comms
    // Expected: -1 to-hit, +1 cluster (qualifies)
    // Verification: getTonnage() >= 3.5 check
}
```

**Test 6.3: Light Dedicated Comms Don't Qualify**
```java
@Test
void testLightCommsDoNotQualify() {
    // Setup: Unit with 2-ton dedicated comms
    // Expected: +2 to-hit, -2 cluster (below threshold)
    // Verification: Must be >= 3.5 tons
}
```

#### Category 7: Artemis/Streak Incompatibility

**Test 7.1: ARAD No Artemis Bonus**
```java
@Test
void testARADDoesntGetArtemisBonus() {
    // Setup: ARAD fired from Artemis-equipped launcher
    // Expected: No Artemis cluster bonus
    // Verification: ARAD lacks M_ARTEMIS_CAPABLE flag
}
```

**Test 7.2: Target Artemis Triggers ARAD**
```java
@Test
void testTargetArtemisTriggersARAD() {
    // Setup: Target has Artemis FCS
    // Expected: -1 to-hit, +1 cluster (Artemis qualifies)
    // Verification: Artemis is qualifying electronics
}
```

#### Category 8: Excluded Equipment

**Test 8.1: Streak Doesn't Trigger**
```java
@Test
void testStreakDoesntTrigger() {
    // Setup: Target with Streak SRM launcher
    // Expected: +2 to-hit, -2 cluster (Streak excluded)
    // Verification: Official ruling exclusion
}
```

**Test 8.2: TAG Doesn't Trigger**
```java
@Test
void testTAGDoesntTrigger() {
    // Setup: Target with TAG
    // Expected: +2 to-hit, -2 cluster (TAG excluded)
    // Verification: Official ruling exclusion
}
```

#### Category 9: iNarc Pod Types

**Test 9.1: iNarc Homing Triggers**
```java
@Test
void testINarcHomingTriggers() {
    // Setup: Target with friendly iNarc homing pod
    // Expected: -1 to-hit, +1 cluster (homing qualifies)
    // Verification: Standard iNarc works like Narc
}
```

**Test 9.2: iNarc Nemesis Doesn't Trigger**
```java
@Test
void testINarcNemesisDoesntTrigger() {
    // Setup: Target with friendly iNarc Nemesis pod
    // Expected: +2 to-hit, -2 cluster (Nemesis excluded)
    // Verification: Official ruling exclusion
}
```

**Test 9.3: iNarc Haywire Doesn't Trigger**
```java
@Test
void testINarcHaywireDoesntTrigger() {
    // Setup: Target with iNarc Haywire pod
    // Expected: +2 to-hit, -2 cluster (Haywire excluded)
    // Verification: Official ruling exclusion
}
```

#### Category 10: Multiple Systems

**Test 10.1: Multiple Systems Don't Stack**
```java
@Test
void testMultipleSystemsDontStack() {
    // Setup: Target with C3 + ECM + Active Probe
    // Expected: -1 to-hit, +1 cluster (not cumulative)
    // Verification: "Modifiers not cumulative" rule
}
```

**Test 10.2: Any One System Sufficient**
```java
@Test
void testAnyOneSystemSufficient() {
    // Setup: Target with only Blue Shield
    // Expected: -1 to-hit, +1 cluster (one system enough)
    // Verification: "At least one" requirement
}
```

### Manual Testing Scenarios

**REQUIRED TESTING BEFORE PR:**

**Complete Testing Matrix:**

| # | Scenario | Setup | Expected To-Hit | Expected Cluster | Expected Report | Status |
|---|----------|-------|-----------------|------------------|-----------------|--------|
| 1 | **Normal Bonus** | Enemy with electronics, no ECM | -1 (bonus) | N/A (parent reports) | `(w/ +1 bonus)` | ⏳ Pending |
| 2 | **ECM Blocking** | Enemy with electronics + ECM | -1 (bonus) | 0 (blocked) | `(ARAD bonus blocked by ECM)` | ✅ Passed |
| 3 | **Narc Override** | Enemy with electronics + ECM + friendly Narc | -1 (bonus) | +1 (Narc override) | `(ARAD bonus - Narc override)` + `(w/ +1 bonus)` | ⏳ Pending |
| 4 | **No Electronics** | Enemy without electronics | +2 (penalty) | -2 (penalty) | `(ARAD penalty - no electronics detected)` + `(w/ -2 malus)` | ✅ Passed |
| 5 | **Building Target** | Fire at building/hex | +2 (penalty) | -2 (penalty) | `(ARAD penalty - no electronics detected)` + `(w/ -2 malus)` | ⏳ Pending |
| 6 | **Stealth Armor** | Enemy with active Stealth + C3 | +2 (penalty) | -2 (penalty) | `(ARAD penalty - no electronics detected)` + `(w/ -2 malus)` | ⏳ Pending |
| 7 | **Stealth + Narc** | Enemy with active Stealth + friendly Narc | -1 (bonus) | +1 (Narc override) | `(ARAD bonus - Narc override)` + `(w/ +1 bonus)` | ⏳ Pending |
| 8 | **Multiple Systems** | Enemy with C3 + ECM + Active Probe (no ECM field) | -1 (bonus) | +1 (not cumulative) | `(w/ +1 bonus)` | ⏳ Pending |

**Detailed Testing Scenarios:**

**Scenario 1: Normal Bonus (Enemy with Electronics, No ECM)**
- Setup: Enemy mech with C3/ECM/Active Probe, no ECM field present
- Expected: -1 to-hit, cluster report shows `(w/ +1 bonus)` (parent reporting)
- Verification: Standard ARAD bonus without interference
- **Status:** ⏳ Pending

**Scenario 2: ECM Blocking (Enemy with ECM Active)**
- Setup: Enemy Phoenix Hawk with Guardian ECM in ECM field
- Expected: -1 to-hit (electronics detected), cluster report shows `(ARAD bonus blocked by ECM)`
- Verification: Report 3363 displayed, no cluster bonus applied
- **Status:** ✅ Passed (Test 1 - 2025-01-16)

**Scenario 3: Narc Override (Narc + ECM)**
- Setup: Enemy with electronics + ECM field + friendly Narc tag
- Expected: -1 to-hit, cluster reports `(ARAD bonus - Narc override)` + `(w/ +1 bonus)`
- Verification: Report 3364 displayed, Narc overrides ECM blocking
- **Status:** ⏳ Pending

**Scenario 4: No Electronics (Clean Target)**
- Setup: Enemy Charger (no electronics) in ECM field
- Expected: +2 to-hit, cluster reports `(ARAD penalty - no electronics detected)` + `(w/ -2 malus)`
- Verification: Report 3368 + parent malus reporting
- **Status:** ✅ Passed (Test 3 - 2025-01-16)

**Scenario 5: Building Target (Non-Entity)**
- Setup: Fire ARAD at building or hex
- Expected: +2 to-hit, cluster reports `(ARAD penalty - no electronics detected)` + `(w/ -2 malus)`
- Verification: Report 3368 for non-entity targets
- **Status:** ⏳ Pending (SRM ARAD = Phase 6)

**Scenario 6: Stealth Armor (Active)**
- Setup: Enemy mech with active Stealth Armor + C3 (no Narc)
- Expected: +2 to-hit, -2 cluster, report shows `(ARAD penalty - no electronics detected)`
- Verification: Stealth blocks internal systems (C3/ECM/etc.), only Narc works
- **Status:** ⏳ Pending

**Scenario 7: Stealth + Narc Override**
- Setup: Enemy with active Stealth + friendly Narc pod
- Expected: -1 to-hit, +1 cluster, reports `(ARAD bonus - Narc override)` + `(w/ +1 bonus)`
- Verification: External Narc not blocked by Stealth
- **Status:** ⏳ Pending

**Scenario 8: Multiple Systems (No Stacking)**
- Setup: Enemy with C3 + ECM + Active Probe, no ECM field
- Expected: -1 to-hit, `(w/ +1 bonus)` (NOT +3 or cumulative)
- Verification: Bonuses don't stack, only one system needed
- **Status:** ⏳ Pending

**Additional Edge Case Testing:**

**Scenario 9: Powered-Down Equipment**
- Setup: Mech with C3 shut down (powered off)
- Expected: +2 to-hit, -2 cluster (treated as no electronics)
- **Status:** ⏳ Pending

**Scenario 10: Mobile Headquarters (Heavy Comms)**
- Setup: Mobile HQ with ≥3.5 tons dedicated comms
- Expected: -1 to-hit, +1 cluster bonus
- **Status:** ⏳ Pending

**Scenario 11: Standard Vedette (Built-in Comms Only)**
- Setup: Vedette vehicle (only 1-ton built-in comms)
- Expected: +2 to-hit, -2 cluster (built-in comms don't count)
- **Status:** ⏳ Pending

**Scenario 12: Indirect Fire + Narc**
- Setup: LRM indirect fire at Narc-tagged target
- Expected: Standard indirect penalties + ARAD bonus applies
- **Status:** ⏳ Pending

---

**Testing Completion Requirements:**

**Before Opening PR:**
- ✅ Scenarios 1-5 must be 100% passing (core functionality)
- ✅ Scenarios 6-8 must be tested (Stealth/Narc/ECM interactions)
- ⏳ Scenarios 9-12 recommended but optional (edge cases)

**Current Status:**
- Core Tests: 2/5 passing (40%)
- Stealth/Narc Tests: 0/3 passing (0%)
- Edge Cases: 0/4 tested (0%)
- **Overall: 2/12 scenarios tested (17%)**

**Next Testing Steps:**
1. Test Scenario 1 (Normal Bonus - no ECM)
2. Test Scenario 3 (Narc Override)
3. Test Scenario 6 (Stealth Armor)
4. Test Scenario 7 (Stealth + Narc)
5. Test Scenario 8 (Multiple Systems)

---

## Implementation Status and Commits

### Current Status

**Branch:** `nova-cews-fixes`
**Phase:** LRM ARAD Implementation (Phases 1-5 Complete)
**Testing:** In Progress (3/12 scenarios tested - 25%)

### Implementation Phases Status

- ✅ **Phase 1:** Add ARAD munition to AmmoType.java
- ✅ **Phase 2:** Create ARADEquipmentDetector utility class
- ✅ **Phase 3:** Create LRMARADHandler extending LRMHandler
- ✅ **Phase 4:** Add ARAD to-hit modifiers to ComputeToHit.java
- ✅ **Phase 5:** Integrate LRM ARAD handler into LRMWeapon class
- ⏳ **Phase 6:** Create SRMARADHandler and MMLARADHandler (Pending)
- ⏳ **Phase 7:** Write comprehensive unit tests (Pending)
- ⏳ **Phase 8:** Manual testing scenarios (In Progress - 25%)
- ⏳ **Phase 9:** Documentation and code review (Pending)

### Commits

**Note:** Commits not yet pushed - work in progress on local branch

**Session 2025-01-16:**

1. **Equipment Detection Fixes**
   - Changed all detection methods to use `getMisc()` instead of `getEquipment()`
   - Fixed MiscTypeFlag warnings when encountering weapons
   - Files: `ARADEquipmentDetector.java`

2. **Add ARAD Report Messages (Initial)**
   - Added Report 3363: ECM blocking message
   - Added Report 3364: Narc override message
   - Added Report 3368: No electronics penalty message
   - Files: All 4 `report-messages*.properties` files

3. **Implement Visible ECM Reports**
   - Override `calcHits()` in LRMARADHandler
   - Add visible report messages for ARAD cluster modifiers
   - Remove debug logging for production
   - Files: `LRMARADHandler.java`

4. **Fix Code Analysis Warnings**
   - Fixed duplicate property key in Spanish localization (line 690)
   - Removed JavaDoc blank lines in LRMARADHandler
   - Removed trailing spaces from all properties files
   - Files: `LRMARADHandler.java`, all 4 `report-messages*.properties`

5. **Add Normal Bonus Report Message**
   - Added Report 3369: Normal bonus (electronics detected)
   - Updated `calcHits()` to display Report 3369 for normal bonus case
   - Improved player feedback clarity for standard ARAD bonus
   - Files: `LRMARADHandler.java`, all 4 `report-messages*.properties`

### Testing Progress

**Completed Tests (3/12):**
- ✅ Scenario 2: ECM Blocking (Report 3363 verified)
- ✅ Scenario 4: No Electronics Penalty (Report 3368 verified)
- ⚠️ Scenario 1: Normal Bonus (ECM field interference - retest needed)

**Pending Tests (9/12):**
- ⏳ Scenario 1: Normal Bonus (clean retest without ECM)
- ⏳ Scenario 3: Narc Override
- ⏳ Scenario 5: Building Target
- ⏳ Scenario 6: Stealth Armor
- ⏳ Scenario 7: Stealth + Narc
- ⏳ Scenario 8: Multiple Systems
- ⏳ Scenarios 9-12: Edge cases

**Testing Notes:**
- Report 3369 added after user feedback - normal bonus now has explicit message
- Scenario 1 retest needed: ensure no ECM field present (use C3-only target)
- All ARAD scenarios now have clear player-visible feedback

### Files Modified

**Java Source Files:**
1. `megamek/src/megamek/common/weapons/handlers/lrm/LRMARADHandler.java`
   - Override `calcHits()` for visible reports
   - Added Report 3363, 3364, 3368, 3369 display logic

2. `megamek/src/megamek/common/weapons/handlers/ARADEquipmentDetector.java`
   - Changed equipment detection to use `getMisc()`

**Localization Files:**
1. `megamek/resources/megamek/common/report-messages.properties` (English)
2. `megamek/resources/megamek/common/report-messages_de.properties` (German)
3. `megamek/resources/megamek/common/report-messages_es.properties` (Spanish)
4. `megamek/resources/megamek/common/report-messages_ru.properties` (Russian)

**Reports Added:**
- **3363:** `\ (ARAD bonus blocked by ECM)`
- **3364:** `\ (ARAD bonus - Narc override)`
- **3368:** `\ (ARAD penalty - no electronics detected)`
- **3369:** `\ (ARAD bonus - electronics detected)` ← Added 2025-01-16

### Next Steps

1. Complete LRM ARAD testing (Scenarios 1, 3, 5-8)
2. Implement SRM ARAD handler (Phase 6)
3. Implement MML ARAD handler (Phase 6)
4. Write unit tests (Phase 7)
5. Create comprehensive testing documentation
6. Code review and PR preparation

---

## Edge Cases and Special Handling

### Edge Case 1: Stealth Armor Priority

**Scenario:** Target has active Stealth Armor + multiple internal systems (C3, ECM, Active Probe)

**Handling:**
1. Check for active Stealth Armor FIRST
2. If active, skip all internal system checks
3. Only check for external Narc pods
4. Return: Narc status only

**Code Pattern:**
```java
if (hasActiveStealthArmor(target)) {
    return isNarcTagged(target);  // Only Narc counts
}
// Otherwise check internal systems
```

### Edge Case 2: Equipment Destruction During Attack

**Scenario:** Equipment destroyed between attack declaration and resolution

**Handling:**
- Equipment state checked at resolution time
- Use standard validation: !isDestroyed() && !isMissing()
- Equipment destroyed mid-turn won't trigger ARAD

**Code Pattern:**
```java
boolean isValid = !equipment.isDestroyed() &&
                  !equipment.isMissing() &&
                  !equipment.isBreached();
```

### Edge Case 3: C3 Network Disruption

**Scenario:** C3 system powered on but not networked (isolated)

**Handling:**
- ARAD only cares if C3 is powered on
- Network status irrelevant (official ruling)
- Isolated C3 still triggers ARAD bonus

**Rationale:** System is still emitting, even if not connected

### Edge Case 4: Mixed Power States

**Scenario:** Target has C3 (on), ECM (off), Active Probe (on)

**Handling:**
- Check each system independently
- Return TRUE if ANY system is on
- First qualifying system found = sufficient

**Code Pattern:**
```java
return hasActiveProbe(target) ||  // Short-circuits
       hasC3(target) ||
       hasECM(target);
```

### Edge Case 5: ECM vs. Narc Priority

**Scenario:** Narc-tagged target in hostile ECM field

**Handling:**
1. Check if target is Narc-tagged
2. If yes, skip ECM check entirely
3. Return full ARAD bonus
4. Narc explicitly overrides ECM for ARAD

**Code Pattern:**
```java
boolean isNarced = target.isNarcedBy(team) || target.isINarcedBy(team);
if (isNarced) {
    return 1;  // Full bonus, ignore ECM
}
// Otherwise check ECM
```

### Edge Case 6: iNarc Pod Type Identification

**Scenario:** Need to distinguish iNarc Homing vs. Nemesis vs. Haywire

**Challenge:** `isINarcedBy()` may not distinguish pod types

**Solution Options:**
1. Check Entity's attached equipment for specific pod types
2. Assume all iNarc are homing (simplification)
3. Add pod type parameter to isINarcedBy() (requires engine modification)

**Recommended:** Option 1 - Check attached equipment

**Research Needed:** How does Entity store iNarc pod type information?

### Edge Case 7: Friendly vs. Enemy Narc

**Scenario:** Target has multiple Narc pods from different teams

**Handling:**
- Only friendly team Narc pods count
- Use `isNarcedBy(attackingEntity.getOwner().getTeam())`
- Enemy Narc pods ignored

**Code Pattern:**
```java
boolean isFriendlyNarc = target.isNarcedBy(
    attackingEntity.getOwner().getTeam());
```

### Edge Case 8: Minimum Cluster Result

**Scenario:** ARAD -2 cluster modifier against non-emitting target

**Handling:**
- Cluster roll minimum is 2 (per rules)
- Handler returns -2
- Cluster calculation must enforce minimum 2
- May need to override cluster calculation method

**Research Needed:** Where is cluster minimum enforced?

### Edge Case 9: Communications Equipment Edge Values

**Scenario:** Communications equipment at exactly 3.5 tons

**Handling:**
- Use >= 3.5 tons (inclusive)
- 3.5 tons qualifies
- 3.4 tons does not

**Code Pattern:**
```java
if (equipment.getTonnage() >= 3.5) {
    // Qualifies
}
```

### Edge Case 10: Artemis on Target vs. Launcher

**Scenario:** Artemis FCS on launcher, Artemis on target

**Handling:**
- **Launcher Artemis:** ARAD gets NO bonus (incompatible)
- **Target Artemis:** ARAD gets bonus (qualifying electronics)
- Two separate checks

**Code Pattern:**
```java
// Launcher check (incompatibility)
if (weapon.getLinkedBy().hasFlag(F_ARTEMIS)) {
    // ARAD shouldn't be fired (ammo selection prevents this)
}

// Target check (electronics detection)
if (target.hasArtemis()) {
    // Qualifies for ARAD bonus
}
```

---

## Code Reference Guide

### Key Method References

#### Entity.java Methods

**C3 Detection:**
```java
// megamek/src/megamek/common/units/Entity.java
public boolean hasC3()     // Line 6131
public boolean hasC3i()    // Line 6168
public boolean hasC3S()    // Line 5998
public boolean hasC3M()    // Line 6072
public boolean hasC3MM()   // Line 6087
```

**Other Systems:**
```java
public boolean hasECM()           // Line 5706
public boolean hasActiveProbe()   // Search for method
```

**Narc Detection:**
```java
public boolean isNarcedBy(int team)   // Search for method
public boolean isINarcedBy(int team)  // Search for method
```

#### MiscType.java Flags

**Equipment Type Flags:**
```java
// megamek/src/megamek/common/equipment/MiscType.java
public static final long F_BAP = 1L << 113;          // Active Probe
public static final long F_ECM = 1L << 112;          // ECM
public static final long F_C3S = 1L << 76;           // C3 Slave
public static final long F_C3I = 1L << 77;           // C3i
public static final long F_BLUE_SHIELD = 1L << 154;  // Blue Shield
public static final long F_COMMUNICATIONS = 1L << 102; // Comms
public static final long F_ARTEMIS = ...;            // Artemis IV
public static final long F_ARTEMIS_V = ...;          // Artemis V
public static final long F_STEALTH = ...;            // Stealth Armor
```

#### ComputeECM.java

**ECM Detection:**
```java
// megamek/src/megamek/common/compute/ComputeECM.java
public static boolean isAffectedByECM(Entity attacker,
                                      Coords attackerPos,
                                      Coords targetPos)
```

**Usage Pattern:**
```java
boolean isECMAffected = ComputeECM.isAffectedByECM(
    attackingEntity,
    attackingEntity.getPosition(),
    target.getPosition());
```

#### AmmoType.java

**Munitions Enum:**
```java
// megamek/src/megamek/common/equipment/AmmoType.java
public enum Munitions {
    M_STANDARD("Standard"),
    M_CLUSTER("Cluster"),
    M_FOLLOW_THE_LEADER("Follow The Leader"),
    // Add M_ARAD here (around line 1960)
    // ...
}
```

**MunitionMutator Pattern:**
```java
// Lines 680-690
private static final MunitionMutator XXX_MUNITION_MUTATOR =
    new MunitionMutator(
        "Display Name",
        costMultiplier,
        Munitions.M_XXX,
        techAdvancement,
        "reference");
```

**Munition List Addition:**
```java
// Around line 3416
addMunition(lrm, XXX_MUNITION_MUTATOR);
```

#### LRMWeapon.java

**Handler Selection:**
```java
// megamek/src/megamek/common/weapons/lrms/LRMWeapon.java
// Lines 166-168
@Override
protected AttackHandler getCorrectHandler(ToHitData toHit,
                                          WeaponAttackAction waa,
                                          Game game,
                                          GameManager manager) {
    AmmoType atype = (AmmoType) ammo.getType();

    if (atype.getMunitionType().contains(
        AmmoType.Munitions.M_FOLLOW_THE_LEADER)) {
        return new LRMFollowTheLeaderHandler(toHit, waa, game, manager);
    }

    // Add ARAD check here
    if (atype.getMunitionType().contains(AmmoType.Munitions.M_ARAD)) {
        return new LRMARADHandler(toHit, waa, game, manager);
    }

    return new LRMHandler(toHit, waa, game, manager);
}
```

#### LRMHandler.java

**Cluster Modifier Pattern:**
```java
// megamek/src/megamek/common/weapons/handlers/lrm/LRMHandler.java
// Lines 139-220
protected int calcHits(Vector<Report> vPhaseReport) {
    int nMissilesModifier = getClusterModifiers(false);

    // Check for Artemis (lines 156-175)
    if (mLinker != null && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
        if (!bECMAffected) {
            nMissilesModifier += 2;  // Artemis bonus
        }
    }

    // Add custom modifiers via getSalvoBonus()
    nMissilesModifier += getSalvoBonus();

    // Calculate hits using modifier
    // ...
}
```

**getSalvoBonus() Override:**
```java
// Override in custom handler
@Override
public int getSalvoBonus() {
    // Return cluster modifier: +1, 0, or -2
    return modifier;
}
```

#### ComputeToHit.java

**Munition Check Pattern:**
```java
// megamek/src/megamek/common/actions/compute/ComputeToHit.java
// Lines 142-145
EnumSet<AmmoType.Munitions> munition = EnumSet.of(
    AmmoType.Munitions.M_STANDARD);
if (ammoType != null) {
    munition = ammoType.getMunitionType();
}

// Later (around line 1540)
if (munition.contains(AmmoType.Munitions.M_NARC_CAPABLE)) {
    // Apply Narc bonus
}

// Add ARAD check here
if (munition.contains(AmmoType.Munitions.M_ARAD)) {
    // Apply ARAD to-hit modifier
}
```

#### Follow the Leader Reference

**Handler Implementation:**
```java
// megamek/src/megamek/common/weapons/handlers/lrm/
//     LRMFollowTheLeaderHandler.java

@Override
public int getSalvoBonus() {
    if (ComputeECM.isAffectedByECM(attackingEntity,
                                   attackingEntity.getPosition(),
                                   target.getPosition())) {
        return 0;  // ECM negates bonus
    } else {
        return nSalvoBonus;  // Usually +1
    }
}

@Override
protected int calculateNumCluster() {
    if (ComputeECM.isAffectedByECM(...)) {
        return super.calculateNumCluster();
    } else {
        return Integer.MAX_VALUE;  // All missiles hit
    }
}
```

### Test File References

**Example Test Locations:**
- Movement tests: `megamek/unittests/megamek/common/moves/`
- Attack tests: `megamek/unittests/megamek/common/actions/WeaponAttackActionToHitTest.java`
- Handler tests: Search for `*HandlerTest.java`
- Game tests: `megamek/unittests/megamek/server/totalWarfare/TWGameManagerTest.java`

**Mockito Pattern:**
```java
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Test
void testExample() {
    // Mock entities
    Game mockGame = mock(Game.class);
    Entity mockTarget = mock(Entity.class);

    // Define behavior
    when(mockTarget.hasECM()).thenReturn(true);

    // Test
    // ...
}
```

---

## Open Questions

### Question 1: Equipment Power State Tracking

**Question:** How does MegaMek track powered-down equipment?

**Methods to Research:**
- `Mounted.isShutDown()`
- `Mounted.curMode()`
- `Mounted.getMode()`
- Equipment activation states

**Impact:** Required for powered-down equipment detection

**Status:** Needs research

### Question 2: iNarc Pod Type Detection

**Question:** How to distinguish between iNarc Homing, Nemesis, and Haywire pods? Also need to determine pod team ownership.

**Methods to Research:**
- `Entity.isINarcedBy()` - does it include pod type?
- Entity attached equipment storage
- iNarc pod type enumeration
- Pod team/ownership tracking

**Impact:** Required for friendly vs enemy Nemesis distinction

**Status:** Needs research

**Priority:** HIGH - Forum rulings require distinguishing friendly vs enemy Nemesis pods

**Fallback:** Treat all friendly iNarc as triggering bonuses initially

### Question 3: Stealth Armor Mode Detection

**Question:** How to detect if Stealth Armor is actively running (mode "On")?

**Methods to Research:**
- `Mounted.curMode()`
- Stealth Armor activation checks
- ECM requirement for Stealth

**Impact:** Required for Stealth blocking logic

**Status:** Needs research

### Question 4: Cluster Hit Minimum Enforcement

**Question:** Where is the minimum cluster result enforced?

**Methods to Research:**
- `LRMHandler.calcHits()`
- Cluster table calculation
- Minimum result checks

**Impact:** Need to ensure -2 modifier doesn't go below 2 hits

**Status:** Needs research

### Question 5: Built-in Communications Detection

**Question:** How to distinguish built-in cockpit comms from dedicated equipment?

**Current Approach:**
- Only check equipment in `Entity.getEquipment()` list
- Assumption: Built-in comms not in equipment list

**Verification Needed:**
- Confirm built-in comms not in equipment list
- Check for F_COMMUNICATIONS flag on cockpit

**Status:** Needs verification

### Question 6: Existing ARAD Implementation

**Question:** Has ARAD been partially implemented already?

**Actions:**
1. Search codebase for "ARAD"
2. Search codebase for "Anti-Radiation"
3. Check if M_ARAD enum exists
4. Check for ARAD handlers

**Impact:** May save implementation time or reveal conflicts

**Status:** Needs investigation

### Question 7: iNarc Nemesis Redirection

**Question:** How does iNarc Nemesis redirection work in MegaMek?

**Research:**
- How are attacks redirected?
- Does ARAD need special handling for redirection?
- Or does existing redirection logic handle it?

**Impact:** Ensure ARAD can't bypass Nemesis (per ruling)

**Status:** Needs research

### Question 8: TAG Triggering ARAD Bonuses

**Question:** Does TAG (Target Acquisition Gear) trigger ARAD bonuses?

**Resolution:** **RESOLVED** - TAG DOES trigger ARAD bonuses

**Source:** Xotl forum ruling (precedence: RAW → forum rulings)

**Implementation:** Add TAG detection to qualifying electronics check
- Check if unit activated TAG earlier in turn
- TAG counts as active electronic emission

**Status:** RESOLVED per forum ruling

### Question 9: Ghost Targets Triggering ARAD Bonuses

**Question:** Do Ghost Targets trigger ARAD bonuses?

**Resolution:** **RESOLVED** - Ghost Targets DO trigger ARAD bonuses

**Source:** Forum ruling URL #5: "Units generating ghost targets qualify as emitting sources"

**Implementation:** Add Ghost Target generation detection
- Check if unit is generating Ghost Targets
- Active Ghost Target generation counts as emitting

**Status:** RESOLVED per forum ruling

### Question 10: ECCM Interaction with iNarc ECM Pods

**Question:** How does ECCM interact with iNarc ECM Pods for ARAD purposes?

**Context:** Forum ruling states "ECCM counters ECM pod effects"

**Methods to Research:**
- How MegaMek handles ECCM vs ECM pod interaction
- Whether ECCM prevents ARAD bonus from ECM pod-tagged targets
- Integration with `ComputeECM.isAffectedByECM()`

**Impact:** Affects whether targets with ECM pods grant ARAD bonuses in ECCM fields

**Status:** Needs research

**Priority:** MEDIUM - Edge case interaction

---

## Implementation Checklist

### Pre-Implementation

- [ ] Verify current branch: `git branch --show-current`
- [ ] Search for existing ARAD work
- [ ] Research equipment power state methods
- [ ] Research iNarc pod type detection
- [ ] Research Stealth Armor mode detection
- [ ] Review Follow the Leader implementation
- [ ] Review AmmoType.java structure

### Phase 1: Core Setup

- [ ] Add M_ARAD to Munitions enum
- [ ] Create ARAD_MUNITION_MUTATOR
- [ ] Add ARAD to LRM munition list
- [ ] Add ARAD to SRM munition list
- [ ] Add ARAD to MML munition list
- [ ] Verify compilation
- [ ] Test ammo appears in selection

### Phase 2: Handler Implementation

- [ ] Create LRMARADHandler.java
- [ ] Implement constructor
- [ ] Implement targetHasQualifyingElectronics()
- [ ] Implement hasActiveStealthArmor()
- [ ] Implement hasActiveProbe()
- [ ] Implement hasArtemis()
- [ ] Implement hasBlueShield()
- [ ] Implement hasC3()
- [ ] Implement hasHeavyComms()
- [ ] Implement hasECM()
- [ ] Implement isNarcTagged()
- [ ] Implement isValidEquipment()
- [ ] Implement getSalvoBonus() override
- [ ] Test compilation

### Phase 3: To-Hit Integration

- [ ] Add ARAD check to ComputeToHit.java
- [ ] Implement equipment detection in ComputeToHit
- [ ] Or create shared utility class
- [ ] Test to-hit modifiers
- [ ] Verify no Narc bonus stacking

### Phase 4: Weapon Integration

- [ ] Add handler selection to LRMWeapon.java
- [ ] Test LRM ARAD selection
- [ ] Add handler selection to SRMWeapon.java
- [ ] Test SRM ARAD selection
- [ ] Add handler selection to MMLWeapon.java
- [ ] Test MML ARAD selection

### Phase 5: SRM/MML Handlers

- [ ] Create SRMARADHandler.java
- [ ] Test SRM ARAD functionality
- [ ] Create MMLARADHandler.java
- [ ] Test MML ARAD functionality

### Phase 6: Testing

- [ ] Create LRMARADHandlerTest.java
- [ ] Write all unit tests (see Testing Strategy)
- [ ] Run unit tests
- [ ] Fix failing tests
- [ ] Manual testing scenarios
- [ ] Edge case verification

### Phase 7: Documentation

- [ ] Update CHANGELOG.md
- [ ] Create issue documentation
- [ ] Document testing results
- [ ] Note any deviations or limitations

### Phase 8: Code Review

- [ ] Self-review code
- [ ] Check compliance with MegaMek style guide
- [ ] Verify no Unicode in code/logs
- [ ] Check logging uses templates
- [ ] Verify JavaDoc on public methods
- [ ] Run full test suite

---

## Notes and Reminders

### Critical Rules

1. **NEVER create git commits** - User handles manually
2. **NEVER use Unicode** in code/logs (Windows compatibility)
3. **ALWAYS verify branch** before editing files
4. **ALWAYS ask permission** before creating files
5. **Bug fix scope**: Only fix the specific issue, don't refactor

### Coding Standards

- Use 4 spaces for indentation (never tabs)
- K&R brace style (opening brace on same line)
- Braces mandatory for all control structures
- Use templated logging: `LOGGER.debug("Message {}", value);`
- JavaDoc required for all public/protected methods

### Testing Requirements

- Unit tests required for game logic changes
- Use GameBoardTestCase for movement tests
- Use Mockito for complex entity setup
- GUI tests must handle headless environments

### Communication Style

- Be conversational and natural
- Get straight to the point
- Don't treat user as beginner
- Use human-like language
- Mark recommendations clearly

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2025-01-15 | Initial design document | Claude |
| 1.1 | 2025-01-16 | Added Consolidated Rules Reference section with complete equipment lists and modifier tables; Resolved TAG and Ghost Targets conflicts (both trigger bonuses per forum rulings); Updated Nemesis pod handling (friendly vs enemy distinction); Added availability/historical context; Clarified powered-down timing, Stealth active state, and Narc non-stacking; Added ECCM interaction question; Updated Open Questions with resolutions | Claude |

---

**END OF DOCUMENT**
