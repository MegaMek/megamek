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
import megamek.common.alphaStrike.AlphaStrikeElement;

import static megamek.common.alphaStrike.ASUnitType.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASAeroSpecialAbilityConverter extends ASSpecialAbilityConverter2 {

    private final Aero aero = (Aero) entity;

    /**
     * Do not call this directly. Use ASSpecialAbilityConverter.getConverter instead.
     * Constructs a special ability converter for fighter-type Aero units (AF, CF, Aero SV).
     *
     * @param entity The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report The calculation report to write to
     */
    protected ASAeroSpecialAbilityConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
    }

    @Override
    protected void processUnitFeatures() {
        super.processUnitFeatures();

        if (aero.getCockpitType() == Aero.COCKPIT_COMMAND_CONSOLE) {
            assign(Aero.getCockpitTypeString(aero.getCockpitType()), MHQ, 1);
        }

        if (entity instanceof FixedWingSupport) {
            int bombPoints = ((FixedWingSupport) entity).getMaxBombPoints();
            int bombValue = ASConverter.roundUp(0.2d * bombPoints);
            if (bombValue > 0) {
                assign("Fixed Wing Support Bombs (" + bombPoints + ")", BOMB, bombValue);
            }
        } else {
            assign("Fighter Bombs", BOMB, element.getSize());
        }

        if (aero.isVSTOL()) {
            assign("VSTOL gear or capable", VSTOL);
        }
        
        if (element.isType(AF)) {
            assign("Space capable", SPC);
            assign("Aerospace Fighter Fuel (" + aero.getFuel() + ")", FUEL, (int) Math.round(0.05 * aero.getFuel()));
        }
    }

    @Override
    protected void processATMO() {
        if (!element.isType(AF)) {
            assign("CF / Fixed Wing Support", ATMO);
        }
    }
}