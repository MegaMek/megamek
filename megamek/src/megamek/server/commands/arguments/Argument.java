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

/**
 * Generic Argument class, can be extended for different argument types for server commands
 * @param <T>
 * @author Luana Coppio
 */
public abstract class Argument<T> {
    protected T value;
    private final String name;
    private final String description;

    /**
     * Constructor for Generic Argument
     * @param name          name of the argument
     * @param description   description of the argument
     */
    public Argument(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public T getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return the string representation of the argument
     */
    public String getRepr() {
        return "<" + getName() + "=#>";
    }

    public abstract String getHelp();

    public abstract void parse(String input) throws IllegalArgumentException;

}
