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

package megamek.server.commands.arguments;

import megamek.client.ui.Messages;

/**
 * Argument for an Integer type.
 *
 * @author Luana Coppio
 */
public class IntegerArgument extends Argument<Integer> {
    private final int minValue;
    private final int maxValue;
    private final Integer defaultValue;

    public IntegerArgument(String name, String description) {
        this(name, description, Integer.MIN_VALUE, Integer.MAX_VALUE, null);
    }

    public IntegerArgument(String name, String description, int minValue, int maxValue) {
        this(name, description, minValue, maxValue, null);
    }

    public IntegerArgument(String name, String description, int minValue, int maxValue, Integer defaultValue) {
        super(name, description);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
    }

    @Override
    public Integer getValue() {
        if (value == null && defaultValue != null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public void parse(String input) throws IllegalArgumentException {
        if (input == null && defaultValue != null) {
            value = defaultValue;
            return;
        } else {
            if (input == null) {
                throw new IllegalArgumentException(getName() + " is required.");
            }
        }
        try {
            int parsedValue = Integer.parseInt(input);
            if (parsedValue < minValue || parsedValue > maxValue) {
                throw new IllegalArgumentException(getName() + " must be between " + minValue + " and " + maxValue);
            }
            value = parsedValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(getName() + " must be an integer.");
        }
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    @Override
    public String getHelp() {
        return getDescription() + (minValue == Integer.MIN_VALUE ? "" : " Min: " + minValue) +
              (maxValue == Integer.MAX_VALUE ? "" : " Max: " + maxValue) +
              (defaultValue != null ?
                    " [default: " + defaultValue + "]. " + Messages.getString("Gamemaster.cmd.params.optional") :
                    " " + Messages.getString("Gamemaster.cmd.params.required"));
    }

}
