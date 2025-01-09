/*
 * Copyright (c) 2024-2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */

package megamek.common.copy;

public interface RefBreak<T> {

    /**
     * Copy the object, creating a new instance with the same values.
     * This is NOT a deep or a perfect copy, it is instead a "serviceable" copy. It allows to create a new instance
     * that has NO references to the previous one, so it can run on a different thread or be modified without affecting
     * the original object.
     * If the object it returns is incomplete for your purposes, you should override this method and complete it or expand the
     * implementation.
     * @return a new instance of the object with the same values.
     */
    T copy();

}
