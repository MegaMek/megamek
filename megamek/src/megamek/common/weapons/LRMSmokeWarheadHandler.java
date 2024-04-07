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

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.GameManager;
import megamek.server.SmokeCloud;

import java.util.Vector;

/**
 * @author FogHat
 */
public class LRMSmokeWarheadHandler extends LRMHandler {
    private static final long serialVersionUID = -30934685350251837L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public LRMSmokeWarheadHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.WeaponHandler#specialResolution(java.util.Vector,
     * megamek.common.Entity, boolean)
     */
    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
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
            // scatterable LRMs scatter like dive bombing
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
        if (atype.getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD)) {
            int damage = wtype.getRackSize() * calcDamagePerHit();
            int smokeType = SmokeCloud.SMOKE_LIGHT;
            if (damage > 5) {
                smokeType = SmokeCloud.SMOKE_HEAVY;
            }

            gameManager.deliverMissileSmoke(center, smokeType, vPhaseReport);
        } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_ANTI_TSM)) {
            gameManager.deliverMissileSmoke(center, SmokeCloud.SMOKE_GREEN, vPhaseReport);
            return false;
        }
        return true;
    }
}
