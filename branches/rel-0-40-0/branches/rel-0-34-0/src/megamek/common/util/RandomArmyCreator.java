/*
 * MegaMek -
 * Copyright (C) 2006 Ben Mazur (bmazur@sev.org)
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
package megamek.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import megamek.common.Compute;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.TechConstants;
import megamek.common.UnitType;

public class RandomArmyCreator {
    /**
     * Parameters for the random army generator
     */
    public static class Parameters {
        /**
         * Number of mechs to include in the army
         */
        public int mechs;

        /**
         * Number of combat vehicles to include
         */
        public int tanks;

        /**
         * Number of battle armor infantry to include
         */
        public int ba;

        /**
         * Number of conventional infantry to include
         */
        public int infantry;

        /**
         * Maximum battle value
         */
        public int maxBV;

        /**
         * Minimum battle value
         */
        public int minBV;

        /**
         * Latest design year
         */
        public int maxYear = 9999;

        /**
         * Earliest design year
         */
        public int minYear = 0;

        /**
         * A value from TechConstants, which will filter the units
         */
        public int tech;

        /**
         * Canon units only?
         */
        public boolean canon;

        /**
         * If true, add extra infantry to pad out the BV and get closer to
         * maximum
         */
        public boolean padWithInfantry;
    }

    /**
     * Sorting MechSummary by BV
     */
    static Comparator<MechSummary> bvComparator = new Comparator<MechSummary>() {
        public int compare(MechSummary a, MechSummary b) {
            if (a.getBV() > b.getBV()) {
                return 1;
            } else if (b.getBV() > a.getBV()) {
                return -1;
            } else {
                return 0;
            }
        }
    };

    private static ArrayList<MechSummary> generateArmy(
            ArrayList<MechSummary> unitList, int count, int targetBV,
            int allowedVariance) {
        ArrayList<MechSummary> units = new ArrayList<MechSummary>();
        if ((count < 1) || (unitList.size() < 1)) {
            return units;
        }
        // first pick any random mechs
        int selection[] = new int[count];
        int currentBV = 0;
        for (int i = 0; i < count; i++) {
            selection[i] = Compute.randomInt(unitList.size());
            currentBV += unitList.get(selection[i]).getBV();
        }
        Arrays.sort(selection);
        // now try and bring into range
        int bottom, top;
        bottom = 0;
        top = unitList.size() - 1;
        int giveUp = 0;
        while (((currentBV < targetBV - allowedVariance) || (currentBV > targetBV))
                && (giveUp++ < 40000)) {
            if (top == bottom) {
                break;
            }
            if (currentBV < targetBV - allowedVariance) {
                // under BV, reroll above the weakest unit
                bottom = Math.max(bottom, selection[0]);
                currentBV = 0;
                for (int i = 0; i < count; i++) {
                    selection[i] = Compute.randomInt(top - bottom) + bottom;
                    currentBV += unitList.get(selection[i]).getBV();
                }
            } else if (currentBV > targetBV) {
                // over BV, reroll below the highest unit
                top = Math.min(top, selection[selection.length - 1]);
                currentBV = 0;
                for (int i = 0; i < count; i++) {
                    selection[i] = Compute.randomInt(top - bottom) + bottom;
                    currentBV += unitList.get(selection[i]).getBV();
                }
            }
            Arrays.sort(selection);
        }
        for (int i = 0; i < count; i++) {
            MechSummary m = unitList.get(selection[i]);
            units.add(m);
        }
        return units;
    }

    private static int countBV(ArrayList<MechSummary> units) {
        int bv = 0;
        for (MechSummary m : units) {
            bv += m.getBV();
        }
        return bv;
    }

    public static void main(String[] args) {
        Parameters p = new Parameters();
        p.mechs = 4;
        p.tanks = 4;
        p.infantry = 0;
        p.ba = 4;
        p.maxBV = 8000;
        p.minBV = 7600;
        p.minYear = 3050;
        p.maxYear = 3055;
        p.tech = TechConstants.T_IS_TW_NON_BOX;
        p.canon = true;
        p.padWithInfantry = true;
        ArrayList<MechSummary> units = generateArmy(p);

        int totalBV = 0;
        for (MechSummary m : units) {
            totalBV += m.getBV();
            System.out.print(m.getChassis());
            System.out.print(" ");
            System.out.print(m.getModel());
            System.out.print(" ");
            System.out.println(m.getBV());
        }
        System.out.print("Total: ");
        System.out.println(totalBV);

    }

    public static ArrayList<MechSummary> generateArmy(Parameters p) {
        int allowedVariance = java.lang.Math.abs(p.maxBV - p.minBV);
        MechSummary[] all = MechSummaryCache.getInstance().getAllMechs();
        ArrayList<MechSummary> allMechs = new ArrayList<MechSummary>();
        ArrayList<MechSummary> allTanks = new ArrayList<MechSummary>();
        ArrayList<MechSummary> allInfantry = new ArrayList<MechSummary>();
        ArrayList<MechSummary> allBA = new ArrayList<MechSummary>();
        for (MechSummary m : all) {
            if ((p.tech != TechConstants.T_ALL) && (p.tech != m.getType())) {
                // advanced rules includes basic too
                if (p.tech == TechConstants.T_CLAN_ADVANCED) {
                    if (m.getType() != TechConstants.T_CLAN_TW) {
                        continue;
                    }
                } else if (p.tech == TechConstants.T_IS_ADVANCED) {
                    if ((m.getType() != TechConstants.T_INTRO_BOXSET)
                            && (m.getType() != TechConstants.T_IS_TW_NON_BOX)) {
                        continue;
                    }
                } else if (p.tech == TechConstants.T_IS_TW_NON_BOX) {
                    if (m.getType() != TechConstants.T_INTRO_BOXSET) {
                        continue;
                    }
                } else if (p.tech == TechConstants.T_TW_ALL) {
                    if ((m.getType() != TechConstants.T_INTRO_BOXSET)
                            && (m.getType() != TechConstants.T_IS_TW_NON_BOX)
                            && (m.getType() != TechConstants.T_CLAN_TW)) {
                        continue;
                    }
                } else if (p.tech == TechConstants.T_IS_TW_ALL) {
                    if ((m.getType() != TechConstants.T_INTRO_BOXSET)
                            && (m.getType() != TechConstants.T_IS_TW_NON_BOX)) {
                        continue;
                    }
                } else {
                    continue;
                }
            }
            if (((m.getYear() < p.minYear) || (m.getYear() > p.maxYear))
                    && !m.getUnitType().equals(
                            UnitType.getTypeName(UnitType.INFANTRY))) {
                continue;
            }
            if (p.canon && !m.isCanon()) {
                continue;
            }

            // Unit accepted, add to the appropriate list
            if (m.getUnitType().equals(UnitType.getTypeName(UnitType.MEK))) {
                allMechs.add(m);
            } else if (m.getUnitType()
                    .equals(UnitType.getTypeName(UnitType.TANK))
                    || m.getUnitType().equals(
                            UnitType.getTypeName(UnitType.VTOL))) {
                allTanks.add(m);
            } else if (m.getUnitType().equals(
                    UnitType.getTypeName(UnitType.BATTLE_ARMOR))) {
                allBA.add(m);
            } else if (m.getUnitType().equals(
                    UnitType.getTypeName(UnitType.INFANTRY))) {
                allInfantry.add(m);
            }
        }
        Collections.<MechSummary> sort(allMechs, bvComparator);
        Collections.<MechSummary> sort(allTanks, bvComparator);
        Collections.<MechSummary> sort(allInfantry, bvComparator);
        Collections.<MechSummary> sort(allBA, bvComparator);

        // get the average BV for each unit class, to determine how to split up
        // the total
        int averageMechBV = countBV(allMechs) / Math.max(1, allMechs.size());
        int averageTankBV = countBV(allTanks) / Math.max(1, allTanks.size());
        int averageInfBV = countBV(allInfantry) / Math.max(1, allInfantry.size());
        int averageBaBV = countBV(allBA) / Math.max(1, allBA.size());
        int helpWeight = Math.max(1, p.mechs * averageMechBV + p.tanks
                * averageTankBV + p.infantry * averageInfBV + p.ba * averageBaBV);

        int baBV = (p.ba * averageBaBV * p.maxBV) / helpWeight;
        if ((p.ba > 0) && (allBA.size() > 0)) {
            baBV = Math.max(baBV, p.ba * allBA.get(0).getBV());
            baBV = Math.min(baBV, p.ba * allBA.get(allBA.size() - 1).getBV());
        } else {
            baBV = 0;
        }
        int mechBV = (p.mechs * averageMechBV * p.maxBV) / helpWeight;
        if ((p.mechs > 0) && (allMechs.size() > 0)) {
            mechBV = Math.max(mechBV, p.mechs * allMechs.get(0).getBV());
            mechBV = Math.min(mechBV, p.mechs
                    * allMechs.get(allMechs.size() - 1).getBV());
        } else {
            mechBV = 0;
        }
        int tankBV = (p.tanks * averageTankBV * p.maxBV) / helpWeight;
        if ((p.tanks > 0) && (allTanks.size() > 0)) {
            tankBV = Math.max(tankBV, p.tanks * allTanks.get(0).getBV());
            tankBV = Math.min(tankBV, p.tanks
                    * allTanks.get(allTanks.size() - 1).getBV());
        } else {
            tankBV = 0;
        }

        // add the units in roughly increasing BV order
        ArrayList<MechSummary> units = generateArmy(allBA, p.ba, baBV,
                allowedVariance);
        units.addAll(generateArmy(allTanks, p.tanks, tankBV + baBV
                - countBV(units), allowedVariance));
        units.addAll(generateArmy(allMechs, p.mechs, mechBV + tankBV + baBV
                - countBV(units), allowedVariance));
        if (p.padWithInfantry) {
            int inf = (p.maxBV - countBV(units)) / averageInfBV;
            units.addAll(generateArmy(allInfantry, inf, p.maxBV
                    - countBV(units), allowedVariance));
        } else {
            units.addAll(generateArmy(allInfantry, p.infantry, p.maxBV
                    - countBV(units), allowedVariance));
        }
        return units;
    }

}
