/*
 *  MegaMek - Copyright (C) 2016 The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.bayweapons.ArtilleryBayWeapon;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.missiles.MissileWeapon;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Primarily concerned with calculating BattleForce values for an undamaged entity, and exporting
 * stats in csv form.
 * 
 * @author Neoancient
 */
public class BattleForceElement {
    
    static final int RANGE_BAND_SHORT = 0;
    static final int RANGE_BAND_MEDIUM = 1;
    static final int RANGE_BAND_LONG = 2;
    static final int RANGE_BAND_EXTREME = 3;
    static final int RANGE_BAND_NUM_GROUND = 3;
    static final int RANGE_BAND_NUM_AERO = 4;
    
    static final int[] STANDARD_RANGES = {0, 4, 16, 24};
    static final int[] CAPITAL_RANGES = {0, 13, 25, 41};
    
    public static final int SHORT_RANGE = STANDARD_RANGES[RANGE_BAND_SHORT];
    public static final int MEDIUM_RANGE = STANDARD_RANGES[RANGE_BAND_MEDIUM];
    public static final int LONG_RANGE = STANDARD_RANGES[RANGE_BAND_LONG];
    public static final int EXTREME_RANGE = STANDARD_RANGES[RANGE_BAND_EXTREME];
    
    protected String name;
    protected int size;
    protected LinkedHashMap<String,Integer> movement = new LinkedHashMap<>();
    protected double armor;
    protected double threshold = -1;
    protected int structure;
    protected int rangeBands = RANGE_BAND_NUM_GROUND;
    protected WeaponLocation[] weaponLocations;
    protected String[] locationNames;
    protected int[] heat;
    protected double points;
    protected EnumMap<BattleForceSPA,Integer> specialAbilities = new EnumMap<>(BattleForceSPA.class);
    
    public BattleForceElement(Entity en) {
        name = en.getShortName();
        size = en.getBattleForceSize();
        computeMovement(en);
        armor = en.getBattleForceArmorPointsRaw();
        if (en instanceof Aero) {
            threshold = armor / 10.0;
        }
        structure = en.getBattleForceStructurePoints();
        if (en instanceof Aero) {
        	rangeBands = RANGE_BAND_NUM_AERO;
        }
        initWeaponLocations(en);
        heat = new int[rangeBands];
        computeDamage(en);
        points = calculatePointValue(en);
        en.addBattleForceSpecialAbilities(specialAbilities);
    }
    
    protected void initWeaponLocations(Entity en) {
        weaponLocations = new WeaponLocation[en.getNumBattleForceWeaponsLocations()];
        locationNames = new String[weaponLocations.length];
        for (int loc = 0; loc < locationNames.length; loc++) {
            weaponLocations[loc] = new WeaponLocation();
            locationNames[loc] = en.getBattleForceLocationName(loc);
            if (locationNames[loc].length() > 0) {
                locationNames[loc] += ":";
            }
        }
    }
    
    protected void computeMovement(Entity en) {
    	en.setBattleForceMovement(movement);    	
    }
    
    public String getName() {
        return name;
    }
    
    public int getSize() {
        return size;
    }
    
    public Set<String> getMovementModes() {
    	return movement.keySet();
    }
    
    public int getMovement(String mode) {
    	return movement.get(mode);
    }
    
    public int getPrimaryMovementValue() {
    	return movement.values().iterator().next();
    }
    
    public String getMovementAsString() {
    	return movement.entrySet().stream()
    			.map(e -> (e.getKey().equals("k") ? "0." + e.getValue() : e.getValue())
    					+ e.getKey())
    			.collect(Collectors.joining("/"));    	
    }
    
    public int getFinalArmor() {
        return (int) Math.round(armor);
    }
    
    public double getArmor() {
        return armor;
    }
    
    public int getFinalThreshold() {
        return (int) Math.ceil(threshold);
    }
    
    public double getThreshold() {
        return threshold;
    }
    
    public int getStructure() {
        return structure;
    }
    
    public double getDamage(int loc, int rangeIndex, int damageClass) {
    	return weaponLocations[loc].getDamage(damageClass, rangeIndex);
    }
    
    public double getDamage(int rangeIndex) {
    	return weaponLocations[0].getDamage(rangeIndex);
    }
    
