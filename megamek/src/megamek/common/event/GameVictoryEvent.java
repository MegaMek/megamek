/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.event;

import java.io.Serial;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import megamek.common.game.Game;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.Entity;

/**
 * An event that is fired at the end of the victory phase, before the game state is reset. It can be used to retrieve
 * information from the game before the state is reset and the lounge phase begins.
 *
 * @see Game#end(int, int)
 * @see GameListener
 */
public class GameVictoryEvent extends GameEvent implements PostGameResolution {
    @Serial
    private static final long serialVersionUID = -8470655646019563063L;

    /**
     * Track game entities
     */
    private final Vector<Entity> entities = new Vector<>();
    private final Hashtable<Integer, Entity> entityIds = new Hashtable<>();

    /**
     * Track entities removed from the game (probably by death)
     */
    Vector<Entity> vOutOfGame;

    /**
     * @param source event source
     */
    public GameVictoryEvent(Object source, Game game) {
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
     * @return an enumeration of all the entities in the game.
     */
    @Override
    public Enumeration<Entity> getEntities() {
        return entities.elements();
    }

    /**
     * @return the entity with the given id number, if any.
     */
    @Override
    public Entity getEntity(int id) {
        return entityIds.get(id);
    }

    /**
     * @return an enumeration of salvageable entities.
     */
    // TODO: Correctly implement "Captured" Entities
    @Override
    public Enumeration<Entity> getGraveyardEntities() {
        Vector<Entity> graveyard = new Vector<>();

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
     * @return an enumeration of wrecked entities.
     */
    @Override
    public Enumeration<Entity> getWreckedEntities() {
        Vector<Entity> wrecks = new Vector<>();
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
    @Override
    public Enumeration<Entity> getRetreatedEntities() {
        Vector<Entity> sanctuary = new Vector<>();

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
    @Override
    public Enumeration<Entity> getDevastatedEntities() {
        Vector<Entity> smithereens = new Vector<>();

        for (Entity entity : vOutOfGame) {
            if (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED) {
                smithereens.addElement(entity);
            }
        }

        return smithereens.elements();
    }
}
