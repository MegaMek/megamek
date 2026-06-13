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
 * Argument for a String type.
 *
 * @author Luana Coppio
 */
public class StringArgument extends Argument<String> {
    private final String defaultValue;

    public StringArgument(String name, String description, String defaultValue) {
        super(name, description);
        this.defaultValue = defaultValue;
    }

    public StringArgument(String name, String description) {
        this(name, description, null);
    }

    @Override
    public String getValue() {
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
        value = input;
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
