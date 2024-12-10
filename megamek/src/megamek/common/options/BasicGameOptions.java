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
import java.util.Hashtable;
import java.util.Vector;

public abstract class BasicGameOptions extends AbstractOptions {


    private static final Option.OptionValue[] baseOptions = {
        Option.of(OptionsConstants.BASE_FRIENDLY_FIRE, false),
        Option.of(OptionsConstants.BASE_SKIP_INELIGIBLE_MOVEMENT, false),
        Option.of(OptionsConstants.BASE_SKIP_INELIGIBLE_FIRING, false),
        Option.of(OptionsConstants.BASE_SKIP_INELIGIBLE_PHYSICAL, true),
        Option.of(OptionsConstants.BASE_TEAM_INITIATIVE, true),
        Option.of(OptionsConstants.BASE_AUTOSAVE_MSG, true),
        Option.of(OptionsConstants.BASE_PARANOID_AUTOSAVE, false),
        Option.of(OptionsConstants.BASE_MAX_NUMBER_ROUND_SAVES, 3),
        Option.of(OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT, true),
        Option.of(OptionsConstants.BASE_BLIND_DROP, false),
        Option.of(OptionsConstants.BASE_REAL_BLIND_DROP, false),
        Option.of(OptionsConstants.BASE_SET_ARTY_PLAYER_HOMEEDGE, false),
        Option.of(OptionsConstants.BASE_SET_DEFAULT_TEAM_1, false),
        Option.of(OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER0, false),
        Option.of(OptionsConstants.BASE_RESTRICT_GAME_COMMANDS, false),
        Option.of(OptionsConstants.BASE_DISABLE_LOCAL_SAVE, false),
        Option.of(OptionsConstants.BASE_BRIDGECF, 0),
        Option.of(OptionsConstants.BASE_RNG_TYPE, 1),
        Option.of(OptionsConstants.BASE_RNG_LOG, false),
        Option.of(OptionsConstants.BASE_TURN_TIMER_TARGETING, 0),
        Option.of(OptionsConstants.BASE_TURN_TIMER_MOVEMENT, 0),
        Option.of(OptionsConstants.BASE_TURN_TIMER_FIRING, 0),
        Option.of(OptionsConstants.BASE_TURN_TIMER_PHYSICAL, 0),
        Option.of(OptionsConstants.BASE_TURN_TIMER_ALLOW_EXTENSION, true),
        Option.of(OptionsConstants.BASE_SUPPRESS_UNIT_TOOLTIP_IN_REPORT_LOG, true),
        Option.of(OptionsConstants.BASE_GM_CONTROLS_DONE_REPORT_PHASE, false),
        Option.of(OptionsConstants.BASE_HIDE_UNOFFICIAL, false),
        Option.of(OptionsConstants.BASE_HIDE_LEGACY, false)
    };

    private static final Option.OptionValue[] victoryOptions = {
        Option.of(OptionsConstants.VICTORY_CHECK_VICTORY, true)
    };


    private void addOptions(IBasicOptionGroup group, Option.OptionValue[] options) {
        for (var entry : options) {
            addOption(group, entry.getName(), entry.getType(), entry.getValue());
        }
    }

    @Override
    public synchronized void initialize() {
        IBasicOptionGroup base = addGroup("basic");
        addOptions(base, baseOptions);

        IBasicOptionGroup victory = addGroup("victory");
        addOptions(victory, victoryOptions);
    }

    public abstract Vector<IOption> loadOptions();

    public abstract Vector<IOption> loadOptions(File file, boolean print);

}
