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

import megamek.common.Coords;
import megamek.common.InGameObject;

import java.util.*;

public interface World<IN_GAME_OBJECT, TARGETABLE> {
    List<InGameObject> getInGameObjects();
    Map<Integer, Integer> getTeamByPlayer();
    List<IN_GAME_OBJECT> getMyUnits();
    List<TARGETABLE> getAlliedUnits();
    List<TARGETABLE> getEnemyUnits();
    boolean useBooleanOption(String option);
    boolean contains(Coords position);
    List<IN_GAME_OBJECT> getEntities(List<Integer> ids);
}
