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

import java.util.HashMap;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.Entity;
import megamek.common.LandAirMek;
import megamek.common.MPCalculationSetting;
import megamek.common.Mek;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.QuadVee;
import megamek.common.alphaStrike.AlphaStrikeElement;

public class ASMekSpecialAbilityConverter extends ASSpecialAbilityConverter {

    private final Mek mek = (Mek) entity;

    /**
     * Do not call this directly. Use ASSpecialAbilityConverter.getConverter instead. Constructs a special ability
     * converter for Mek units.
     *
     * @param entity  The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report  The calculation report to write to
     */
    protected ASMekSpecialAbilityConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
    }

    @Override
    protected void processUnitFeatures() {
        super.processUnitFeatures();

        if (entity.isOmni()) {
            assign("Omni Unit", OMNI);
        }

        String cockpitName = Mek.getCockpitDisplayString(mek.getCockpitType());
        switch (mek.getCockpitType()) {
            case Mek.COCKPIT_INTERFACE:
                assign(cockpitName, DN);
                break;
            case Mek.COCKPIT_COMMAND_CONSOLE:
            case Mek.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE:
            case Mek.COCKPIT_SMALL_COMMAND_CONSOLE:
                assign(cockpitName, MHQ, 1);
                break;
            case Mek.COCKPIT_VRRP:
                assign(cockpitName, VR);
                break;
        }

        if (mek.isIndustrial()) {
            if (mek.hasAdvancedFireControl()) {
                assign(cockpitName, AFC);
            } else {
                assign(cockpitName, BFC);
            }
        } else {
            assign("BattleMek", SRCH);
        }

        if (entity instanceof LandAirMek) {
            LandAirMek lam = (LandAirMek) entity;
            double bombs = entity.countWorkingMisc(MiscType.F_BOMB_BAY);
            int bombValue = ASConverter.roundUp(bombs / 5);
            if (bombValue > 0) {
                assign("LAM with Bombs", BOMB, bombValue);
            }
            assign("LAM Fuel (" + lam.getFuel() + ")", FUEL, (int) Math.round(0.05 * lam.getFuel()));
            var lamMoves = new HashMap<String, Integer>();
            if (lam.getLAMType() == LandAirMek.LAM_BIMODAL) {
                lamMoves.put("a", lam.getCurrentThrust());
                report.addLine("Bimodal Movement", "BIM");
                element.getSpecialAbilities().replaceSUA(BIM, lamMoves);
            } else {
                lamMoves.put("g", lam.getAirMekCruiseMP(MPCalculationSetting.AS_CONVERSION) * 2);
                lamMoves.put("a", lam.getCurrentThrust());
                report.addLine("LAM Movement", "LAM");
                element.getSpecialAbilities().replaceSUA(LAM, lamMoves);
            }
            element.getMovement().putAll(lamMoves);
        }

        if (entity instanceof QuadVee) {
            assign("QuadVee", QV);
        }

        if (element.isBattleMek()) {
            assign("BattleMek", SOA);
        }

        if (entity.getWeight() > 100) {
            assign("Superheavy Mek", LG);
        }
    }

    @Override
    protected void processSEALandSOA(Mounted<?> misc) {
        if (mek.isIndustrial()) {
            MiscType miscType = (MiscType) misc.getType();
            if (miscType.hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                assign(misc, SEAL);
                if (hasSoaCapableEngine()) {
                    assign("SEAL and no ICE", SOA);
                }
            }
        }
    }
}
