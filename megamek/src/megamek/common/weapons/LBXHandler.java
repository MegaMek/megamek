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

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class LBXHandler extends AmmoWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = 6803847280685526644L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public LBXHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
        sSalvoType = " pellet(s) ";
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            double toReturn = Compute.directBlowInfantryDamage(
                    wtype.getDamage(), bDirect ? toHit.getMoS() / 3 : 0,
                    WeaponType.WEAPON_CLUSTER_BALLISTIC,
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);
            if (bGlancing) {
                toReturn /= 2;
            }
            return (int) Math.floor(toReturn);
        }
        return 1;
    }

    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        int av = super.calcAttackValue();
        if (usesClusterTable()) {
            // basically 60% of normal
            return (int) Math.floor(0.6 * av);
        }
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        if (bGlancing) {
            av = (int) Math.floor(av / 2.0);

        }
        av = (int) Math.floor(getBracketingMultiplier() * av);
        return av;
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

            shotsHit = Compute.missilesHit(wtype.getRackSize(), nHitsModifier,
                                           game.getPlanetaryConditions().hasEMI());
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

    @Override
    protected boolean usesClusterTable() {
        return ((AmmoType) ammo.getType()).getMunitionType() == AmmoType.M_CLUSTER;
    }

    @Override
    protected boolean canDoDirectBlowDamage() {
        return false;
    }

}
