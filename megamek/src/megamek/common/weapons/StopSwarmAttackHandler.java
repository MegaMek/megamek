/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.server.GameManager;

/**
 * @author Andrew Hunter
 * @since Sep 23, 2004
 */
public class StopSwarmAttackHandler extends WeaponHandler {
    private static final long serialVersionUID = 7078803294398264979L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public StopSwarmAttackHandler(ToHitData toHit, WeaponAttackAction waa,
            Game g, GameManager m) {
        super(toHit, waa, g, m);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        Entity entityTarget = (Entity) target;
        // ... but only as their *only* attack action.
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            Report r = new Report(3105);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        }
        // swarming ended succesfully
        Report r = new Report(3110);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        // Only apply the "stop swarm 'attack'" to the swarmed Mek.
        if (ae.getSwarmTargetId() != target.getId()) {
            Entity other = game.getEntity(ae.getSwarmTargetId());
            other.setSwarmAttackerId(Entity.NONE);
        } else {
            entityTarget.setSwarmAttackerId(Entity.NONE);
        }
        ae.setSwarmTargetId(Entity.NONE);
        return false;
    }
}
