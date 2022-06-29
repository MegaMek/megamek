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
 * This class represents an AlphaStrike Element which is a single unit such as a Mek with
 * AlphaStrike values such as S, M, L damage and a single Armor and Structure value.
 *
 * @author Neoancient
 * @author Simon (Juliez)
 */
public class AlphaStrikeElement {

    public static final String INCH = "\"";

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

    //TODO: When used for gameplay, original values are probably required for any values that can change
    //TODO: Maybe add a copy of the original AS element instead of duplicated fields?

    /** The unit's display name; may include a duplicate unit marker such as "#2". */
    private String name;

    private String chassis;
    private String model;
    private int mulId = -1;
    private int pointValue;

    private ASUnitType asUnitType;
    private int size;
    private int tmm;
    private Map<String,Integer> movement = new LinkedHashMap<>();
    private UnitRole role;
    private int skill = 4;

    /**
     * The normal damage values of a ground unit (S/M/L) or fighter (S/M/L/E).
     * Large Aerospace and large SV use the arcs field instead.
     */
    private ASDamageVector standardDamage;
    private int overheat;

    /**
     * The multiple arced damage value groups and specials of spaceships, MSs and large SVs.
     * Ground units and fighters use standardDamage instead.
     */
    private EnumMap<ASArcs, ASArcSummary> arcs = new EnumMap<>(ASArcs.class);

    private int armor;
    private int structure;
    private int threshold = -1;

    /** Battle Armor squad size. */
    private int squadSize;

    /**
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
    private EnumMap<BattleForceSPA, Object> specialUnitAbilities = new EnumMap<>(BattleForceSPA.class);

    protected int[] heat; //TODO: whats this?

    /**
     * AlphaStrike Quirks.
     * Ideally these would be converted/filtered according to AS Companion, p. 59, but
     * currently, the TW quirks are just reproduced here.
     */
    private Quirks quirks = new Quirks();

    // The following are used during conversion from TW to AS
    protected ASConverter.WeaponLocation[] weaponLocations;
    protected String[] locationNames;

    /** @return The AS element's chassis, such as "Atlas". */
    public String getChassis() {
        return chassis;
    }

    /** @return The AS element's model, such as "AS7-D". */
    public String getModel() {
        return model;
    }

    /** @return The AS element's battlefield role (ROLE). */
    public UnitRole getRole() {
        return role;
    }

    /** @return The AS element's display name, including duplicate markers such as "#2". */
    public String getName() {
        return name;
    }

    /** @return The AS element's size (SZ). */
    public int getSize() {
        return size;
    }

    /** @return The AS element's Target Movement Modifier (TMM). */
    public int getTMM() {
        return tmm;
    }

    /** @return The TW quirks of this AS element (currently not converted to AS quirks). */
    public Quirks getQuirks() {
        return quirks;
    }

    /** @return The AS element's Pilot Skill (SKILL). Unless another skill has been set, this is 4. */
    public int getSkill() {
        return skill;
    }

    /** @return The AS element's type (TP), e.g. BM or CV. */
    public ASUnitType getType() {
        return asUnitType;
    }

    /** @return The AS element's extra Overheat Damage capability (OV) (not the current heat buildup). */
    public int getOverheat() {
        return overheat;
    }

    /** @return The AS element's Point Value (PV). This is not adjusted for damage. */
    public int getPointValue() {
        return pointValue;
    }

    /** @return The AS element's current Armor (A).*/
    public int getArmor() {
        return armor;
    }

    /** @return The AS element's Threshold (TH). Returns -1 for elements that don't use Threshold. */
    public int getThreshold() {
        return threshold;
    }

    /** @return The AS element's current Structure (S).*/
    public int getStructure() {
        return structure;
    }

    /** @return The AS element's entire movement capability (MV). */
    public Map<String, Integer> getMovement() {
        return movement;
    }

    /**
     * @return The movement value (in inches where applicable) for the given movement mode key such as "" or "j".
     * Returns 0 when the unit does not have the given movement mode.
     */
    public int getMovement(String mode) {
        return movement.getOrDefault(mode, 0);
    }

    public int getRangeBands() {
        return usesSML() ? RANGEBANDS_SML : RANGEBANDS_SMLE;
    }

    public int getSquadSize() {
        return squadSize;
    }

    public void setQuirks(Quirks quirks) {
        this.quirks = quirks;
    }

    /** @return True if this element has the given quirk. */
    public boolean hasQuirk(String name) {
        return quirks.booleanOption(name);
    }

    /** Sets the AS element's chassis to the given value, such as "Atlas". */
    public void setChassis(String newChassis) {
        chassis = newChassis;
    }

