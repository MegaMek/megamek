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

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 * 
 */
public class InfantryAttackHandler extends WeaponHandler {
    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public InfantryAttackHandler(ToHitData toHit, WeaponAttackAction waa,
            IGame g, Server s) {
        super(toHit, waa, g, s);
    }

    protected void handleEntityDamage(Entity entityTarget, Vector vPhaseReport,
            Building bldg, int hits, int nCluster, int nDamPerHit,
            int bldgAbsorbs) {
        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(), toHit
                .getSideTable(), waa.getAimedLocation(), waa.getAimingMode());
        // Do criticals.
        Vector specialDamageReport = server.criticalEntity(entityTarget, hit
                .getLocation());

        // Replace "no effect" results with 4 points of damage.
        if (((Report) specialDamageReport.lastElement()).messageId == 6005) {
            int damage = 4;
            if (ae instanceof BattleArmor)
                damage += ((BattleArmor) ae).getVibroClawDamage();
            // ASSUMPTION: buildings CAN'T absorb *this* damage.
            // specialDamage = damageEntity(entityTarget, hit, damage);
            specialDamageReport = server.damageEntity(entityTarget, hit,
                    damage, false, 0, false, false, throughFront);
        } else {
            // add newline _before_ last report
            try {
                ((Report) specialDamageReport.elementAt(specialDamageReport
                        .size() - 2)).newlines++;
            } catch (ArrayIndexOutOfBoundsException aiobe) {
                System.err
                        .println("ERROR: no previous report when trying to add newline");
            }
        }
        // Report the result
        Server.combineVectors(vPhaseReport, specialDamageReport);
    }
}
