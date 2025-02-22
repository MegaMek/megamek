/*
 * MegaMek - Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server.commands.arguments;

import megamek.client.ui.Messages;
import megamek.codeUtilities.StringUtility;
import megamek.common.util.StringUtil;

/**
 * Argument for a special type that can be Increase, Decrease or Set an Integer value.
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
        return getDescription() + (minValue == Integer.MIN_VALUE ? "": " Min: " + minValue) +
            (maxValue == Integer.MAX_VALUE ? "": " Max: " + maxValue) +
                " " + Messages.getString("Gamemaster.cmd.params.required");
    }

}
