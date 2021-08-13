/*
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

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.SmokeCloud;

/**
 * @author FogHat
 */
public class SRMSmokeWarheadHandler extends SRMHandler {

    /**
     *
     */

    private static final long serialVersionUID = -40939686257250837L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public SRMSmokeWarheadHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.WeaponHandler#specialResolution(java.util.Vector,
     * megamek.common.Entity, boolean)
     */
    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport,
            Entity entityTarget) {
        Coords coords = target.getPosition();
        Coords center = coords;

        AmmoType atype = (AmmoType) ammo.getType();

        if (!bMissed) {
            Report r = new Report(3190);
            r.subject = subjectId;
            r.player = ae.getOwnerId();
            r.add(coords.getBoardNum());
            vPhaseReport.addElement(r);
        } else {
            // scatterable SRMs scatter like dive bombs
            coords = Compute.scatter(coords, 1);
            if (game.getBoard().contains(coords)) {
                // misses and scatters to another hex
                Report r = new Report(3195);
                r.subject = subjectId;
                r.add(coords.getBoardNum());
                vPhaseReport.addElement(r);
            } else {
                // misses and scatters off-board
                Report r = new Report(3200);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                return !bMissed;
            }
        }

        // Handle munitions.
        if (atype.getMunitionType() == AmmoType.M_SMOKE_WARHEAD) {
            int damage = wtype.getRackSize() * calcDamagePerHit();
            int smokeType = SmokeCloud.SMOKE_LIGHT;
            if (damage > 5) {
                smokeType = SmokeCloud.SMOKE_HEAVY;
            }

            server.deliverMissileSmoke(center, smokeType, vPhaseReport);
        } else if (atype.getMunitionType() == AmmoType.M_ANTI_TSM) {
            server.deliverMissileSmoke(center, SmokeCloud.SMOKE_GREEN, vPhaseReport);
        }
        return true;
    }
}
