/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.battleValue;

import static megamek.client.ui.clientGUI.calculationReport.CalculationReport.formatForReport;

import java.util.function.Predicate;

import megamek.common.MPCalculationSetting;
import megamek.common.board.Coords;
import megamek.common.equipment.Mounted;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;

/**
 * Battle Value calculator for Structures (AbstractBuildingEntity).
 * <p>
 * Structures follow special BV calculation rules from TO:AUE:
 * <p>
 * Defensive BV = (Armor × 2.5 + CF × 1.5 + Defensive Equipment BV) × 0.5 Offensive BV = (Weapon BV + Hex Count × 50) ×
 * Speed Factor
 * <p>
 * Special rules: - No arc modifiers (all weapons count full BV) - Ammo BV capped at weapon BV per weapon type - Speed
 * factor uses maximum MP rating
 */
public class AbstractBuildingEntityBVCalculator extends BVCalculator {

    AbstractBuildingEntityBVCalculator(Entity entity) {
        super(entity);
    }

    @Override
    protected void processArmor() {
        // Structures use Armor Factor × 2.5
        int armorFactor = entity.getOArmor(0);
        double armorBV = armorFactor * 2.5;

        defensiveValue += armorBV;
        bvReport.addLine("Armor Factor:",
              armorFactor + " × 2.5",
              "= " + formatForReport(armorBV));
    }

    @Override
    protected void processStructure() {
        // Structures use Construction Factor (CF) × 1.5
        AbstractBuildingEntity building = (AbstractBuildingEntity) entity;

        if (!building.getCoordsList().isEmpty()) {
            Coords firstHex = building.getCoordsList().get(0);
            int cf = building.getCurrentCF(firstHex);
            double cfBV = cf * 1.5;

            defensiveValue += cfBV;
            bvReport.addLine("Construction Factor:",
                  cf + " × 1.5",
                  "= " + formatForReport(cfBV));
        }
    }

    @Override
    protected void processDefensiveFactor() {
        // Structures apply a final × 0.5 multiplier to defensive BV
        double beforeFactor = defensiveValue;
        defensiveValue *= 0.5;
        bvReport.addLine("Defensive Factor:",
              formatForReport(beforeFactor) + " × 0.5",
              "= " + formatForReport(defensiveValue));
    }

    @Override
    protected Predicate<Mounted<?>> frontWeaponFilter() {
        // Structures don't apply arc modifiers - all weapons count as "front"
        return weapon -> true;
    }

    @Override
    protected Predicate<Mounted<?>> rearWeaponFilter() {
        // Structures don't have rear arc penalties
        return weapon -> false;
    }

    @Override
    protected void processWeight() {
        // Structures add (Hex Count × 50) to offensive BV
        AbstractBuildingEntity building = (AbstractBuildingEntity) entity;
        int hexCount = building.getOriginalHexCount();
        double hexBV = hexCount * 50;

        offensiveValue += hexBV;
        bvReport.addLine("Building Hexes:",
              hexCount + " × 50",
              "= " + formatForReport(hexBV));
    }

    @Override
    protected int offensiveSpeedFactorMP() {
        // Use maximum MP rating for speed factor
        return entity.getRunMP(MPCalculationSetting.BV_CALCULATION);
    }

}
