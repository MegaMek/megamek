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
package megamek.common.alphaStrike.conversion;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.alphaStrike.*;
import megamek.common.weapons.missiles.MissileWeapon;
import megamek.common.weapons.other.CLFussilade;

import java.util.*;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.ITechnology.TECH_BASE_CLAN;
import static megamek.common.alphaStrike.AlphaStrikeElement.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASDamageConverter2 {

    // Used for CI and BA, ASC p.102
    protected static final int[] TROOP_FACTOR = {
            0, 0, 1, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12,
            13, 14, 15, 16, 16, 17, 17, 17, 18, 18
    };
    protected static final String rdUp = "rt, ru";
    protected static final String rdNm = "rt, rn";

    protected Entity entity;
    protected AlphaStrikeElement element;
    protected CalculationReport report;

    // The locations where damage and special abilities are going to end up in. [0] contains the unit's standard
    // damage and central abilities, the others can be turrets, arcs or rear.
    protected ASArcSummary[] locations;
    protected String[] locationNames;
    protected int rearLocation;
    protected int turretLocation;

    private final Map<WeaponType, Boolean> ammoForWeapon = new HashMap<>();
    protected final boolean hasTargetingComputer;
    protected List<Mounted> weaponsList;
    protected boolean needsHeatAdjustment = false;
    protected double heatAdjustFactorSM = 1;
    protected double heatAdjustFactorLE = 1;

    protected ASDamage finalSDamage;
    protected ASDamage finalMDamage;
    protected ASDamage finalLDamage;
    protected ASDamage finalEDamage;

    static ASDamageConverter2 getASDamageConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        if (element.isBattleArmor()) {
            return new ASBattleArmorDamageConverter(entity, element, report);
        } else if (element.isConventionalInfantry()) {
            return new ASConvInfantryDamageConverter(entity, element, report);
        } else if (element.usesArcs()) {
            return new ASArcedDamageConverter(entity, element, report);
        } else if (element.isAerospace()) {
            return new ASAeroDamageConverter(entity, element, report);
        } else {
            return new ASDamageConverter2(entity, element, report);
        }
    }

    /**
     * Do not call this directly. Use the static getASDamageConverter instead.
     * Constructs a damage converter for ground units.
     *
     * @param entity The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report The calculation report to write to
     */
    protected ASDamageConverter2(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        this.element = element;
        this.entity = entity;
        this.report = report;
        locations = new ASArcSummary[ASLocationMapper.damageLocationsCount(entity)];
        locationNames = new String[locations.length];
        for (int index = 0; index < locationNames.length; index++) {
            locationNames[index] = ASLocationMapper.locationName(entity, index);
            locations[index] = ASArcSummary.createTurretSummary(element);
        }
        rearLocation = getRearLocation();
        turretLocation = getTurretLocation();
        hasTargetingComputer = entity.hasTargComp();
        weaponsList = new ArrayList<>(entity.getWeaponList());
        weaponsList.removeIf(Objects::isNull);
    }

    void convert() {
        report.addEmptyLine();
        report.addSubHeader("Damage Conversion:");
        assembleAmmoCounts();
        processDamage();
        processSpecialAbilities();
        writeLocationsToElement();
    }

    protected void processSpecialAbilities() {
        report.addEmptyLine();
        report.addSubHeader("Weapon-based Special Abilities:");
        for (Mounted weapon : weaponsList) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            assignSpecialAbilities(weapon, weaponType);
        }
        if (foundNoSpecialAbility()) {
            report.addLine("None", "", "");
        }
    }

    private boolean foundNoSpecialAbility() {
        return Arrays.stream(locations).map(ASArcSummary::getSpecials).allMatch(ASSpecialAbilityCollection::isEmpty);
    }

    protected void processDamage() {
        calculateHeatAdjustment();
        processSDamage();
        processMDamage();
        processLDamage();
        processHT();
        if (rearLocation != -1) {
            processSpecialDamage(REAR, rearLocation);
        }
        processFrontSpecialDamage(IF);
        processFrontSpecialDamage(LRM);
        processFrontSpecialDamage(SRM);
        processFrontSpecialDamage(AC);
        processFrontSpecialDamage(FLK);
        processFrontSpecialDamage(IATM);
        processFrontSpecialDamage(TOR);
        processFrontSpecialDamage(REL);
        processTurrets();
    }

    protected void processTurrets() {
        if (getTurretLocation() != -1) {
            processSpecialDamage(STD, turretLocation);
            processSpecialDamage(IF, turretLocation);
            processSpecialDamage(LRM, turretLocation);
            processSpecialDamage(SRM, turretLocation);
            processSpecialDamage(AC, turretLocation);
            processSpecialDamage(FLK, turretLocation);
            processSpecialDamage(IATM, turretLocation);
            processSpecialDamage(TOR, turretLocation);
            processSpecialDamage(REL, turretLocation);
        }
    }

    protected void calculateHeatAdjustment() {
        if (element.usesOV()) {
            int heatCapacity = getHeatCapacity();
            int mediumRangeFrontHeat = getHeatGeneration(false, false);
            if (mediumRangeFrontHeat - 4 > heatCapacity) {
                needsHeatAdjustment = true;
                heatAdjustFactorSM = (double) heatCapacity / (mediumRangeFrontHeat - 4);
            }
        }
    }

    /**
     * Sums up the front-facing damage values for the given range and reports the counted weapons.
     * Includes modifiers for low ammo, AES etc. and the location multiplier (0.5 for some Aero arcs)
     *
     * @param range The range, e.g. AlphaStrikeElement.MEDIUM_RANGE
     * @return The raw damage sum
     */
    protected double assembleFrontDamage(int range) {
        double rawDamage = 0;
        for (Mounted weapon : weaponsList) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            //TODO: replace the range with the range bracket; ranges in hexes have no place in AS conversion:
            double baseDamage = determineDamage(weapon, range);
            double locationMultiplier = ASLocationMapper.damageLocationMultiplier(entity, 0, weapon);
            if ((locationMultiplier > 0) && (baseDamage > 0)) {
                double damageMultiplier = getDamageMultiplier(weapon, weaponType);
                double modifiedDamage = baseDamage * damageMultiplier * locationMultiplier;
                String calculation = "+ " + formatForReport(modifiedDamage);
                calculation += (damageMultiplier != 1) ? " (" + formatForReport(baseDamage) + " x " +
                        formatForReport(damageMultiplier) + ")" : "";
                rawDamage += modifiedDamage;
                report.addLine(getWeaponDesc(weapon), calculation, "= " + formatForReport(rawDamage));
            }
        }
        return rawDamage;
    }

    protected double determineDamage(Mounted weapon, int range) {
        if (((WeaponType) weapon.getType()).getDamage() == WeaponType.DAMAGE_ARTILLERY) {
            return 0;
        }
        return ((WeaponType) weapon.getType()).getBattleForceDamage(range, weapon.getLinkedBy());
    }

    protected void processSDamage() {
        report.addLine("--- Short Range Damage:", "");
        double sDamage = assembleFrontDamage(SHORT_RANGE);

        if (needsHeatAdjustment) {
            report.addLine("Adjusted Damage: ",
                    formatForReport(sDamage) + " x (see M)",
                    "= " + formatForReport(sDamage * heatAdjustFactorSM));
            sDamage = sDamage * heatAdjustFactorSM;
        }

        finalSDamage = ASDamage.createDualRoundedUp(sDamage);
        report.addLine("Final S damage:",
                formatForReport(sDamage) + ", " + rdUp, "= " + finalSDamage.toStringWithZero());
    }

    protected void processMDamage() {
        report.addEmptyLine();
        report.addLine("--- Medium Range Damage:", "");
        double rawMDamage = assembleFrontDamage(MEDIUM_RANGE);
        double mDamage = rawMDamage;
        int roundedUpRaw = ASConverter.roundUp(roundUpToTenth(rawMDamage));
        int roundedUpAdjusted = 0;

        if (element.usesOV()) {
            int heatCapacity = getHeatCapacity();
            int mediumRangeFrontHeat = getHeatGeneration(false, false);
            report.addLine("Heat Generation M: ", formatForReport(mediumRangeFrontHeat), "");
            if (!needsHeatAdjustment) {
                report.addLine("Heat Capacity: ", heatCapacity + ", no heat adjustment", "");
            } else {
                report.addLine("Heat Capacity: ", heatCapacity + "", "");
                report.addLine("Raw M damage:",
                        formatForReport(rawMDamage) + ", " + rdUp, "= " + roundedUpRaw);
                double mDamageAdjusted = rawMDamage * heatAdjustFactorSM;
                roundedUpAdjusted = ASConverter.roundUp(roundUpToTenth(mDamageAdjusted));
                element.setOverheat(Math.min(roundedUpRaw - roundedUpAdjusted, 4));
                report.addLine("Adjusted Damage: ",
                        formatForReport(rawMDamage) + " x " + heatCapacity + " / (" + mediumRangeFrontHeat + " - 4)",
                        "= " + formatForReport(mDamageAdjusted));
                mDamage = mDamageAdjusted;
            }
        }

        finalMDamage = ASDamage.createDualRoundedUp(mDamage);
        report.addLine("Final M damage:",
                formatForReport(mDamage) + ", " + rdUp, "= " + finalMDamage.toStringWithZero());

        if (element.hasOV()) {
            report.addLine("Damage difference",
                    roundedUpRaw + " - " + roundedUpAdjusted,
                    "OV " + element.getOverheat());
        }
    }

    protected void processLDamage() {
        report.addEmptyLine();
        report.addLine("--- Long Range Damage:", "");
        double rawLDamage = assembleFrontDamage(LONG_RANGE);
        double lDamage = rawLDamage;
        int roundedUpRaw = ASConverter.roundUp(roundUpToTenth(rawLDamage));
        int roundedUpAdjusted = 0;

        // Determine OVL from long range damage and heat
        if (element.getOverheat() > 0) {
            int heatCapacity = getHeatCapacity();
            double longRangeFrontHeat = getHeatGeneration(false, true);
            report.addLine("Heat Generation L: ", formatForReport(longRangeFrontHeat), "");
            if (longRangeFrontHeat - 4 <= heatCapacity) {
                report.addLine("Heat Capacity: ", heatCapacity + ", no heat adjustment", "");
            } else {
                report.addLine("Heat Capacity: ", heatCapacity + "", "");
                heatAdjustFactorLE = (double) heatCapacity / (longRangeFrontHeat - 4);
                double lDamageAdjusted = rawLDamage * heatAdjustFactorLE;
                roundedUpAdjusted = ASConverter.roundUp(roundUpToTenth(lDamageAdjusted));
                report.addLine("Raw L damage:",
                        formatForReport(rawLDamage) + ", dual rounded", "= " + roundedUpRaw);
                report.addLine("Adjusted Damage: ",
                        formatForReport(rawLDamage) + " x " + heatCapacity + " / ("
                                + formatForReport(longRangeFrontHeat) + " - 4)",
                        "= " + formatForReport(lDamageAdjusted));
                if (roundedUpAdjusted < roundedUpRaw) {
                    element.addSPA(OVL);
                    lDamage = lDamageAdjusted;
                } else {
                    report.addLine("Damage difference",
                            roundedUpRaw + " = " + roundedUpAdjusted + ", no OVL, no adjustment", "");
                }
            }
        }

        finalLDamage = ASDamage.createDualRoundedUp(lDamage);
        report.addLine("Final L damage:",
                formatForReport(lDamage) + ", " + rdUp, "= " + finalLDamage.toStringWithZero());

        if (element.hasSUA(OVL)) {
            report.addLine("Damage difference",
                    roundedUpRaw + " > " + roundedUpAdjusted,
                    "OVL");
        }
    }

    protected void processEDamage() { }

    protected double getDamageMultiplier(Mounted weapon, WeaponType weaponType) {
        // Low ammo count
        double damageModifier = ammoForWeapon.getOrDefault(weaponType, true) ? 1 : 0.75;

        // Oneshot or Fusillade
        if (weaponType.hasFlag(WeaponType.F_ONESHOT) && !(weaponType instanceof CLFussilade)) {
            damageModifier *= .1;
        }

        // Targetting Computer
        if (hasTargetingComputer && weaponType.hasFlag(WeaponType.F_DIRECT_FIRE)
                && (weaponType.getAmmoType() != AmmoType.T_AC_LBX)
                && (weaponType.getAmmoType() != AmmoType.T_AC_LBX_THB)) {
            damageModifier *= 1.10;
        }

        // Actuator Enhancement System
        if (entity.hasWorkingMisc(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM, -1, weapon.getLocation())
                && ((weapon.getLocation() == Mech.LOC_LARM) || (weapon.getLocation() == Mech.LOC_RARM))) {
            damageModifier *= 1.05;
        }

        return damageModifier;
    }

    protected void assembleAmmoCounts() {
        ArrayList<Mounted> weaponsList = new ArrayList<>(entity.getWeaponList());
        weaponsList.removeIf(Objects::isNull);
        Map<WeaponType, Integer> weaponCount = new HashMap<>();
        // Get weapon counts
        for (Mounted weapon : weaponsList) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            if ((weaponType.getAmmoType() != AmmoType.T_NA)
                    && !weaponType.hasFlag(WeaponType.F_ONESHOT)
                    && (!(entity instanceof BattleArmor) || weaponType instanceof MissileWeapon)) {
                weaponCount.merge(weaponType, 1, Integer::sum);
            }
        }
        // Get ammo counts per weapon type
        for (WeaponType weaponType : weaponCount.keySet()) {
            int ammoCount = 0;
            for (Mounted ammo : entity.getAmmo()) {
                AmmoType ammoType = (AmmoType) ammo.getType();
                if ((ammoType.getAmmoType() == weaponType.getAmmoType())
                        && (ammoType.getRackSize() == weaponType.getRackSize())) {
                    ammoCount += ammo.getUsableShotsLeft();
                }
            }
            int divisor = 1;
            if (weaponType.getAmmoType() == AmmoType.T_AC_ROTARY) {
                divisor = 6;
            } else if (weaponType.getAmmoType() == AmmoType.T_AC_ULTRA
                    || weaponType.getAmmoType() == AmmoType.T_AC_ULTRA_THB) {
                divisor = 2;
            }
            ammoForWeapon.put(weaponType, ammoCount / weaponCount.get(weaponType) >= 10 * divisor);
        }
    }

    protected void assignSpecialAbilities(Mounted weapon, WeaponType weaponType) {
        if (weaponType.hasFlag(WeaponType.F_TAG)) {
            if (weaponType.hasFlag(WeaponType.F_C3MBS)) {
                assignToLocations(weapon, C3BSM, 1);
                assignToLocations(weapon, MHQ, 6);
            } else if (weaponType.hasFlag(WeaponType.F_C3M)) {
                assignToLocations(weapon, C3M, 1);
                assignToLocations(weapon, MHQ, 5);
            }
            assignToLocations(weapon, (weaponType.getShortRange() < 5) ? LTAG : TAG);
        }

        if (weaponType.hasFlag(WeaponType.F_TSEMP)) {
            assignToLocations(weapon, weaponType.hasFlag(WeaponType.F_ONESHOT) ? TSEMPO : TSEMP, 1);
        }

        if (weaponType.getAtClass() == WeaponType.CLASS_TELE_MISSILE) {
            assignToLocations(weapon, TELE);
        }

        processNarc(weapon, weaponType);
        processTaser(weapon, weaponType);
        processAMS(weapon, weaponType);
        processArtillery(weapon, weaponType);
    }

    protected void processNarc(Mounted weapon, WeaponType weaponType) {
        if (weaponType.getAmmoType() == AmmoType.T_INARC) {
            assignToLocations(weapon, INARC, 1);
        } else if (weaponType.getAmmoType() == AmmoType.T_NARC) {
            assignToLocations(weapon, SNARC, 1);
        }
    }

    protected void processTaser(Mounted weapon, WeaponType weaponType) {
        if (weaponType.getAmmoType() == AmmoType.T_TASER) {
            assignToLocations(weapon, MTAS, 1);
        }
    }

    protected void processAMS(Mounted weapon, WeaponType weaponType) {
        if (weaponType.getInternalName().equals("ISAPDS")
                || weaponType.getInternalName().equals("ISBAAPDS")) {
            assignToLocations(weapon, RAMS);
        } else if (weaponType.hasFlag(WeaponType.F_AMS)) {
            assignToLocations(weapon, AMS);
        }
    }

    protected void processArtillery(Mounted weapon, WeaponType weaponType) {
        if (weaponType.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
            assignToLocations(weapon, getArtilleryType(weaponType), 1);
        }
    }

    /**
     * Checks the location multiplier for all locations and assigns the given SUA to any location where
     * the multiplier is not zero, i.e. to any location that the weapon is counted towards.
     *
     * @param weapon The weapon to check
     * @param sua The special unit ability to add
     */
    protected void assignToLocations(Mounted weapon, BattleForceSUA sua) {
        for (int loc = 0; loc < locations.length; loc++) {
            if ((ASLocationMapper.damageLocationMultiplier(entity, loc, weapon) != 0)
                    && !locations[loc].hasSUA(sua)) {
                locations[loc].getSpecials().addSPA(sua);
                reportAssignToLocations(weapon, sua, "", loc);
            }
        }
    }

    /**
     * Checks the location multiplier for all locations and assigns the given SUA with the given ability
     * value to any location where the multiplier is not zero, i.e. to any location that the weapon is
     * counted towards. If the SUA is already present, the ability value is added.
     *
     * @param weapon The weapon to check
     * @param sua The special unit ability to add
     * @param abilityValue The ability value to add
     */
    protected void assignToLocations(Mounted weapon, BattleForceSUA sua, int abilityValue) {
        for (int loc = 0; loc < locations.length; loc++) {
            if (ASLocationMapper.damageLocationMultiplier(entity, loc, weapon) != 0) {
                locations[loc].getSpecials().addSPA(sua, abilityValue);
                reportAssignToLocations(weapon, sua, abilityValue + "", loc);
            }
        }
    }

    /**
     * Checks the location multiplier for all locations and assigns the given SUA with the given ability
     * value to any location where the multiplier is not zero, i.e. to any location that the weapon is
     * counted towards. If the SUA is already present, the ability value is added.
     *
     * @param weapon The weapon to check
     * @param sua The special unit ability to add
     * @param abilityValue The ability value to add
     */
    protected void assignToLocations(Mounted weapon, BattleForceSUA sua, double abilityValue) {
        for (int loc = 0; loc < locations.length; loc++) {
            if (ASLocationMapper.damageLocationMultiplier(entity, loc, weapon) != 0) {
                // The location multiplier is always 1 except for some of the large aerospace units, where it affects only PNT
                abilityValue *= ASLocationMapper.damageLocationMultiplier(entity, loc, weapon);
                locations[loc].getSpecials().addSPA(sua, abilityValue);
                reportAssignToLocations(weapon, sua, formatForReport(abilityValue), loc);
            }
        }
    }

    protected void reportAssignToLocations(Mounted weapon, BattleForceSUA sua, String abilityValue, int loc) {
        String locationText = (loc > 0) ? " (" + ASLocationMapper.locationName(entity, loc) + ")" : "";
        report.addLine(getWeaponDesc(weapon), sua.toString() + abilityValue + locationText);
    }

    /** Determines if the element has the HT ability and what the value is. Overridden for CI. */
    protected void processHT() {
        report.startTentativeSection();
        report.addEmptyLine();
        report.addLine("--- Heat Damage (HT):", "");
        int totalHeatS = 0;
        int totalHeatM = 0;
        int totalHeatL = 0;
        for (Mounted weapon : weaponsList) {
            double locationMultiplier = ASLocationMapper.damageLocationMultiplier(entity, 0, weapon);
            WeaponType weaponType = (WeaponType) weapon.getType();
            int heatS = weaponType.getAlphaStrikeHeatDamage(RANGE_BAND_SHORT);
            int heatM = weaponType.getAlphaStrikeHeatDamage(RANGE_BAND_MEDIUM);
            int heatL = weaponType.getAlphaStrikeHeatDamage(RANGE_BAND_LONG);
            boolean hasHeatDamage = heatS + heatM + heatL > 0;
            if ((locationMultiplier > 0) && hasHeatDamage) {
                totalHeatS += heatS;
                totalHeatM += heatM;
                totalHeatL += heatL;
                String calculation = "+ " + heatS + "/" + heatM + "/" + heatL;
                String currentTotal = "+ " + totalHeatS + "/" + totalHeatM + "/" + totalHeatL;
                report.addLine(getWeaponDesc(weapon), calculation, currentTotal);
            }
        }
        if (totalHeatS + totalHeatM + totalHeatL > 0) {
            int htS = resultingHTValue(totalHeatS);
            int htM = resultingHTValue(totalHeatM);
            int htL = resultingHTValue(totalHeatL);
            if (htS + htM + htL > 0) {
                ASDamageVector finalHtValue = ASDamageVector.createNormRndDmg(htS, htM, htL);
                locations[0].getSpecials().addSPA(HT, finalHtValue);
                report.addLine("Final Ability", "", "HT" + finalHtValue);
            } else {
                report.addLine("Final Ability", "No HT", "");
            }
            report.endTentativeSection();
        } else {
            report.discardTentativeSection();
        }
    }

    /**
     * Processes damage values for special abilities such as LRM, but not REAR and not TUR.
     */
    protected void processFrontSpecialDamage(BattleForceSUA dmgType) {
        processSpecialDamage(dmgType, 0);
    }

    /**
     * Processes damage values. The dmgType indicates the special ability for which this is (LRM, REAR, etc.).
     * location indicates the target of this damage, i.e. if it is the unit's standard damage and
     * standard ability block, the REAR ability or the TUR block, see ASLocationMapper.
     * When the location is rearLocation, dmgType must be REAR (as REAR has no LRM damage or the like)
     * When the location is turretLocation, TUR indicates it's the TUR's standard damage, LRM
     * indicates it's the LRM damage within the TUR block.
     */
    protected void processSpecialDamage(BattleForceSUA dmgType, int location) {
        report.startTentativeSection();
        report.addEmptyLine();
        String turMarker = (location == turretLocation) ? "(TUR) " : "";
        report.addLine("--- " + turMarker + dmgType + " Damage:", "");
        double[] damage = assembleSpecialDamage(dmgType, location);

        String finalText = "Final value:";
        if (needsHeatAdjustment) {
            damage[0] *= heatAdjustFactorSM;
            damage[1] *= heatAdjustFactorSM;
            if (dmgType != IF) {
                finalText = "Adjusted final value:";
            }
            if (element.hasSUA(OVL)) {
                damage[2] *= heatAdjustFactorLE;
                damage[3] *= heatAdjustFactorLE;
                finalText = "Adjusted final value:";
            }
        }

        if (qualifiesForSpecial(damage, dmgType)) {
            if (dmgType == IF) {
                ASDamage finalIFDamage = ASDamage.createDualRoundedNormal(damage[2]);
                report.addLine(finalText, formatForReport(damage[2]) + ", " + rdNm, "IF" + finalIFDamage);
                locations[location].getSpecials().addSPA(IF, finalIFDamage);
            } else if (dmgType == REL) {
                report.addLine(finalText, formatForReport(damage[1]), "REL");
                locations[location].getSpecials().addSPA(REL);
            } else if (dmgType == PNT) {
                int finalPNTDamage = ASConverter.roundUp(roundUpToTenth(damage[0]));
                report.addLine(finalText, formatForReport(damage[0]) + ", " + rdUp, "PNT" + finalPNTDamage);
                locations[location].getSpecials().addSPA(PNT, finalPNTDamage);
            } else {
                List<Double> damageList = Arrays.asList(damage[0], damage[1], damage[2], damage[3]);
                ASDamageVector finalDamage;
                if ((location == turretLocation) && (dmgType == STD)) {
                    // The turret's standard damage
                    finalDamage = ASDamageVector.createUpRndDmgMinus(damageList, rangesForSpecial(dmgType));
                } else if (dmgType.isAnyOf(FLK, REAR, TOR)) {
                    finalDamage = ASDamageVector.createNormRndDmg(damageList, rangesForSpecial(dmgType));
                } else {
                    finalDamage = ASDamageVector.createNormRndDmgNoMin(damageList, rangesForSpecial(dmgType));
                }
                report.addLine(finalText,
                        formatAsVector(damage[0], damage[1], damage[2], damage[3], dmgType) + ", " + rdNm,
                        "" + dmgType + finalDamage);
                locations[location].getSpecials().addSPA(dmgType, finalDamage);
            }
            report.endTentativeSection();
        } else if (damage[0] + damage[1] + damage[2] + damage[3] > 0) {
            report.addLine("", "No " + dmgType, "");
            report.endTentativeSection();
        } else {
            report.discardTentativeSection();
        }
    }

    /**
     * Sums up the damage values for all ranges and reports the counted weapons.
     * The dmgType indicates the special ability for which this sum is (LRM, REAR, etc.).
     * location indicates the target of this damage, i.e. if it is the unit's standard damage and
     * standard ability block, the REAR ability or the TUR block, see ASLocationMapper.
     * When the location is rearLocation, dmgType must be REAR (as REAR has no LRM damage or the like)
     * When the location is turretLocation, TUR indicates it's the TUR's standard damage, LRM
     * indicates it's the LRM damage within the TUR block.
     *
     * @return The raw damage sums for the four ranges
     */
    protected double[] assembleSpecialDamage(BattleForceSUA dmgType, int location) {
        double[] rawDmg = new double[4];
        Arrays.fill(rawDmg, 0);
        for (Mounted weapon : weaponsList) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            double locationMultiplier = ASLocationMapper.damageLocationMultiplier(entity, location, weapon);
            if (!countsforSpecial(weapon, dmgType) || (locationMultiplier == 0)) {
                continue;
            }
            // STD means a turret's standard damage, this may use Artemis, all other specials don't
            Mounted linked = (dmgType == STD) ? weapon.getLinkedBy() : null;
            double dmgS = determineSpecialsDamage(weaponType, linked, SHORT_RANGE, dmgType);
            double dmgM = determineSpecialsDamage(weaponType, linked, MEDIUM_RANGE, dmgType);
            double dmgL = determineSpecialsDamage(weaponType, linked, LONG_RANGE, dmgType);
            double dmgE = determineSpecialsDamage(weaponType, linked, EXTREME_RANGE, dmgType);
            if ((dmgS > 0) || (dmgM > 0) || (dmgL > 0) || (dmgE > 0)) {
                double dmgMultiplier = getDamageMultiplier(weapon, weaponType);
                String calculation = "+ " + formatAsVector(dmgS, dmgM, dmgL, dmgE, dmgType);
                calculation += (dmgMultiplier != 1) ? " x " + formatForReport(dmgMultiplier) : "";
                dmgS *= dmgMultiplier * mmlMultiplier(weaponType, dmgType, ASRange.SHORT);
                dmgM *= dmgMultiplier * mmlMultiplier(weaponType, dmgType, ASRange.MEDIUM);
                dmgL *= dmgMultiplier * mmlMultiplier(weaponType, dmgType, ASRange.LONG);
                dmgE *= dmgMultiplier * mmlMultiplier(weaponType, dmgType, ASRange.EXTREME);
                rawDmg[0] += dmgS;
                rawDmg[1] += dmgM;
                rawDmg[2] += dmgL;
                rawDmg[3] += dmgE;
                report.addLine(getWeaponDesc(weapon), calculation,
                        "= " + formatAsVector(rawDmg[0], rawDmg[1], rawDmg[2], rawDmg[3], dmgType));
            }
        }
        return rawDmg;
    }

    protected double determineSpecialsDamage(WeaponType weaponType, Mounted linked, int range, BattleForceSUA dmgType) {
        if (weaponType.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
            return 0;
        }
        return weaponType.getBattleForceDamage(range, linked);
    }

    /**
     * Returns true when the heat-adjusted and tenth-rounded damage values in the List allow the given spa.
     * Only used for the damage specials LRM, SRM, TOR, IATM, AC, FLK
     */
    protected static boolean qualifiesForSpecial(double[] damage, BattleForceSUA dmgType) {
        if (dmgType.isAnyOf(FLK, TOR, IF, REAR, TUR, MSL, CAP, SCAP, STD, PNT)
                && damage[0] + damage[1] + damage[2] + damage[3] > 0) {
            return true;
        } else {
            return roundUpToTenth(damage[1]) >= 1;
        }
    }

    protected int rangesForSpecial(BattleForceSUA dmgType) {
        if (dmgType == SRM) {
            return 2;
        } else if (dmgType == PNT) {
            return 1;
        } else {
            return element.getRangeBands();
        }
    }

    protected String formatAsVector(double s, double m, double l, double e, BattleForceSUA dmgType) {
            StringBuilder vector = new StringBuilder();
        if (dmgType == IF) {
            vector.append(formatForReport(l));
        } else {
            int ranges = rangesForSpecial(dmgType);
            vector.append(formatForReport(s));
            if (ranges > 1) {
                vector.append("/").append(formatForReport(m));
            }
            if (ranges > 2) {
                vector.append("/").append(formatForReport(l));
            }
            if (ranges > 3) {
                vector.append("/").append(formatForReport(e));
            }
        }
        return vector.toString();
    }

    protected boolean countsforSpecial(Mounted weapon, BattleForceSUA dmgType) {
        WeaponType weaponType = (WeaponType) weapon.getType();
        switch (dmgType) {
            case LRM:
                return !MountedHelper.isAnyArtemis(weapon.getLinkedBy())
                        && (weaponType.getBattleForceClass() == WeaponType.BFCLASS_LRM)
                        || (weaponType.getBattleForceClass() == WeaponType.BFCLASS_MML);
            case SRM:
                return !MountedHelper.isAnyArtemis(weapon.getLinkedBy())
                        && (weaponType.getBattleForceClass() == WeaponType.BFCLASS_SRM)
                        || (weaponType.getBattleForceClass() == WeaponType.BFCLASS_MML);
            case FLK:
                return weaponType.getBattleForceClass() == WeaponType.BFCLASS_FLAK;
            case AC:
                return weaponType.getBattleForceClass() == WeaponType.BFCLASS_AC;
            case TOR:
                return weaponType.getBattleForceClass() == WeaponType.BFCLASS_TORP;
            case IATM:
                return weaponType.getBattleForceClass() == WeaponType.BFCLASS_IATM;
            case IF:
                return weaponType.isAlphaStrikeIndirectFire();
            case REL:
                return weaponType.getBattleForceClass() == WeaponType.BFCLASS_REL;
            case REAR:
            case TUR:
                return true;
            case PNT:
                return weaponType.isAlphaStrikePointDefense();
            case MSL:
                return weaponType.getBattleForceClass() == WeaponType.BFCLASS_CAPITAL_MISSILE;
            case CAP:
                return weaponType.getBattleForceClass() == WeaponType.BFCLASS_CAPITAL;
            case SCAP:
                return weaponType.getBattleForceClass() == WeaponType.BFCLASS_SUBCAPITAL;
            case STD:
                return (weaponType.getBattleForceClass() != WeaponType.BFCLASS_CAPITAL_MISSILE)
                        && (weaponType.getBattleForceClass() != WeaponType.BFCLASS_CAPITAL)
                        && (weaponType.getBattleForceClass() != WeaponType.BFCLASS_SUBCAPITAL);
            default:
                return false;
        }
    }

    private double mmlMultiplier(WeaponType weaponType, BattleForceSUA dmgType, ASRange range) {
        if (weaponType.getBattleForceClass() == WeaponType.BFCLASS_MML) {
            if ((dmgType == LRM) && (range == ASRange.SHORT)) {
                return 0;
            } else if ((dmgType == LRM) && (range == ASRange.MEDIUM)) {
                return 0.5;
            } else if ((dmgType == SRM) && (range == ASRange.LONG)) {
                return 0;
            } else if ((dmgType == SRM) && (range == ASRange.MEDIUM)) {
                return 0.5;
            }
        }
        return 1;
    }

    protected static boolean isArtilleryCannon(WeaponType weapon) {
        return (weapon.getAmmoType() == AmmoType.T_LONG_TOM_CANNON)
                || (weapon.getAmmoType() == AmmoType.T_SNIPER_CANNON)
                || (weapon.getAmmoType() == AmmoType.T_THUMPER_CANNON);
    }

    /** Translates an Artillery WeaponType to the AlphaStrike Special Unit Ability, if any can be found. */
    protected static BattleForceSUA getArtilleryType(WeaponType weaponType) {
        switch (weaponType.getAmmoType()) {
            case AmmoType.T_ARROW_IV:
                return (weaponType.getTechBase() == TECH_BASE_CLAN) ? ARTAC : ARTAIS;
            case AmmoType.T_LONG_TOM:
                return ARTLT;
            case AmmoType.T_SNIPER:
                return ARTS;
            case AmmoType.T_THUMPER:
                return ARTT;
            case AmmoType.T_LONG_TOM_CANNON:
                return ARTLTC;
            case AmmoType.T_SNIPER_CANNON:
                return ARTSC;
            case AmmoType.T_THUMPER_CANNON:
                return ARTTC;
            case AmmoType.T_CRUISE_MISSILE:
                switch (weaponType.getRackSize()) {
                    case 50:
                        return ARTCM5;
                    case 70:
                        return ARTCM7;
                    case 90:
                        return ARTCM9;
                    case 120:
                        return ARTCM12;
                }
            case AmmoType.T_BA_TUBE:
                return ARTBA;
        }
        return UNKNOWN;
    }

    private static int resultingHTValue(int heatSum) {
        if (heatSum > 10) {
            return 2;
        } else if (heatSum > 4) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Returns the given number, rounded up to the nearest tenth. ASC Converting Heat Errata v1.2:
     * A value of 0.401 -> 0.5; i.e. any fraction will make it round up, not just the second digit.
     */
    public static double roundUpToTenth(double number) {
        double intermediate = 10 * number; // 0.401 -> 4.01 or 4.00999999 or 4.010000001
        double result = (int) intermediate; // 4
        if (intermediate - (int) intermediate > 0.000001) { // -> 0.01 or 0.009999 or 0.01000001
            result += 1; // 5
        }
        return result / 10; // 0.5 or 0.4999999999

    }

    protected int getRearLocation() {
        for (int loc = 0; loc < locations.length; loc++) {
            if (locationNames[loc].equals("REAR")) {
                return loc;
            }
        }
        return -1;
    }

    protected int getTurretLocation() {
        for (int loc = 0; loc < locations.length; loc++) {
            if (locationNames[loc].equals("TUR")) {
                return loc;
            }
        }
        return -1;
    }

    /**
     * Returns the total generated heat (weapons and movement) for a Mek or Aero for the purpose of finding OV / OVL values.
     * If onlyRear is true, only rear-facing weapons are included, otherwise only front-facing weapons are included!
     * If onlyLongRange is true, only weapons with an L damage value are included.
     */
    protected int getHeatGeneration(boolean onlyRear, boolean onlyLongRange) {
        if (entity instanceof Mech) {
            return getMekHeatGeneration((Mech) entity, element, onlyRear, onlyLongRange);
        } else {
            return 0;
        }
    }

    /**
     * Returns the total generated heat (weapons and movement) for a Mech for the purpose of finding OV / OVL values.
     * If onlyRear is true, rear-facing weapons are included, otherwise only front-facing weapons are included!
     * If onlyLongRange is true, only weapons with an L damage value are included.
     */
    private int getMekHeatGeneration(Mech entity, AlphaStrikeElement element, boolean onlyRear, boolean onlyLongRange) {
        int totalHeat = 0;

        if (entity.getJumpMP() > 0) {
            totalHeat += getJumpHeat(entity, element);
        } else if (!entity.isIndustrial() && entity.hasEngine()) {
            totalHeat += entity.getEngine().getRunHeat(entity);
        }

        for (Mounted mount : entity.getWeaponList()) {
            totalHeat += weaponHeat(mount, onlyRear, onlyLongRange);
        }

        if (entity.hasWorkingMisc(MiscType.F_STEALTH, -1)
                || entity.hasWorkingMisc(MiscType.F_VOIDSIG, -1)
                || entity.hasWorkingMisc(MiscType.F_NULLSIG, -1)) {
            totalHeat += 10;
        }

        if (entity.hasWorkingMisc(MiscType.F_CHAMELEON_SHIELD, -1)) {
            totalHeat += 6;
        }

        return totalHeat;
    }

    protected int weaponHeat(Mounted weapon, boolean onlyRear, boolean onlyLongRange) {
        WeaponType weaponType = (WeaponType) weapon.getType();
        if (weaponType.hasFlag(WeaponType.F_ONESHOT)
                || (onlyRear && !weapon.isRearMounted())
                || (!onlyRear && weapon.isRearMounted())
                || (onlyLongRange && weaponType.getBattleForceDamage(LONG_RANGE) == 0)) {
            return 0;
        } else {
            return weaponHeat(weaponType);
        }
    }

    private static int getJumpHeat(Entity entity, AlphaStrikeElement element) {
        if ((entity.getJumpType() == Mech.JUMP_IMPROVED)
                && (entity.getEngine().getEngineType() == Engine.XXL_ENGINE)) {
            return Math.max(3, element.getJumpMove() / 2);
        } else if (entity.getJumpType() == Mech.JUMP_IMPROVED) {
            return Math.max(3, ASConverter.roundUp(0.25 * element.getJumpMove()));
        } else if (entity.getEngine().getEngineType() == Engine.XXL_ENGINE) {
            return Math.max(6, element.getJumpMove());
        } else {
            return Math.max(3, element.getJumpMove() / 2);
        }
    }

    protected static int weaponHeat(WeaponType weaponType) {
        int ammoType = weaponType.getAmmoType();
        if (ammoType == AmmoType.T_AC_ROTARY) {
            return weaponType.getHeat() * 6;
        } else if ((ammoType == AmmoType.T_AC_ULTRA) || (ammoType == AmmoType.T_AC_ULTRA_THB)) {
            return weaponType.getHeat() * 2;
        } else {
            return weaponType.getHeat();
        }
    }

    /** Returns the heat dissipation for Meks and AFs, according to ASC - Converting Heat Errata v1.2. */
    protected int getHeatCapacity() {
        int heatCapacity = 0;
        if (entity instanceof Mech) {
            heatCapacity = ((Mech) entity).getHeatCapacity(false, false);
        } else if (entity.isFighter() || element.usesArcs()) {
            heatCapacity = entity.getHeatCapacity(false);
        }
        long coolantPodCount = entity.getEquipment().stream().filter(Mounted::isCoolantPod).count();
        if (coolantPodCount > 0) {
            heatCapacity += coolantPodCount;
        }
        if (entity.hasWorkingMisc(MiscType.F_PARTIAL_WING)) {
            heatCapacity += 3;
        }
        if (element.hasSUA(RHS)) {
            heatCapacity += 1;
        }
        if (element.hasSUA(ECS)) {
            heatCapacity += 1;
        }
        return heatCapacity;
    }

    protected String getWeaponDesc(Mounted weapon) {
        StringBuilder desc = new StringBuilder(weapon.getShortName());
        if (weapon.isRearMounted()) {
            desc.append(" (R)");
        }
        if (weapon.isMechTurretMounted()) {
            desc.append(" (T)");
        }
        if (weapon.isSponsonTurretMounted()) {
            desc.append(" (ST)");
        }
        if (weapon.isPintleTurretMounted()) {
            desc.append(" (PT)");
        }
        if (element.isBattleArmor()) {
            if (weapon.getBaMountLoc() == BattleArmor.MOUNT_LOC_BODY) {
                desc.append(" (Body)");
            }
            if (weapon.getBaMountLoc() == BattleArmor.MOUNT_LOC_LARM) {
                desc.append(" (Left arm)");
            }
            if (weapon.getBaMountLoc() == BattleArmor.MOUNT_LOC_RARM) {
                desc.append(" (Right arm)");
            }
            if (weapon.getBaMountLoc() == BattleArmor.MOUNT_LOC_TURRET) {
                desc.append(" (Turret)");
            }
            if (weapon.isDWPMounted()) {
                desc.append(" (DWP)");
            }
            if (weapon.isSquadSupportWeapon()) {
                desc.append(" (SSWM)");
            }
            if (weapon.isAPMMounted()) {
                desc.append(" (APM)");
            }
        }
        desc.append(" (").append(entity.getLocationAbbr(weapon.getLocation())).append(")");
        return desc.toString();
    }

    protected void writeLocationsToElement() {
        element.setStandardDamage(new ASDamageVector(finalSDamage, finalMDamage, finalLDamage, finalEDamage,
                element.getRangeBands(), true));
        element.setSpecialAbilities(locations[0].getSpecials());
        if (turretLocation != -1) {
            element.getSpecialAbilities().replaceSPA(TUR, locations[turretLocation].getSpecials());
        }
        if ((rearLocation != -1) && locations[rearLocation].getSpecials().hasSPA(REAR)) {
            element.getSpecialAbilities().replaceSPA(REAR, locations[rearLocation].getSpecials().getSPA(REAR));
        }
    }
}