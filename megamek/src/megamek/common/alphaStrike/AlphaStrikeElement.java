/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.alphaStrike;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.options.Quirks;

import static megamek.common.alphaStrike.BattleForceSPA.*;
import static megamek.common.alphaStrike.ASUnitType.*;
import static java.util.stream.Collectors.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import java.lang.String;

/**
 * Primarily concerned with calculating AlphaStrike values for an undamaged entity, and exporting
 * stats in csv form.

 * @author Neoancient
 *
 */
public class AlphaStrikeElement {

    static final int RANGEBANDS_SML = 3;
    static final int RANGEBANDS_SMLE = 4;
    public static final int RANGE_BAND_SHORT = 0;
    public static final int RANGE_BAND_MEDIUM = 1;
    public static final int RANGE_BAND_LONG = 2;
    public static final int RANGE_BAND_EXTREME = 3;

    static final int[] STANDARD_RANGES = {0, 4, 16, 24};
    static final int[] CAPITAL_RANGES = {0, 13, 25, 41};

    public static final int SHORT_RANGE = STANDARD_RANGES[RANGE_BAND_SHORT];
    public static final int MEDIUM_RANGE = STANDARD_RANGES[RANGE_BAND_MEDIUM];
    public static final int LONG_RANGE = STANDARD_RANGES[RANGE_BAND_LONG];
    public static final int EXTREME_RANGE = STANDARD_RANGES[RANGE_BAND_EXTREME];

    protected String name;
    protected String chassis;
    protected String model;
    protected UnitRole role;
    protected int size;
    protected Map<String,Integer> movement = new LinkedHashMap<>();
    protected int tmm;

    /**
     * The normal damage values of a ground unit (S/M/L) or fighter (S/M/L/E).
     * Spaceships and LG support vehicles use arcDamage instead.
     */
    protected ASDamageVector standardDamage;

    protected int overheat;
    protected double armor;
    protected double threshold = -1;
    protected int structure;
    protected int rangeBands = RANGEBANDS_SML;
    protected WeaponLocation[] weaponLocations;
    protected String[] locationNames;
    protected int[] heat;
    protected double points;
    protected EnumMap<BattleForceSPA,Integer> specialAbilities = new EnumMap<>(BattleForceSPA.class);
    
    // AP weapon mounts have a set damage value.
    static final double AP_MOUNT_DAMAGE = 0.05;

    private int mulId = -1;
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
    
    /** 
     * The multiple arced damage value groups and specials of spaceships, MSs and large SVs.
     * Ground units and fighters use standardDamage instead. 
     */
    public EnumMap<ASArcs, ASArcSummary> arcs = new EnumMap<>(ASArcs.class);

    /**
     * AlphaStrike Quirks.
     * Ideally these would be converted/filtered according to AS Companion, p. 59, but
     * currently, the TW quirks are just reproduced here.
     */
    private Quirks quirks = new Quirks();

    public int getSize() {
        return size;
    }


    public Quirks getQuirks() {
        return quirks;
    }

    public void setQuirks(Quirks quirks) {
        this.quirks = quirks;
    }

    public boolean hasQuirk(String name) {
        return quirks.booleanOption(name);
    }

    public int getSkill() {
        return skill;
    }

    public void setSkill(int skill) {
        this.skill = skill;
    }

    private int skill = 4;

    public AlphaStrikeElement() {
        
    }
    
//    public AlphaStrikeElement(Entity en) {
//        name = en.getShortName();
//        size = en.getBattleForceSize();
//        computeMovement(en);
//        armor = en.getBattleForceArmorPointsRaw();
//        if (en instanceof Aero) {
//            threshold = armor / 10.0;
//        }
//        structure = en.getBattleForceStructurePoints();
//        if (en instanceof Aero) {
//        	rangeBands = RANGEBANDS_SMLE;
//        }
//        initWeaponLocations(en);
//        heat = new int[rangeBands];
//        computeDamage(en);
//        points = calculatePointValue(en);
//        en.addBattleForceSpecialAbilities(specialAbilities);
//        asUnitType = ASUnitType.getUnitType(en);
//        if (en.getEntityType() == Entity.ETYPE_INFANTRY) {
//            double divisor = ((Infantry) en).calcDamageDivisor();
//            if (((Infantry)en).isMechanized()) {
//                divisor /= 2.0;
//            }
//            armor *= divisor;
//        }
//        //Armored Glove counts as an additional AP mounted weapon
//        if (en instanceof BattleArmor && en.hasWorkingMisc(MiscType.F_ARMORED_GLOVE)) {
//            double apDamage = AP_MOUNT_DAMAGE * (TROOP_FACTOR[Math.min(((BattleArmor)en).getShootingStrength(), 30)] + 0.5);
//            weaponLocations[0].addDamage(0, apDamage);
//            weaponLocations[0].addDamage(WeaponType.BFCLASS_STANDARD, 0, apDamage);
//        }
//    }
    
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

