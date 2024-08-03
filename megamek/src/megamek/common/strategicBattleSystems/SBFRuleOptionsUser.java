/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.strategicBattleSystems;

import megamek.common.options.SBFRuleOptions;

/**
 * This interface is implemented by objects that regularly need to check the rules options
 * used in an SBF game. It is meant as a convenience and provides methods to easily check which rules
 * are in effect.
 */
public interface SBFRuleOptionsUser {

    /**
     * This method must be implemented to provide access to the SBF options used in the present game
     * so that the convenience methods of this interface work correctly.
     *
     * @return the SBFRuleOptions used by the present SBF game
     */
    SBFRuleOptions getOptions();

    /**
     * @return True when this game uses Detection and Reconnaissance, IO:BF p.195 aka double blind.
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
     * @return True when in this game, players on a team share their vision. This has no effect unless
     * the game also uses Detection and Reconnaissance.
     * @see #usesDoubleBlind()
     */
    default boolean usesTeamVision() {
        return getOptions().booleanOption(SBFRuleOptions.BASE_TEAM_VISION);
    }
}
