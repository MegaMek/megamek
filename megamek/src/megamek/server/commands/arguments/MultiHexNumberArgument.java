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

import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.Coords;

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
 *
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