    /**
     * NEW version - Adds a Special Unit Ability associated with a whole ASArcSummary such as TUR. If
     * that SPA is already present, the new value replaces the former.
     */
    public void addSPA(BattleForceSPA spa, ASArcSummary value) {
        specialUnitAbilities.put(spa, value);
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

//    protected void initWeaponLocations(Entity en) {
//        weaponLocations = new WeaponLocation[en.getNumAlphaStrikeWeaponsLocations()];
//        locationNames = new String[weaponLocations.length];
//        for (int loc = 0; loc < locationNames.length; loc++) {
//            weaponLocations[loc] = new WeaponLocation();
//            locationNames[loc] = en.getAlphaStrikeLocationName(loc);
//            if (locationNames[loc].length() > 0) {
//                locationNames[loc] += ":";
//            }
//        }
//    }
    
    protected double locationMultiplier(Entity en, int loc, Mounted mount) {
    	return en.getAlphaStrikeLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
    }
    
//    protected void computeMovement(Entity en) {
//    	en.setAlphaStrikeMovement(movement);
//    }
    
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
    
    protected double getConvInfantryStandardDamage(int range, Infantry inf) {
        if (inf.getPrimaryWeapon() == null) {
            return inf.getDamagePerTrooper() * TROOP_FACTOR[Math.min(inf.getShootingStrength(), 30)]
                    / 10.0;
        } else {
            return 0;
        }
    }
    
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
        return specialUnitAbilities.keySet().stream()
                .filter(this::showSpecial)
                .map(spa -> formatSPAString(spa, getSPA(spa)))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(","));
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
        if (isAnyTypeOf(BM, PM) && (spa == SOA)) {
            return false;
        }
        if (isAnyTypeOf(CV, BM) && (spa == SRCH)) {
            return false;
        }
        if (!isAnyTypeOf(JS, WS, SC, DA, DS) && spa.isDoor()) {
            return false;
        }
        if (hasAutoSeal() && (spa == SEAL)) {
            return false;
        }
        return true;
    }

    public boolean hasAutoSeal() {
        return isSubmarine()
                || isAnyTypeOf(BM, AF, SC, DS, JS, WS, SS, DA);
//                || isType(BA) Exoskeleton??
    }

    public boolean isSubmarine() {
        return isType(CV) && getPrimaryMovementType().equals("s");
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
    
//    public void writeCsv(BufferedWriter w) throws IOException {
//        w.write(name);
//        w.write("\t");
//        w.write(asUnitType.toString());
//        w.write("\t");
//        w.write(Integer.toString(size));
//        w.write("\t");
//        w.write(getMovementAsString());
//        w.write("\t");
//        w.write(Integer.toString((int)Math.round(armor)));
//        if (threshold >= 0) {
//            w.write("-" + (int)Math.ceil(threshold));//TODO: threshold
//        }
//        w.write("\t");
//        w.write(Integer.toString(structure));
//        w.write("\t");
//        StringJoiner sj = new StringJoiner(", ");
//        for (int loc = 0; loc < weaponLocations.length; loc++) {
//            StringBuilder str = new StringBuilder();
//            String damStr = getASDamageString(loc, false);
//            if (damStr.length() > 0) {
//                str.append(damStr);
//                sj.add(str.toString());
//            }
//        }
//        if (sj.length() > 0) {
//            w.write(sj.toString());
//        } else {
//            w.write(rangeBands > 3? "0/0/0/0" : "0/0/0");
//        }
//        w.write("\t");
//        sj = new StringJoiner(", ");
//        for (int loc = 0; loc < weaponLocations.length; loc++) {
//            if (weaponLocations[loc].getOverheat() >= 1) {
//                sj.add(locationNames[loc] + Math.max(4, (int)Math.round(weaponLocations[loc].getOverheat())));
//            }
//        }
//        if (sj.length() > 0) {
//            w.write(sj.toString());
//        } else {
//            w.write("-");
//        }
//        w.write("\t");
//        w.write(Integer.toString(getFinalPoints()));
//        w.write("\t");
//        w.write(specialAbilities.keySet().stream()
//                .filter(spa -> spa.usedByAlphaStrike()
//                        && !spa.isDoor())
//                .map(spa -> formatSPAString(spa))
//                .collect(Collectors.joining(", ")));
//        w.newLine();
//    }

    public void writeCsv(BufferedWriter w) throws IOException {
        w.write(name);
        w.write("\t");
        w.write(asUnitType + "");
        w.write("\t");
        w.write(size + "");
        w.write("\t");
        w.write(getMovementAsString());
        w.write("\t");
        w.write(getTMM() + "");
        w.write("\t");
        w.write(getFinalArmor() + "");
        w.write("\t");
        w.write(getFinalThreshold() + "");
        w.write("\t");
        w.write(structure + "");
        w.write("\t");
        w.write(getStandardDamage() + "");
        w.write("\t");
        w.write(getOverheat() + "");
        w.write("\t");
        w.write(getFinalPoints() + "");
        w.write("\t");
        w.write(getSpecialsString());
        w.newLine();
    }

    public int getFinalPoints() {
        return Math.max(1, (int) Math.round(points));
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

    public static String formatSPAString(BattleForceSPA spa, @Nullable Object spaObject) {
        if (spa == TUR) {
            return "TUR(" + spaObject + ")";
        } else if (spa == BIM || spa == LAM) {
            return lamString(spa, spaObject);
        } else if ((spa == C3BSS) || (spa == C3M) || (spa == C3BSM) || (spa == C3EM)
                || (spa == INARC) || (spa == CNARC) || (spa == SNARC)) {
            return spa.toString() + ((int) spaObject == 1 ? "" : (int) spaObject);
        } else {
            return spa.toString() + (spaObject != null ? spaObject : "");
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
                    for (Entry<BattleForceSPA, Object> specialDmg : 
                        ((Map<BattleForceSPA, Object>) turDamage).entrySet()) {
                        result += "," + specialDmg.getKey() + specialDmg.getValue();
                    }
                }
            }
            result +=")";
        }
        return result;
    }
    
    /** 
     * Returns the formatted LAM/BIM Special Ability string such as LAM(36"g/4a). Requires LAM or BIM to
     * be present. 
     */ 
    private static String lamString(BattleForceSPA spa, Object spaObject) {
        String result = spa.toString() + "(";
        if (spa == LAM) {
            result += ((Map<String, Integer>)spaObject).get("g") + "\"" + "g/";
        }
        result += ((Map<String, Integer>)spaObject).get("a") + "a)";
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

    /** Returns the element's jump movement (in inches) or 0 if it has no jump movement. */
    public int getJumpMove() {
        return movement.getOrDefault("j", 0);
    }
    
    public ASUnitType getType() {
        return asUnitType;
    }

    /** Returns true if this AS Element is of the given type. */
    public boolean isType(ASUnitType type) {
        return asUnitType == type;
    }

    /** Returns true if this AS Element is any of the given types. */
    public boolean isAnyTypeOf(ASUnitType type, ASUnitType... furtherTypes) {
        return isType(type) || Arrays.stream(furtherTypes).anyMatch(this::isType);
    }
    
    public boolean usesThreshold() {
        return threshold != -1;
    }
    
    /** 
     * Returns true if this unit uses the 4 firing arcs of Warships, Dropships and other units. 
     * When this is the case, the standardDamage of this unit is not used and is zero. Instead,
     * arcDamage is used. 
     */
    public boolean usesArcs() {
        if (isAnyTypeOf(JS, WS, DA, DS, SS, SC) || (isType(SV) && hasAnySPAOf(LG, SLG, VLG))) {
            return true;
        } else {
            return false;
        }
    }

    /** Returns true if this AlphaStrike element is either BA or CI. */
    public boolean isInfantry() {
        return isAnyTypeOf(BA, CI);
    }

    /** Returns true if this AS Element Type represents a ground unit. */
    public boolean isGround() {
        return !isAerospace();
    }

    /** Returns true if this AS Element Type represents an aerospace unit (including some SV units). */
    public boolean isAerospace() {
        return isAnyTypeOf(AF, CF, SC, DS, DA, JS, WS, SS)
                || (isType(SV) && getMovementModes().contains("a") || getMovementModes().contains("k")
                || getMovementModes().contains("i") || getMovementModes().contains("p"));
    }


    public int getMulId() {
        return mulId;
    }

    public void setMulId(int mulId) {
        this.mulId = mulId;
    }

    public class WeaponLocation {
        List<Double> standardDamage = new ArrayList<>();
        Map<Integer,List<Double>> specialDamage = new HashMap<>();
        List<Integer> heatDamage = new ArrayList<>();
        double indirect;
        double overheat;

        WeaponLocation() {
            while (standardDamage.size() < 4) {
                standardDamage.add(0.0);
            }
            while (heatDamage.size() < 4) {
                heatDamage.add(0);
            }
        }

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
            if (damageClass == WeaponType.BFCLASS_FLAK) {
                return specialDamage.containsKey(damageClass)
                        && specialDamage.get(damageClass).stream().mapToDouble(Double::doubleValue).sum() > 0;
            } else {
                return specialDamage.containsKey(damageClass)
                        && specialDamage.get(damageClass).get(1) >= 1;
            }
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

        public List<Double> getDamageForClass(int damageClass) {
            if (specialDamage.containsKey(damageClass)) {
                return specialDamage.get(damageClass);
            }
            return new ArrayList<>();
        }

        public String formatDamageUp() {
            return formatDamageUp(standardDamage);
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

        public String getSpecialDamageString(int damageClass) {
            if (!specialDamage.containsKey(damageClass)) {
                return rangeBands > 3? "0/0/0/0" : "0/0/0";
            }
            List<Double> damageList = specialDamage.get(damageClass);
            if (damageClass == WeaponType.BFCLASS_SRM) {
                return WeaponType.BF_CLASS_NAMES[WeaponType.BFCLASS_SRM]
                        + specialDamageString(damageList.get(0)) + "/"
                        + specialDamageString(damageList.get(1));
            } else {
                return WeaponType.BF_CLASS_NAMES[damageClass]
                        + specialDamage.get(damageClass).stream().map(this::specialDamageString).collect(Collectors.joining("/"));
            }
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
//            while (damage.size() < rangeBands) {
//                damage.add(0.0);
//            }
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


    /** Returns the primary movement value, i.e. the type "" for ground units or "s" for submarines. */
    public int getPrimaryMovementValue() {
        return movement.values().iterator().next();
    }

    /** Returns the primary movement type String, such as "" for ground units or "s" for submarines. */
    public String getPrimaryMovementType() {
        return movement.keySet().iterator().next();
    }

    public Set<String> getMovementModes() {
        return movement.keySet();
    }

    public int getDmgS() {
        double dmgS = weaponLocations[0].standardDamage.get(0);
        return dmgS < 0.5 ? 0 : (int)Math.ceil(dmgS);
    }

    public boolean isMinimalDmgS() {
        double dmgS = weaponLocations[0].standardDamage.get(0);
        return (dmgS < 0.5) && (dmgS > 0);
    }

    public int getDmgM() {
        double dmgM = weaponLocations[0].standardDamage.get(1);
        return dmgM < 0.5 ? 0 : (int)Math.ceil(dmgM);
    }

    public boolean isMinimalDmgM() {
        double dmgM = weaponLocations[0].standardDamage.get(1);
        return (dmgM < 0.5) && (dmgM > 0);
    }

    public int getDmgL() {
        double dmgL = weaponLocations[0].standardDamage.get(2);
        return dmgL < 0.5 ? 0 : (int)Math.ceil(dmgL);
    }

    public boolean isMinimalDmgL() {
        double dmgL = weaponLocations[0].standardDamage.get(2);
        return (dmgL < 0.5) && (dmgL > 0);
    }

    public int getStructure() {
        return structure;
    }

    public int getMovement(String mode) {
        return movement.get(mode);
    }

    public String getChassis() {
        return chassis;
    }

    public String getModel() {
        return model;
    }

    public UnitRole getRole() {
        return role;
    }

    public String getName() {
        return name;
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
}
