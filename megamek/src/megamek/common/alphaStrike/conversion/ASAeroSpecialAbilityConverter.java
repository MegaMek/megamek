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

package megamek.common.alphaStrike.conversion;

import static megamek.common.alphaStrike.ASUnitType.AF;
import static megamek.common.alphaStrike.BattleForceSUA.ATMO;
import static megamek.common.alphaStrike.BattleForceSUA.BOMB;
import static megamek.common.alphaStrike.BattleForceSUA.FUEL;
import static megamek.common.alphaStrike.BattleForceSUA.MHQ;
import static megamek.common.alphaStrike.BattleForceSUA.SPC;
import static megamek.common.alphaStrike.BattleForceSUA.VSTOL;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.FixedWingSupport;
import megamek.common.alphaStrike.AlphaStrikeElement;

public class ASAeroSpecialAbilityConverter extends ASSpecialAbilityConverter {

    private final Aero aero = (Aero) entity;

    /**
     * Do not call this directly. Use ASSpecialAbilityConverter.getConverter instead. Constructs a special ability
     * converter for fighter-type Aero units (AF, CF, Aero SV).
     *
     * @param entity  The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report  The calculation report to write to
     */
    protected ASAeroSpecialAbilityConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
    }

    @Override
    protected void processENE() {
        if (element.getStandardDamage().hasDamage()) {
            super.processENE();
        }
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

        if (aero.isVSTOL() || aero.isSTOL() || element.isType(AF)) {
            assign("VSTOL or STOL gear or capable", VSTOL);
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
