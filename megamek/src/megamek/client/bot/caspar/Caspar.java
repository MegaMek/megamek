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
import megamek.client.bot.common.BoardQuickRepresentation;
import megamek.client.bot.common.formation.Formation;
import megamek.client.bot.common.minefield.MinefieldDeploymentPlannerStrategy;
import megamek.client.bot.common.StructOfUnitArrays;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.PathRanker;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.princess.UnitBehavior;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.util.BoardUtilities;
import megamek.logging.MMLogger;
import org.nd4j.linalg.api.ops.Op;

import java.util.Optional;
import java.util.Set;

/**
 * The bot client for CASPAR (Combat Algorithmic System for Predictive Analysis and Response).
 * @author Luana Coppio
 */
public class Caspar extends Princess implements AdvancedAgent {
    private static final MMLogger logger = MMLogger.create(Caspar.class);

    private BoardQuickRepresentation boardQuickRepresentation;
    private StructOfUnitArrays enemyUnitsSOU;
    private StructOfUnitArrays friendlyUnitsSOU;
    private StructOfUnitArrays ownUnitsSOU;
    private final CasparAI casparAI;
    /**
     * Constructor - initializes a new instance of the Princess bot.
     *
     * @param name The display name.
     * @param host The host address to which to connect.
     * @param port The port on the host where to connect.
     */
    public Caspar(String name, String host, int port, String modelName) {
        super(name, host, port);
        this.deploymentPlannerStrategy = MinefieldDeploymentPlannerStrategy.STRATEGIC;
        this.casparAI = new CasparAI.Builder(this, modelName).build();
    }

    @Override
    public void initializePathRankers() {
        super.initializePathRankers();

        CasparStandardPathRanker casparStandardPathRanker = new CasparStandardPathRanker(this);
        casparStandardPathRanker.setPathEnumerator(precognition.getPathEnumerator());
        pathRankers.put(PathRanker.PathRankerType.Basic, casparStandardPathRanker);
    }

    @Override
    public Player getLocalPlayer() {
        return super.getLocalPlayer();
    }

    @Override
    public Coords getWaypointForEntity(Entity unit) {
        return getUnitBehaviorTracker().getWaypointForEntity(unit).orElse(null);
    }

    @Override
    public UnitBehavior.BehaviorType getBehaviorType(Entity unit) {
        return getUnitBehaviorTracker().getBehaviorType(unit, this);
    }

    @Override
    public Set<Coords> getDestinationCoords(Entity mover) {
        return getClusterTracker().getDestinationCoords(mover, getHomeEdge(mover), false);
    }

    @Override
    public Set<Coords> getDestinationCoordsWithTerrainReduction(Entity mover) {
        return getClusterTracker().getDestinationCoords(mover, getHomeEdge(mover), true);
    }

    @Override
    public Set<Coords> getOppositeSideDestinationCoordsWithTerrainReduction(Entity mover) {
        CardinalEdge destinationEdge = BoardUtilities.determineOppositeEdge(mover);
        return getClusterTracker().getDestinationCoords(mover, destinationEdge, true);
    }

    @Override
    public StructOfUnitArrays getEnemyUnitsSOU() {
        if (enemyUnitsSOU == null) {
            enemyUnitsSOU = new StructOfUnitArrays(getEnemyEntities());
        }
        return enemyUnitsSOU;
    }

    @Override
    public StructOfUnitArrays getFriendlyUnitsSOU() {
        if (friendlyUnitsSOU == null) {
            friendlyUnitsSOU = new StructOfUnitArrays(getFriendEntities());
        }
        return friendlyUnitsSOU;
    }

    @Override
    public StructOfUnitArrays getOwnUnitsSOU() {
        if (ownUnitsSOU == null) {
            ownUnitsSOU = new StructOfUnitArrays(getEntitiesOwned());
        }
        return enemyUnitsSOU;
    }

    @Override
    public BoardQuickRepresentation getBoardQuickRepresentation() {
        if (boardQuickRepresentation == null) {
            boardQuickRepresentation = new BoardQuickRepresentation(getGame().getBoard());
        }
        return boardQuickRepresentation;
    }

    @Override
    public Optional<Formation> getFormationFor(Entity unit) {
        return casparAI.getFormationManager().getUnitFormation(unit);
    }

    @Override
    public TacticalPlanner getTacticalPlanner() {
        return casparAI.getTacticalPlanner();
    }

    private void resetSOU() {
        getEnemyUnitsSOU().update(getEnemyEntities());
        getFriendlyUnitsSOU().update(getFriendEntities());
        getOwnUnitsSOU().update(getEntitiesOwned());
    }

    private void resetQuickRepresentation() {
        getBoardQuickRepresentation().update(getBoard());
        getBoardQuickRepresentation().updateThreatHeatmap(getEnemyUnitsSOU(), getOwnUnitsSOU());
    }

    @Override
    protected void endOfTurnProcessing() {
        super.endOfTurnProcessing();;
        resetSOU();
        resetQuickRepresentation();
    }

    @Override
    protected void exchangeSetup() {
        resetSOU();
        resetQuickRepresentation();
    }
}
