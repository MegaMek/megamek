/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot;

import java.util.Vector;

import megamek.common.Entity;
import megamek.common.INarcPod;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.AbstractAttackAction;
import megamek.common.actions.BrushOffAttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.actions.PushAttackAction;

/**
 * TODO: add more options, pushing, kick both for quad mechs, etc.
 *
 * also, what
 * are the conditions for multiple physical attacks?
 */
public class PhysicalOption {
    public final static int NONE = 0;
    public final static int PUNCH_LEFT = 1;
    public final static int PUNCH_RIGHT = 2;
    public final static int PUNCH_BOTH = 3;
    public final static int KICK_LEFT = 4;
    public final static int KICK_RIGHT = 5;
    public final static int USE_CLUB = 6; // Includes sword, hatchet, mace,
                                            // and found clubs
    public final static int USE_CLAW = 7; // Level 3 rules, not incorporated
                                            // yet
    public final static int PUSH_ATTACK = 8;
    public final static int TRIP_ATTACK = 9; // Level 3 rules, not
                                                // incorporated yet
    public final static int BRUSH_LEFT = 10;
    public final static int BRUSH_RIGHT = 11;
    public final static int BRUSH_BOTH = 12;
    public final static int THRASH_INF = 13;

    Entity attacker;
    Entity target;
    INarcPod i_target;
    double expectedDmg;
    int type;
    Mounted club;

    public PhysicalOption(Entity attacker) {
        this.attacker = attacker;
        this.type = NONE;
    }

    public PhysicalOption(Entity attacker, Targetable target, double dmg,
            int type, Mounted club) {
        this.attacker = attacker;
        if (target instanceof Entity) {
            this.target = (Entity) target;
        }
        if (target instanceof INarcPod) {
            this.i_target = (INarcPod) target;
        }
        this.expectedDmg = dmg;
        this.type = type;
        this.club = club;
    }

    public AbstractAttackAction toAction() {
        switch (type) {
            case PUNCH_LEFT:
                return new PunchAttackAction(attacker.getId(), target.getId(),
                        PunchAttackAction.LEFT);
            case PUNCH_RIGHT:
                return new PunchAttackAction(attacker.getId(), target.getId(),
                        PunchAttackAction.RIGHT);
            case PUNCH_BOTH:
                return new PunchAttackAction(attacker.getId(), target.getId(),
                        PunchAttackAction.BOTH);
            case KICK_LEFT:
                return new KickAttackAction(attacker.getId(), target.getId(),
                        KickAttackAction.LEFT);
            case KICK_RIGHT:
                return new KickAttackAction(attacker.getId(), target.getId(),
                        KickAttackAction.RIGHT);
            case USE_CLUB:
                if (club != null) {
                    return new ClubAttackAction(attacker.getId(), target
                            .getId(), club, ToHitData.HIT_NORMAL);
                }
                return null;
            case PUSH_ATTACK:
                return new PushAttackAction(attacker.getId(), target.getId(),
                        target.getPosition());
            case TRIP_ATTACK:
                return null; // Trip attack not implemented yet
            case BRUSH_LEFT:
                if (target == null) {
                    return new BrushOffAttackAction(attacker.getId(), i_target
                            .getTargetType(), i_target.getTargetId(),
                            BrushOffAttackAction.LEFT);
                }
                return new BrushOffAttackAction(attacker.getId(), target
                        .getTargetType(), target.getId(),
                        BrushOffAttackAction.LEFT);
            case BRUSH_RIGHT:
                if (target == null) {
                    return new BrushOffAttackAction(attacker.getId(), i_target
                            .getTargetType(), i_target.getTargetId(),
                            BrushOffAttackAction.RIGHT);
                }
                return new BrushOffAttackAction(attacker.getId(), target
                        .getTargetType(), target.getId(),
                        BrushOffAttackAction.RIGHT);
            case BRUSH_BOTH:
                if (target == null) {
                    return new BrushOffAttackAction(attacker.getId(), i_target
                            .getTargetType(), i_target.getTargetId(),
                            BrushOffAttackAction.BOTH);
                }
                return new BrushOffAttackAction(attacker.getId(), target
                        .getTargetType(), target.getId(),
                        BrushOffAttackAction.BOTH);
                /*
                 * case THRASH_INF : return new
                 * ThrashAttackAction(attacker.getId(), target.getId());
                 */
        }
        return null;
    }

    public Vector<EntityAction> getVector() {
        AbstractAttackAction aaa = toAction();
        Vector<EntityAction> v = new Vector<EntityAction>();
        if (aaa != null) {
            v.addElement(aaa);
        }
        return v;
    }
}
