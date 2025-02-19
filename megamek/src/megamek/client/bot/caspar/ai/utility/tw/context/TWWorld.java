/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.bot.caspar.ai.utility.tw.context;

import megamek.ai.utility.QuickBoardRepresentation;
import megamek.ai.utility.StructOfUnitArrays;
import megamek.ai.utility.World;
import megamek.client.bot.caspar.ai.utility.tw.Cluster;
import megamek.client.bot.caspar.ai.utility.tw.ClusteringService;
import megamek.client.bot.princess.Princess;
import megamek.common.*;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TWWorld implements World {

    private final Game game;
    private final ClusteringService clusteringService;
    private final Princess princess;
    private StructOfArraysEntity enemies;
    private StructOfArraysEntity allies;
    private StructOfArraysEntity ownUnits;
    private QuickBoardRepresentation boardRepresentation;

    public TWWorld(Game game, Princess princess, ClusteringService clusteringService) {
        this.game = game;
        this.princess = princess;
        this.clusteringService = clusteringService;
        this.boardRepresentation = new QuickBoardRepresentation(game.getBoard());
        this.game.addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                if (e.getNewPhase().isInitiative()) {
                    initializeClusters();
                    boardRepresentation.updateThreatHeatmap(enemies);
                }
            }

            @Override
            public void gameBoardNew(GameBoardNewEvent e) {
                boardRepresentation = new QuickBoardRepresentation(e.getNewBoard());
            }

            @Override
            public void gameBoardChanged(GameBoardChangeEvent e) {
                super.gameBoardChanged(e);
            }
        });
    }

    public void resetReferences() {
        enemies = new StructOfArraysEntity(princess.getEnemyEntities());
        allies = new StructOfArraysEntity(princess.getFriendEntities());
        ownUnits = new StructOfArraysEntity(princess.getEntitiesOwned());
    }

    public void initializeClusters() {
        clusteringService.buildClusters(new ArrayList<>(princess.getEntitiesOwned()));
    }

    public Coords getEntityClusterCentroid(Targetable entity) {
        return clusteringService.getClusterMidpoint(entity);
    }

    @Override
    public List<Targetable> getMyUnits() {
        return new ArrayList<>(princess.getEntitiesOwned());
    }

    @Override
    public List<Targetable> getAlliedUnits() {
        return new ArrayList<>(princess.getFriendEntities());
    }

    @Override
    public List<Targetable> getEnemyUnits() {
        return new ArrayList<>(princess.getEnemyEntities());
    }

    @Override
    public boolean useBooleanOption(String option) {
        return game.getOptions().booleanOption(option);
    }

    @Override
    public double[] getHeatmap() {
        return boardRepresentation.getNormalizedThreatLevelHeatmap();
    }

    @Override
    public QuickBoardRepresentation getQuickBoardRepresentation() {
        return boardRepresentation;
    }

    public Game getGame() {
        return game;
    }

    @Override
    public StructOfUnitArrays getStructOfEnemyUnitArrays() {
        return enemies;
    }

    @Override
    public StructOfUnitArrays getStructOfAllyUnitArrays() {
        return allies;
    }

    @Override
    public StructOfUnitArrays getStructOfOwnUnitsArrays() {
        return ownUnits;
    }

}