    public double getDamage(int rangeIndex, int damageClass) {
    	return getDamage(0, rangeIndex, damageClass);
    }
    
    public double getIndirectFire() {
    	return getIndirectFire(0);
    }
    
    public double getIndirectFire(int loc) {
    	return weaponLocations[loc].getIF();
    }
    
    public String getLocationName(int loc) {
        return locationNames[loc];
    }
    
    public Integer getSPA(BattleForceSPA spa) {
    	return specialAbilities.get(spa);
    }
    
    public boolean hasSPA(BattleForceSPA spa) {
    	return specialAbilities.containsKey(spa);
    }

    public int getFinalPoints() {
        return Math.max(1, (int) Math.round(points));
    }
    
    public double getPoints() {
        return points;
    }
    
    public void computeDamage(Entity en) {
        double[] baseDamage = new double[rangeBands];
        boolean hasTC = en.hasTargComp();
        int[] ranges;
        double pointDefense = 0;
        int bombRacks = 0;
        //Track weapons we've already calculated ammunition for
        HashMap<String,Boolean> ammoForWeapon = new HashMap<>();

        ArrayList<Mounted> weaponsList = en.getWeaponList();

        for (int pos = 0; pos < weaponsList.size(); pos++) {
            Arrays.fill(baseDamage, 0);
            double damageModifier = 1;
            Mounted mount = weaponsList.get(pos);
            if ((mount == null)
                    || (en.getEntityType() == Entity.ETYPE_INFANTRY
                        && mount.getLocation() == Infantry.LOC_INFANTRY)) {
                continue;
            }

            WeaponType weapon = (WeaponType) mount.getType();
            
            ranges = weapon.isCapital() ? CAPITAL_RANGES : STANDARD_RANGES;
            
            if (weapon.getAmmoType() == AmmoType.T_INARC) {
               specialAbilities.merge(BattleForceSPA.INARC, 1, Integer::sum);
               continue;
            } else if (weapon.getAmmoType() == AmmoType.T_NARC) {
                if (weapon.hasFlag(WeaponType.F_BA_WEAPON)) {
                    specialAbilities.merge(BattleForceSPA.CNARC, 1, Integer::sum);
                } else {
                    specialAbilities.merge(BattleForceSPA.SNARC, 1, Integer::sum);
                }
                continue;
            }
            
            if (weapon.getAtClass() == WeaponType.CLASS_SCREEN) {
                specialAbilities.merge(BattleForceSPA.SCR, 1, Integer::sum);
                continue;
            }

            if (weapon.hasFlag(WeaponType.F_AMS)) {
                specialAbilities.put(BattleForceSPA.AMS, null);
                continue;
            }

            if (weapon.hasFlag(WeaponType.F_TAG)) {
                if (weapon.hasFlag(WeaponType.F_C3MBS)) {
                    specialAbilities.merge(BattleForceSPA.C3BSM, 1, Integer::sum);
                    specialAbilities.merge(BattleForceSPA.MHQ, 12, Integer::sum); //count half-tons
                } else if (weapon.hasFlag(WeaponType.F_C3M)) {
                    specialAbilities.merge(BattleForceSPA.C3M, 1, Integer::sum);
                    specialAbilities.merge(BattleForceSPA.MHQ, 10, Integer::sum);
                }
                if (weapon.getShortRange() < 5) {
                    specialAbilities.put(BattleForceSPA.LTAG, null);
                } else {
                    specialAbilities.put(BattleForceSPA.TAG, null);
                }
                continue;
            }
            
            if (weapon.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
                addArtillery(weapon);
                continue;
            }
            if (weapon instanceof ArtilleryBayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    Mounted m = en.getEquipment(index);
                    if (m.getType() instanceof WeaponType) {
                        addArtillery((WeaponType) m.getType());
                    }
                }
            }
            
            if (weapon.getAmmoType() == AmmoType.T_BA_MICRO_BOMB) {
                bombRacks++;
                continue;
            }
            
            if (weapon.getAmmoType() == AmmoType.T_TASER) {
                if (en instanceof BattleArmor) {
                    specialAbilities.merge(BattleForceSPA.BTA, 1, Integer::sum);
                } else {
                    specialAbilities.merge(BattleForceSPA.MTA, 1, Integer::sum);
                }
                continue;
            }
            
            if (weapon.hasFlag(WeaponType.F_TSEMP)) {
                if (weapon.hasFlag(WeaponType.F_ONESHOT)) {
                    specialAbilities.merge(BattleForceSPA.TSEMPO, 1, Integer::sum);
                } else {
                    specialAbilities.merge(BattleForceSPA.TSEMP, 1, Integer::sum);
                }
                continue;
            }
            
            if (weapon instanceof InfantryAttack) {
                specialAbilities.put(BattleForceSPA.AM, null);
                continue;
            }

            // Check ammo weapons first since they had a hidden modifier
            if ((weapon.getAmmoType() != AmmoType.T_NA)
                    && !weapon.hasFlag(WeaponType.F_ONESHOT)
                    && (!(en instanceof BattleArmor) || weapon instanceof MissileWeapon)) {
                if (!ammoForWeapon.containsKey(weapon.getName())) {
                    int weaponsForAmmo = 1;
                    for (int nextPos = 0; nextPos < weaponsList.size(); nextPos++) {
                        if (nextPos == pos) {
                            continue;
                        }
                        
                        Mounted nextWeapon = weaponsList.get(nextPos);
    
                        if (nextWeapon == null) {
                            continue;
                        }
    
                        if (nextWeapon.getType().equals(weapon)) {
                            weaponsForAmmo++;
                        }
    
                    }
                    int ammoCount = 0;
                    // Check if they have enough ammo for all the guns to last at
                    // least 10 rounds
                    for (Mounted ammo : en.getAmmo()) {
    
                        AmmoType at = (AmmoType) ammo.getType();
                        if ((at.getAmmoType() == weapon.getAmmoType())
                                && (at.getRackSize() == weapon.getRackSize())) {
                            // RACs are always fired on 6 shot so that means you
                            // need 6 times the ammo to avoid the ammo damage
                            // modifier
                            if (at.getAmmoType() == AmmoType.T_AC_ROTARY) {
                                ammoCount += at.getShots() / 6;
                            } else if (at.getAmmoType() == AmmoType.T_AC_ULTRA
                                    || at.getAmmoType() == AmmoType.T_AC_ULTRA_THB) {
                                ammoCount += at.getShots() / 2;
                            } else {
                                ammoCount += at.getShots();
                            }
                        }
                    }
    
                    ammoForWeapon.put(weapon.getName(), (ammoCount / weaponsForAmmo) >= 10);
                }
                if (!ammoForWeapon.get(weapon.getName())) {
                    damageModifier *= 0.75;
                }
            }

            if (weapon.hasFlag(WeaponType.F_ONESHOT)) {
                damageModifier *= .1;
            }
            
            if (weapon instanceof BayWeapon) {
                for (int index : mount.getBayWeapons()) {
                    Mounted m = en.getEquipment(index);
                    if (m.getType() instanceof WeaponType) {
                        for (int r = 0; r < rangeBands; r++) {
                            baseDamage[r] += ((WeaponType) m.getType()).getBattleForceDamage(ranges[r], m.getLinkedBy());
                            heat[r] += ((WeaponType) m.getType()).getBattleForceHeatDamage(ranges[r]);
                        }
                    }
                }
            } else {
                for (int r = 0; r < rangeBands; r++) {
                    if (en instanceof BattleArmor) {
                        baseDamage[r] = getBattleArmorDamage(weapon, ranges[r], ((BattleArmor) en),
                                mount.isAPMMounted());
                    } else {
                        baseDamage[r] = weapon.getBattleForceDamage(ranges[r], mount.getLinkedBy());
                    }
                    heat[r] += weapon.getBattleForceHeatDamage(ranges[r]);
                }
            }

            // Targetting Computer
            if (hasTC && weapon.hasFlag(WeaponType.F_DIRECT_FIRE)
                    && (weapon.getAmmoType() != AmmoType.T_AC_LBX)
                    && (weapon.getAmmoType() != AmmoType.T_AC_LBX_THB)) {
                damageModifier *= 1.10;
            }
            
            for (int loc = 0; loc < weaponLocations.length; loc++) {
                double locMultiplier = locationMultiplier(en, loc, mount);
                if (locMultiplier == 0) {
                    continue;
                }
                for (int r = 0; r < rangeBands; r++) {
                    double dam = baseDamage[r] * damageModifier * locMultiplier;
                    if (!weapon.isCapital()) {
                        weaponLocations[loc].addDamage(r, dam);
                    }
                    if (weapon.getBattleForceClass() == WeaponType.BFCLASS_MML) {
                        if (r == RANGE_BAND_SHORT) {
                            weaponLocations[loc].addDamage(WeaponType.BFCLASS_SRM, r, dam);
                        } else if (r == RANGE_BAND_MEDIUM) {
                            weaponLocations[loc].addDamage(WeaponType.BFCLASS_SRM, r, dam / 2.0);
                            weaponLocations[loc].addDamage(WeaponType.BFCLASS_LRM, r, dam / 2.0);
                        } else {
                            weaponLocations[loc].addDamage(WeaponType.BFCLASS_LRM, r, dam);                            
                        }
                    } else {
                        weaponLocations[loc].addDamage(weapon.getBattleForceClass(), r, dam);
                    }
                    if (r == RANGE_BAND_LONG && !(en instanceof Aero) && weapon.hasIndirectFire()) {
                        weaponLocations[loc].addIF(dam);
                    }
                }
            }
            if (en instanceof Aero && weapon.getAtClass() == WeaponType.CLASS_POINT_DEFENSE) {
                pointDefense += baseDamage[RANGE_BAND_SHORT] * damageModifier;
            }
        }
        
