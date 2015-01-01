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

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class LRMScatterableHandler extends MissileWeaponHandler {

    /**
     * 
     */
    private static final long serialVersionUID = -3661776853552779877L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public LRMScatterableHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#specialResolution(java.util.Vector,
     *      megamek.common.Entity, boolean)
     */
    protected boolean specialResolution(Vector<Report> vPhaseReport,
            Entity entityTarget, boolean bMissed) {
        Coords coords = target.getPosition();
        AmmoType atype = (AmmoType) ammo.getType();
        //only report to player if mine delivery
        int whoReport = Report.PLAYER;
        if(atype.getMunitionType() == AmmoType.M_FLARE) {
            whoReport = Report.PUBLIC;
        }    
        if (!bMissed) {
            r = new Report(3190, whoReport);
            r.subject = subjectId;
            r.add(coords.getBoardNum());
            vPhaseReport.addElement(r);
        } else {
            // according to http://www.classicbattletech.com/forums/index.php/topic,41188.0.html
            // scatterable LRMs scatter like dive bombing
            coords = Compute.scatterDiveBombs(coords);
            if (game.getBoard().contains(coords)) {
                // misses and scatters to another hex
                r = new Report(3195, whoReport);
                r.subject = subjectId;
                r.add(coords.getBoardNum());
                vPhaseReport.addElement(r);
            } else {
                // misses and scatters off-board
                r = new Report(3200);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                return !bMissed;
            }
        }

        // Handle the thunder munitions.
        if (atype.getMunitionType() == AmmoType.M_THUNDER_AUGMENTED) {
            server.deliverThunderAugMinefield(coords, ae.getOwner().getId(),
                    atype.getRackSize(), ae.getId());
        } else if (atype.getMunitionType() == AmmoType.M_THUNDER) {
            server.deliverThunderMinefield(coords, ae.getOwner().getId(), atype
                    .getRackSize(), ae.getId());
        } else if (atype.getMunitionType() == AmmoType.M_THUNDER_INFERNO) {
            server.deliverThunderInfernoMinefield(coords,
                    ae.getOwner().getId(), atype.getRackSize(), ae.getId());
        } else if (atype.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB) {
            server.deliverThunderVibraMinefield(coords, ae.getOwner().getId(),
                    atype.getRackSize(), waa.getOtherAttackInfo(), ae.getId());
        } else if (atype.getMunitionType() == AmmoType.M_THUNDER_ACTIVE) {
            server.deliverThunderActiveMinefield(coords, ae.getOwner().getId(),
                    atype.getRackSize(), ae.getId());
        }
        return true;
    }
}
