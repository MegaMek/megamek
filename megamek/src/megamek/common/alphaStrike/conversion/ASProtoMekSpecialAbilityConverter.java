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

import static megamek.common.alphaStrike.BattleForceSUA.*;
import static megamek.common.alphaStrike.BattleForceSUA.GLD;

public class ASProtoMekSpecialAbilityConverter extends ASSpecialAbilityConverter2 {

    /**
     * Do not call this directly. Use ASSpecialAbilityConverter.getConverter instead.
     * Constructs a special ability converter for ProtoMeks (PM).
     *
     * @param entity The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report The calculation report to write to
     */
    protected ASProtoMekSpecialAbilityConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
    }

    @Override
    protected void processMiscMounted(Mounted misc) {
        super.processMiscMounted(misc);

        if (misc.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
            if (entity.getWeight() < 10) {
                assign("Light PM with mag clamps", MCS);
            } else {
                assign("Heavy PM with mag clamps", UCS);
            }
        }
    }

    @Override
    protected void processSEALandSOA(Mounted misc) {
        super.processSEALandSOA(misc);

        assign("ProtoMek", SOA);
    }

    @Override
    protected void processUnitFeatures() {
        super.processUnitFeatures();

        if (entity.getMovementMode().equals(EntityMovementMode.WIGE)) {
            assign("Glider ProtoMek", GLD);
        }
    }
}
