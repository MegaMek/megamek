/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */

package megamek.common.autoresolve.event;

import megamek.common.Entity;
import megamek.common.IEntityRemovalConditions;
import megamek.common.IGame;
import megamek.common.annotations.Nullable;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.event.PostGameResolution;
import megamek.server.victory.VictoryResult;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

/**
 * @author Luana Coppio
 */
public class AutoResolveConcludedEvent implements PostGameResolution {

    private final IGame game;
    private final boolean controlledScenario;
    private final Vector<Entity> survived = new Vector<>();
    private final Vector<Entity> retreated = new Vector<>();
    private final Vector<Entity> graveyard = new Vector<>();
    private final Vector<Entity> devastated = new Vector<>();
    private final Vector<Entity> wrecked = new Vector<>();
    private final VictoryResult victoryResult;
    private final File logFile;

    public AutoResolveConcludedEvent(SimulationContext game, VictoryResult victoryResult, File logFile) {
        this.controlledScenario = (game.getLocalPlayer().getTeam() == victoryResult.getWinningTeam());
        this.victoryResult = victoryResult;
        this.game = game;
        this.logFile = logFile;

        game.getInGameObjects().stream()
            .filter(Entity.class::isInstance)
            .map(Entity.class::cast)
            .forEach(e -> e.setOwner(game.getPlayer(e.getOwnerId())));

        game.getGraveyard().stream()
            .filter(Entity.class::isInstance)
            .map(Entity.class::cast)
            .forEach(e -> e.setOwner(game.getPlayer(e.getOwnerId())));

        game.getInGameObjects().stream()
            .filter(Entity.class::isInstance)
            .map(Entity.class::cast)
            .forEach(survived::addElement);

        game.getGraveyard().stream()
            .filter(Entity.class::isInstance)
            .map(Entity.class::cast)
            .filter(entity -> entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_SALVAGEABLE ||
                entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_EJECTED)
            .forEach(graveyard::addElement);

        game.getGraveyard().stream()
            .filter(Entity.class::isInstance)
            .map(Entity.class::cast)
            .filter(entity -> entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_NEVER_JOINED ||
                entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_IN_RETREAT ||
                entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_CAPTURED ||
                entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_PUSHED)
            .forEach(retreated::addElement);

        game.getGraveyard().stream()
            .filter(Entity.class::isInstance)
            .map(Entity.class::cast)
            .filter(entity -> entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED)
            .forEach(devastated::addElement);

        game.getGraveyard().stream()
            .filter(Entity.class::isInstance)
            .map(Entity.class::cast)
            .filter(entity -> entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED ||
                entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_EJECTED ||
                entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_SALVAGEABLE)
            .forEach(wrecked::addElement);
    }

    public VictoryResult getVictoryResult() {
        return victoryResult;
    }

    public IGame getGame() {
        return game;
    }

    public boolean controlledScenario() {
        return controlledScenario;
    }

    @Override
    public Enumeration<Entity> getEntities() {
        return survived.elements();
    }

    @Override
    public Entity getEntity(int id) {
        return (Entity) game.getEntityFromAllSources(id);
    }

    @Override
    public Enumeration<Entity> getGraveyardEntities() {
        return graveyard.elements();
    }

    @Override
    public Enumeration<Entity> getWreckedEntities() {
        return wrecked.elements();
    }

    @Override
    public Enumeration<Entity> getRetreatedEntities() {
        return retreated.elements();
    }

    @Override
    public Enumeration<Entity> getDevastatedEntities() {
        return devastated.elements();
    }

    @Nullable
    public File getLogFile() {
        return logFile;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("game", game)
            .append("controlledScenario", controlledScenario)
            .append("survived", survived)
            .append("retreated", retreated)
            .append("graveyard", graveyard)
            .append("devastated", devastated)
            .append("wrecked", wrecked)
            .toString();
    }
}
