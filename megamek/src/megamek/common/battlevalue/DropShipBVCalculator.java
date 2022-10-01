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
package megamek.common.battlevalue;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.weapons.bayweapons.BayWeapon;

import java.util.*;
import java.util.function.Predicate;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;

public class DropShipBVCalculator extends AeroBVCalculator {

    private static final int BVLOC_NOSE = 0;
    private static final int BVLOC_LEFT = 1;
    private static final int BVLOC_LEFT_AFT = 2;
    private static final int BVLOC_AFT = 3;
    private static final int BVLOC_RIGHT_AFT = 4;
    private static final int BVLOC_RIGHT = 5;
    private final Dropship dropship;
    private int nominalNoseLocation;
    private int nominalLeftLocation;
    private int nominalRightLocation;

    DropShipBVCalculator(Entity entity) {
        super(entity);
        dropship = (Dropship) entity;
    }

    @Override
    protected boolean usesWeaponHeat() {
        return false;
    }

    @Override
    protected void processExplosiveEquipment() { }

    @Override
    protected int heatEfficiency() {
        bvReport.addLine("Heat Efficiency:", " = " + aero.getHeatCapacity(), "");
        return aero.getHeatCapacity();
    }

    @Override
    protected Predicate<Mounted> rearWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_AFT);
    }

    protected Predicate<Mounted> leftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_LWING) && !weapon.isRearMounted();
    }

    protected Predicate<Mounted> leftAftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_LWING) && weapon.isRearMounted();
    }

    protected Predicate<Mounted> rightWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_RWING) && !weapon.isRearMounted();
    }

    protected Predicate<Mounted> rightAftWeaponFilter() {
        return weapon -> (weapon.getLocation() == Dropship.LOC_RWING) && weapon.isRearMounted();
    }

    protected double arcFactor(Mounted equipment) {
        if (!frontAndRearDecided) {
            return 1;
        } else if (isNominalArc(nominalNoseLocation, equipment)) {
            return 1;
        } else if (isNominalArc(nominalLeftLocation, equipment)) {
            return heatEfficiencyExceeded ? 0.5 : 1;
        } else if (isNominalArc(nominalRightLocation, equipment)) {
            return heatEfficiencyExceeded ? 0.25 : 0.5;
        } else {
            return 0.25;
        }
    }

    @Override
    protected boolean isNominalRear(Mounted weapon) {
        return false;
    }

    @Override
    protected boolean isNominalRearArc(Mounted weapon) {
        return super.isNominalRearArc(weapon);
    }

    @Override
    protected boolean isNominalSideArc(Mounted weapon) {
        return heatEfficiencyExceeded;
    }

    protected boolean isNominalArc(int nominalBvLocation, Mounted equipment) {
        switch (nominalBvLocation) {
            case BVLOC_NOSE:
                return equipment.getLocation() == Dropship.LOC_NOSE;
            case BVLOC_LEFT:
                return (equipment.getLocation() == Dropship.LOC_LWING) && !equipment.isRearMounted();
            case BVLOC_LEFT_AFT:
                return (equipment.getLocation() == Dropship.LOC_LWING) && equipment.isRearMounted();
            case BVLOC_AFT:
                return equipment.getLocation() == Dropship.LOC_AFT;
            case BVLOC_RIGHT_AFT:
                return (equipment.getLocation() == Dropship.LOC_RWING) && equipment.isRearMounted();
            case BVLOC_RIGHT:
                return (equipment.getLocation() == Dropship.LOC_RWING) && !equipment.isRearMounted();
        }
        return false;
    }

    @Override
    protected void determineFront() {
        Predicate<Mounted> frontFilter = frontWeaponFilter();
        Predicate<Mounted> rearFilter = rearWeaponFilter();
        Predicate<Mounted> leftFilter = leftWeaponFilter();
        Predicate<Mounted> rightFilter = rightWeaponFilter();
        Predicate<Mounted> leftAftFilter = leftAftWeaponFilter();
        Predicate<Mounted> rightAftFilter = rightAftWeaponFilter();
        Map<Integer, Double> bvPerArc = new HashMap<>();
        double weaponsBVFront = processWeaponSection(false, frontFilter, false);
        double weaponsBVRear = processWeaponSection(false, rearFilter, false);
        double weaponsBVLeft = processWeaponSection(false, leftFilter, false);
        double weaponsBVRight = processWeaponSection(false, rightFilter, false);
        double weaponsBVAftLeft = processWeaponSection(false, leftAftFilter, false);
        double weaponsBVAftRight = processWeaponSection(false, rightAftFilter, false);
        bvPerArc.put(BVLOC_NOSE, weaponsBVFront);
        bvPerArc.put(BVLOC_LEFT, weaponsBVLeft);
        bvPerArc.put(BVLOC_RIGHT, weaponsBVRight);
        bvPerArc.put(BVLOC_AFT, weaponsBVRear);
        bvPerArc.put(BVLOC_LEFT_AFT, weaponsBVAftLeft);
        bvPerArc.put(BVLOC_RIGHT_AFT, weaponsBVAftRight);
        final double maxBV = bvPerArc.values().stream().mapToDouble(bv -> bv).max().orElse(0);
        for (Map.Entry<Integer, Double> entry : bvPerArc.entrySet()) {
            if (entry.getValue() == maxBV) {
                nominalNoseLocation = entry.getKey();
                break;
            }
        }
        int firstAdjacentArc = getAdjacentLocationCCW(nominalNoseLocation);
        int secondAdjacentArc = getAdjacentLocationCW(nominalNoseLocation);
        if (bvPerArc.get(firstAdjacentArc) > bvPerArc.get(secondAdjacentArc)) {
            nominalLeftLocation = firstAdjacentArc;
            nominalRightLocation = secondAdjacentArc;
        } else {
            nominalLeftLocation = secondAdjacentArc;
            nominalRightLocation = firstAdjacentArc;
        }

        bvReport.addLine("Nominal Nose Location",
                arcName(nominalNoseLocation) + ", Weapon BV: " + formatForReport(bvPerArc.get(nominalNoseLocation)),
                "");
        bvReport.addLine("Nominal Left Location",
                arcName(nominalLeftLocation) + ", Weapon BV: " + formatForReport(bvPerArc.get(nominalLeftLocation)),
                "");
        bvReport.addLine("Nominal Right Location",
                arcName(nominalRightLocation) + ", Weapon BV: " + formatForReport(bvPerArc.get(nominalRightLocation)),
                "");
        frontAndRearDecided = true;
    }

    @Override
    protected void processWeapons() {
        int heatEfficiency = heatEfficiency();

        double totalHeatSum = processArc(nominalNoseLocation);
        bvReport.addLine("Total \"Nose\" Heat", formatForReport(totalHeatSum), "");
        heatEfficiencyExceeded = totalHeatSum > heatEfficiency;
        if (heatEfficiencyExceeded) {
            bvReport.addLine("Heat Efficiency Exceeded", "", "");
        }

        totalHeatSum += processArc(nominalLeftLocation);
        bvReport.addLine("Total \"Left\" Heat", formatForReport(totalHeatSum), "");
        heatEfficiencyExceeded = totalHeatSum > heatEfficiency;

        processArc(nominalRightLocation);
        processArc(getOppositeLocation(nominalNoseLocation));
        processArc(getOppositeLocation(nominalLeftLocation));
        processArc(getOppositeLocation(nominalRightLocation));
    }

    protected double processArc(final int bvNominalLocation) {
        double arcHeat = 0;
        bvReport.addLine(arcName(bvNominalLocation) + ":", "", "");
        for (Mounted weapon: dropship.getTotalWeaponList()) {
            if (isNominalArc(bvNominalLocation, weapon) && countAsOffensiveWeapon(weapon)) {
                arcHeat += weaponHeat(weapon);
            }
        }
        processWeaponSection(true, weapon -> isNominalArc(bvNominalLocation, weapon), true);
//        for (Mounted ammo: dropship.getAmmo()) {
//            if (isNominalArc(bvNominalLocation, ammo) && ammoCounts(ammo)) {
//                AmmoType ammoType = (AmmoType) ammo.getType();
//                String key = ammoType.getAmmoType() + ":" + ammoType.getRackSize();
//                offensiveValue += weaponsForExcessiveAmmo.get(key);
//                bvReport.addLine("- " + names.get(key), "", "= " + formatForReport(offensiveValue));
//            }
//        }
        return arcHeat;
    }

