/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Coords;
import megamek.common.CriticalSlot;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Mounted;
import megamek.common.PlanetaryConditions;
import megamek.common.Report;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.SmokeCloud;

public class RapidfireHVACWeaponHandler extends RapidfireACWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = 7326881584091651519L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public RapidfireHVACWeaponHandler(ToHitData t, WeaponAttackAction w,
            IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.WeaponHandler#handle(megamek.common.IGame.Phase,
     * java.util.Vector)
     */
    @Override
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {

        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_START_FIRE)
                && (game.getPlanetaryConditions().getAtmosphere() >= PlanetaryConditions.ATMO_TRACE)) {
            int rear = (ae.getFacing() + 3 + (weapon.isMechTurretMounted() ? weapon
                    .getFacing() : 0)) % 6;
            Coords src = ae.getPosition();
            Coords rearCoords = src.translated(rear);
            IBoard board = game.getBoard();
            IHex currentHex = board.getHex(src);

            if (!board.contains(rearCoords)) {
                rearCoords = src;
            } else if (board.getHex(rearCoords).getLevel() > currentHex
                    .getLevel() + 4) {
                rearCoords = src;
            } else if ((board.getBuildingAt(rearCoords) != null)
                    && (board.getHex(rearCoords).terrainLevel(
                            Terrains.BLDG_ELEV)
                            + board.getHex(rearCoords).getLevel() > currentHex
                            .getLevel() + 4)) {
                rearCoords = src;
            }

            server.createSmoke(rearCoords, SmokeCloud.SMOKE_HEAVY, 2);
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
        if (roll == 2) {
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
            vPhaseReport.addAll(server.explodeEquipment(ae, wloc, weapon));
            r.choose(false);
            vPhaseReport.addElement(r);
            return false;
        } else {
            return super.doChecks(vPhaseReport);
        }
    }

}