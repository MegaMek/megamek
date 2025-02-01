/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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

package megamek.common.util;

import megamek.common.*;
import megamek.common.internationalization.Internationalization;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;
import java.util.Vector;

/**
 * Methods that implement the Hazardous Liquid Pool as described in TO:AR p. 47
 *
 */
public class HazardousLiquidPoolUtil {

    private enum HazardousLiquidClass {
        CLASS_0(0, 0, 1),
        CLASS_1(1, 0, 2),
        CLASS_2(1, 0, 1),
        CLASS_3(1, 2, 1),
        DEADLY(2, 0, 1);

        private final int numberOfDice;
        private final int staticExtraDamage;
        private final int divisor;

        HazardousLiquidClass(int numberOfDice, int staticExtraDamage, int divisor) {
            this.numberOfDice = numberOfDice;
            this.staticExtraDamage = staticExtraDamage;
            this.divisor = divisor;
        }
    }

    /**
     * Use this method to apply damage and generate reports for a unit moving in or starting its turn in hazardous liquid.
     * @param entity Entity in the liquid
     * @param eruption If this is from an eruoption
     * @param depth Int depth of the liquid
     * @param twGameManager Current game manager (to damage entity)
     * @return reports that should be added
     */
    public static List<Report> getHazardousLiquidDamage(Entity entity, boolean eruption, int depth, TWGameManager twGameManager){
        List<Report> reports = new Vector<>();


        // First, what flavor is the hazardous liquid at this moment?
        Report hazardousLiquidClassReport = new Report (2520);
        hazardousLiquidClassReport.addDesc(entity);

        Roll hazardousLiquidClassRoll = Compute.rollD6(1);
        hazardousLiquidClassReport.add(hazardousLiquidClassRoll.getIntValue());

        HazardousLiquidClass hazardousLiquidClass = switch (hazardousLiquidClassRoll.getIntValue()) {
            case 6 -> HazardousLiquidClass.DEADLY;
            case 5 -> HazardousLiquidClass.CLASS_3;
            case 4 -> HazardousLiquidClass.CLASS_2;
            case 3 -> HazardousLiquidClass.CLASS_1;
            default -> HazardousLiquidClass.CLASS_0;
        };
        hazardousLiquidClassReport.add(Internationalization.getText("HazardousLiquidPoolUtil." + hazardousLiquidClass.name() + ".text"));
        reports.add(hazardousLiquidClassReport);

        // Class 0 does no damage, so let's return.
        if (hazardousLiquidClass == HazardousLiquidClass.CLASS_0) {
            return reports;
        }

        Report preDamageReport = new Report(2524);
        preDamageReport.addDesc(entity);
        preDamageReport.subject = entity.getId();
        reports.add(preDamageReport);


        // A standing Mek is only hit in its legs, unless it's in deep spicy juice
        int toHitTable = ToHitData.HIT_NORMAL;
        if ((entity instanceof Mek && !entity.isProne() && !eruption)
                || depth > 1) {
            toHitTable = ToHitData.HIT_BELOW;
        }

        //Calculate damage per TO:AR p. 47 "HAZARDOUS LIQUID POOLS TABLE"
        int totalDamage =  Math.floorDiv(Compute.d6(hazardousLiquidClass.numberOfDice), hazardousLiquidClass.divisor) + hazardousLiquidClass.staticExtraDamage;
        totalDamage *= getHazardousLiquidPoolDamageMultiplierForUnsealed(entity);
        totalDamage = (int) (Math.floor(totalDamage / getHazardousLiquidPoolDamageDivisorForInfantry(entity)));

        // After all that math let's make sure we do at least 1 damage.
        totalDamage = Math.max(totalDamage, 1);
        while (totalDamage > 0) {
            int damage = Math.min(totalDamage, 5);
            totalDamage = (Math.max(totalDamage - 5, 0));
            HitData hitData = entity.rollHitLocation(toHitTable, ToHitData.SIDE_RANDOM);
            reports.addAll((twGameManager.damageEntity(entity, hitData, damage)));

        }

        return reports;
    }

    /**
     * Support vehicles and industrial meks without environmental sealing take double damage
     * @param entity
     * @return
     */
    public static int getHazardousLiquidPoolDamageMultiplierForUnsealed(Entity entity) {
        // IndustrialMeks and Support Vehicles take Double Damage
        // unless they have environmental sealing
        if ((entity.isIndustrialMek() || entity.isSupportVehicle())
            && (!entity.hasEnvironmentalSealing())) {
            return 2;
        }
        return 1;
    }

    /**
     * Infantry units take more or less damage depending on if they have XCT training and the appropriate gear
     * @param entity
     * @return
     */
    public static double getHazardousLiquidPoolDamageDivisorForInfantry(Entity entity) {
        // If infantry have XCT training and appropriate gear they take 1/3 damage
        // Otherwise they take double damage.
        // BA take damage as normal.
        if (entity.isInfantry() && !entity.isBattleArmor() && entity instanceof Infantry inf) {
            if (inf.hasSpecialization(Infantry.XCT) && inf.getArmorKit() != null && inf.getArmorKit().hasSubType(MiscType.S_TOXIC_ATMO)) {
                return 3.0;
            } else {
                return .5;
            }
        }
        return 1;
    }
}
