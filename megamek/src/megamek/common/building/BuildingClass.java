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


public enum BuildingClass {

    //                  dmgFr dmgTo
    STANDARD        (   0.5,  1    ),
    HANGAR          (   1,    1    ),
    FORTRESS        (   2,    0.5  ),
    GUN_EMPLACEMENT (   2,    0.5  );
    // CASTLE_BRIAN    (  10,   0.1  );

    // LATER Investigate why Castle Brian was originally left out and see to
    //       add support for it
    //
    // A comment originally in Building.java stated:
    //
    // leaving out Castles Brian until issues with damage scaling are resolved

    /**
     * Retrieves the {@linkplain BuildingClass} corresponding to the given
     * integer id, if it's valid (ie: in [0,3]).
     *         
     * @see #getId()
     */
    public static Optional<BuildingClass> ofId(int id) {
        try {
            return Optional.of(BuildingClass.values()[id]);
        } catch (@SuppressWarnings("unused") ArrayIndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    /**
     * Same as {@link #ofId(int)}, but throws an exception on invalid ids
     */
    public static BuildingClass ofRequiredId(int id) throws IllegalArgumentException {
        try {
            return BuildingClass.values()[id];
        } catch (@SuppressWarnings("unused") ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(Integer.toString(id));
        }
    }

    private BuildingClass(double damageFromScaleMultiplier, double damageToScaleMultiplier) {
        this.damageFromScaleMultiplier = damageFromScaleMultiplier;
        this.damageToScaleMultiplier   = damageToScaleMultiplier;
    }

    private final double damageFromScaleMultiplier;
    private final double damageToScaleMultiplier;

    /**
     * Retrieves the identifier corresponding to this building class.
     * 
     * Values are the same as the "old" constants in {@link Building}:
     * 
     * <pre>
     *    public static final int STANDARD = 0;
     *    public static final int HANGAR = 1;
     *    public static final int FORTRESS = 2;
     *    public static final int GUN_EMPLACEMENT = 3;
     * </pre>
     * 
     * @return the id corresponding to this construction type
     */
    public int getId() {
        return ordinal();
    }

    /**
     * @return the damage scale multiplier for units passing through this
     *         building
     */
    public double getDamageFromScaleMultiplier() {
        return damageFromScaleMultiplier;
    }

    /**
     * @return the damage scale multiplier for damage applied to this building
     *         (and occupants)
     */
    public double getDamageToScaleMultiplier() {
        return damageToScaleMultiplier;
    }

}
