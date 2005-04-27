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

package megamek.common.event;

import megamek.common.actions.AttackAction;

/**
 * Instances of this class are sent when Attack occurs
 *
 * @see GameListener
 */
public class GameAttackEvent extends GameEvent {

    protected AttackAction attack; 

    /**
     * Cunstruct new GameAttackEvent
     * @param source sender
     * @param attack
     */
    public GameAttackEvent(Object source, AttackAction attack) {
        super(source, GAME_NEW_ATTACK);
        this.attack = attack;
    }

    /**
     * @return the attack.
     */
    public AttackAction getAttack() {
        return attack;
    }

}
