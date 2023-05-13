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

import java.util.HashMap;

import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASMekSpecialAbilityConverter extends ASSpecialAbilityConverter {

    private final Mech mek = (Mech) entity;

    /**
     * Do not call this directly. Use ASSpecialAbilityConverter.getConverter instead.
     * Constructs a special ability converter for Mek units.
     *
     * @param entity The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report The calculation report to write to
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

        String cockpitName = Mech.getCockpitDisplayString(mek.getCockpitType());
        switch (mek.getCockpitType()) {
            case Mech.COCKPIT_INTERFACE:
                assign(cockpitName, DN);
                break;
            case Mech.COCKPIT_COMMAND_CONSOLE:
            case Mech.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE:
            case Mech.COCKPIT_SMALL_COMMAND_CONSOLE:
                assign(cockpitName, MHQ, 1);
                break;
            case Mech.COCKPIT_VRRP:
                assign(cockpitName, VR);
                break;
        }

        if (mek.isIndustrial()) {
            if (mek.getCockpitType() == Mech.COCKPIT_STANDARD) {
                assign(cockpitName, AFC);
            } else {
                assign(cockpitName, BFC);
            }
        } else {
            assign("BattleMek", SRCH);
        }

        if (entity instanceof LandAirMech) {
            LandAirMech lam = (LandAirMech) entity;
            double bombs = entity.countWorkingMisc(MiscType.F_BOMB_BAY);
            int bombValue = ASConverter.roundUp(bombs / 5);
            if (bombValue > 0) {
                assign("LAM with Bombs", BOMB, bombValue);
            }
            assign("LAM Fuel (" + lam.getFuel() + ")", FUEL, (int) Math.round(0.05 * lam.getFuel()));
            var lamMoves = new HashMap<String, Integer>();
            if (lam.getLAMType() == LandAirMech.LAM_BIMODAL) {
                lamMoves.put("a", lam.getCurrentThrust());
                report.addLine("Bimodal Movement", "BIM");
                element.getSpecialAbilities().replaceSUA(BIM, lamMoves);
            } else {
                lamMoves.put("g", lam.getAirMechCruiseMP(false, false) * 2);
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
    protected void processSEALandSOA(Mounted misc) {
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
