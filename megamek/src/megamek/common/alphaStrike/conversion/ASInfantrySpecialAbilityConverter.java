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

import static megamek.common.alphaStrike.BattleForceSUA.*;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;

public class ASInfantrySpecialAbilityConverter extends ASSpecialAbilityConverter {

    private final Infantry infantry = (Infantry) entity;

    /**
     * Do not call this directly. Use ASSpecialAbilityConverter.getConverter instead. Constructs a special ability
     * converter for fighter-type Aero units (AF, CF, Aero SV).
     *
     * @param entity  The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report  The calculation report to write to
     */
    protected ASInfantrySpecialAbilityConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
    }

    @Override
    protected void processENE() {
    }

    @Override
    protected void processMiscMounted(Mounted<?> misc) {
        super.processMiscMounted(misc);

        if (entity instanceof BattleArmor) {
            if (misc.getType().hasFlag(MiscType.F_VISUAL_CAMO)
                  && !misc.getType().getName().equals(BattleArmor.MIMETIC_ARMOR)) {
                assign("Visual Camo, not Mimetic", LMAS);
            } else if (misc.getType().hasFlag(MiscType.F_TOOLS)
                  && misc.getType().hasFlag(MiscTypeFlag.S_MINESWEEPER)) {
                assign("Minesweeper", MSW);
            } else if (misc.getType().hasFlag(MiscType.F_PARAFOIL)) {
                assign(misc, PAR);
            } else if (misc.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                assign(misc, XMEC);
            }
        }
    }

    @Override
    protected void processSEALandSOA(Mounted<?> misc) {
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
        // CHECKSTYLE IGNORE ForbiddenWords FOR 2 LINES
        if ((entity instanceof BattleArmor) && ((BattleArmor) entity).canDoMechanizedBA()) {
            assign("BA / Mech.", MEC);
        }

    }
}
