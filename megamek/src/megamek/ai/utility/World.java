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

package megamek.ai.utility;

import megamek.common.InGameObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface World<IN_GAME_OBJECT, TARGETABLE> {

    List<InGameObject> getInGameObjects();

    Map<Integer, Integer> getTeamByPlayer();

    @SuppressWarnings("unchecked")
    default void rebuildStateForPlayer(int playerId) {
        var teamByPlayer = getTeamByPlayer();
        var teamId = teamByPlayer.get(playerId);

        var myUnits = getInGameObjects()
            .stream()
            .filter(i -> i.getOwnerId() == playerId)
            .map(i -> (IN_GAME_OBJECT) i)
            .toList();

        setMyUnits(myUnits);

        var alliedUnits = getInGameObjects().stream()
            .filter(e -> e.getOwnerId() != playerId)
            .filter(e -> (teamByPlayer.getOrDefault(e.getOwnerId(), 0) != 0)
                && Objects.equals(teamByPlayer.getOrDefault(e.getOwnerId(), 0), teamId))
            .map(e -> (TARGETABLE) e)
            .toList();

        setAlliedUnits(alliedUnits);

        var enemyUnits = getInGameObjects().stream()
            .filter(e -> e.getOwnerId() != playerId)
            .filter(e -> (teamByPlayer.getOrDefault(e.getOwnerId(), 0) == 0)
                || !Objects.equals(teamByPlayer.getOrDefault(e.getOwnerId(), 0), teamId))
            .map(e -> (TARGETABLE) e)
            .toList();

        setEnemyUnits(enemyUnits);
    }

    void setMyUnits(List<IN_GAME_OBJECT> myUnits);
    void setAlliedUnits(List<TARGETABLE> alliedUnits);
    void setEnemyUnits(List<TARGETABLE> enemyUnits);

    List<IN_GAME_OBJECT> getMyUnits();
    List<TARGETABLE> getAlliedUnits();
    List<TARGETABLE> getEnemyUnits();

    boolean useBooleanOption(String option);
}
