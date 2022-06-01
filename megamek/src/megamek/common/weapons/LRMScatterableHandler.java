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
import megamek.common.Game;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.GameManager;

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
     * @param m
     */
    public LRMScatterableHandler(ToHitData t, WeaponAttackAction w, Game g,
            GameManager m) {
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
    protected boolean specialResolution(Vector<Report> vPhaseReport,
            Entity entityTarget) {
        Coords coords = target.getPosition();
        AmmoType atype = (AmmoType) ammo.getType();
        long amType = atype.getMunitionType();
        boolean mineDelivery = amType == AmmoType.M_THUNDER
                || amType == AmmoType.M_THUNDER_ACTIVE
                || amType == AmmoType.M_THUNDER_AUGMENTED
                || amType == AmmoType.M_THUNDER_INFERNO
                || amType == AmmoType.M_THUNDER_VIBRABOMB;
        int whoReport = Report.PUBLIC;
        // only report to player if mine delivery
        if (mineDelivery) {
            whoReport = Report.HIDDEN;
        }
        int density = atype.getRackSize();
        if (amType == AmmoType.M_THUNDER_AUGMENTED) {
            density = density / 2 + density % 2;
        }
        if (!bMissed) {
            Report r = new Report(3190, whoReport);
            r.subject = subjectId;
            r.player = ae.getOwnerId();
            r.add(coords.getBoardNum());
            vPhaseReport.addElement(r);
        } else {
            // Per TacOps errata 3.4, thunder munitions scatter like artillery,
            // i.e. by MoF; for simplicity's sake, we'll for now treat other
            // LRM attacks using this handler the same.
            coords = Compute.scatter(coords, -toHit.getMoS());
            if (mineDelivery) {
                density -= 5;
                // If density drops to 0 or less, we're done here.
                if (density <= 0) {
                    Report r = new Report(3198, whoReport);
                    r.subject = subjectId;
                    r.player = ae.getOwnerId();
                    vPhaseReport.addElement(r);
                    return true;
                }
            }
            if (game.getBoard().contains(coords)) {
                // misses and scatters to another hex
                int reportNr = mineDelivery ? 3197 : 3195;
                Report r = new Report(reportNr, whoReport);
                r.subject = subjectId;
                r.player = ae.getOwnerId();
                r.add(coords.getBoardNum());
                vPhaseReport.addElement(r);
            } else {
                // misses and scatters off-board
                Report r = new Report(3200);
                r.subject = subjectId;
                r.player = ae.getOwnerId();
                vPhaseReport.addElement(r);
                return true;
            }
        }

        // Handle the thunder munitions.
        if (atype.getMunitionType() == AmmoType.M_THUNDER_AUGMENTED) {
            gameManager.deliverThunderAugMinefield(coords, ae.getOwner().getId(),
                    density, ae.getId());
        } else if (atype.getMunitionType() == AmmoType.M_THUNDER) {
            gameManager.deliverThunderMinefield(coords, ae.getOwner().getId(),
                    density, ae.getId());
        } else if (atype.getMunitionType() == AmmoType.M_THUNDER_INFERNO) {
            gameManager.deliverThunderInfernoMinefield(coords, ae.getOwner().getId(),
                    density, ae.getId());
        } else if (atype.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB) {
            gameManager.deliverThunderVibraMinefield(coords, ae.getOwner().getId(),
                    density, waa.getOtherAttackInfo(), ae.getId());
        } else if (atype.getMunitionType() == AmmoType.M_THUNDER_ACTIVE) {
            gameManager.deliverThunderActiveMinefield(coords, ae.getOwner().getId(),
                    density, ae.getId());
        }
        return true;
    }
}
