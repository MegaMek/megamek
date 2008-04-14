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

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class StopSwarmAttackHandler extends WeaponHandler {
    /**
     * 
     */
    private static final long serialVersionUID = 7078803294398264979L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public StopSwarmAttackHandler(ToHitData toHit, WeaponAttackAction waa,
            IGame g, Server s) {
        super(toHit, waa, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        Entity entityTarget = (Entity) target;
        // ... but only as their *only* attack action.
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(3105);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else {
            // swarming ended succesfully
            r = new Report(3110);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            // Only apply the "stop swarm 'attack'" to the swarmed Mek.
            if (ae.getSwarmTargetId() != target.getTargetId()) {
                Entity other = game.getEntity(ae.getSwarmTargetId());
                other.setSwarmAttackerId(Entity.NONE);
            } else {
                entityTarget.setSwarmAttackerId(Entity.NONE);
            }
            ae.setSwarmTargetId(Entity.NONE);
            return false;
        }
    }
}
