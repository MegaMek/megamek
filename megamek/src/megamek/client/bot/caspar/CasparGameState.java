/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.caspar;

import megamek.client.bot.common.AdvancedAgent;
import megamek.client.bot.common.Agent;
import megamek.client.bot.common.BoardQuickRepresentation;
import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;
import megamek.client.bot.common.StructOfUnitArrays;
import megamek.client.bot.common.formation.Formation;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.MovePath;
import megamek.common.Player;
import megamek.common.TargetRoll;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.BoardClusterTracker;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CasparGameState implements GameState {
    private final AdvancedAgent agent;

    public CasparGameState(AdvancedAgent agent) {
        this.agent = agent;
    }

    @Override
    public List<Entity> getEnemyUnits() {
        return agent.getEnemyEntities();
    }

    @Override
    public Set<Coords> getUnexploredAreas() {
        throw new NotImplementedException("TODO - Implement");
    }

    @Override
    public List<Entity> getFriendlyUnits() {
        return agent.getFriendEntities();
    }

    @Override
    public List<Entity> getOwnedUnits() {
        return agent.getEntitiesOwned();
    }

    @Override
    public List<Entity> getMyTeamUnits() {
        List<Entity> units = agent.getEntitiesOwned();
        units.removeAll(agent.getFriendEntities());
        return units;
    }

    @Override
    public StructOfUnitArrays getEnemyUnitsSOU() {
        return agent.getEnemyUnitsSOU();
    }

    @Override
    public StructOfUnitArrays getFriendlyUnitsSOU() {
        return agent.getFriendlyUnitsSOU();
    }

    @Override
    public StructOfUnitArrays getOwnUnitsSOU() {
        return agent.getOwnUnitsSOU();
    }

    @Override
    public List<Coords> getStrategicPoints() {
        return agent.getStrategicGoalsManager().getStrategicGoals();
    }

    @Override
    public Optional<Formation> getFormationFor(Entity unit) {
        return agent.getFormationFor(unit);
    }

    @Override
    public Game getGame() {
        return agent.getGame();
    }

    @Override
    public BoardQuickRepresentation getBoardQuickRepresentation() {
        return agent.getBoardQuickRepresentation();
    }

    @Override
    public Player getLocalPlayer() {
        return agent.getLocalPlayer();
    }

    @Override
    public Enumeration<ArtilleryAttackAction> getArtilleryAttacks() {
        return agent.getGame().getArtilleryAttacks();
    }

    @Override
    public double successProbability(Pathing pathing) {
        if (pathing instanceof MovePath movePath) {
            return agent.getMovePathSuccessProbability(movePath);
        }
        return 1f;
    }

    @Override
    public boolean useExtremeRange() {
        return getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE);
    }

    @Override
    public BehaviorSettings getBehaviorSettings() {
        return agent.getBehaviorSettings();
    }

    @Override
    public TacticalPlanner getTacticalPlanner() {
        return agent.getTacticalPlanner();
    }
}
