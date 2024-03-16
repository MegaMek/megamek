/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.GameManager;

import java.util.Vector;

/**
 * @author Sebastian Brocks
 */
public class SRMFragHandler extends SRMHandler {
    private static final long serialVersionUID = -2281133981582906299L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public SRMFragHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
        sSalvoType = " fragmentation missile(s) ";
        damageType = DamageType.FRAGMENTATION;
    }

    /**
     * Calculate the damage per hit.
     * 
     * @return an <code>int</code> representing the damage dealt per hit.
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = 2;
        // during a swarm, all damage gets applied as one block to one location
        if ((ae instanceof BattleArmor)
                && (weapon.getLocation() == BattleArmor.LOC_SQUAD)
                && !(weapon.isSquadSupportWeapon())
                && (ae.getSwarmTargetId() == target.getId())) {
            toReturn *= ((BattleArmor) ae).getShootingStrength();
        }
        // against infantry, we have 1 hit
        if (target.isConventionalInfantry()) {
            toReturn *= wtype.getRackSize();
            if (bDirect) {
                toReturn += toHit.getMoS() / 3.0;
            }
            
            toReturn = applyGlancingBlowModifier(toReturn, true);
        }

        if ((target instanceof Entity) && !target.isConventionalInfantry()) {
            toReturn = 0;
        }
        return (int) Math.ceil(toReturn);
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport, Building bldg, int nDamage) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        // report that damage was "applied" to terrain

        // Fragmentation does double damage to woods
        nDamage *= 2;

        Report r = new Report(3385);
        r.indent(2);
        r.subject = subjectId;
        r.add(nDamage);
        vPhaseReport.addElement(r);

        // Any clear attempt can result in accidental ignition, even
        // weapons that can't normally start fires. that's weird.
        // Buildings can't be accidentally ignited.
        if ((bldg != null)
                && gameManager.tryIgniteHex(target.getPosition(), subjectId, false,
                        false,
                        new TargetRoll(wtype.getFireTN(), wtype.getName()), 5,
                        vPhaseReport)) {
            return;
        }

        Vector<Report> clearReports = gameManager.tryClearHex(target.getPosition(), nDamage, subjectId);
        if (!clearReports.isEmpty()) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
    }

    @Override
    protected void handleBuildingDamage(Vector<Report> vPhaseReport, Building bldg, int nDamage,
                                        Coords coords) {
    }
}