    /** Sets the AS element's model to the given value, such as "AS7-D". */
    public void setModel(String newModel) {
        model = newModel;
    }

    /** Sets the AS element's battlefield role (ROLE). */
    public void setRole(UnitRole newRole) {
        role = newRole;
    }

    /** Sets the AS element's display name. */
    public void setName(String newName) {
        name = newName;
    }

    /** Sets the AS element's size (SZ). */
    public void setSize(int newSize) {
        size = newSize;
    }

    /** Sets the AS element's Target Movement Modifier (TMM). */
    public void setTMM(int newTMM) {
        tmm = newTMM;
    }

    /** Sets the AS element's Pilot Skill (SKILL). */
    public void setSkill(int newSkill) {
        skill = newSkill;
    }

    /** Sets the AS element's type (TP), e.g. BM or CV. */
    public void setType(ASUnitType newType) {
        asUnitType = newType;
    }

    /** Sets the AS element's extra Overheat Damage capability (OV) (not the current heat buildup). */
    public void setOverheat(int newOV) {
        overheat = newOV;
    }

    /** Sets the AS element's Point Value (PV). */
    public void setPointValue(int newPointValue) {
        pointValue = newPointValue;
    }

    /** Sets the AS element's current Armor (A).*/
    public void setArmor(int newArmor) {
        armor = newArmor;
    }

    /** Sets the AS element's Threshold (TH). */
    public void setThreshold(int newThreshold) {
        threshold = newThreshold;
    }

    /** Sets the AS element's current Structure (S). */
    public void setStructure(int newStructure) {
        structure = newStructure;
    }

    /** Sets the AS element's movement (MV). */
    public void setMovement(Map<String, Integer> newMovement) {
        movement = newMovement;
    }

    /** Sets the AS element's standard damage (SML or SMLE, but not arc damage). */
    public void setStandardDamage(ASDamageVector newDamage) {
        standardDamage = newDamage;
    }

    /** Sets the AS element's Battle Armor Squad Size. Does not check if this actually is a BA. */
    public void setSquadSize(int newSquadSize) {
        squadSize = newSquadSize;
    }

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

    /** @return True if this AS element uses three range bands S, M and L (equivalent to {@link #isGround()}). */
    public boolean usesSML() {
        return isGround();
    }

    /** @return True if this AS element uses four range bands S, M, L and E (equivalent to {@link #isAerospace()}). */
    public boolean usesSMLE() {
        return isAerospace();
    }

    /**
     * @return The standard damage (SML or SMLE depending on type). This will be empty for
     * elements that use arcs.
     */
    public ASDamageVector getStandardDamage() {
        return standardDamage;
    }

    /**
     * @return True when this AS element has the given Special Unit Ability. When the element has
     * the spa and the spa is associated with some value, this value can be assumed to be non-empty
     * or greater than zero. E.g., if an element has the MHQ spa, then MHQ >= 1. If it has IF, then
     * the IF value is at least 0*.
     */
    public boolean hasSPA(BattleForceSPA spa) {
        return specialUnitAbilities.containsKey(spa);
    }

    /**
     * Removes the given Special Unit Ability from the element.
     *
     * @param spa The SPA to remove
     */
    public void removeSPA(BattleForceSPA spa) {
        specialUnitAbilities.remove(spa);
    }

    /** @return True when this AS element has at least one of the given Special Unit Abilities. */
    public boolean hasAnySPAOf(BattleForceSPA spa, BattleForceSPA... furtherSPAs) {
        return hasSPA(spa) || Arrays.stream(furtherSPAs).anyMatch(this::hasSPA);
    }

    /**
     * @return The formatted String for the complete movement capability of this AS element, e.g. 4"/6"j. This
     * includes all movement modes of the element.
     */
    public String getMovementAsString() {
    	return movement.entrySet().stream().map(this::moveString).collect(joining("/"));    	
    }
    
    /** @return The formatted String for a single movement mode entry, e.g. 4a or 12"j. */
    private String moveString(Entry<String, Integer> moveMode) {
        if (moveMode.getKey().equals("k")) {
            return "0." + moveMode.getValue() + "k";
        } else if (moveMode.getKey().equals("a")) {
            return moveMode.getValue() + "a";
        } else if (moveMode.getKey().equals("p")) {
            return moveMode.getValue() + "p";
        } else if (isAnyTypeOf(DS, WS, DA, JS, SS) && moveMode.getKey().isBlank()) {
            return moveMode.getValue() + "";
        } else {
            return moveMode.getValue() + INCH + moveMode.getKey();
        }
    }

