/*
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class HAGWeaponHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = -8193801876308832102L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public HAGWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
        salvoType = " projectiles ";
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    @Override
    protected int calcnCluster() {
        return 5;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if (target.isConventionalInfantry()) {
            double toReturn = weaponType.getRackSize();
            toReturn = Compute.directBlowInfantryDamage(
                    toReturn, bDirect ? toHit.getMoS() / 3 : 0,
                    weaponType.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, attackerEntity.getId(), calcDmgPerHitReport);
            toReturn = applyGlancingBlowModifier(toReturn, true);
            return (int) toReturn;
        }
        return 1;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs can't mount HAGs
        if (target.isConventionalInfantry()) {
            return 1;
        }
        int nHits;
        int nHitsModifier = getClusterModifiers(true);
        if (nRange <= weaponType.getShortRange()) {
            nHitsModifier += 2;
        } else if (nRange > weaponType.getMediumRange()) {
            nHitsModifier -= 2;
        }

        if (allShotsHit()) {
            nHits = weaponType.getRackSize();
        } else {
            nHits = Compute.missilesHit(weaponType.getRackSize(), nHitsModifier);
        }
        Report r = new Report(3325);
        r.subject = subjectId;
        r.add(nHits);
        r.add(salvoType);
        r.newlines = 0;
        r.add(toHit.getTableDesc());
        vPhaseReport.addElement(r);
        if (nHitsModifier != 0) {
            r = new Report(3340);
            if (nHitsModifier < 0) {
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
        return nHits;
    }

    @Override
    protected boolean usesClusterTable() {
        return true;
    }
}
