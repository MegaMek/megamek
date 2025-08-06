/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.Set;

import megamek.common.Targetable;

/**
 * @author Deric Page (deric dot page at usa dot net)
 * @since 9/5/14 2:48 PM
 */
public interface IHonorUtil {

    /**
     * Indicates whether or not the identified unit can be considered broken.
     *
     * @param targetId The target to be checked.
     * @param playerId The ID of the player owning the target.
     *
     * @return TRUE if the unit is on the broken units list without being on the honorless enemies list.
     */
    boolean isEnemyBroken(int targetId, int playerId, boolean forcedWithdrawal);

    /**
     * Indicates whether or not the identified player is on the dishonored enemies list.
     *
     * @param playerId The ID of the player to be checked.
     *
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
     * Checks the given {@link Targetable} to see if it should be counted as broken:<br> Forced Withdrawal is turned
     * on<br> Given unit is Crippled<br>
     *
     * @param target           The unit to be checked.
     * @param forcedWithdrawal Set TRUE if the Forced Withdrawal rule is in effect.
     */
    void checkEnemyBroken(Targetable target, boolean forcedWithdrawal);

    Set<Integer> getDishonoredEnemies();

    boolean iAmAPirate();
}
