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

import megamek.common.Targetable;

/**
 * @author Deric Page (deric dot page at usa dot net)
 * @since 9/5/14 2:48 PM
 */
public interface IHonorUtil {

    /**
     * Indicates whether or not the identified unit can be considered broken.
     *
     * @param targetid The target to be checked.
     * @param playerId The ID of the player owning the target.
     * @return TRUE if the unit is on the broken units list without being on the honorless enemies list.
     */
    boolean isEnemyBroken(int targetid, int playerId, boolean forcedWithdrawal);

    /**
     * Indicates whether or not the identified player is on the dishonored enemies list.
     *
     * @param playerId The ID of the player to be checked.
     * @return TRUE if the player is on the dishonored enemies list.
     */
    boolean isEnemyDishonored(int playerId);

    /**
     * Adds the identified player to the dishonored enemies list.
     *
     * @param playerId The ID of the player to be added.
     */
    void setEnemyDishonored(int playerId);

    /**
     * Checks the given {@link Targetable} to see if it should be counted as broken:<br>
     * Forced Withdrawal is turned on<br>
     * Given unit is Crippled<br>
     *
     * @param target           The unit to be checked.
     * @param forcedWithdrawal Set TRUE if the Forced Withdrawal rule is in effect.
     */
    void checkEnemyBroken(Targetable target, boolean forcedWithdrawal);
}
