/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.strategicBattleSystems;

import java.util.ArrayList;

import megamek.client.ui.clientGUI.calculationReport.FlexibleCalculationReport;
import megamek.common.Entity;
import megamek.common.ForceAssignable;
import megamek.common.Game;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.force.Force;
import megamek.common.force.Forces;

public final class SBFFormationConverter extends BaseFormationConverter<SBFFormation> {

    public SBFFormationConverter(Force force, Game game) {
        super(force, game, new SBFFormation());
    }

    public static void calculateStatsFromUnits(SBFFormation formation) {
        SBFFormationConverter converter = new SBFFormationConverter(null, null);
        converter.formation = formation;
        converter.calcSbfFormationStats();
        formation.setConversionReport(converter.report);
    }

    @Override
    public SBFFormation convert() {
        if (!canConvertToSbfFormation(force, game)) {
            return null;
        }
        report.addHeader("Strategic BattleForce Conversion for ");
        report.addHeader(force.getName());

        Forces forces = game.getForces();
        for (Force subforce : forces.getFullSubForces(force)) {
            var thisUnit = new ArrayList<AlphaStrikeElement>();
            for (ForceAssignable entity : forces.getFullEntities(subforce)) {
                if (entity instanceof Entity) {
                    thisUnit.add(ASConverter.convert((Entity) entity, new FlexibleCalculationReport()));
                }
            }
            SBFUnit convertedUnit = new SBFUnitConverter(thisUnit, subforce.getName(), report).createSbfUnit();
            formation.addUnit(convertedUnit);
        }
        formation.setName(force.getName());
        calcSbfFormationStats();
        formation.setConversionReport(report);
        return formation;
    }

}
