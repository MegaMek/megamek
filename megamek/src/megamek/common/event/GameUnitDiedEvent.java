/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.event;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.IEntityRemovalConditions;
import megamek.server.GameManager;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * An event that we will fire when an entity is destroyed or otherwise dies;
 * attackers will register listeners temporarily, allowing us to determine if
 * the current death occurred because of a specific units action - and give that
 * unit credit for the kill
 *
 * @see Game#end(int, int)
 * @see GameListener
 */
public class GameUnitDiedEvent extends GameEvent {
    private static final long serialVersionUID = -8470655646019563063L;

    /**
     * Track game entity that just died
     */
    private final Entity entity;
    private final Game game;
    private final int damage;
    private final String reason;

    /**
     * @param source event source
     * @param game the game in question
     * @param entity that died
     */
    @SuppressWarnings("unchecked")
    public GameUnitDiedEvent(Object source, Game game, Entity entity, int damage, String reason) {
        super(source);
        this.entity = entity;
        this.game = game;
        this.damage = damage;
        this.reason = reason;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameUnitDied(this);
    }

    @Override
    public String getEventName() {
        return "Unit Died";
    }

    public Entity getEntity(){
        return this.entity;
    }

    public Game getGame() { return this.game; }
    public int getDamage() { return this.damage; }
    public String getReason() { return this.reason; }
}