        if (en.getEntityType() == Entity.ETYPE_INFANTRY) {
            int baseRange = 0;
            if (((Infantry) en).getSecondaryWeapon() != null && ((Infantry) en).getSecondaryN() >= 2) {
                baseRange = ((Infantry) en).getSecondaryWeapon().getInfantryRange();
            } else if (((Infantry) en).getPrimaryWeapon() != null) {
                baseRange = ((Infantry) en).getPrimaryWeapon().getInfantryRange();
            }
            int range = baseRange * 3;
            for (int r = 0; r < STANDARD_RANGES.length; r++) {
                if (range >= STANDARD_RANGES[r]) {
                    weaponLocations[0].addDamage(r, getConvInfantryStandardDamage(STANDARD_RANGES[r],
                            (Infantry) en));
                } else {
                    break;
                }
            }
        }
        
        if (en instanceof BattleArmor) {
            int vibroClaws = en.countWorkingMisc(MiscType.F_VIBROCLAW);
            if (vibroClaws > 0) {
                weaponLocations[0].addDamage(0, vibroClaws);
                weaponLocations[0].addDamage(WeaponType.BFCLASS_STANDARD, 0, vibroClaws);
            }
            if (bombRacks > 0) {
                specialAbilities.put(BattleForceSPA.BOMB,
                        (bombRacks * ((BattleArmor) en).getShootingStrength()) / 5);
            }
        }
        
