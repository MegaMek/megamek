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
package megamek.common.options;

import java.io.File;
import java.util.Vector;

public abstract class BasicGameOptions extends AbstractOptions {

    @Override
    public synchronized void initialize() {
        IBasicOptionGroup base = addGroup("basic");
        addOption(base, OptionsConstants.BASE_FRIENDLY_FIRE, false);
        addOption(base, OptionsConstants.BASE_SKIP_INELIGABLE_MOVEMENT, false);
        addOption(base, OptionsConstants.BASE_SKIP_INELIGABLE_FIRING, false);
        addOption(base, OptionsConstants.BASE_SKIP_INELIGABLE_PHYSICAL, true);
        addOption(base, OptionsConstants.BASE_TEAM_INITIATIVE, true);
        addOption(base, OptionsConstants.BASE_AUTOSAVE_MSG, true);
        addOption(base, OptionsConstants.BASE_PARANOID_AUTOSAVE, false);
        addOption(base, OptionsConstants.BASE_MAX_NUMBER_ROUND_SAVES, 3);
        addOption(base, OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT, true);
        addOption(base, OptionsConstants.BASE_BLIND_DROP, false);
        addOption(base, OptionsConstants.BASE_REAL_BLIND_DROP, false);
        addOption(base, OptionsConstants.BASE_SET_ARTY_PLAYER_HOMEEDGE, false);
        addOption(base, OptionsConstants.BASE_SET_DEFAULT_TEAM_1, false);
        addOption(base, OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER0, false);
        addOption(base, OptionsConstants.BASE_RESTRICT_GAME_COMMANDS, false);
        addOption(base, OptionsConstants.BASE_DISABLE_LOCAL_SAVE, false);
        addOption(base, OptionsConstants.BASE_BRIDGECF, 0);
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
