/*
 *  MegaMek - Copyright (C) 2016 The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common;

import static megamek.common.BattleForceSPA.*;
import static megamek.common.ASUnitType.*;
import static java.util.stream.Collectors.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.lang.String;

/**
 * Primarily concerned with calculating AlphaStrike values for an undamaged entity, and exporting
 * stats in csv form.

 * @author Neoancient
 *
 */
public class AlphaStrikeElement extends BattleForceElement {
    
    // AP weapon mounts have a set damage value.
    static final double AP_MOUNT_DAMAGE = 0.05;

    protected ASUnitType asUnitType;
    /** 
     * NEW Version
     * This covers all SpAs, including the damage values such as SRM2/2.
     * SpAs not associated with a number, e.g. RCN, have null assigned as Object.
     * SpAs associated with a number, such as MHQ5 (but not IF2), have an Integer or Double as Object.
     * SpAs assoicated with one or more damage numbers, such as IF2 or AC2/2/-, 
     * have an ASDamageVector or ASDamage as Object. 
     * TUR has a List<List<Object>> wherein each List<Object> contains a 
     * ASDamageVector as the first item and a Map<BattleForceSPA, ASDamageVector> as the second item.
     * This represents multiple turrets, each with a standard damage value and SpA damage values.
     * If TUR is present, none of the objects is null and the outer List must contain one item (one turret)
     * with standard damage at least.
     * BIM and LAM have a Map<String, Integer> as Object similar to the element's movement field. 
     */
    protected EnumMap<BattleForceSPA, Object> specialUnitAbilities = new EnumMap<>(BattleForceSPA.class);
    
    public AlphaStrikeElement() {
        
    };
    
    public AlphaStrikeElement(Entity en) {
        super(en);
        asUnitType = ASUnitType.getUnitType(en);
        if (en.getEntityType() == Entity.ETYPE_INFANTRY) {
            double divisor = ((Infantry) en).calcDamageDivisor();
            if (((Infantry)en).isMechanized()) {
                divisor /= 2.0;
            }
            armor *= divisor;
        }
        //Armored Glove counts as an additional AP mounted weapon
        if (en instanceof BattleArmor && en.hasWorkingMisc(MiscType.F_ARMORED_GLOVE)) {
            double apDamage = AP_MOUNT_DAMAGE * (TROOP_FACTOR[Math.min(((BattleArmor)en).getShootingStrength(), 30)] + 0.5);
            weaponLocations[0].addDamage(0, apDamage);
            weaponLocations[0].addDamage(WeaponType.BFCLASS_STANDARD, 0, apDamage);
        }
    }
    
    /** 
     * NEW version - Adds a Special Unit Ability that is not associated with any
     * additional information or number, e.g. RCN.
     */
    public void addSPA(BattleForceSPA spa) {
        specialUnitAbilities.put(spa, null);
    }

    /** 
     * NEW version - Adds a Special Unit Ability associated with an integer number such as C3M#. If
     * that SPA is already present, the given number is added to the one already present. If the present
     * number is a Double type value, that type is preserved.
     */
    public void addSPA(BattleForceSPA spa, int number) {
        if (!specialUnitAbilities.containsKey(spa)) {
            specialUnitAbilities.put(spa, number);
        } else {
            if (specialUnitAbilities.get(spa) instanceof Integer) {
                specialUnitAbilities.put(spa, (int) specialUnitAbilities.get(spa) + number);
            } else if (specialUnitAbilities.get(spa) instanceof Double) {
                specialUnitAbilities.put(spa, (double) specialUnitAbilities.get(spa) + number);
            }
        }
    }
    
    /** 
     * NEW version - Adds a Special Unit Ability associated with a possibly non-integer number such 
     * as MHQ2. If that SPA is already present, the given number is added to the one already present.
     * if the previosly present number was an integer, it will be converted to a Double type value.
     */
    public void addSPA(BattleForceSPA spa, double number) {
        if (!specialUnitAbilities.containsKey(spa)) {
            specialUnitAbilities.put(spa, number);
        } else {
            if (specialUnitAbilities.get(spa) instanceof Integer) {
                specialUnitAbilities.put(spa, (int)specialUnitAbilities.get(spa) + number);
            } else if (specialUnitAbilities.get(spa) instanceof Double) {
                specialUnitAbilities.put(spa, (double)specialUnitAbilities.get(spa) + number);
            }
        }
    }
    
    /** 
     * NEW version - Replaces the value associated with a Special Unit Ability with the given Object.
     * The previously present associated Object, if any, is discarded. If the ability was not present, 
     * it is added.  
     */
    public void replaceSPA(BattleForceSPA spa, Object newValue) {
        specialUnitAbilities.put(spa, newValue);
    }
    
