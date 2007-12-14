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

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

public class PlasmaRifleHandler extends AmmoWeaponHandler {
    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public PlasmaRifleHandler(ToHitData toHit, WeaponAttackAction waa, IGame g,
            Server s) {
        super(toHit, waa, g, s);
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common.Entity, java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int nDamPerHit, int bldgAbsorbs) {
        if (entityTarget instanceof Mech) {
            if (!bSalvo) {
                //hits
                r = new Report(3390);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            }
            r = new Report(3400);
            r.subject = subjectId;
            r.indent(2);
            r.choose(true);
            int extraHeat = Compute.d6();
            r.add(extraHeat);
            r.newlines = 0;
            vPhaseReport.addElement(r);
            entityTarget.heatBuildup += extraHeat;
        }
        super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                nCluster, nDamPerHit, bldgAbsorbs);
    }
    
    /*
     * (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        if (target instanceof Mech) {
            return 10;
        } else {
            return 1;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    protected int calcnCluster() {
        if (target instanceof Mech) {
            bSalvo = false;
            return 1;
        } else {
            if (target instanceof Infantry && !(target instanceof BattleArmor))
                return Compute.d6(2);
            bSalvo = true;
            return 5;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor)ae).getShootingStrength();
            }
            return 1;
        }
        int toReturn;
        // against mechs, 1 hit with 10 damage, plus heat
        if (target instanceof Mech) {
            toReturn = 1;
        // otherwise, 10+2d6 damage
        // but fireresistant BA armor gets no damage from heat, and half the normal one, so only 5 damage 
        } else {
            if (target instanceof BattleArmor && ((BattleArmor)target).hasFireresistantArmor())
                toReturn = 10/2;
            toReturn = 10+Compute.d6(2);
        }
        return toReturn;
    }
}
