# Testing Guide for PR #7483: Laser Heat Sink Bug Fixes

## Quick Setup

### Fetch and Checkout the PR

```bash
cd /path/to/your/megamek
git fetch origin pull/7483/head:pr-7483
git checkout pr-7483
```

### Build in IntelliJ

1. Open the project in IntelliJ IDEA
2. Open Gradle tool window: `View → Tool Windows → Gradle`
3. Run: `megamek → Tasks → build → build`
4. Run MegaMek: Right-click `megamek/src/megamek/MegaMek.java` → Run

## What to Test

This PR fixes two bugs with laser heat sinks to match current Tactical Operations rules:

### 1. Water Immersion Bonus
**Bug:** Laser heat sinks didn't get +6 heat capacity underwater  
**Fix:** Now they receive the standard +6 bonus (same as regular heat sinks)

**How to Test:**
```
Setup:
- Use a mech with laser heat sinks (e.g., Notos Prime: 20 laser HS)
- Place in water depth 4+

Test Steps:
1. Walk (+1 heat)
2. Fire weapons for 20 heat
3. End turn in deep water

Expected: 
- Heat generated: 21
- Heat dissipated: 26 (20 base + 6 water bonus)
- Final heat: 0 (was incorrectly showing 1 before fix)
```

### 2. Extreme Temperature Effects
**Bug:** Laser heat sinks were immune to temperature effects  
**Fix:** Now affected by extreme hot/cold (same as regular heat sinks)

**How to Test:**
```
Setup:
- Use a mech with laser heat sinks
- Set temperature to 100°C (Planetary Conditions)

Test Steps:
1. Stand still (no movement heat)
2. Fire weapons for 20 heat
3. End turn

Expected:
- Heat from weapons: 20
- Heat from temperature: +5 (for 100°C)
- Total heat: 25
- Heat dissipated: 20
- Final heat: 5 (was incorrectly showing 0 before fix)
- Report should show temperature modifier
```

## What's Already Working

These features were NOT changed and should still work:
- Night/dusk modifiers (reduces modifier by 1)
- Ammo explosion bonus (+1 to avoid roll at heat 19+)

## Reporting Results

After testing, comment on PR #7483 with:
- ✅ Water immersion: [PASS/FAIL + details]
- ✅ Extreme temperature: [PASS/FAIL + details]  
- Screenshots of heat calculations
- Any unexpected behavior

## Code Changes

For reference, the PR makes minimal changes:

**File 1:** `megamek/src/megamek/common/units/Mek.java` (lines 1588-1590)
- Removed early return for laser heat sinks in `getHeatCapacityWithWater()`

**File 2:** `megamek/src/megamek/server/totalWarfare/HeatResolver.java` (lines 1241-1251)
- Removed `laserHS` variable and condition check in `adjustHeatExtremeTemp()`

## Return to Main Branch

When done testing:
```bash
git checkout main
```
