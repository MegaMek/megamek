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
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.InGameObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TWWorld implements World<Entity, Entity> {

    private final Game game;
    private final List<Entity> myUnits = new ArrayList<>();
    private final List<Entity> alliedUnits = new ArrayList<>();
    private final List<Entity> enemyUnits = new ArrayList<>();

    public TWWorld(Game game) {
        this.game = game;
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
    public void setMyUnits(List<Entity> myUnits) {
        this.myUnits.clear();
        this.myUnits.addAll(myUnits);
    }

    @Override
    public void setAlliedUnits(List<Entity> alliedUnits) {
        this.alliedUnits.clear();
        this.alliedUnits.addAll(alliedUnits);
    }

    @Override
    public void setEnemyUnits(List<Entity> enemyUnits) {
        this.enemyUnits.clear();
        this.enemyUnits.addAll(enemyUnits);
    }

    @Override
    public List<Entity> getMyUnits() {
        return myUnits;
    }

    @Override
    public List<Entity> getAlliedUnits() {
        return alliedUnits;
    }

    @Override
    public List<Entity> getEnemyUnits() {
        return enemyUnits;
    }

    @Override
    public boolean useBooleanOption(String option) {
        return game.getOptions().booleanOption(option);
    }
}
