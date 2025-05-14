/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import java.util.*;

/**
 * @author Luana Coppio
 */
public class AutoResolveConcludedEvent implements PostGameResolution {

    private final IGame game;
    private final boolean controlledScenario;
    private final List<Entity> survived = new ArrayList<>();
    private final List<Entity> retreated = new ArrayList<>();
    private final List<Entity> graveyard = new ArrayList<>();
    private final List<Entity> devastated = new ArrayList<>();
    private final List<Entity> wrecked = new ArrayList<>();
    private final Map<Integer, Entity> entityById = new HashMap<>();
    private final VictoryResult victoryResult;
    private final File logFile;

    public AutoResolveConcludedEvent(SimulationContext game, VictoryResult victoryResult, File logFile) {
        this.controlledScenario = (game.getLocalPlayer().getTeam() == victoryResult.getWinningTeam());
        this.victoryResult = victoryResult;
        this.game = game;
        this.logFile = logFile;
        var inGameObjects = game.getInGameObjects();
        var graveyardObjects = game.getGraveyard();

        for (var inGameObject : inGameObjects) {
            if (inGameObject instanceof Entity entity) {
                entity.setOwner(game.getPlayer(entity.getOwnerId()));
                entityById.put(entity.getId(), entity);
                survived.add(entity);
            }
        }

        var salvageableConditions = Set.of(IEntityRemovalConditions.REMOVE_SALVAGEABLE, IEntityRemovalConditions.REMOVE_EJECTED);
        var retreatedConditions = Set.of(IEntityRemovalConditions.REMOVE_NEVER_JOINED, IEntityRemovalConditions.REMOVE_IN_RETREAT,
            IEntityRemovalConditions.REMOVE_PUSHED);
        var lostUnitConditions = Set.of(IEntityRemovalConditions.REMOVE_DEVASTATED, IEntityRemovalConditions.REMOVE_CAPTURED);
        var wreckedConditions = Set.of(IEntityRemovalConditions.REMOVE_DEVASTATED, IEntityRemovalConditions.REMOVE_EJECTED,
            IEntityRemovalConditions.REMOVE_SALVAGEABLE);

        for (var graveyardEntity : graveyardObjects) {
            if (graveyardEntity instanceof Entity entity) {
                entity.setOwner(game.getPlayer(entity.getOwnerId()));
                entityById.put(entity.getId(), entity);
                if (salvageableConditions.contains(entity.getRemovalCondition())) {
                    graveyard.add(entity);
                }
                if (retreatedConditions.contains(entity.getRemovalCondition())) {
                    retreated.add(entity);
                }
                if (lostUnitConditions.contains(entity.getRemovalCondition())) {
                    devastated.add(entity);
                }
                if (wreckedConditions.contains(entity.getRemovalCondition())) {
                    wrecked.add(entity);
                }
            }
        }
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
        return Collections.enumeration(survived);
    }

    @Override
    public Entity getEntity(int id) {
        return entityById.get(id);
    }

    @Override
    public Enumeration<Entity> getGraveyardEntities() {
        return Collections.enumeration(graveyard);
    }

    @Override
    public Enumeration<Entity> getWreckedEntities() {
        return Collections.enumeration(wrecked);
    }

    @Override
    public Enumeration<Entity> getRetreatedEntities() {
        return Collections.enumeration(retreated);
    }

    @Override
    public Enumeration<Entity> getDevastatedEntities() {
        return Collections.enumeration(devastated);
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
