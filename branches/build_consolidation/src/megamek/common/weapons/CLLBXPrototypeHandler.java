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
 * Created on Oct 15, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class CLLBXPrototypeHandler extends LBXHandler {

    /**
     *
     */
    private static final long serialVersionUID = -7348571086193319403L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public CLLBXPrototypeHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(Vector<Report>
     * vPhaseReport)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs can't mount LBXs
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            return 1;
        }

        int shotsHit;
        int nHitsModifier = 0;

        if (allShotsHit()) {
            shotsHit = wtype.getRackSize();
            if (game.getOptions().booleanOption("tacops_range")
                    && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
                shotsHit = (int) Math.ceil(shotsHit * .75);
            }
        } else {
            // TacOps Cluster Hit Penalties p. 83
            boolean tacopscluster = game.getOptions().booleanOption(
                    "tacops_clusterhitpen");
            if (tacopscluster) {
                if (nRange <= 1) {
                    nHitsModifier += 1;
                } else if (nRange <= wtype.getMediumRange()) {
                    nHitsModifier += 0;
                } else {
                    nHitsModifier -= 1;
                }
            }
            if (game.getOptions().booleanOption("tacops_range")
                    && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
                nHitsModifier -= 2;
            }

            if (bGlancing) {
                nHitsModifier -= 4;
            }

            if (bDirect) {
                nHitsModifier += (toHit.getMoS() / 3) * 2;
            }

            if (game.getPlanetaryConditions().hasEMI()) {
                nHitsModifier -= 2;
            }

            // flat modifier of -1, because of prototype
            nHitsModifier -= 1;

            shotsHit = Compute.missilesHit(wtype.getRackSize(), nHitsModifier,
                    game.getPlanetaryConditions().hasEMI());
        }

        Report r = new Report(3325);
        r.subject = subjectId;
        r.add(shotsHit);
        r.add(" pellet(s) ");
        r.add(toHit.getTableDesc());
        r.newlines = 0;
        vPhaseReport.addElement(r);
        if (nHitsModifier != 0) {
            if (nHitsModifier > 0) {
                r = new Report(3340);
            } else {
                r = new Report(3341);
            }
            r.subject = subjectId;
            r.add(nHitsModifier);
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return shotsHit;
    }

}
