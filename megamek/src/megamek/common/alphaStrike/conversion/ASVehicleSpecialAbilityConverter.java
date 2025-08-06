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

import static megamek.common.alphaStrike.BattleForceSUA.ATMO;
import static megamek.common.alphaStrike.BattleForceSUA.OMNI;
import static megamek.common.alphaStrike.BattleForceSUA.SEAL;
import static megamek.common.alphaStrike.BattleForceSUA.SOA;
import static megamek.common.alphaStrike.BattleForceSUA.SRCH;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.Entity;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.VTOL;
import megamek.common.alphaStrike.AlphaStrikeElement;

public class ASVehicleSpecialAbilityConverter extends ASSpecialAbilityConverter {

    /**
     * Do not call this directly. Use ASSpecialAbilityConverter.getConverter instead. Constructs a special ability
     * converter for Combat and ground support vehicles.
     *
     * @param entity  The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report  The calculation report to write to
     */
    protected ASVehicleSpecialAbilityConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
    }

    @Override
    protected void processSEALandSOA(Mounted<?> misc) {
        MiscType miscType = (MiscType) misc.getType();
        if (miscType.hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
            assign(misc, SEAL);
            if (hasSoaCapableEngine()) {
                assign("SEAL and no ICE/Steam", SOA);
            }
        }
    }

    @Override
    protected void processUnitFeatures() {
        super.processUnitFeatures();

        if (entity.isOmni()) {
            assign("Omni Unit", OMNI);
        }

        if (!element.isSupportVehicle()) {
            assign("Combat Vehicle", SRCH);
        }
    }

    @Override
    protected void processATMO() {
        if (entity instanceof VTOL) {
            assign("VTOL", ATMO);
        }
    }
}
