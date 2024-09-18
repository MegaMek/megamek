/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

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
