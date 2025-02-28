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

/**
 * Argument for an Integer type.
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
        return getDescription() + (minValue == Integer.MIN_VALUE ? "": " Min: " + minValue) +
            (maxValue == Integer.MAX_VALUE ? "": " Max: " + maxValue) +
            (defaultValue != null ?
                " [default: " + defaultValue + "]. " + Messages.getString("Gamemaster.cmd.params.optional") :
                " " + Messages.getString("Gamemaster.cmd.params.required"));
    }

}
