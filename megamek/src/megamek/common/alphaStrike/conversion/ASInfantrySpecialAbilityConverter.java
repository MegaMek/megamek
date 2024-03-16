/*
 * Copyright (c) 2022 - The MegaMek Teamisc. All Rights Reserved.
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
import megamek.common.options.OptionsConstants;

import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASInfantrySpecialAbilityConverter extends ASSpecialAbilityConverter {

    private final Infantry infantry = (Infantry) entity;

    /**
     * Do not call this directly. Use ASSpecialAbilityConverter.getConverter instead.
     * Constructs a special ability converter for fighter-type Aero units (AF, CF, Aero SV).
     *
     * @param entity The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report The calculation report to write to
     */
    protected ASInfantrySpecialAbilityConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
    }

    @Override
    protected void processENE() { }

    @Override
    protected void processMiscMounted(Mounted misc) {
        super.processMiscMounted(misc);

        if (entity instanceof BattleArmor) {
            if (misc.getType().hasFlag(MiscType.F_VISUAL_CAMO)
                    && !misc.getType().getName().equals(BattleArmor.MIMETIC_ARMOR)) {
                assign("Visual Camo, not Mimetic", LMAS);
            } else if (misc.getType().hasFlag(MiscType.F_TOOLS)
                    && ((misc.getType().getSubType() & MiscType.S_MINESWEEPER) == MiscType.S_MINESWEEPER)) {
                assign("Minesweeper", MSW);
            } else if (misc.getType().hasFlag(MiscType.F_PARAFOIL)) {
                assign(misc, PAR);
            } else if (misc.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                assign(misc, XMEC);
            }
        }
    }

    @Override
    protected void processSEALandSOA(Mounted misc) {
        if (misc.getType().hasFlag(MiscType.F_SPACE_ADAPTATION)) {
            assign(misc, SOA);
        }
    }

    @Override
    protected void processUnitFeatures() {
        super.processUnitFeatures();

        element.getSpecialAbilities().mergeSUA(CAR, (int) Math.ceil(entity.getWeight()));
        
        report.addLine("Infantry transport weight", "CAR" + (int) Math.ceil(entity.getWeight()));
        
        if (entity.getMovementMode().isUMUInfantry()) {
            report.addLine("UMU Gear", "UMU");
            element.getSpecialAbilities().setSUA(UMU);
        }
        
        if (infantry.hasSpecialization(Infantry.FIRE_ENGINEERS)) {
            assign("Fire Engineers", FF);
        }
        if (infantry.hasSpecialization(Infantry.MINE_ENGINEERS)) {
            assign("Mine Engineers", MSW);
        }
        if (infantry.hasSpecialization(Infantry.MOUNTAIN_TROOPS)) {
            assign("Mountain Troops", MTN);
        }
        if (infantry.hasSpecialization(Infantry.PARATROOPS)) {
            assign("Paratroopers", PAR);
        }
        if (infantry.hasSpecialization(Infantry.SCUBA)) {
            assign("Scuba Gear", UMU);
        }
        if (infantry.hasSpecialization(Infantry.TRENCH_ENGINEERS)) {
            assign("Trench Engineers", TRN);
        }
        
        if (entity.hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
            assign("TSM implants", TSI);
        }
        
        if ((entity instanceof BattleArmor) && ((BattleArmor) entity).canDoMechanizedBA()) {
            assign("BA / Mech.", MEC);
        }
    }
}