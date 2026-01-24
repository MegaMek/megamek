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

package megamek.common.options;

import java.io.File;
import java.util.Vector;

public abstract class BasicGameOptions extends AbstractOptions {

    @Override
    public synchronized void initialize() {
        IBasicOptionGroup base = addGroup("basic");
        addOption(base, OptionsConstants.BASE_FRIENDLY_FIRE, false);
        addOption(base, OptionsConstants.BASE_SKIP_INELIGIBLE_MOVEMENT, false);
        addOption(base, OptionsConstants.BASE_SKIP_INELIGIBLE_FIRING, false);
        addOption(base, OptionsConstants.BASE_SKIP_INELIGIBLE_PHYSICAL, true);
        addOption(base, OptionsConstants.BASE_TEAM_INITIATIVE, true);
        addOption(base, OptionsConstants.BASE_AUTOSAVE_MSG, true);
        addOption(base, OptionsConstants.BASE_PARANOID_AUTOSAVE, false);
        addOption(base, OptionsConstants.BASE_MAX_NUMBER_ROUND_SAVES, 3);
        addOption(base, OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT, true);
        addOption(base, OptionsConstants.BASE_BLIND_DROP, false);
        addOption(base, OptionsConstants.BASE_REAL_BLIND_DROP, false);
        addOption(base, OptionsConstants.BASE_SET_ARTY_PLAYER_HOME_EDGE, false);
        addOption(base, OptionsConstants.BASE_SET_DEFAULT_TEAM_1, false);
        addOption(base, OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER_0, false);
        addOption(base, OptionsConstants.BASE_RESTRICT_GAME_COMMANDS, false);
        addOption(base, OptionsConstants.BASE_DISABLE_LOCAL_SAVE, false);
        addOption(base, OptionsConstants.BASE_BRIDGE_CF, 0);
        addOption(base, OptionsConstants.BASE_RNG_TYPE, 1);
        addOption(base, OptionsConstants.BASE_RNG_LOG, false);
        addOption(base, OptionsConstants.BASE_TURN_TIMER_TARGETING, 0);
        addOption(base, OptionsConstants.BASE_TURN_TIMER_MOVEMENT, 0);
        addOption(base, OptionsConstants.BASE_TURN_TIMER_FIRING, 0);
        addOption(base, OptionsConstants.BASE_TURN_TIMER_PHYSICAL, 0);
        addOption(base, OptionsConstants.BASE_TURN_TIMER_ALLOW_EXTENSION, true);
        addOption(base, OptionsConstants.BASE_SUPPRESS_UNIT_TOOLTIP_IN_REPORT_LOG, true);
        addOption(base, OptionsConstants.BASE_GM_CONTROLS_DONE_REPORT_PHASE, false);
        addOption(base, OptionsConstants.BASE_HIDE_UNOFFICIAL, false);
        addOption(base, OptionsConstants.BASE_HIDE_LEGACY, false);

        IBasicOptionGroup victory = addGroup("victory");
        addOption(victory, OptionsConstants.VICTORY_CHECK_VICTORY, true);
    }

    public abstract Vector<IOption> loadOptions();

    public abstract Vector<IOption> loadOptions(File file, boolean print);

}
