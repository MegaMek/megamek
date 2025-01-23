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

package megamek.client.bot.duchess.ai.utility.tw.context;

import megamek.ai.utility.World;
import megamek.client.bot.duchess.ai.utility.tw.Cluster;
import megamek.client.bot.duchess.ai.utility.tw.ClusteringService;
import megamek.client.bot.princess.Princess;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.InGameObject;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TWWorld implements World<Entity, Entity> {

    private final Game game;
    private final ClusteringService clusteringService;
    private final Princess princess;

    public TWWorld(Game game, Princess princess, ClusteringService clusteringService) {
        this.game = game;
        this.princess = princess;
        this.clusteringService = clusteringService;

        this.game.addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                if (e.getNewPhase().isInitiative()) {
                    initializeClusters();
                }
            }
        });
    }

    public void initializeClusters() {
        clusteringService.buildClusters(princess.getFriendEntities());
    }

    public Cluster getEntityCluster(Entity entity) {
        return clusteringService.getCluster(entity);
    }

    public Coords getEntityClusterCentroid(Entity entity) {
        return clusteringService.getClusterMidpoint(entity);
    }

    public List<Entity> getEntities() {
        return game.getEntitiesVector();
    }

    @Override
    public List<InGameObject> getInGameObjects() {
        return game.getInGameObjects();
    }

    @Override
    public Map<Integer, Integer> getTeamByPlayer() {
        return game.getTeamByPlayer();
    }

    @Override
    public List<Entity> getMyUnits() {
        return princess.getFriendEntities();
    }

    @Override
    public List<Entity> getAlliedUnits() {
        return princess.getFriendEntities();
    }

    @Override
    public List<Entity> getEnemyUnits() {
        return princess.getEnemyEntities();
    }

    @Override
    public boolean useBooleanOption(String option) {
        return game.getOptions().booleanOption(option);
    }

    @Override
    public boolean contains(Coords position) {
        return game.getBoard().contains(position);
    }

    public Game getGame() {
        return game;
    }

    public Cluster getClosestEnemyCluster(Cluster cluster) {
        return clusteringService.getClosestEnemyCluster(cluster);
    }
}
