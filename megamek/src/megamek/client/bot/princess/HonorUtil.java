/*
 * MoralUtilImpl.java
 *
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot.princess;

import megamek.common.Entity;
import megamek.common.Targetable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Deric Page (deric dot page at usa dot net)
 * @version %Id%
 * @since 9/5/14 2:53 PM
 */
public class HonorUtil implements IHonorUtil {

    private final Set<Integer> dishonoredEnemies = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Integer> brokenEnemies = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private void checkEnemyBroken(Entity entity, boolean forcedWithdrawal) {
        if (forcedWithdrawal && entity.isCrippled()) {
            brokenEnemies.add(entity.getId());
        }
    }

    @Override
    public void checkEnemyBroken(Targetable target, boolean forcedWithdrawal) {
        if (target instanceof Entity) {
            checkEnemyBroken((Entity) target, forcedWithdrawal);
        }
    }

    @Override
    public boolean isEnemyBroken(int entityId, int playerId, boolean forcedWithdrawal) {
        return brokenEnemies.contains(entityId) && !isEnemyDishonored(playerId);
    }

    @Override
    public boolean isEnemyDishonored(int playerId) {
        return dishonoredEnemies.contains(playerId);
    }

    @Override
    public void setEnemyDishonored(int playerId) {
        dishonoredEnemies.add(playerId);
    }
}
