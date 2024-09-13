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
package megamek.common.loaders;

/**
 * This exception is thrown if a mek or other cannot be properly saved from a
 * file due to IO errors, file format errors, missing fields, or whatever.
 *
 * @author sleet01
 */
public class EntitySavingException extends Exception {
    private static final long serialVersionUID = -4472736483205970852L;

    /**
     * Creates new <code>EntityLoadingException</code> without detail message.
     */
    public EntitySavingException() {

    }

    /**
     * Constructs an <code>EntityLoadingException</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public EntitySavingException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public EntitySavingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
