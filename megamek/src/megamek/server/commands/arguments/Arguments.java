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

import megamek.common.Coords;

import java.util.List;
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
            throw new IllegalArgumentException("Argument " + name + " is not of type " + clazz.getSimpleName() + " it is " + argument.getClass().getSimpleName());
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
            throw new IllegalArgumentException("Argument " + name + " is not of type " + clazz.getSimpleName() + " it is " + argument.getClass().getSimpleName());
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
