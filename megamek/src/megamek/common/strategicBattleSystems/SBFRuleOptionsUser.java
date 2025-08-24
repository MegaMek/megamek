/*
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

package megamek.common.strategicBattleSystems;

import megamek.common.options.SBFRuleOptions;

/**
 * This interface is implemented by objects that regularly need to check the rules options used in an SBF game. It is
 * meant as a convenience and provides methods to easily check which rules are in effect.
 */
public interface SBFRuleOptionsUser {

    /**
     * This method must be implemented to provide access to the SBF options used in the present game so that the
     * convenience methods of this interface work correctly.
     *
     * @return the SBFRuleOptions used by the present SBF game
     */
    SBFRuleOptions getOptions();

    /**
     * @return True when this game uses Detection and Reconnaissance, IO:BF p.195 aka double-blind.
     */
    default boolean usesDoubleBlind() {
        return getOptions().booleanOption(SBFRuleOptions.BASE_RECON);
    }

    /**
     * @return True when this game uses Advanced Initiative, IO:BF p.194.
     */
    default boolean usesAdvancedInitiative() {
        return getOptions().booleanOption(SBFRuleOptions.INIT_MODIFIERS);
    }

    /**
     * @return True when this game uses Battlefield Intelligence, IO:BF p.206.
     */
    default boolean usesBattlefieldInt() {
        return getOptions().booleanOption(SBFRuleOptions.INIT_BATTLEFIELD_INT);
    }

    /**
     * @return True when this game allows adjusting formations, IO:BF p.198.
     */
    default boolean usesAdjustingFormations() {
        return getOptions().booleanOption(SBFRuleOptions.BASE_ADJUST_FORMATIONS);
    }

    /**
     * @return True when this game allows Sprinting movement, IO:BF p.199.
     */
    default boolean usesSprintingMove() {
        return getOptions().booleanOption(SBFRuleOptions.MOVE_SPRINT);
    }

    /**
     * @return True when in this game, players on a team share their vision. This has no effect unless the game also
     *       uses Detection and Reconnaissance.
     *
     * @see #usesDoubleBlind()
     */
    default boolean usesTeamVision() {
        return getOptions().booleanOption(SBFRuleOptions.BASE_TEAM_VISION);
    }
}
