/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.*;
import megamek.common.actions.PhysicalAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.server.gameManager.*;

/**
 * @author Andrew Hunter
 * Created on Oct 15, 2004
 */
public class CLLBXPrototypeHandler extends LBXHandler {
    private static final long serialVersionUID = -7348571086193319403L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public CLLBXPrototypeHandler(ToHitData t, WeaponAttackAction w, Game g,
                                 GameManager m) {
        super(t, w, g, m);
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
        if (target.isConventionalInfantry()) {
            return 1;
        }

        int shotsHit;
        int nHitsModifier = getClusterModifiers(true);

        if (allShotsHit()) {
            shotsHit = wtype.getRackSize();
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)
                && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG])) {
                shotsHit = (int) Math.ceil(shotsHit * .75);
            }
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE)
                    && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
                shotsHit = (int) Math.ceil(shotsHit * .5);
            }            
        } else {
            // flat modifier of -1, because of prototype
            nHitsModifier -= 1;
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            shotsHit = Compute.missilesHit(wtype.getRackSize(), nHitsModifier, conditions.getEMI().isEMI());
        }

        Report r = new Report(3325);
        r.subject = subjectId;
        r.add(shotsHit);
        r.add(sSalvoType);
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
