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

import megamek.common.Building;
import megamek.common.Entity;
import megamek.common.EquipmentMode;
import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.TagInfo;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

public class TAGHandler extends WeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = -967656770476044773L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public TAGHandler(ToHitData toHit, WeaponAttackAction waa, IGame g, Server s) {
        super(toHit, waa, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common
     * .Entity, java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        if (entityTarget == null) {
            Report r = new Report(3187);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        } else {
            int priority = 1;
            EquipmentMode mode = (weapon.curMode());
            if (mode != null) {
                if (mode.getName() == "1-shot") {
                    priority = 1;
                } else if (mode.getName() == "2-shot") {
                    priority = 2;
                } else if (mode.getName() == "3-shot") {
                    priority = 3;
                } else if (mode.getName() == "4-shot") {
                    priority = 4;
                }
            }
            if (priority < 1) {
                priority = 1;
            }
            // it is possible for 2 or more tags to hit the same entity
            TagInfo info = new TagInfo(ae.getId(), Targetable.TYPE_ENTITY,
                    entityTarget, priority, false);
            game.addTagInfo(info);
            entityTarget.setTaggedBy(ae.getId());
            Report r = new Report(3188);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.WeaponHandler#handleSpecialMiss(megamek.common
     * .Entity, boolean, megamek.common.Building, java.util.Vector)
     */
    @Override
    protected boolean handleSpecialMiss(Entity entityTarget,
            boolean bldgDamagedOnMiss, Building bldg,
            Vector<Report> vPhaseReport) {
        int priority = 1;
        EquipmentMode mode = (weapon.curMode());
        if (mode != null) {
            if (mode.getName().equals("1-shot")) {
                priority = 1;
            } else if (mode.getName().equals("2-shot")) {
                priority = 2;
            } else if (mode.getName().equals("3-shot")) {
                priority = 3;
            } else if (mode.getName().equals("4-shot")) {
                priority = 4;
            }
        }
        // add even misses, as they waste homing missiles.
        TagInfo info = new TagInfo(ae.getId(), target.getTargetType(), 
                target, priority, true);
        game.addTagInfo(info);
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.AttackHandler#cares(int)
     */
    @Override
    public boolean cares(IGame.Phase phase) {
        if (phase == IGame.Phase.PHASE_OFFBOARD) {
            return true;
        }
        return false;
    }
}