    /** 
     * NEW version - Adds a Special Unit Ability associated with a single damage value such as IF2. If
     * that SPA is already present, the new damage value replaces the former.
     */
    public void addSPA(BattleForceSPA spa, ASDamage damage) {
        specialUnitAbilities.put(spa, damage);
    }
    
    /** 
     * NEW version - Adds a Special Unit Ability associated with a full damage vector such as LRM1/2/2. If
     * that SPA is already present, the new damage value replaces the former.
     */
    public void addSPA(BattleForceSPA spa, ASDamageVector damage) {
        specialUnitAbilities.put(spa, damage);
    }
    
    /** NEW version - Adds the TUR Special Unit Ability with a List<List<Object>>. */
    public void addTurSPA(List<List<Object>> turAbility) {
        specialUnitAbilities.put(TUR, turAbility);
    }
    
    /** NEW version - Adds the LAM Special Unit Ability with a LAM movement map. */
    public void addLamSPA(Map<String, Integer> specialMoves) {
        specialUnitAbilities.put(LAM, specialMoves);
    }
    
    /** NEW version - Adds the BIM Special Unit Ability with a LAM movement map. */
    public void addBimSPA(Map<String, Integer> specialMoves) {
        specialUnitAbilities.put(BIM, specialMoves);
    }
    
    public boolean usesSML() {
        return rangeBands == RANGEBANDS_SML;
    }
    
    public boolean usesSMLE() {
        return rangeBands == RANGEBANDS_SMLE;
    }
    
    public ASDamageVector getStandardDamage() {
        return standardDamage;
    }
    
    public boolean hasSPA(BattleForceSPA spa) {
        return specialUnitAbilities.containsKey(spa);
    }
    
    public void removeSPA(BattleForceSPA spa) {
        specialUnitAbilities.remove(spa);
    }
    
    public boolean hasAnySPAOf(BattleForceSPA spa, BattleForceSPA... furtherSPAs) {
        if (hasSPA(spa)) {
            return true;
        }
        for (BattleForceSPA furtherSPA : furtherSPAs) {
            if (hasSPA(furtherSPA)) {
                return true;
            }
        }
        return false;
    }

    protected void initWeaponLocations(Entity en) {
        weaponLocations = new WeaponLocation[en.getNumAlphaStrikeWeaponsLocations()];
        locationNames = new String[weaponLocations.length];
        for (int loc = 0; loc < locationNames.length; loc++) {
            weaponLocations[loc] = new WeaponLocation();
            locationNames[loc] = en.getAlphaStrikeLocationName(loc);
            if (locationNames[loc].length() > 0) {
                locationNames[loc] += ":";
            }
        }
    }
    
    protected double locationMultiplier(Entity en, int loc, Mounted mount) {
    	return en.getAlphaStrikeLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
    }
    
    @Override
    protected void computeMovement(Entity en) {
    	en.setAlphaStrikeMovement(movement);    	
    }
    
    @Override
    public String getMovementAsString() {
    	return movement.entrySet().stream().map(this::moveString).collect(joining("/"));    	
    }
    
    /** Returns the formatted String for a single movement type, e.g. 4a or 12"j. */
    private String moveString(Entry<String, Integer> move) {
        if (move.getKey().equals("k")) {
            return "0." + move.getValue() + "k";
        } else if (move.getKey().equals("a")) {
            return move.getValue() + "a";
        } else if (move.getKey().equals("p")) {
            return move.getValue() + "p";
        } else if (isAnyTypeOf(DS, WS, DA, JS, SS) && move.getKey().isBlank()) {
            return move.getValue() + "";
        } else {
            return move.getValue() + "\"" + move.getKey();
        }
    }
    
    public int getTMM() {
        return tmm;
    }
    
    public int getTargetMoveModifier() {
    	int base = getPrimaryMovementValue();
    	if (base > 34) {
    		return 5;
    	} else if (base > 18) {
    		return 4;
    	} else if (base > 12) {
    		return 3;
    	} else if (base > 8) {
    		return 2;
    	} else if (base > 4) {
    		return 1;
    	}
    	return 0;
    }
    
    protected static final int[] TROOP_FACTOR = {
        0, 0, 1, 2, 3, 3, 4, 4, 5, 5, 6,
        7, 8, 8, 9, 9, 10, 10, 11, 11, 12,
        13, 14, 15, 16, 16, 17, 17, 17, 18, 18
    };
    
