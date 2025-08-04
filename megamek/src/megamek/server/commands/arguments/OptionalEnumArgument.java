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

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import megamek.client.ui.Messages;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Nullable Argument for an Enum type.
 *
 * @param <E>
 *
 * @author Luana Coppio
 */
public class OptionalEnumArgument<E extends Enum<E>> extends EnumArgument<E> {

    public OptionalEnumArgument(String name, String description, Class<E> enumType) {
        super(name, description, enumType, null);
    }

    @Override
    public void parse(String input) throws IllegalArgumentException {
        if (input == null) {
            return;
        }
        try {
            if (NumberUtils.isCreatable(input)) {
                value = enumType.getEnumConstants()[Integer.parseInt(input)];
            } else {
                value = Enum.valueOf(enumType, input.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(getName() + " must be one of: " + getEnumConstantsString());
        }
    }

    public boolean isPresent() {
        return value != null;
    }

    public boolean isEmpty() {
        return value == null;
    }

    private String getEnumConstantsString() {
        return IntStream.range(0, enumType.getEnumConstants().length)
              .mapToObj(i -> i + ": " + enumType.getEnumConstants()[i])
              .collect(Collectors.joining(", "));
    }

    @Override
    public String getHelp() {
        return getDescription() +
              " [" + getEnumConstantsString() + "] " +
              (defaultValue != null ? " [default: " + defaultValue + "]. " : ". ") +
              Messages.getString("Gamemaster.cmd.params.optional");
    }

}
