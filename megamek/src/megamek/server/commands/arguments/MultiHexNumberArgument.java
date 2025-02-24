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
import megamek.common.Coords;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Argument for a List of Coords from HexNumber type.</p>
 * <p>It accepts multiple hexNumbers and X,Y positions with the following patters</p>
 * <pre>{@code //pattern 1
 * String pattern1 = "1,2-3,4-12,44-151,4";
 * String pattern2 = "0102-0342-1244-151004";
 *
 * MultiHexNumberArgument arg1 = new MultiHexNumberArgument(name, description).parse(pattern1);
 * MultiHexNumberArgument arg2 = new MultiHexNumberArgument(name, description).parse(pattern2);
 * assert args1.getValue() == List.of(new Coords(0, 1), new Coords(2, 3), new Coords(11, 43), new Coords(150, 3));
 * assert args1.getValue() == args2.getValue();}</pre>
 * @author Luana Coppio
 */
public class MultiHexNumberArgument extends Argument<List<Coords>> {

    public MultiHexNumberArgument(String name, String description) {
        super(name, description);
    }

    @Override
    public List<Coords> getValue() {
        return value;
    }

    @Override
    public void parse(String input) throws IllegalArgumentException {
        if (input == null) {
            throw new IllegalArgumentException(getName() + " is required.");
        }
        try {
            List<Coords> coords = new ArrayList<>();
            for (var hex : input.split("-")) {
                Coords parsedValue = Coords.parseHexNumber(hex);
                coords.add(parsedValue);
                if (parsedValue.getX() < 0 || parsedValue.getY() < 0) {
                    throw new IllegalArgumentException(getName() + " must be a positive coordinate.");
                }
            }
            value = coords;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(getName() + " must be an a valid X,Y or HexNumber.");
        }
    }

    @Override
    public String getHelp() {
        return getDescription() + " " + Messages.getString("Gamemaster.cmd.params.required");
    }

}
