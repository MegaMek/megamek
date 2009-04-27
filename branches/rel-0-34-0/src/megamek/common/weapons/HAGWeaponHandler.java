/**
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class HAGWeaponHandler extends AmmoWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = -8193801876308832102L;

    Mounted ammo;

    /**
     * @param t
     * @param w
     * @param g
     */
    public HAGWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);

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
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            double toReturn = wtype.getRackSize();
            toReturn = Compute.directBlowInfantryDamage(toReturn, bDirect ? toHit.getMoS()/3 : 0, Compute.WEAPON_CLUSTER_BALLISTIC, ((Infantry)target).isMechanized());
            if ( bGlancing ) {
                toReturn /=2;
            }
            toReturn = Math.ceil(toReturn);
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
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            return 1;
        }
        int nHits;
        int nHitsModifier = 0;
        if (nRange <= wtype.getShortRange()) {
            nHitsModifier += 2;
        } else if (nRange > wtype.getMediumRange()) {
            nHitsModifier -= 2;
        }

        boolean tacopscluster = game.getOptions().booleanOption("tacops_clusterhitpen");
        if (tacopscluster) {
            if (nRange <= 1) {
                nHitsModifier += 1;
            } else if (nRange <= wtype.getMediumRange()) {
                nHitsModifier += 0;
            } else {
                nHitsModifier -= 1;
            }
        }

        if (game.getOptions().booleanOption("tacops_range") && nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG]) {
            nHitsModifier -= 2;
        }

        if(game.getPlanetaryConditions().hasEMI()) {
            nHitsModifier -= 2;
        }

        if (allShotsHit()) {
            nHits = wtype.getRackSize();
        } else {
            nHits = Compute.missilesHit(wtype.getRackSize(), nHitsModifier);
        }
        r = new Report(3325);
        r.subject = subjectId;
        r.add(nHits);
        r.add(" projectiles ");
        r.add(toHit.getTableDesc());
        r.newlines = 0;
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
        r.newlines = 0;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return nHits;
    }

    @Override
    protected boolean usesClusterTable() {
        return true;
    }

    @Override
    protected boolean canDoDirectBlowDamage(){
        return false;
    }
}
