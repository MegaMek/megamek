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

import java.util.Optional;

/**
 * Optional Argument for a Password String type.
 * @author Luana Coppio
 */
public class OptionalPasswordArgument extends Argument<Optional<String>> {

    public OptionalPasswordArgument(String name, String description) {
        super(name, description);
    }

    @Override
    public Optional<String> getValue() {
        return value;
    }

    @Override
    public void parse(String input) throws IllegalArgumentException {
        if (input == null) {
            value = Optional.empty();
            return;
        }
        value = Optional.of(input);

    }

    @Override
    public String getRepr() {
        return "[" + getName() + "]";
    }

    @Override
    public String getHelp() {
        return getDescription() + ". " + Messages.getString("Gamemaster.cmd.params.optional");
    }
}
