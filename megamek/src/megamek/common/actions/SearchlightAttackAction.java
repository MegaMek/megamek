/*
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

package megamek.common.actions;

import java.util.Enumeration;

import java.util.Vector;

import megamek.common.Coords;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.LosEffects;
import megamek.common.Report;
import megamek.common.Targetable;

/**
 * Used for aiming a searchlight at a target.
 */
public class SearchlightAttackAction
    extends AbstractAttackAction
{
    
    // default to attacking an entity
    public SearchlightAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }
    
    public SearchlightAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }
    
    public boolean isPossible(IGame game) {
        return SearchlightAttackAction.isPossible(game, getEntityId(),
                           game.getTarget(getTargetType(), getTargetId()));
    }

    public static boolean isPossible(IGame game, int attackerId, Targetable target) {
        final Entity attacker = game.getEntity(attackerId);
        if(attacker == null || !attacker.isUsingSpotlight()) 
            return false;
        if(!Compute.isInArc(attacker.getPosition(), attacker.getSecondaryFacing(), target.getPosition(), Compute.ARC_FORWARD))
            return false;
        LosEffects los = LosEffects.calculateLos(game,attackerId,target);
        return los.canSee();
    }

    /**
     * illuminate an entity and all entities that are between us and the hex
     */
    public Vector resolveAction (IGame game) {
        Vector reports = new Vector();
        Report r;
        if(!isPossible(game)) {
            r = new Report(3445);
            r.subject = this.getEntityId();
            r.newlines = 1;
            reports.addElement(r);
            return reports;
        }
        final Entity attacker = getEntity(game);
        final Coords apos = attacker.getPosition();
        final Targetable target = getTarget(game);
        final Coords tpos = target.getPosition();

        if(attacker.usedSearchlight()) {
            r = new Report(3450);
            r.subject = this.getEntityId();
            r.add(attacker.getDisplayName());
            r.newlines = 1;
            reports.addElement(r);
            return reports;
        }
        attacker.setUsedSearchlight(true);

        Coords[] in = Coords.intervening(apos, tpos); //nb includes attacker & target
        for (int i = 0; i < in.length; i++) {
            for (Enumeration e = game.getEntities(in[i]);e.hasMoreElements();) {
                Entity en = (Entity)e.nextElement();
                LosEffects los = LosEffects.calculateLos(game,getEntityId(),en);
                if(los.canSee()) {
                    en.setIlluminated(true);
                    r = new Report(3455);
                    r.subject = this.getEntityId();
                    r.newlines = 1;
                    r.add(en.getDisplayName());
                    r.add(attacker.getDisplayName());
                    reports.addElement(r);
                }
            }
        }
        return reports;
    }

    public boolean willIlluminate (IGame game, Entity who) {
        if(!isPossible(game))
            return false;
        final Entity attacker = getEntity(game);
        final Coords apos = attacker.getPosition();
        final Targetable target = getTarget(game);
        final Coords tpos = target.getPosition();

        Coords[] in = Coords.intervening(apos, tpos); //nb includes attacker & target
        for (int i = 0; i < in.length; i++) {
            for (Enumeration e = game.getEntities(in[i]);e.hasMoreElements();) {
                Entity en = (Entity)e.nextElement();
                LosEffects los = LosEffects.calculateLos(game,getEntityId(),en);
                if(los.canSee() && en.equals(who))
                    return true;
            }
        }
        return false;
    }
}
