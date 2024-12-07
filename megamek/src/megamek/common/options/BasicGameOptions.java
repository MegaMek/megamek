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

    private static final Object[][] baseOptions = {
        { OptionsConstants.BASE_FRIENDLY_FIRE, false },
        { OptionsConstants.BASE_SKIP_INELIGIBLE_MOVEMENT, false },
        { OptionsConstants.BASE_SKIP_INELIGIBLE_FIRING, false },
        { OptionsConstants.BASE_SKIP_INELIGIBLE_PHYSICAL, true },
        { OptionsConstants.BASE_TEAM_INITIATIVE, true },
        { OptionsConstants.BASE_AUTOSAVE_MSG, true },
        { OptionsConstants.BASE_PARANOID_AUTOSAVE, false },
        { OptionsConstants.BASE_MAX_NUMBER_ROUND_SAVES, 3 },
        { OptionsConstants.BASE_EXCLUSIVE_DB_DEPLOYMENT, true },
        { OptionsConstants.BASE_BLIND_DROP, false },
        { OptionsConstants.BASE_REAL_BLIND_DROP, false },
        { OptionsConstants.BASE_SET_ARTY_PLAYER_HOMEEDGE, false },
        { OptionsConstants.BASE_SET_DEFAULT_TEAM_1, false },
        { OptionsConstants.BASE_SET_PLAYER_DEPLOYMENT_TO_PLAYER0, false },
        { OptionsConstants.BASE_RESTRICT_GAME_COMMANDS, false },
        { OptionsConstants.BASE_DISABLE_LOCAL_SAVE, false },
        { OptionsConstants.BASE_BRIDGECF, 0 },
        { OptionsConstants.BASE_RNG_TYPE, 1 },
        { OptionsConstants.BASE_RNG_LOG, false },
        { OptionsConstants.BASE_TURN_TIMER_TARGETING, 0 },
        { OptionsConstants.BASE_TURN_TIMER_MOVEMENT, 0 },
        { OptionsConstants.BASE_TURN_TIMER_FIRING, 0 },
        { OptionsConstants.BASE_TURN_TIMER_PHYSICAL, 0 },
        { OptionsConstants.BASE_TURN_TIMER_ALLOW_EXTENSION, true },
        { OptionsConstants.BASE_SUPPRESS_UNIT_TOOLTIP_IN_REPORT_LOG, true },
        { OptionsConstants.BASE_GM_CONTROLS_DONE_REPORT_PHASE, false },
        { OptionsConstants.BASE_HIDE_UNOFFICIAL, false },
        { OptionsConstants.BASE_HIDE_LEGACY, false }
    };

    private static final Object[][] victoryOptions = {
        { OptionsConstants.VICTORY_CHECK_VICTORY, true }
    };

    private void addOptions(IBasicOptionGroup group, Object[][] options) {
        for (Object[] entry : options) {
            String name = (String) entry[0];
            Object defaultValue = entry[1];

            if (defaultValue instanceof Integer) {
                addOption(group, name, IOption.INTEGER, defaultValue);
            } else {
                // Fallback: treat as boolean if the type isn't recognized
                addOption(group, name, IOption.BOOLEAN, defaultValue);
            }
        }
    }


    @Override
    public synchronized void initialize() {
        // Pre-size HashMap to reduce rehashing (just an estimate)
        optionsHash = new Hashtable<>(512);

        IBasicOptionGroup base = addGroup("basic");
        addOptions(base, baseOptions);

        IBasicOptionGroup victory = addGroup("victory");
        addOptions(victory, victoryOptions);
    }

    public abstract Vector<IOption> loadOptions();

    public abstract Vector<IOption> loadOptions(File file, boolean print);

}