    @Override
    protected double getConvInfantryStandardDamage(int range, Infantry inf) {
        if (inf.getPrimaryWeapon() == null) {
            return inf.getDamagePerTrooper() * TROOP_FACTOR[Math.min(inf.getShootingStrength(), 30)]
                    / 10.0;
        } else {
            return 0;
        }
    }
    
    @Override
    protected double getBattleArmorDamage(WeaponType weapon, int range, BattleArmor ba, boolean apmMount) {
        double dam = 0;
        if (apmMount) {
            if (range == 0) {
                dam = AP_MOUNT_DAMAGE;
            }
        } else {
            dam = weapon.getBattleForceDamage(range);
        }
        return dam * (TROOP_FACTOR[Math.min(ba.getShootingStrength(), 30)] + 0.5);        
    }
    
    public ASUnitType getUnitType() {
        return asUnitType;
    }
    
    //TODO: Override calculatePointValue(Entity en)
    
    public String getASDamageString(int loc) {
    	return getASDamageString(loc, true);
    }
    
    public String getSMLDamageString() {
        if (!usesSML()) {
            return "Error: this AlphaStrike element does not use the S/M/L damage format!";
        }
        if (!weaponLocations[0].hasDamage()) {
            return "";
        }
        StringBuilder str = new StringBuilder(locationNames[0]);
        str.append(weaponLocations[0].formatDamageRounded(true));
        return str.toString();
    }
    
    public int getOverheat() {
        return overheat;
    }
    
