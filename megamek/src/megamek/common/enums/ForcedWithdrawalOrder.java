/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.enums;

/**
 * A gamemaster's standing order on whether a bot-controlled unit withdraws under Forced Withdrawal, set from the
 * Edit Damage dialog. It lets a scenario pull an unhurt unit off the field, or keep a crippled one fighting, without
 * changing the bot's own behavior settings.
 */
public enum ForcedWithdrawalOrder {
    /** No order: the unit withdraws once crippled, if its bot follows Forced Withdrawal. */
    BOT_RULES,
    /** The unit withdraws now, whatever its condition, even if its bot does not follow Forced Withdrawal. */
    WITHDRAW,
    /** The unit never withdraws, even when crippled: a fight to the death. */
    FIGHT_TO_THE_DEATH;

    /**
     * Returns whether a unit under this order withdraws under Forced Withdrawal. This is the one place that decides
     * it, so the bot's movement, firing and honor rules all agree.
     *
     * @param botFollowsForcedWithdrawal {@code true} if the unit's bot follows the Forced Withdrawal rules
     * @param isCrippled                 {@code true} if the unit is crippled, counting crew damage
     *
     * @return {@code true} if the unit withdraws
     */
    public boolean isWithdrawing(boolean botFollowsForcedWithdrawal, boolean isCrippled) {
        return switch (this) {
            case BOT_RULES -> botFollowsForcedWithdrawal && isCrippled;
            case WITHDRAW -> true;
            case FIGHT_TO_THE_DEATH -> false;
        };
    }
}
