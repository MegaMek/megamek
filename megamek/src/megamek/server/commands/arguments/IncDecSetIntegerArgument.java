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

package megamek.server.commands.arguments;

import megamek.client.ui.Messages;
import megamek.codeUtilities.StringUtility;
import megamek.common.util.StringUtil;

/**
 * Argument for a special type that can be Increase, Decrease or Set an Integer value.
 *
 * @author Luana Coppio
 */
public class IncDecSetIntegerArgument extends Argument<Integer> {
    public enum Operation {
        CHANGE, SET
    }

    private final int minValue;
    private final int maxValue;
    private Operation operation;

    public IncDecSetIntegerArgument(String name, String description, int minValue, int maxValue) {
        super(name, description);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    public Operation getOperation() {
        return operation;
    }

    @Override
    public void parse(String input) throws IllegalArgumentException {
        if (input == null) {
            throw new IllegalArgumentException(getName() + " is required.");
        }
        try {
            operation = Operation.SET;
            if (StringUtility.isNullOrBlank(input)) {
                value = 0;
            } else if (StringUtil.isNumeric(input)) {
                value = Integer.parseInt(input);
            } else {
                int adjustment = 0;
                for (final char tick : input.toCharArray()) {
                    if ('+' == tick) {
                        adjustment++;
                    } else if ('-' == tick) {
                        adjustment--;
                    }
                }
                value = adjustment;
                operation = Operation.CHANGE;
            }

            if (Operation.SET.equals(operation) && (value < minValue) || (value > maxValue)) {
                throw new IllegalArgumentException(getName() + " must be between " + minValue + " and " + maxValue);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(getName() + " must be an integer.");
        }
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
              " " + Messages.getString("Gamemaster.cmd.params.required");
    }

}