//    protected void processAmmo() {
//        bvReport.startTentativeSection();
//        bvReport.addLine("Ammo:", "", "");
//        boolean hasAmmo = false;
//        for (String key : keys) {
//            if (!weaponsForExcessiveAmmo.containsKey(key)) {
//                // Coolant Pods have no matching weapon
//                if (key.equals(Integer.valueOf(AmmoType.T_COOLANT_POD).toString() + "1")) {
//                    offensiveValue += ammoMap.get(key);
//                }
//                continue;
//            }
//            if (!ammoMap.containsKey(key)) {
//                continue;
//            }
//            String calculation = "+ ";
//
//            if (ammoMap.get(key) > weaponsForExcessiveAmmo.get(key)) {
//                offensiveValue += weaponsForExcessiveAmmo.get(key) * fireControlModifier();
//                calculation += formatForReport(weaponsForExcessiveAmmo.get(key)) + " (Excessive)";
//            } else {
//                offensiveValue += ammoMap.get(key) * fireControlModifier();
//                calculation += formatForReport(ammoMap.get(key));
//            }
//            calculation += (fireControlModifier() != 1) ? " x " + formatForReport(fireControlModifier()) : "";
//            bvReport.addLine("- " + names.get(key), calculation, "= " + formatForReport(offensiveValue));
//            hasAmmo = true;
//        }
//        if (hasAmmo) {
//            bvReport.endTentativeSection();
//        } else {
//            bvReport.discardTentativeSection();
//        }
//    }

    public static int calculateBV(Dropship dropShip, boolean ignoreC3, boolean ignoreSkill, CalculationReport bvReport) {


        bvReport.addSubHeader("Offensive Battle Rating Calculation:");
        // calculate heat efficiency
        int aeroHeatEfficiency = dropShip.getHeatCapacity();
        bvReport.addLine("Base Heat Efficiency ", "", aeroHeatEfficiency);

        // get arc BV and heat
        double[] arcBVs = new double[dropShip.locations() + 2];
        double[] arcHeats = new double[dropShip.locations() + 2];
        double[] ammoBVs = new double[dropShip.locations() + 2];
        bvReport.addLine("Arc BV and Heat", "", "");

        // cycle through locations
        for (int loc = 0; loc < (dropShip.locations() + 2); loc++) {
            int l = loc;
            boolean isRear = (loc >= dropShip.locations());
            String rear = "";
            if (isRear) {
                l = l - 3;
                rear = " (R)";
            }
            dropShip.getLocationName(l);
            bvReport.addLine(dropShip.getLocationName(l) + rear, "BV", "Heat");
            double arcBV = 0.0;
            double arcHeat = 0.0;
            double arcAmmoBV = 0.0;
            TreeMap<String, Double> weaponsForExcessiveAmmo = new TreeMap<>();
            for (Mounted mounted : dropShip.getTotalWeaponList()) {
                if (mounted.getLocation() != l) {
                    continue;
                }
                if (mounted.isRearMounted() != isRear) {
                    continue;
                }
                // only count non-damaged equipment
                if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed() || mounted.isBreached()) {
                    continue;
                }
                WeaponType wtype = (WeaponType) mounted.getType();
                double weaponHeat = wtype.getHeat();
                double dBV = wtype.getBV(dropShip);
                // skip bays
                if (wtype instanceof BayWeapon) {
                    continue;
                }
                // don't count defensive weapons
                if (wtype.hasFlag(WeaponType.F_AMS)) {
                    continue;
                }
                // don't count screen launchers, they are defensive
                if (wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                    continue;
                }
                // double heat for ultras
                if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                    weaponHeat *= 2;
                }
                // Six times heat for RAC
                if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                    weaponHeat *= 6;
                }
                // calc MG Array here:
                if (wtype.hasFlag(WeaponType.F_MGA)) {
                    double mgBV = 0;
                    for (int eqNum : mounted.getBayWeapons()) {
                        Mounted mg = dropShip.getEquipment(eqNum);
                        if ((mg != null) && (!mg.isDestroyed())) {
                            mgBV += mg.getType().getBV(dropShip);
                        }
                    }
                    dBV = mgBV * 0.67;
                }
                // and we'll add the tcomp here too
                if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    if (dropShip.hasTargComp()) {
                        dBV *= 1.25;
                    }
                }
                // artemis bumps up the value
                if (mounted.getLinkedBy() != null) {
                    Mounted mLinker = mounted.getLinkedBy();
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                        dBV *= 1.2;
                    }
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                        dBV *= 1.1;
                    }

                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                        dBV *= 1.3;
                    }
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                        dBV *= 1.15;
                    }
                    if ((mLinker.getType() instanceof MiscType)
                            && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                        dBV *= 1.15;
                    }
                }
                // add up BV of ammo-using weapons for each type of weapon,
                // to compare with ammo BV later for excessive ammo BV rule
                if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA))
                        || wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_INFANTRY)
                        || (wtype.getAmmoType() == AmmoType.T_NA))) {
                    String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                    if (!weaponsForExcessiveAmmo.containsKey(key)) {
                        weaponsForExcessiveAmmo.put(key, wtype.getBV(dropShip));
                    } else {
                        weaponsForExcessiveAmmo.put(key, wtype.getBV(dropShip) + weaponsForExcessiveAmmo.get(key));
                    }
                }
                bvReport.addLine(wtype.getName(), "+ " + dBV, "+ ", weaponHeat);
                arcBV += dBV;
                arcHeat += weaponHeat;
            }
            // now ammo
            Map<String, Double> ammo = new HashMap<>();
            ArrayList<String> keys = new ArrayList<>();
            for (Mounted mounted : dropShip.getAmmo()) {
                if (mounted.getLocation() != l) {
                    continue;
                }
                if (mounted.isRearMounted() != isRear) {
                    continue;
                }
                AmmoType atype = (AmmoType) mounted.getType();
                // we need to deal with cases where ammo is loaded in multi-ton increments
                // (on dropships and jumpships) - lets take the ratio of shots to shots left
                double ratio = mounted.getUsableShotsLeft() / atype.getShots();

                // if the ratio is less than one, we will treat as a full ton
                // since we don't make that adjustment elsewhere
                if (ratio < 1.0) {
                    ratio = 1.0;
                }

                // don't count depleted ammo
                if (mounted.getUsableShotsLeft() == 0) {
                    continue;
                }

                // don't count AMS, it's defensive
                if (atype.getAmmoType() == AmmoType.T_AMS) {
                    continue;
                }
                // don't count screen launchers, they are defensive
                if (atype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) {
                    continue;
                }

                // don't count oneshot ammo, it's considered part of the launcher.
                if (mounted.getLocation() == Entity.LOC_NONE) {
                    // assumption: ammo without a location is for a oneshot weapon
                    continue;
                }
                double abv = ratio * atype.getBV(dropShip);
                String key = atype.getAmmoType() + ":" + atype.getRackSize();
                String key2 = atype.getName() + ";" + key;
                // MML needs special casing so they don't count double
                if (atype.getAmmoType() == AmmoType.T_MML) {
                    key2 = "MML " + atype.getRackSize() + " Ammo;" + key;
                }
                // same for the different AR10 ammos
                if (atype.getAmmoType() == AmmoType.T_AR10) {
                    key2 = "AR10 Ammo;" + key;
                }
                if (!keys.contains(key2)) {
                    keys.add(key2);
                }
                if (!ammo.containsKey(key)) {
                    ammo.put(key, abv);
                } else {
                    ammo.put(key, abv + ammo.get(key));
                }
            }
            // now cycle through ammo hash and deal with excessive ammo issues
            for (String fullkey : keys) {
                String[] k = fullkey.split(";");
                String key = k[1];
                if (weaponsForExcessiveAmmo.get(key) != null) {
                    if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                        bvReport.addLine(k[0], "+ " + weaponsForExcessiveAmmo.get(key) + "*", "");
                        arcAmmoBV += weaponsForExcessiveAmmo.get(key);
                    } else {
                        bvReport.addLine(k[0], "+ " + ammo.get(key), "");
                        arcAmmoBV += ammo.get(key);
                    }
                }
            }
            bvReport.addLine(dropShip.getLocationName(l) + rear + " Weapon Totals", "" + arcBV, "", arcHeat);
            bvReport.addLine(dropShip.getLocationName(l) + rear + " Ammo Totals", "" + arcAmmoBV, "");
            bvReport.addLine(dropShip.getLocationName(l) + rear + " Totals", "" + (arcBV + arcAmmoBV), "");
            arcBVs[loc] = arcBV;
            arcHeats[loc] = arcHeat;
            ammoBVs[loc] = arcAmmoBV;
        }

        double weaponBV = 0.0;
        // ok, now lets loop through the arcs and find the highest value BV arc
        int highArc = Integer.MIN_VALUE;
        int adjArcH = Integer.MIN_VALUE;
        int adjArcL = Integer.MIN_VALUE;
        double adjArcHMult = 1.0;
        double adjArcLMult = 0.5;
        double highBV = 0.0;
        double heatUsed = 0.0;
        for (int loc = 0; loc < arcBVs.length; loc++) {
            if (arcBVs[loc] > highBV) {
                highArc = loc;
                highBV = arcBVs[loc];
            }
        }
        // now lets identify the adjacent arcs
        if (highArc > Integer.MIN_VALUE) {
            heatUsed += arcHeats[highArc];
            // now get the BV and heat for the two adjacent arcs
            int adjArcCW = 0;//= getAdjacentLocCW(highArc);
            int adjArcCCW = 0;//dropShip.getAdjacentLocCCW(highArc);
            double adjArcCWBV = 0.0;
            double adjArcCWHeat = 0.0;
            if (adjArcCW > Integer.MIN_VALUE) {
                adjArcCWBV = arcBVs[adjArcCW];
                adjArcCWHeat = arcHeats[adjArcCW];
            }
            double adjArcCCWBV = 0.0;
            double adjArcCCWHeat = 0.0;
            if (adjArcCCW > Integer.MIN_VALUE) {
                adjArcCCWBV = arcBVs[adjArcCCW];
                adjArcCCWHeat = arcHeats[adjArcCCW];
            }
            if (adjArcCWBV > adjArcCCWBV) {
                adjArcH = adjArcCW;
                if ((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    adjArcHMult = 0.5;
                }
                heatUsed += adjArcCWHeat;
                adjArcL = adjArcCCW;
                if ((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    adjArcLMult = 0.25;
                }
                heatUsed += adjArcCCWHeat;
            } else {
                adjArcH = adjArcCCW;
                if ((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    adjArcHMult = 0.5;
                }
                heatUsed += adjArcCCWHeat;
                adjArcL = adjArcCW;
                if ((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    adjArcLMult = 0.25;
                }
                heatUsed += adjArcCWHeat;
            }
        }

        // ok now add in ammo to arc bvs
        for (int i = 0; i < arcBVs.length; i++) {
            arcBVs[i] = arcBVs[i] + ammoBVs[i];
        }

        // ok now lets go in and add the arcs
        double totalHeat;
        if (highArc > Integer.MIN_VALUE) {
            // ok now add the BV from this arc and reset to zero
            totalHeat = arcHeats[highArc];
//            bvReport.addLine("Highest BV Arc (" + getArcName(dropShip, highArc) + ")" + arcBVs[highArc] + "*1.0",
//                    "+ " + arcBVs[highArc], "Total Heat: " + totalHeat);
            weaponBV += arcBVs[highArc];
            arcBVs[highArc] = 0.0;

            if (adjArcH > Integer.MIN_VALUE) {
                totalHeat += arcHeats[adjArcH];
                String over = "";
                if (totalHeat > aeroHeatEfficiency) {
                    over = " (Greater than heat efficiency)";
                }
//                bvReport.addLine("Adjacent High BV Arc (" + getArcName(dropShip, adjArcH) + ") " + arcBVs[adjArcH] + "*" + adjArcHMult,
//                        "+ " + (arcBVs[adjArcH] * adjArcHMult),
//                        "Total Heat: " + totalHeat + over);
                weaponBV += adjArcHMult * arcBVs[adjArcH];
                arcBVs[adjArcH] = 0.0;
            }

            if (adjArcL > Integer.MIN_VALUE) {
                totalHeat += arcHeats[adjArcL];
                String over = "";
                if (totalHeat > aeroHeatEfficiency) {
                    over = " (Greater than heat efficiency)";
                }
//                bvReport.addLine("Adjacent Low BV Arc (" + getArcName(dropShip, adjArcL) + ") " + arcBVs[adjArcL] + "*" + adjArcLMult,
//                        "+ " + (adjArcLMult * arcBVs[adjArcL]),
//                        "Total Heat: " + totalHeat + over);
                weaponBV += adjArcLMult * arcBVs[adjArcL];
                arcBVs[adjArcL] = 0.0;
            }
            // ok now we can cycle through the rest and add 25%
            bvReport.addLine("Remaining Arcs", "", "");
            for (int loc = 0; loc < arcBVs.length; loc++) {
                if (arcBVs[loc] <= 0) {
                    continue;
                }
//                bvReport.addLine(getArcName(dropShip, loc) + " " + arcBVs[loc] + "*0.25",
//                        "+" + (0.25 * arcBVs[loc]), "");
                weaponBV += (0.25 * arcBVs[loc]);
            }
        }





        bvReport.addLine("Total Weapons BV Adjusted For Heat:", "", weaponBV);


        return 0;
    }


    private String arcName(int bvLocation) {
        switch (bvLocation) {
            case BVLOC_NOSE:
                return dropship.getLocationAbbr(Dropship.LOC_NOSE);
            case BVLOC_LEFT:
                return dropship.getLocationAbbr(Dropship.LOC_LWING);
            case BVLOC_LEFT_AFT:
                return dropship.getLocationAbbr(Dropship.LOC_LWING) + " (R)";
            case BVLOC_AFT:
                return dropship.getLocationAbbr(Dropship.LOC_AFT);
            case BVLOC_RIGHT_AFT:
                return dropship.getLocationAbbr(Dropship.LOC_RWING) + " (R)";
            case BVLOC_RIGHT:
                return dropship.getLocationAbbr(Dropship.LOC_RWING);
        }
        return "Error: Unexpected location value.";
    }

    /** @return The adjacent firing arc location, counter-clockwise */
    public int getAdjacentLocationCCW(int bvLocation) {
        return (bvLocation + 1) % 6;
    }

    /** @return The adjacent firing arc location, clockwise */
    public int getAdjacentLocationCW(int bvLocation) {
        return (bvLocation + 5) % 6;
    }

    /** @return The opposite firing arc location, counter-clockwise */
    public int getOppositeLocation(int bvLocation) {
        return (bvLocation + 3) % 6;
    }
}