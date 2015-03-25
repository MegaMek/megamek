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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import megamek.common.Entity;
import megamek.common.IEntityRemovalConditions;
import megamek.common.IGame;

/**
 * An event that is fired at the end of the victory phase, before the game state
 * is reset.  It can be used to retrieve information from the game before the
 * state is reset and the lounge phase begins.
 *
 * @see IGame#end(int, int)
 * @see GameListener
 */
public class GameVictoryEvent extends GameEvent {

    /**
     *
     */
    private static final long serialVersionUID = -8470655646019563063L;

    /**
     * Track game entities
     */
    private Vector<Entity> entities = new Vector<Entity>();
    private Hashtable<Integer, Entity> entityIds = new Hashtable<Integer, Entity>();

    /**
     * Track entities removed from the game (probably by death)
     */
    Vector<Entity> vOutOfGame = new Vector<Entity>();

    /**
     * @param source event source
     */
    @SuppressWarnings("unchecked")
    public GameVictoryEvent(Object source, IGame game) {
        super(source);
        for (Entity entity : game.getEntitiesVector()) {
            entities.add(entity);
            entityIds.put(entity.getId(), entity);
        }

        vOutOfGame = (Vector<Entity>) game.getOutOfGameEntitiesVector().clone();
        for (Entity entity : vOutOfGame) {
            entityIds.put(entity.getId(), entity);
        }
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameVictory(this);
    }

    @Override
    public String getEventName() {
        return "Game Victory";
    }

    /**
     * Returns an enumeration of all the entites in the game.
     */
    public Enumeration<Entity> getEntities() {
        return entities.elements();
    }

    /**
     * Returns the entity with the given id number, if any.
     */
    public Entity getEntity(int id) {
        return entityIds.get(new Integer(id));
    }

    /**
     * Returns an enumeration of salvagable entities.
     */
    // TODO: Correctly implement "Captured" Entities
    public Enumeration<Entity> getGraveyardEntities() {
        Vector<Entity> graveyard = new Vector<Entity>();

        for (Entity entity : vOutOfGame) {
            if ((entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_SALVAGEABLE)
                || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_CAPTURED)
                || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_EJECTED)) {
                graveyard.addElement(entity);
            }
        }

        return graveyard.elements();
    }

    /**
     * Returns an enumeration of wrecked entities.
     */
    public Enumeration<Entity> getWreckedEntities() {
        Vector<Entity> wrecks = new Vector<Entity>();
        for (Entity entity : vOutOfGame) {
            if ((entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_SALVAGEABLE)
                || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_EJECTED)
                || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_CAPTURED)) {
                wrecks.addElement(entity);
            }
        }

        return wrecks.elements();
    }

    /**
     * Returns an enumeration of entities that have retreated
     */
    public Enumeration<Entity> getRetreatedEntities() {
        Vector<Entity> sanctuary = new Vector<Entity>();

        for (Entity entity : vOutOfGame) {
            if ((entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_IN_RETREAT)
                || (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_PUSHED)) {
                sanctuary.addElement(entity);
            }
        }

        return sanctuary.elements();
    }

    /**
     * Returns an enumeration of entities that were utterly destroyed
     */
    public Enumeration<Entity> getDevastatedEntities() {
        Vector<Entity> smithereens = new Vector<Entity>();

        for (Entity entity : vOutOfGame) {
            if (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED) {
                smithereens.addElement(entity);
            }
        }

        return smithereens.elements();
    }
}