    public String getSpecialsString() {
        //TODO REMOVE:    ONLY FOR COMPARISON WITH THE MUL
        String s = specialUnitAbilities.keySet().stream()
                .filter(this::showSpecial)
                .map(spa -> formatSPAString(spa))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(","));
        if (hasAnySPAOf(JMPW, JMPS) && !s.isBlank()) {
            s += ",";
        }
        s += hasSPA(JMPW) ? formatSPAString(JMPW) : ""; 
        s += hasSPA(JMPS) ? formatSPAString(JMPS) : "";
        return s;
        
        
        
        
        
        
//        return specialUnitAbilities.keySet().stream()
//                .filter(this::showSpecial)
//                .map(spa -> formatSPAString(spa))
//                .sorted(String.CASE_INSENSITIVE_ORDER)
//                .collect(Collectors.joining(","));
    }
    
    public boolean hasIF() {
        return weaponLocations[0].getIF() > 0; 
    }
    
    public int getIF() {
        return (int)Math.round(weaponLocations[0].getIF());
    }
    
    public boolean isMinimalIF() {
        return weaponLocations[0].getIF() < 0.5 && weaponLocations[0].getIF() > 0;
    }
    
    
    public boolean showSpecial(BattleForceSPA spa) {
        if ((asUnitType == BM) && (spa == SOA || spa == SRCH)) {
            return false;
        }
        if ((asUnitType == CV) && (spa == SRCH)) {
            return false;
        }
        if (!isAnyTypeOf(JS, WS, SC, DA, DS) && spa.isDoor()) {
            return false;
        }
        if (isAnyTypeOf(BM, AF, SC, DS, JS, WS, SS, DA) && (spa == SEAL)) {
            return false;
        }
        return true;
    }
   
    
    public String getASDamageString(int loc, boolean showIfNoDamage) {
    	if (!weaponLocations[loc].hasDamage()) {
    		return "";
    	}
        StringBuilder str = new StringBuilder(locationNames[loc]);
        if (locationNames[loc].length() > 0) {
            str.append("(");
        }
        str.append(weaponLocations[loc].formatDamageRounded(true));
        for (int i = WeaponType.BFCLASS_CAPITAL; i < WeaponType.BFCLASS_NUM; i++) {
            if (weaponLocations[loc].hasDamageClass(i)) {
                str.append(";").append(WeaponType.BF_CLASS_NAMES[i])
                    .append(weaponLocations[loc].formatDamageRounded(i, true));
            }
        }
        for (int i = 1; i < WeaponType.BFCLASS_CAPITAL; i++) {
            if (weaponLocations[loc].hasDamageClass(i)) {
                str.append(";").append(WeaponType.BF_CLASS_NAMES[i])
                    .append(weaponLocations[loc].formatDamageRounded(i, true));
            }
        }
        if (weaponLocations[loc].getIF() >= 0.5) {
            str.append(";IF").append((int)Math.round(weaponLocations[loc].getIF()));
        }
        if (locationNames[loc].length() > 0) {
            str.append(")");
        }
        return str.toString();
    }
    
    @Override
    public void writeCsv(BufferedWriter w) throws IOException {
        w.write(name);
        w.write("\t");
        w.write(asUnitType.toString());
        w.write("\t");
        w.write(Integer.toString(size));
        w.write("\t");
        w.write(getMovementAsString());
        w.write("\t");
        w.write(Integer.toString((int)Math.round(armor)));
        if (threshold >= 0) {
            w.write("-" + (int)Math.ceil(threshold));//TODO: threshold
        }
        w.write("\t");
        w.write(Integer.toString(structure));
        w.write("\t");
        StringJoiner sj = new StringJoiner(", ");
        for (int loc = 0; loc < weaponLocations.length; loc++) {
            StringBuilder str = new StringBuilder();
            String damStr = getASDamageString(loc, false);
            if (damStr.length() > 0) {
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
                sj.add(locationNames[loc] + Math.max(4, (int)Math.round(weaponLocations[loc].getOverheat())));
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
                .filter(spa -> spa.usedByAlphaStrike()
                        && !spa.isDoor())
                .map(spa -> formatSPAString(spa))
                .collect(Collectors.joining(", ")));
        w.newLine();
    }
    
    protected String formatSPAString(BattleForceSPA spa) {
        /* BOMB rating for ASFs and CFs is one less than for BF */
//        if (spa.equals(BOMB) && isAnyTypeOf(ASUnitType.AF, ASUnitType.CF)) {
//            return spa.toString() + ((int) getSPA(spa) - 1);
//        }
        if (spa == IF) {
            return spa.toString() + getSPA(spa);
        } else if (spa == TUR) {
            return turretString();
        } else if (spa == BIM || spa == LAM) {
            return lamString();
        } else if (spa == SRM) {
            return spa.toString() + ((ASDamageVector) getSPA(spa)).getSMString();
        } else if ((spa == C3BSS) || (spa == C3M) || (spa == C3BSM) || (spa == C3EM)
                || (spa == INARC) || (spa == CNARC) || (spa == SNARC)) {
            return spa.toString() + ((int) getSPA(spa) == 1 ? "" : (int) getSPA(spa));
        } else {
            return spa.toString() + (getSPA(spa) != null ? getSPA(spa) : "");
        }
    }
    
    /** 
     * Returns the formatted TUR Special Ability string such as TUR(3/3/1,AC2/2/-). Requires TUR to
     * be present. 
     */ 
    private String turretString() {
        List<List<Object>> turList = (List<List<Object>>) getSPA(TUR);
        String result = "";
        for (List<Object> currentTurret : turList) {
            result += "TUR(";
            for (Object turDamage : currentTurret) {
                if (turDamage instanceof ASDamageVector) {
                    result += turDamage;
                } else {
                    for (Entry<BattleForceSPA, Object> specialDmg : ((Map<BattleForceSPA, Object>) turDamage).entrySet()) {
                        result += "," + specialDmg.getKey().toString() + specialDmg.getValue();
                    }
                }
            }
            result +=")";
        }
        return result;
    }
    
    /** 
     * Returns the formatted TUR Special Ability string such as TUR(3/3/1,AC2/2/-). Requires TUR to
     * be present. 
     */ 
    private String lamString() {
        BattleForceSPA spa = hasSPA(LAM) ? LAM : BIM;
        Map<String, Integer> movelist = (Map<String, Integer>) getSPA(spa);
        String result = spa.toString() + "(";
        if (spa == LAM) {
            result += movelist.get("g") + "\"" + "g/";
        }
        result += movelist.get("a") + "a)";
        return result;
    }
    
    public String specialDamageString(double damageValue) {
        if (damageValue == 0) {
            return "-";
        } else if (damageValue < 0.5) {
            return "0*";
        } else {
            return String.valueOf((int)Math.round(damageValue));
        }
    }
    
    public Object getSPA(BattleForceSPA spa) {
        return specialUnitAbilities.get(spa);
    }
    
    public boolean usesOVL() {
        return isAnyTypeOf(BM, AF);
    }
    
    public boolean isJumpCapable() {
        return movement.keySet().contains("j");
    }
    
    public int getJumpMove() {
        return movement.getOrDefault("j", 0);
    }
    
    public ASUnitType getType() {
        return asUnitType;
    }
    
    public boolean isType(ASUnitType type) {
        return asUnitType == type;
    }
    
    public boolean isAnyTypeOf(ASUnitType type, ASUnitType... types) {
        if (isType(type)) {
            return true;
        }
        for (ASUnitType furtherType : types) {
            if (isType(furtherType)) {
                return true;
            }   
        }
        return false;
    }
    
    public boolean usesThreshold() {
        return threshold != -1;
    }
    
    
}
