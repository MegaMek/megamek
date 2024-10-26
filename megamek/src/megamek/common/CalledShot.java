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
package megamek.common;

import java.io.Serializable;

import megamek.common.annotations.Nullable;

/**
 * Some static methods for called shots
 */
public class CalledShot implements Serializable {
    private static final long serialVersionUID = 8746351140726246311L;

    // locations for called shots
    public static final int CALLED_NONE = 0;
    public static final int CALLED_HIGH = 1;
    public static final int CALLED_LOW = 2;
    public static final int CALLED_LEFT = 3;
    public static final int CALLED_RIGHT = 4;
    public static final int CALLED_NUM = 5;

    private int current;
    private static final String[] calledLocNames = { "", "HIGH", "LOW", "LEFT", "RIGHT" };

    public CalledShot() {
        current = CALLED_NONE;
    }

    public String getDisplayableName() {
        if (current >= CALLED_NUM) {
            return "Unknown";
        }
        return calledLocNames[current];
    }

    public int switchCalledShot() {
        current = current + 1;
        if (current >= CALLED_NUM) {
            current = CALLED_NONE;
        }
        return current;
    }

    public int getCall() {
        return current;
    }

    public @Nullable String isValid(Targetable target) {
        if (current == CALLED_NONE) {
            return null;
        }

        if (!(target instanceof Entity)) {
            return "called shots on entities only";
        }
        Entity te = (Entity) target;
        if (te instanceof Infantry || te instanceof ProtoMek) {
            return "no called shots on infantry/ProtoMeks";
        }

        // only meks can be high or low
        if (!(te instanceof Mek) && (current == CALLED_HIGH)) {
            return "called shots (high) only on Meks";
        } else if (!(te instanceof Mek) && (current == CALLED_LOW)) {
            return "called shots (low) only on Meks";
        }

        return null;
    }

    public void reset() {
        current = CALLED_NONE;
    }
}
