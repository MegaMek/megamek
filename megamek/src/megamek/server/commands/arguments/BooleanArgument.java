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

import java.util.List;

/**
 * Argument for a boolean type.
 * @author Luana Coppio
 */
public class BooleanArgument extends Argument<Boolean> {
    private final Boolean defaultValue;

    public BooleanArgument(String name, String description, Boolean defaultValue) {
        super(name, description);
        this.defaultValue = defaultValue;
    }

    public BooleanArgument(String name, String description) {
        this(name, description, null);
    }

    @Override
    public Boolean getValue() {
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
        value = List.of("true", "yes", "1", "on", "y").contains(input.toLowerCase());
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    @Override
    public String getHelp() {
        return getDescription() +
            (defaultValue != null ?
                " [default: " + defaultValue + "]. " + Messages.getString("Gamemaster.cmd.params.optional") :
                " " + Messages.getString("Gamemaster.cmd.params.required"));
    }

}
