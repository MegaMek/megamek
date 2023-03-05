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

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.ForceAssignable;
import megamek.common.UnitRole;
import megamek.common.force.Force;
import megamek.common.options.Quirks;
import megamek.common.strategicBattleSystems.BattleForceSUAFormatter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class represents an AlphaStrike Element which is a single unit such as a Mek with
 * AlphaStrike values such as S, M, L damage and a single Armor and Structure value.
 *
 * @author Neoancient
 * @author Simon (Juliez)
 */
public class AlphaStrikeElement implements Serializable, ASCardDisplayable, ASSpecialAbilityCollector,
        BattleForceSUAFormatter, ForceAssignable {

    static final int RANGEBANDS_SML = 3;
    static final int RANGEBANDS_SMLE = 4;
    public static final int RANGE_BAND_SHORT = 0;
    public static final int RANGE_BAND_MEDIUM = 1;
    public static final int RANGE_BAND_LONG = 2;
    public static final int RANGE_BAND_EXTREME = 3;

    public static final int[] STANDARD_RANGES = {0, 4, 16, 24};
    public static final int[] CAPITAL_RANGES = {0, 13, 25, 41};

    public static final int SHORT_RANGE = STANDARD_RANGES[RANGE_BAND_SHORT];
    public static final int MEDIUM_RANGE = STANDARD_RANGES[RANGE_BAND_MEDIUM];
    public static final int LONG_RANGE = STANDARD_RANGES[RANGE_BAND_LONG];
    public static final int EXTREME_RANGE = STANDARD_RANGES[RANGE_BAND_EXTREME];

    /** The unit's display name; may include a duplicate unit marker such as "#2". */
    private String name;

    private String chassis;
    private String model;
    private int mulId = -1;
    private int pointValue;
    private transient CalculationReport conversionReport = new DummyCalculationReport();

    private String forceString = "";
    private int forceId = Force.NO_FORCE;
    private int id;
    private int ownerId;

    private ASUnitType asUnitType;
    private int size;
    private int tmm;
    private Map<String,Integer> movement = new LinkedHashMap<>();
    private String primaryMovementMode = "";
    private UnitRole role;
    private int skill = 4;

    /**
     * The normal damage values of a ground unit (S/M/L) or fighter (S/M/L/E).
     * Large Aerospace and large SV use the arcs field instead.
     */
    private ASDamageVector standardDamage = ASDamageVector.ZERO;
    private int overheat;

    // The arcs of Large Aerospace units. Other units use standardDamage instead
    private ASArcSummary frontArc = new ASArcSummary();
    private ASArcSummary leftArc = new ASArcSummary();
    private ASArcSummary rightArc = new ASArcSummary();
    private ASArcSummary rearArc = new ASArcSummary();

    private int currentArmor;
    private int currentStructure;
    private int threshold = 0;

    private int fullArmor;
    private int fullStructure;

    /** Battle Armor squad size. */
    private int squadSize = 0;

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
    private ASSpecialAbilityCollection specialAbilities = new ASSpecialAbilityCollection();

    /**
     * AlphaStrike Quirks.
     * Ideally these would be converted/filtered according to AS Companion, p. 59, but
     * currently, the TW quirks are just reproduced here.
     */
    private Quirks quirks = new Quirks();

    @Override
    public String getChassis() {
        return chassis;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public UnitRole getRole() {
        return role;
    }

    /** @return The AS element's display name, including duplicate markers such as "#2". */
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getTMM() {
        return tmm;
    }

    /** @return The TW quirks of this AS element (currently not converted to AS quirks). */
    public Quirks getQuirks() {
        return quirks;
    }

    @Override
    public int getSkill() {
        return skill;
    }

    @Override
    public ASUnitType getASUnitType() {
        return asUnitType;
    }

    @Override
    public int getOV() {
        return overheat;
    }

    @Override
    public int getPointValue() {
        return pointValue;
    }

    @Override
    public int getFullArmor() {
        return fullArmor;
    }

    @Override
    public int getCurrentArmor() {
        return currentArmor;
    }

    @Override
    public int getThreshold() {
        return threshold;
    }

    @Override
    public int getFullStructure() {
        return fullStructure;
    }

    @Override
    public int getCurrentStructure() {
        return currentStructure;
    }

    @Override
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

    /** @return The number of damage range bands this element uses, 3 (SML) for ground units, 4 (SMLE) for aero. */
    public int getRangeBands() {
        return usesSML() ? RANGEBANDS_SML : RANGEBANDS_SMLE;
    }

    @Override
    public int getSquadSize() {
        return isBattleArmor() ? squadSize : 0;
    }

    @Override
    public ASArcSummary getFrontArc() {
        return frontArc;
    }

    @Override
    public ASArcSummary getLeftArc() {
        return leftArc;
    }

    @Override
    public ASArcSummary getRightArc() {
        return rightArc;
    }

    @Override
    public ASArcSummary getRearArc() {
        return rearArc;
    }

    @Override
    public ASDamageVector getStandardDamage() {
        return standardDamage;
    }

    @Override
    public int getMulId() {
        return mulId;
    }

    /** @return The conversion report for this unit. May be a DummyCalculationReport without information. */
    public CalculationReport getConversionReport() {
        return conversionReport;
    }

    /** @return False when this element has no meaningful conversion report, true when it has. */
    public boolean hasConversionReport() {
        return !(conversionReport == null) && !(conversionReport instanceof DummyCalculationReport);
    }

    @Override
    public ASSpecialAbilityCollection getSpecialAbilities() {
        return specialAbilities;
    }

    /** @return The ASSpecialAbilityCollection object holding the damage and specials info for the given arc. */
    public ASSpecialAbilityCollection getArc(ASArcs arc) {
        switch (arc) {
            case FRONT:
                return frontArc;
            case LEFT:
                return leftArc;
            case REAR:
                return rearArc;
            default:
                return rightArc;
        }
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
    public void setCurrentArmor(int newArmor) {
        currentArmor = newArmor;
    }

    /** Sets the AS element's full (=undamaged) Armor (A). Also sets the current armor to the same value. */
    public void setFullArmor(int newArmor) {
        currentArmor = newArmor;
        fullArmor = newArmor;
    }

    /** Sets the AS element's Threshold (TH). */
    public void setThreshold(int newThreshold) {
        threshold = newThreshold;
    }

    /** Sets the AS element's current Structure (S). */
    public void setCurrentStructure(int newStructure) {
        currentStructure = newStructure;
    }

    /** Sets the AS element's full (=undamaged) Structure (S). Also sets the current structure to the same value. */
    public void setFullStructure(int newStructure) {
        currentStructure = newStructure;
        fullStructure = newStructure;
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

    /** Sets the AS element's Battle Armor Squad Size. Does not check if this actually is a BA. */
    public void setPrimaryMovementMode(String movementMode) {
        primaryMovementMode = movementMode;
    }

    /** Sets the AS element's conversion report to the given report, if it is not null. */
    public void setConversionReport(CalculationReport newReport) {
        if (newReport != null) {
            conversionReport = newReport;
        }
    }

    /** Sets the AS element's special ability collection to the given one, if it is not null. */
    public void setSpecialAbilities(ASSpecialAbilityCollection newAbilities) {
        specialAbilities = Objects.requireNonNull(newAbilities);
    }

    /** Sets the AS element's special ability collection to the given one, if it is not null. */
    public void setArc(ASArcs arc, ASArcSummary arcSummary) {
        Objects.requireNonNull(arc);
        Objects.requireNonNull(arcSummary);
        if (arc == ASArcs.FRONT) {
            frontArc = arcSummary;
        } else if (arc == ASArcs.LEFT) {
            leftArc = arcSummary;
        } else if (arc == ASArcs.RIGHT) {
            rightArc = arcSummary;
        } else {
            rearArc = arcSummary;
        }
    }

    /** Resets the AS element's conversion report to an empty DummyCalculationReport. */
    public void clearConversionReport() {
        conversionReport = new DummyCalculationReport();
    }

    public AlphaStrikeElement() {
        // currently unused
    }

    @Override
    public boolean hasSUA(BattleForceSUA sua) {
        return specialAbilities.hasSUA(sua);
    }

    @Override
    public Object getSUA(BattleForceSUA sua) {
        return specialAbilities.getSUA(sua);
    }

    /**
     * Returns a formatted String for the standard movement capability of this AS element, e.g. 4"/6"j. This
     * includes all movement modes of the element that are typically printed as MV on an AS card.
     * As the only exception, this does not include the a and g movement modes of LandAirMeks, which are
     * printed as special unit abilities.
     *
     * @return A formatted standard movement string, e.g. 4"/6"j.
     */
    public String getMovementAsString() {
        return AlphaStrikeHelper.getMovementAsString(this);
    }

    @Override
    public String getSpecialsDisplayString(String delimiter, BattleForceSUAFormatter element) {
        return specialAbilities.getSpecialsDisplayString(delimiter, this);
    }

    /** @return This AS element's jump movement (in inches) or 0 if it has no jump movement. */
    public int getJumpMove() {
        return getMovement("j");
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
        return getMovement(primaryMovementMode);
    }

    @Override
    public String getPrimaryMovementMode() {
        return primaryMovementMode;
    }

    /**
     * Returns all movement modes available to this unit. For LandAirMeks, this includes one or both of aero
     * and Wige movement modes a and g!
     *
     * @return All movement mode Strings of this AS element, such as ["", "j"].
     */
    public Set<String> getMovementModes() {
        return movement.keySet();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getForceString() {
        return forceString;
    }

    @Override
    public void setForceString(String newForceString) {
        forceString = newForceString;
    }

    @Override
    public int getForceId() {
        return forceId;
    }

    @Override
    public void setForceId(int newId) {
        forceId = newId;
    }

    @Override
    public int getOwnerId() {
        return ownerId;
    }

    @Override
    public boolean showSUA(BattleForceSUA sua) {
        return !AlphaStrikeHelper.hideSpecial(sua, this);
    }

    @Override
    public String formatSUA(BattleForceSUA sua, String delimiter, ASSpecialAbilityCollector collection) {
        return AlphaStrikeHelper.formatAbility(sua, collection, this, delimiter);
    }
}