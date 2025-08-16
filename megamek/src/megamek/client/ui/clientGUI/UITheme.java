/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a UI Theme for Swing.
 */
public class UITheme {
    private final String name;
    private final String className;

    /**
     * Creates a new {@code UITheme} class.
     *
     * @param className The class name to load the UI Theme.
     */
    public UITheme(String className) {
        this(className, className);
    }

    /**
     * Creates a new {@code UITheme} class.
     *
     * @param name      The optional name of the UI Theme.
     * @param className The class name to load the UI Theme.
     */
    public UITheme(String className, String name) {
        this.className = Objects.requireNonNull(className);
        this.name = Optional.ofNullable(name).orElse(className);
    }

    /**
     * Gets the name of the theme.
     *
     * @return The name of the theme.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the class name to load the theme with.
     *
     * @return The class name of the theme.
     */
    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return className.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other == null || other.getClass() != getClass()) {
            return false;
        } else {
            return getClassName().equals(((UITheme) other).getClassName());
        }
    }
}
