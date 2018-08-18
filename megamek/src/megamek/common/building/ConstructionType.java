/*
 * Copyright (c) 2018 The MegaMek Team. All rights reserved.
 *
 * This file is part of MegaMek.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.building;

import java.util.Optional;

public enum ConstructionType {

    // LATER Double-check that WALL really was intended as a construction type
    //
    // The comment in Building.getType() used to be:
    //
    //     Get the construction type of the building.
    //     This value will be one of the constants, LIGHT, MEDIUM, HEAVY, or HARDENED.
    //
    // which seems to imply that WALL is not a valid value for a Building's type?
    //
    // But then, WALL clearly was intended as a construction type constant...
    //
    //    /**
    //     * Various construction types.
    //     */
    //    public static final int LIGHT = 1;
    //    public static final int MEDIUM = 2;
    //    public static final int HEAVY = 3;
    //    public static final int HARDENED = 4;
    //    public static final int WALL = 5;

    //         id    CF   DR/in  DR/out
    LIGHT    (  1,   15,  0,      0.75f  ),
    MEDIUM   (  2,   40,  0,      0.5f   ),
    HEAVY    (  3,   90,  0.5f,   0.25f  ),
    HARDENED (  4,  120,  0.75f,  0      ),
    WALL     (  5,  120,  0,      0      );

    /**
     * Retrieves the {@linkplain ConstructionType} corresponding to the given
     * integer id, if it's valid (ie: in [1,5]).
     *         
     * @see #getId()
     */
    public static Optional<ConstructionType> ofId(int id) {
        for (ConstructionType v : values()) {
            if (id == v.id) return Optional.of(v);
        }
        return Optional.empty();
    }

    /**
     * Same as {@link #ofId(int)}, but throws an exception on invalid ids
     */
    public static ConstructionType ofRequiredId(int id) throws IllegalArgumentException {
        return ofId(id).orElseThrow(() -> new IllegalArgumentException(Integer.toString(id)));
    }

    private ConstructionType( int id,
                              int defaultCF,
                              float damageReductionFromInside,
                              float damageReductionFromOutside ) {
        this.id = id;
        this.defaultCF = defaultCF;
        this.damageReductionFromInside = damageReductionFromInside;
        this.damageReductionFromOutside = damageReductionFromOutside;
    }

    private final int id;
    private final int defaultCF;
    private final float damageReductionFromInside;
    private final float damageReductionFromOutside;

    /**
     * Retrieves the identifier corresponding to this construction type.
     * 
     * Please note this is <em>not</em> the same as {@link #ordinal()} and
     * values are instead the same as the "old" constants in {@link Building}:
     * 
     * <pre>
     * public static final int LIGHT = 1;
     * public static final int MEDIUM = 2;
     * public static final int HEAVY = 3;
     * public static final int HARDENED = 4;
     * public static final int WALL = 5;
     * </pre>
     * 
     * @return the id corresponding to this construction type
     */
    public int getId() {
        return id;
    }

    /**
     * @return the default CF for this construction type
     */
    public int getDefaultCF() {
        return defaultCF;
    }

    /**
     * Per page 175 of Total Warfare, this is how much damage the building
     * absorbs when combat occurs inside the building.
     *
     * @returns the fraction of damage absorbed by the building
     */
    public float getDamageReductionFromInside() {
        return damageReductionFromInside;
    }

    /**
     * Per page 172 of Total Warfare, this is the fraction of a weapon's
     * damage that passes through to infantry inside the building when a
     * building is hit.
     *
     * @return the fraction of damaged applied to infantry
     */
    public float getDamageReductionFromOutside() {
        return damageReductionFromOutside;
    }

}