    /**
     * Returns a formatted SPA string for this AS element. The string is formatted in the way SPAs are
     * printed on an AS element's card or summary with a ', ' between SPAs.
     *
     * @return A formatted Special Unit Ability string for this AS element
     */
    public String getSpecialsString() {
        return getSpecialsString(", ");
    }

    /**
     * Returns a formatted SPA string for this AS element. The given delimiter is inserted between SPAs.
     *
     * @return A formatted Special Unit Ability string for this AS element
     */
    public String getSpecialsString(String delimiter) {
        return specialUnitAbilities.keySet().stream()
                .filter(this::showSpecial)
                .map(spa -> formatSPAString(spa, getSPA(spa)))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(delimiter));
    }

    /**
     * Convenience method to obtain the element's IF damage.
     *
     * @return The ASDamage that represents the element's IF value. If the element does not
     * have IF, this will return ASDamage.ZERO.
     */
    public ASDamage getIF() {
        return hasSPA(IF) ? (ASDamage) getSPA(IF) : ASDamage.ZERO;
    }

    /**
     * Convenience method to obtain the element's LRM ability.
     *
     * @return The ASDamageVector that represents the element's LRM ability. If the element does not
     * have LRM, this will return {@link ASDamageVector#ZERO}.
     */
    public ASDamageVector getLRM() {
        return hasSPA(LRM) ? (ASDamageVector) getSPA(LRM) : ASDamageVector.ZERO;
    }
    
    /**
     * Returns true if the given Special Unit Ability should be shown on this AS element's card or summary.
     * This is usually true but false for some, e.g. BM automatically have SOA and do not need to
     * show this on the unit card.
     *
     * @param spa The Special Unit Ability to check
     * @return True when the given Special Unit Ability should be listed on the element's card
     */
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

    /** @return True when this AS element has the Auto-Seal Special Unit Ability. */
    public boolean hasAutoSeal() {
        return isSubmarine()
                || isAnyTypeOf(BM, AF, SC, DS, JS, WS, SS, DA);
                // TODO               || isType(BA) Exoskeleton??
    }

    /**
     * Returns true when this AS element is a submarine. This checks if it is a combat vehicle
     * and has the "s" primary movement type.
     *
     * @return True when this AS element is as a submarine
     */
    public boolean isSubmarine() {
        return isType(CV) && getPrimaryMovementType().equals("s");
    }

    public void writeCsv(BufferedWriter w) throws IOException {
        w.write(getName());
        w.write("\t");
        w.write(getType() + "");
        w.write("\t");
        w.write(getSize() + "");
        w.write("\t");
        w.write(getMovementAsString());
        w.write("\t");
        w.write(getTMM() + "");
        w.write("\t");
        w.write(getArmor() + "");
        w.write("\t");
        w.write(getThreshold() + "");
        w.write("\t");
        w.write(getStructure() + "");
        w.write("\t");
        w.write(getStandardDamage() + "");
        w.write("\t");
        w.write(getOverheat() + "");
        w.write("\t");
        w.write(getPointValue() + "");
        w.write("\t");
        w.write(getSpecialsString());
        w.newLine();
    }

    /**
     * Creates the formatted SPA string for the given spa and SPA value (the object assigned
     * to the SPA such as an ASDamageVector). For turrets this includes everything in that
     * turret.
     *
     * @return The complete formatted Special Unit Ability string such as "LRM1/1/-".
     */
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
    
    /** @return The formatted LAM/BIM Special Ability string such as LAM(36"g/4a). */
    private static String lamString(BattleForceSPA spa, Object spaObject) {
        String result = spa.toString() + "(";
        if (spa == LAM) {
            result += ((Map<String, Integer>)spaObject).get("g") + INCH + "g/";
        }
        result += ((Map<String, Integer>)spaObject).get("a") + "a)";
        return result;
    }

    /**
     * @return The value associated with the given Special Unit Ability. Depending on the given spa, this
     * value can be null or of different types.
     */
    public Object getSPA(BattleForceSPA spa) {
        return specialUnitAbilities.get(spa);
    }

    /** @return True when this AS element is of a type that can have the OVL ability (BM and AF). */
    public boolean usesOVL() {
        return isAnyTypeOf(BM, AF);
    }

    /**
     * @return True when this AS element is jump-capable. This is the case when its movement modes
     * contains the "j" movement mode.
     */
    public boolean isJumpCapable() {
        return movement.containsKey("j");
    }

    /** @return This AS element's jump movement (in inches) or 0 if it has no jump movement. */
    public int getJumpMove() {
        return getMovement("j");
    }

    /** @return True if this AS element is of the given type. */
    public boolean isType(ASUnitType type) {
        return asUnitType == type;
    }

    /** @return True if this AS element is any of the given types. */
    public boolean isAnyTypeOf(ASUnitType type, ASUnitType... furtherTypes) {
        return isType(type) || Arrays.stream(furtherTypes).anyMatch(this::isType);
    }

    /** @return True if this AS element uses the Threshold value (equivalent to {@link #isAerospace()}). */
    public boolean usesThreshold() {
        return isAerospace();
    }
    
    /** 
     * Returns true if this unit uses the 4 firing arcs of Warships, Dropships and other units.
     * When this is the case, the standardDamage of this unit is not used and is empty. Instead,
     * the arcs field is used.
     *
     * @return True if this unit uses firing arcs
     */
    public boolean usesArcs() {
        return isAnyTypeOf(JS, WS, DA, DS, SS, SC) || (isType(SV) && hasAnySPAOf(LG, SLG, VLG));
    }

    /** @return True if this AS element is Infantry (BA or CI). */
    public boolean isInfantry() {
        return isAnyTypeOf(BA, CI);
    }

    /**
     * @return True if this AS element is a ground unit. An AS element is a ground unit when it is not
     * an aerospace unit. See {@link #isAerospace()}
     */
    public boolean isGround() {
        return !isAerospace();
    }

    /**
     * Returns true if this AS element is an aerospace unit, i.e. a fighter, a capital aerospace
     * element or an aerospace SV. See {@link #isAerospaceSV()}.
     *
     * @return True if this AS element is an aerospace unit (including aero SV units).
     */
    public boolean isAerospace() {
        return isAnyTypeOf(AF, CF, SC, DS, DA, JS, WS, SS) || isAerospaceSV();
    }

    /**
     * Returns true if this AS element is an aerospace SV, i.e. an SV with a movement mode of
     * "a", "k", "i", and "p".
     *
     * @return True if this AS element is an aerospace SV.
     */
    public boolean isAerospaceSV() {
        return isType(SV) && (hasMovementMode("a") || hasMovementMode("k")
                || hasMovementMode("i") || hasMovementMode("p"));
    }

    /** @return This AS element's MUL ID if it has one, -1 otherwise. */
    public int getMulId() {
        return mulId;
    }

    /** @return True if this AS element has a valid MUL ID. */
    public boolean hasMulId() {
        return mulId > 0;
    }

    /**
     * Sets this AS element's MUL ID to the given mulId. The MUL ID should be > 0 when this AS element
     * has a MUL entry and -1 otherwise.
     */
    public void setMulId(int mulId) {
        this.mulId = mulId;
    }

    /** @return The primary movement value in inches, i.e. the type "" for ground units or "s" for submarines. */
    public int getPrimaryMovementValue() {
        return movement.values().iterator().next();
    }

    /** @return The primary (= first) movement type String, such as "" for ground units or "s" for submarines. */
    public String getPrimaryMovementType() {
        return movement.keySet().iterator().next();
    }

    /** @return All movement mode Strings of this AS element, such as ["", "j"]. */
    public Set<String> getMovementModes() {
        return movement.keySet();
    }

    /** @return True if this AS element has the given movement mode. */
    public boolean hasMovementMode(String mode) {
        return movement.keySet().contains(mode);
    }

    /** @return True if this AS element tracks heat (BM, IM, CF, AF and Aerospace SV). */
    public boolean tracksHeat() {
        return isAnyTypeOf(BM, IM, AF, CF) || isAerospaceSV();
    }

    /** @return True if this AS element is a fighter (AF, CF and Aerospace SV). */
    public boolean isFighter() {
        return isAnyTypeOf(AF, CF) || isAerospaceSV();
    }

    /** @return True if this AS element is a BattleMek or Industrial Mek (BM, IM). */
    public boolean isMek() {
        return isAnyTypeOf(BM, IM);
    }

    /** @return True if this AS element is a BattleMek (BM). */
    public boolean isBattleMek() {
        return isType(BM);
    }

    /** @return True if this AS element is a ProtoMek (PM). */
    public boolean isProtoMek() {
        return isType(PM);
    }

    /** @return True if this AS element is a large Aerospace unit, i.e. SC, DS, DA, SC, SS, JS, WS. */
    public boolean isLargeAerospace() {
        return isAnyTypeOf(SC, DS, DA, SC, SS, JS, WS);
    }

    /** @return True if this AS element is a BattleArmor unit, i.e. BA. */
    public boolean isBattleArmor() {
        return isType(BA);
    }
}