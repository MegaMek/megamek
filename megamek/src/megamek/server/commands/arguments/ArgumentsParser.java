/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArgumentsParser {
    private static final String EMPTY_ARGUMENT = null;

    private ArgumentsParser() {}

    // Parses the arguments using the definition
    private static Map<String, Argument<?>> parseArguments(String[] args, List<Argument<?>> argumentDefinitions) {
        Map<String, Argument<?>> parsedArguments = new HashMap<>();
        List<String> positionalArguments = new ArrayList<>();

        // Map argument names to definitions for easy lookup
        Map<String, Argument<?>> argumentMap = new HashMap<>();
        for (Argument<?> argument : argumentDefinitions) {
            argumentMap.put(argument.getName(), argument);
        }

        // Separate positional arguments and named arguments
        boolean namedArgumentStarted = false;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            String[] keyValue = arg.split("=");

            if (keyValue.length == 2) {
                // Handle named arguments
                namedArgumentStarted = true;
                String key = keyValue[0];
                String value = keyValue[1];

                if (!argumentMap.containsKey(key)) {
                    throw new IllegalArgumentException("Unknown argument: " + key);
                }

                Argument<?> argument = argumentMap.get(key);
                argument.parse(value);
                parsedArguments.put(key, argument);
            } else {
                // Handle positional arguments
                if (namedArgumentStarted) {
                    throw new IllegalArgumentException("Positional arguments cannot come after named arguments.");
                }
                positionalArguments.add(arg);
            }
        }

        // Parse positional arguments
        int index = 0;
        for (Argument<?> argument : argumentDefinitions) {
            if (parsedArguments.containsKey(argument.getName())) {
                continue;
            }
            if (index < positionalArguments.size()) {
                String value = positionalArguments.get(index);
                argument.parse(value);
                parsedArguments.put(argument.getName(), argument);
                index++;
            } else {
                // designed to throw an error if the arg doesn't have a default value
                argument.parse(EMPTY_ARGUMENT);
                parsedArguments.put(argument.getName(), argument);
            }
        }

        return parsedArguments;
    }

    public static Arguments parse(String[] args, List<Argument<?>> argumentDefinitions) {
        Map<String, Argument<?>> parsedArguments = parseArguments(args, argumentDefinitions);
        return new Arguments(parsedArguments);
    }
}
