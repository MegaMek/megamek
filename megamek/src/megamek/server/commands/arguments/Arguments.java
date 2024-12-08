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

import java.util.Map;

public class Arguments {

    private final Map<String, Argument<?>> arguments;

    public Arguments(Map<String, Argument<?>> arguments) {
        this.arguments = arguments;
    }

    public Argument<?> get(String name) {
        return arguments.get(name);
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
