/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.handlers;

import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.Terrains;
import megamek.server.SmokeCloud;
import megamek.server.totalwarfare.TWGameManager;

public class RapidfireHVACWeaponHandler extends RapidfireACWeaponHandler {
    private static final long serialVersionUID = 7326881584091651519L;

    public RapidfireHVACWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.handlers.WeaponHandler#handle(megamek.common.game.Game.Phase,
     * java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE)
              && !conditions.getAtmosphere().isVacuum()) {
            int rear = (ae.getFacing() + 3 + (weapon.isMekTurretMounted() ? weapon.getFacing() : 0)) % 6;
            Coords src = ae.getPosition();
            Coords rearCoords = src.translated(rear);
            Board board = game.getBoard(ae);
            Hex currentHex = board.getHex(src);

            if (!board.contains(rearCoords)) {
                rearCoords = src;
            } else if (board.getHex(rearCoords).getLevel() > currentHex.getLevel() + 4) {
                rearCoords = src;
            } else if ((board.getBuildingAt(rearCoords) != null)
                  && (board.getHex(rearCoords).terrainLevel(
                  Terrains.BLDG_ELEV)
                  + board.getHex(rearCoords).getLevel() > currentHex
                  .getLevel() + 4)) {
                rearCoords = src;
            }

            gameManager.createSmoke(rearCoords, board, SmokeCloud.SMOKE_HEAVY, 2);
        }
        return super.handle(phase, vPhaseReport);
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (doAmmoFeedProblemCheck(vPhaseReport)) {
            return true;
        }

        if ((roll.getIntValue() == 2) && !ae.isConventionalInfantry()) {
            Report r = new Report(3162);
            r.subject = subjectId;
            weapon.setJammed(true);
            weapon.setHit(true);
            int wloc = weapon.getLocation();
            for (int i = 0; i < ae.getNumberOfCriticals(wloc); i++) {
                CriticalSlot slot1 = ae.getCritical(wloc, i);
                if ((slot1 == null) ||
                      (slot1.getType() == CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }
                Mounted<?> mounted = slot1.getMount();
                if (mounted.equals(weapon)) {
                    ae.hitAllCriticals(wloc, i);
                    break;
                }
            }
            vPhaseReport.addAll(gameManager.explodeEquipment(ae, wloc, weapon));
            r.choose(false);
            vPhaseReport.addElement(r);
            return false;
        } else {
            return super.doChecks(vPhaseReport);
        }
    }
}
