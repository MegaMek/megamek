/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import megamek.common.Entity;
import megamek.common.Targetable;

/**
 * @author Deric Page (deric dot page at usa dot net)
 * @since 9/5/14 2:53 PM
 */
public class HonorUtil implements IHonorUtil {

    private final Set<Integer> dishonoredEnemies = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Integer> brokenEnemies = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private boolean iAmAPirate = false;

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
        return dishonoredEnemies.contains(playerId) || iAmAPirate(); // pirates have no honor to give in order to give
    }

    @Override
    public void setEnemyDishonored(int playerId) {
        dishonoredEnemies.add(playerId);
    }

    @Override
    public Set<Integer> getDishonoredEnemies() {
        return dishonoredEnemies;
    }

    @Override
    public boolean iAmAPirate() {
        return iAmAPirate;
    }

    public void setIAmAPirate(boolean iAmAPirate) {
        this.iAmAPirate = iAmAPirate;
    }
}
