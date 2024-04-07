/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.server.GameManager;
import megamek.server.SmokeCloud;

/**
 * @author Jason Tighe
 */
public class HVACWeaponHandler extends ACWeaponHandler {
    private static final long serialVersionUID = 7326881584091651519L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public HVACWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.WeaponHandler#handle(megamek.common.Game.Phase,
     * java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_START_FIRE)
                && !conditions.getAtmosphere().isVacuum()) {
            int rear = (ae.getFacing() + 3 + (weapon.isMechTurretMounted() ? weapon.getFacing() : 0)) % 6;
            Coords src = ae.getPosition();
            Coords rearCoords = src.translated(rear);
            Board board = game.getBoard();
            Hex currentHex = board.getHex(src);

            if (!board.contains(rearCoords)) {
                rearCoords = src;
            } else if (board.getHex(rearCoords).getLevel() > currentHex.getLevel()) {
                rearCoords = src;
            } else if ((board.getBuildingAt(rearCoords) != null)
                    && ((board.getHex(rearCoords).terrainLevel(
                            Terrains.BLDG_ELEV) + board.getHex(rearCoords)
                            .getLevel()) > currentHex.getLevel())) {
                rearCoords = src;
            }

            gameManager.createSmoke(rearCoords, SmokeCloud.SMOKE_HEAVY, 2);
        }
        return super.handle(phase, vPhaseReport);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (doAmmoFeedProblemCheck(vPhaseReport)) {
            return true;
        }
        
        if (roll.getIntValue() == 2) {
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
                Mounted mounted = slot1.getMount();
                if (mounted.equals(weapon)) {
                    ae.hitAllCriticals(wloc, i);
                    break;
                }
            }
            vPhaseReport.addAll(gameManager.explodeEquipment(ae, wloc, weapon));
            r.choose(false);
            vPhaseReport.addElement(r);
            return true;
        } else {
            return super.doChecks(vPhaseReport);
        }
    }

}