        if (en instanceof Aero && pointDefense > 0) {
            specialAbilities.put(BattleForceSPA.PNT, (int) Math.ceil(pointDefense / 10.0));
        }

        adjustForHeat(en);
        //Rules state that all flamer and plasma weapons on the unit contribute to the heat rating, so we don't separate by arc
        if (heat[RANGE_BAND_SHORT] > 10) {
            specialAbilities.put(BattleForceSPA.HT, 2);
        } else if (heat[RANGE_BAND_SHORT] > 5) {
            specialAbilities.put(BattleForceSPA.HT, 1);
        }
    }
    
    protected double locationMultiplier(Entity en, int loc, Mounted mount) {
    	return en.getBattleForceLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
    }
    
    protected void addArtillery(WeaponType weapon) {
        BattleForceSPA artType = null;
        switch (weapon.getAmmoType()) {
            case AmmoType.T_ARROW_IV:
                if (weapon.getInternalName().charAt(0) == 'C') {
                    artType = BattleForceSPA.ARTAC;
                } else {
                    artType = BattleForceSPA.ARTAIS;
                }
                break;
            case AmmoType.T_LONG_TOM:
                artType = BattleForceSPA.ARTLT;
                break;
            case AmmoType.T_SNIPER:
                artType = BattleForceSPA.ARTS;
                break;
            case AmmoType.T_THUMPER:
                artType = BattleForceSPA.ARTT;
                break;
            case AmmoType.T_LONG_TOM_CANNON:
                artType = BattleForceSPA.ARTLTC;
                break;
            case AmmoType.T_SNIPER_CANNON:
                artType = BattleForceSPA.ARTSC;
                break;
            case AmmoType.T_THUMPER_CANNON:
                artType = BattleForceSPA.ARTTC;
                break;
            case AmmoType.T_CRUISE_MISSILE:
                switch (weapon.getRackSize()) {
                    case 50:
                        artType = BattleForceSPA.ARTCM5;
                        break;
                    case 70:
                        artType = BattleForceSPA.ARTCM7;
                        break;
                    case 90:
                        artType = BattleForceSPA.ARTCM9;
                        break;
                    case 120:
                        artType = BattleForceSPA.ARTCM12;
                        break;
                }
            case AmmoType.T_BA_TUBE:
                artType = BattleForceSPA.ARTBA;
                break;
        }
        if (artType != null) {
            specialAbilities.merge(artType, 1, Integer::sum);
        }        
    }
    
    /* BattleForce and AlphaStrike calculate infantry damage differently */
    protected double getConvInfantryStandardDamage(int range, Infantry inf) {
        if (inf.getPrimaryWeapon() == null) {
            int baseDamage = (int) Math.ceil(inf.getDamagePerTrooper() * inf.getShootingStrength());
            return Compute.calculateClusterHitTableAmount(7, baseDamage) / 10.0;
        } else {
            return 0;
        }
    }
    
    protected double getBattleArmorDamage(WeaponType weapon, int range, BattleArmor ba, boolean apmMounted) {
        return weapon.getBattleForceDamage(range, ba.getShootingStrength());
    }
    
    protected void adjustForHeat(Entity en) {
        double totalHeat;
        totalHeat = en.getBattleForceTotalHeatGeneration(false) - 4;
        int heatCapacity = calcHeatCapacity(en);
        if (totalHeat > heatCapacity) {
            double adjustment = heatCapacity / totalHeat;
            for (int loc = 0; loc < weaponLocations.length; loc++) {
                if (en.isBattleForceRearLocation(loc)) {
                    continue;
                }
                if (en instanceof Mech || en.getEntityType() == Entity.ETYPE_AERO) {
                    int rangeIndex = 1;
                    int base = (int) Math.round(weaponLocations[loc].getDamage(1));
                    if (base == 0) {
                        rangeIndex = 0;
                        base = (int) Math.round(weaponLocations[loc].getDamage(0));
                    }
                    if (base == 0) {
                        continue;
                    }
                    int heatAdjusted = (int) Math.round(weaponLocations[loc].getDamage(rangeIndex) * adjustment);
                    if (heatAdjusted < base) {
                        weaponLocations[loc].overheat = Math.min(base - heatAdjusted, 4);
                    }
                }
                weaponLocations[loc].adjustForHeat(adjustment);
            }
        }
        if (weaponLocations[0].getOverheat() > 0
                && (en instanceof Mech || en.getEntityType() == Entity.ETYPE_AERO)) {
            int heatLong = en.getWeaponList().stream()
                    .filter(m -> m.getType() instanceof WeaponType
                            && !m.isRearMounted()
                            && !en.isBattleForceRearLocation(m.getLocation()))
                    .map(m -> (WeaponType) m.getType())
                    .filter(w -> w.getLongRange() >= MEDIUM_RANGE)
                    .mapToInt(WeaponType::getHeat)
                    .sum();
            if (heatLong - 4 > heatCapacity) {
                specialAbilities.put(BattleForceSPA.OVL, null);
            }
        }
    }

    public int calcHeatCapacity(Entity en) {
        int capacity = en.getHeatCapacity();
        for (Mounted mounted : en.getEquipment()) {
            if (mounted.getType() instanceof AmmoType
                    && ((AmmoType) mounted.getType()).getAmmoType() == AmmoType.T_COOLANT_POD) {
                capacity++;
            } else if (mounted.getType() instanceof MiscType
                    && mounted.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
                capacity += 1;
            }
        }
        return capacity;
    }

    public double calculatePointValue(Entity en) {
        return en.calculateBattleValue(true, true) / 100.0;
    }
    
    public String getBFDamageString(int loc) {
        StringBuilder str = new StringBuilder(locationNames[loc]);
        if (locationNames[loc].length() > 0) {
            str.append("(");
        }
        str.append(weaponLocations[loc].formatDamageUp(WeaponType.BFCLASS_STANDARD));
        for (int i = WeaponType.BFCLASS_CAPITAL; i < WeaponType.BFCLASS_NUM; i++) {
            if (weaponLocations[loc].hasDamageClass(i)) {
                str.append(";").append(WeaponType.BF_CLASS_NAMES[i])
                    .append(weaponLocations[loc].formatDamageUp(i));
            }
        }
        for (int i = 1; i < WeaponType.BFCLASS_CAPITAL; i++) {
            if (weaponLocations[loc].hasDamageClassRounded(i)) {
                str.append(";").append(WeaponType.BF_CLASS_NAMES[i])
                    .append(weaponLocations[loc].formatDamageRounded(i, false));
            }
        }
        if (weaponLocations[loc].getIF() >= 0.5) {
            str.append(";IF").append((int) Math.round(weaponLocations[loc].getIF()));
        }
        if (locationNames[loc].length() > 0) {
            str.append(")");
        }
        return str.toString();
    }
    
    protected String getASRangeString(double... damage) {
        return Arrays.stream(damage).map(v -> v / 10.0)
                .mapToObj(d -> {
                    if (d > 0.5) {
                        return Integer.toString((int) Math.round(d));
                    } else if (d > 0) {
                        return "0*";
                    } else {
                        return "0";
                    }
                }).collect(Collectors.joining("/"));
    }
    
    public void writeCsv(BufferedWriter w) throws IOException {
        w.write(name);
        w.write("\t");
        w.write(Integer.toString(size));
        w.write("\t");
        w.write(getMovementAsString());
        w.write("\t");
        w.write(Integer.toString(getFinalArmor()));
        if (threshold >= 0) {
            w.write("-" + (int) Math.ceil(threshold));//TODO: threshold
        }
        w.write("\t");
        w.write(Integer.toString(structure));
        w.write("\t");
        StringJoiner sj = new StringJoiner(", ");
        for (int loc = 0; loc < weaponLocations.length; loc++) {
            StringBuilder str = new StringBuilder();
            String damStr = getBFDamageString(loc);
            if (weaponLocations[loc].hasDamageRounded()) {
                str.append(damStr);
                sj.add(str.toString());
            }
        }
        if (sj.length() > 0) {
            w.write(sj.toString());
        } else {
            w.write(rangeBands > 3? "0/0/0/0" : "0/0/0");
        }
        w.write("\t");
        sj = new StringJoiner(", ");
        for (int loc = 0; loc < weaponLocations.length; loc++) {
            if (weaponLocations[loc].getOverheat() >= 1) {
                sj.add(locationNames[loc] + Math.max(4, (int) Math.round(weaponLocations[loc].getOverheat())));
            }
        }
        if (sj.length() > 0) {
            w.write(sj.toString());
        } else {
            w.write("-");
        }
        w.write("\t");
        w.write(Integer.toString(getFinalPoints()));
        w.write("\t");
        w.write(specialAbilities.keySet().stream()
                .filter(spa -> spa.usedByBattleForce() && !spa.isDoor())
                .map(this::formatSPAString)
                .collect(Collectors.joining(", ")));
        w.newLine();
    }
    
    protected String formatSPAString(BattleForceSPA spa) {
        Integer val = specialAbilities.get(spa);
        if (val == null) {
            return spa.toString();
        }
        switch (spa) {
            case AT:
            case MT:
            case PT:
            case ST:
            case VTM:
            case VTH:
                return spa.toString() + val + "D" + specialAbilities.get(spa.getDoor());
            case CT:
                if (val >= 1000) {
                    return "CK" + (val / 1000.0) + "D" + specialAbilities.get(spa.getDoor());
                } else {
                    return spa.toString() + val + "D" + specialAbilities.get(spa.getDoor());
                }
            case MHQ:
                return spa.toString() + (val / 2);
            default:
                return spa.toString() + val;
        }
    }

    protected class WeaponLocation {
        List<Double> standardDamage = new ArrayList<>();
        Map<Integer,List<Double>> specialDamage = new HashMap<>();
        double indirect;
        double overheat;
        
        public boolean hasStandardDamage() {
            return standardDamage.stream().mapToDouble(Double::doubleValue).sum() > 0;
        }
        
        public boolean hasStandardDamageRounded() {
            return standardDamage.stream()
                    .filter(d -> d >= 0.5)
                    .mapToDouble(Double::doubleValue).sum() > 0;
        }
        
        public boolean hasDamage() {
        	return hasStandardDamage()
        			|| specialDamage.keySet().stream().anyMatch(this::hasDamageClass);
        }
        
        public boolean hasDamageRounded() {
        	return hasStandardDamageRounded()
        			|| specialDamage.keySet().stream().anyMatch(this::hasDamageClassRounded);
        }
        
        public boolean hasDamageClass(int damageClass) {
            return specialDamage.containsKey(damageClass)
                    && specialDamage.get(damageClass).stream().mapToDouble(Double::doubleValue).sum() > 0;
        }
        
        public boolean hasDamageClassRounded(int damageClass) {
            return specialDamage.containsKey(damageClass)
                    && specialDamage.get(damageClass).stream()
                    .filter(d -> d >= 0.5)
                    .mapToDouble(Double::doubleValue).sum() > 0;
        }
        
        public void addDamage(int rangeIndex, double val) {
            addDamage(standardDamage, rangeIndex, val);
        }
        
        public void addDamage(int damageClass, int rangeIndex, double val) {
            if (!specialDamage.containsKey(damageClass)) {
                specialDamage.put(damageClass, new ArrayList<>());
            }
            addDamage(specialDamage.get(damageClass), rangeIndex, val);
        }
        
        public double getDamage(int rangeIndex) {
            if (standardDamage.size() > rangeIndex) {
                return standardDamage.get(rangeIndex);
            }
            return 0;
        }
        
        public double getDamage(int damageClass, int rangeIndex) {
            if (specialDamage.containsKey(damageClass)
                    && specialDamage.get(damageClass).size() > rangeIndex) {
                return specialDamage.get(damageClass).get(rangeIndex);
            }
            return 0;
        }

        public String formatDamageUp(int damageClass) {
            if (specialDamage.containsKey(damageClass)) {
                return formatDamageUp(specialDamage.get(damageClass));
            }
            return rangeBands > 3? "0/0/0/0" : "0/0/0";
        }
        
        public String formatDamageRounded(boolean showMinDamage) {
            return formatDamageRounded(standardDamage, showMinDamage);
        }

        public String formatDamageRounded(int damageClass, boolean showMinDamage) {
            if (specialDamage.containsKey(damageClass)) {
                return formatDamageRounded(specialDamage.get(damageClass), showMinDamage);
            }
            return rangeBands > 3? "0/0/0/0" : "0/0/0";
        }
        
        public double getIF() {
            return indirect;
        }
        
        public void addIF(double val) {
            indirect += val;
        }
        
        public double getOverheat() {
            return overheat;
        }
        
        public void setOverheat(double val) {
            overheat = val;
        }
        
        public void adjustForHeat(double mul) {
            for (int i = 0; i < standardDamage.size(); i++) {
                standardDamage.set(i, standardDamage.get(i) * mul);
            }
            for (List<Double> damage : specialDamage.values()) {
                for (int i = 0; i < damage.size(); i++) {
                    damage.set(i, damage.get(i) * mul);
                }
            }
        }
        
        private void addDamage(List<Double> damage, int rangeIndex, double val) {
            while (damage.size() <= rangeIndex) {
                damage.add(0.0);
            }
            damage.set(rangeIndex, damage.get(rangeIndex) + val);
        }
        
        private String formatDamageUp(List<Double> damage) {
            while (damage.size() < rangeBands) {
                damage.add(0.0);
            }
            return damage.stream().map(d -> String.valueOf((int) Math.ceil(d)))
                    .collect(Collectors.joining("/"));
        }

        private String formatDamageRounded(List<Double> damage, boolean showMinDamage) {
            while (damage.size() < rangeBands) {
                damage.add(0.0);
            }
            return damage.stream().map(d -> {
                if (d == 0) {
                    return "0";
                } else if (d < 0.5 && showMinDamage) {
                    return "0*";
                } else {
                    return String.valueOf((int) Math.round(d));
                }
            }).collect(Collectors.joining("/"));
        }
    }
}
