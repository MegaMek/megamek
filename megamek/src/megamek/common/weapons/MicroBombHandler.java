/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class MicroBombHandler extends AmmoWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = -2995118961278208244L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public MicroBombHandler(ToHitData toHit, WeaponAttackAction waa, IGame g,
            Server s) {
        super(toHit, waa, g, s);
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
        if (!bMissed) {
            Report r = new Report(3190);
            r.subject = subjectId;
            r.add(coords.getBoardNum());
            vPhaseReport.add(r);
        } else {
            int moF = -toHit.getMoS();
            if (ae.getCrew().getOptions().booleanOption("golden_goose")) {
                if ((-toHit.getMoS() -2) < 1) {
                    moF = 0;
                } else {
                    moF = -toHit.getMoS() -2;
                }
            }
            coords = Compute.scatterDiveBombs(coords, moF);
            if (game.getBoard().contains(coords)) {
                Report r = new Report(3195);
                r.subject = subjectId;
                r.add(coords.getBoardNum());
                vPhaseReport.add(r);
            } else {
                Report r = new Report(3200);
                r.subject = subjectId;
                vPhaseReport.add(r);
                return !bMissed;
            }
        }
        Infantry ba = (Infantry) ae;
        int ratedDamage = ba.getShootingStrength();
        server.artilleryDamageArea(coords, ae.getPosition(),
                (AmmoType) ammo.getType(), subjectId, ae, ratedDamage * 2,
                ratedDamage, false, 0, vPhaseReport, false);
        return true;
    }
}