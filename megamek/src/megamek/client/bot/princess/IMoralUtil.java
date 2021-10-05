/*
 * MoralUtil.java
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
import megamek.common.Game;
import megamek.common.IPlayer;

/**
 * @author Deric Page <deric dot page at gmail dot com>
 * @since: 5/13/14 8:35 AM
 * @version: %Id%
 */
public interface IMoralUtil {

    /**
     * If a unit's moral is broken, this method should return TRUE.
     *
     * @param unitId The ID of the {@link Entity} being checked (from {@link Entity#getId()}).
     * @return TRUE if the unit is broken.
     */
    boolean isUnitBroken(int unitId);

    /**
     * Triggers the moral check for all units controlled by the Princess bot player.
     *
     * @param forcedWithdrawal Set TRUE if Forced Withdrawal is in effect.
     * @param bravery          The index of the bravery setting in {@link BehaviorSettings}.
     * @param selfPreservation The index of the selfPreservation setting in {@link BehaviorSettings}.
     * @param player           The {@link IPlayer} of the Princess bot.
     * @param game             The game being played.
     */
    void checkMoral(boolean forcedWithdrawal, int bravery, int selfPreservation, IPlayer player, Game game);
}
