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
 * Argument for an Integer type for player ID.
 * @author Luana Coppio
 */
public class PlayerArgument extends Argument<Integer> {

    public PlayerArgument(String name, String description) {
        super(name, description);
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public void parse(String input) throws IllegalArgumentException {
        if (input == null) {
            throw new IllegalArgumentException(getName() + " is required.");
        }
        try {
            int parsedValue = Integer.parseInt(input);
            if (parsedValue < 0) {
                throw new IllegalArgumentException(getName() + " must be an integer ID of a player.");
            }
            value = parsedValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(getName() + " must be an integer ID of a player.");
        }
    }

    @Override
    public String getHelp() {
        return getDescription() + " " + Messages.getString("Gamemaster.cmd.params.required");
    }
}
