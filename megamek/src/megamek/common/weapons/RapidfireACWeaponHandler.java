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

import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Mounted;
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

    private boolean kindRapidFire = false;

    /**
     * @param t
     * @param w
     * @param g
     */
    public RapidfireACWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
    }

    public boolean getKindRapidFire() {
        return kindRapidFire;
    }

    public void setKindRapidFire(boolean kindRapidFire) {
        this.kindRapidFire = kindRapidFire;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.UltraWeaponHandler#doChecks(java.util.Vector)
     */
    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        int jamLevel = 4;
        if (kindRapidFire) {
            jamLevel = 2;
        }
        if ((roll <= jamLevel) && (howManyShots == 2)) {
            if (roll > 2 || kindRapidFire) {
                Report r = new Report(3161);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
                weapon.setJammed(true);
                weapon.setHit(true);
            } else {
                Report r = new Report(3162);
                r.subject = subjectId;
                weapon.setJammed(true);
                weapon.setHit(true);
                int wlocation = weapon.getLocation();
                for (int i = 0; i < ae.getNumberOfCriticals(wlocation); i++) {
                    CriticalSlot slot1 = ae.getCritical(wlocation, i);
                    if ((slot1 == null)
                            || (slot1.getType() != CriticalSlot.TYPE_SYSTEM)) {
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

    @Override
    protected boolean usesClusterTable() {
        return true;
    }

    @Override
    protected boolean canDoDirectBlowDamage() {
        return false;
    }

}
