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

import java.util.Map;

public class Arguments {

    private final Map<String, Argument<?>> arguments;

    public Arguments(Map<String, Argument<?>> arguments) {
        this.arguments = arguments;
    }

    public Argument<?> get(String name) {
        return arguments.get(name);
    }

    public <T> T get(String name, Class<T> clazz) {
        if (!arguments.containsKey(name)) {
            throw new IllegalArgumentException("Argument " + name + " not found.");
        }
        var argument = arguments.get(name);
        if (!clazz.isInstance(argument)) {
            throw new IllegalArgumentException("Argument "
                  + name
                  + " is not of type "
                  + clazz.getSimpleName()
                  + " it is "
                  + argument.getClass().getSimpleName());
        } else {
            // noinspection unchecked
            return (T) argument;
        }
    }

    public <T> T getValue(String name, Class<T> clazz) {
        if (!arguments.containsKey(name)) {
            throw new IllegalArgumentException("Argument " + name + " not found.");
        }
        var argument = arguments.get(name);
        if (!clazz.isInstance(argument.getValue())) {
            throw new IllegalArgumentException("Argument "
                  + name
                  + " is not of type "
                  + clazz.getSimpleName()
                  + " it is "
                  + argument.getClass().getSimpleName());
        } else {
            // noinspection unchecked
            return (T) argument.getValue();
        }
    }

    public boolean getBoolean(String name) {
        return getValue(name, Boolean.class);
    }

    public int getInt(String name) {
        return getValue(name, Integer.class);
    }

    public String getString(String name) {
        return getValue(name, String.class);
    }

    public <T extends Enum<?>> T getEnum(String name, Class<T> clazz) {
        return getValue(name, clazz);
    }

    public boolean hasArg(String name) {
        return arguments.containsKey(name);
    }

    @Override
    public String toString() {
        return "Arguments{" +
              "arguments=" + arguments +
              '}';
    }
}
