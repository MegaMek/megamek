/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.autoresolve.acar;

import megamek.common.options.AbstractOptions;
import megamek.common.options.AbstractOptionsInfo;
import megamek.common.options.OptionsConstants;

/**
 * @author Luana Coppio
 */
public class SimulationOptions extends AbstractOptions {

    public static final SimulationOptions EMPTY = empty();

    public static SimulationOptions empty() {
        return new SimulationOptions(null);
    }

    public SimulationOptions(AbstractOptions abstractOptions) {
        if (abstractOptions != null) {
            this.optionsHash.putAll(abstractOptions.getOptionMap());
        }
    }

    @Override
    protected void initialize() {
        // do nothing
    }

    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return SimulationOptionsInfo.instance;
    }

    private static class SimulationOptionsInfo extends AbstractOptionsInfo {
        private static final AbstractOptionsInfo instance = new SimulationOptionsInfo();

        protected SimulationOptionsInfo() {
            super("SimulationOptions");
        }
    }


    @Override
    public int count() {
        return optionsHash.size();
    }

    @Override
    public int count(String groupKey) {
        return optionsHash.size();
    }

    @Override
    public int intOption(String name) {
        var option = this.getOption(name);

        if (option != null) {
            option.intValue();
        }
        return 0;
    }

    @Override
    public boolean booleanOption(String name) {
        var option = this.getOption(name);

        if (option != null) {
            option.booleanValue();
        }

        return switch (name) {
            case OptionsConstants.VICTORY_USE_BV_DESTROYED,
                 OptionsConstants.VICTORY_USE_BV_RATIO,
                 OptionsConstants.VICTORY_USE_KILL_COUNT,
                 OptionsConstants.VICTORY_COMMANDER_KILLED -> false;
            default -> true;
        };
    }

}
