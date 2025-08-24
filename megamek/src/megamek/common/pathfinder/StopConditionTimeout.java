/*
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
package megamek.common.pathfinder;

/**
 * A timeout stop condition. The shouldStop() returns answer based on time elapsed since initialisation or last
 * restart() call.
 */
public class StopConditionTimeout<E> implements StopCondition<E> {
    // this class should be redesigned to use an executor.
    private E lastEdge;
    private long start;
    private long stop;
    final int timeout;

    public boolean timeoutEngaged;

    public StopConditionTimeout(int timeoutMillis) {
        this.timeout = timeoutMillis;
        restart();
    }

    public E getLastEdge() {
        return lastEdge;
    }

    public int getTimeout() {
        return timeout;
    }

    public void restart() {
        start = System.currentTimeMillis();
        stop = start + timeout;
        lastEdge = null;
        timeoutEngaged = false;
    }

    @Override
    public boolean shouldStop(E e) {
        if (System.currentTimeMillis() > stop) {
            timeoutEngaged = true;
            lastEdge = e;
            return true;
        }
        return false;
    }
}
