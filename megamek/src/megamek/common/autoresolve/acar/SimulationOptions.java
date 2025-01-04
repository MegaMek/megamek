/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.autoresolve.acar;

import megamek.common.options.AbstractOptions;
import megamek.common.options.AbstractOptionsInfo;
import megamek.common.options.OptionsConstants;

/**
 * @author Luana Coppio
 */
public class SimulationOptions extends AbstractOptions  {

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
