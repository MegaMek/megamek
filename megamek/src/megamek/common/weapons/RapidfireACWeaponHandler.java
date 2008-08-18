/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 29, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Andrew Hunter
 */
public class RapidfireACWeaponHandler extends UltraWeaponHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -1770392652874842106L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public RapidfireACWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }


    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage();
        // during a swarm, all damage gets applied as one block to one
        // location
        if (ae instanceof BattleArmor && weapon.getLocation() == BattleArmor.LOC_SQUAD && (ae.getSwarmTargetId() == target.getTargetId())) {
            toReturn *= ((BattleArmor) ae).getShootingStrength();
        }
        // we default to direct fire weapons for anti-infantry damage
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            toReturn = Compute.directBlowInfantryDamage(toReturn, bDirect ? toHit.getMoS()/3 : 0, Compute.WEAPON_DIRECT_FIRE);
        }
       /* if (bGlancing) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }*/
        
        if (game.getOptions().booleanOption("tacops_range") && nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG]) {
            toReturn = (int) Math.floor(toReturn * .75);
        }

        return (int) toReturn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.UltraWeaponHandler#doChecks(java.util.Vector)
     */
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (roll <= 4 && howManyShots == 2) {
            if (roll > 2) {
                r = new Report(3161);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
                weapon.setJammed(true);
                weapon.setHit(true);
            } else {
                r = new Report(3162);
                r.subject = subjectId;
                weapon.setJammed(true);
                weapon.setHit(true);
                int wlocation = weapon.getLocation();
                weapon.setDestroyed(true);
                for (int i = 0; i < ae.getNumberOfCriticals(wlocation); i++) {
                    CriticalSlot slot1 = ae.getCritical(wlocation, i);
                    if (slot1 == null
                            || slot1.getType() != CriticalSlot.TYPE_SYSTEM) {
                        continue;
                    }
                    Mounted mounted = ae.getEquipment(slot1.getIndex());
                    if (mounted.equals(weapon)) {
                        ae.hitAllCriticals(wlocation, i);
                        break;
                    }
                }
                r.choose(false);
                vPhaseReport.addElement(r);
                vPhaseReport.addAll(server.damageEntity(ae, new HitData(
                        wlocation), wtype.getDamage(), false, DamageType.NONE,
                        true));
            }
            return false;
        }
        return false;
    }
    
    protected boolean usesClusterTable() {
        return true;
    }
}
