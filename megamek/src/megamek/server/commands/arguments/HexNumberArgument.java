/*
 * MegaMek - Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.Coords;

/**
 * Argument for a Coords from HexNumber type.
 * @author Luana Coppio
 */
public class HexNumberArgument extends Argument<Coords> {

    public HexNumberArgument(String name, String description) {
        super(name, description);
    }

    @Override
    public Coords getValue() {
        return value;
    }

    @Override
    public void parse(String input) throws IllegalArgumentException {
        if (input == null) {
            throw new IllegalArgumentException(getName() + " is required.");
        }
        try {
            Coords parsedValue = Coords.parseHexNumber(input);
            if (parsedValue.getX() < 0 || parsedValue.getY() < 0) {
                throw new IllegalArgumentException(getName() + " must be a positive coordinate.");
            }
            value = parsedValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(getName() + " must be an a valid X,Y or HexNumber.");
        }
    }

    @Override
    public String getHelp() {
        return getDescription() + " " + Messages.getString("Gamemaster.cmd.params.required");
    }

}
