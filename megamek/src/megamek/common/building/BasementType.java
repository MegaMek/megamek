/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.building;

import java.util.Optional;

import megamek.common.Messages;

// LEGAL (giorgiga) I'm not sure the above copyright is the correct one
//
// The code in this file was originally in Building.java, so I copied the
// license header from that file.

public enum BasementType {

    // LATER UNKNOWN is a magic value and not really a basement type - see if it can be removed

    UNKNOWN               (0,0, "Building.BasementUnknown"             ), //$NON-NLS-1$
    NONE                  (1,0, "Building.BasementNone"                ), //$NON-NLS-1$
    TWO_DEEP_FEET         (2,2, "Building.BasementTwoDeepFeet"         ), //$NON-NLS-1$
    ONE_DEEP_FEET         (3,1, "Building.BasementOneDeepFeet"         ), //$NON-NLS-1$
    ONE_DEEP_NORMAL       (4,1, "Building.BasementOneDeepNormal"       ), //$NON-NLS-1$
    ONE_DEEP_NORMALINFONLY(5,1, "Building.BasementOneDeepNormalInfOnly"), //$NON-NLS-1$
    ONE_DEEP_HEAD         (6,1, "Building.BasementOneDeepHead"         ), //$NON-NLS-1$
    TWO_DEEP_HEAD         (7,2, "Building.BasementTwoDeepHead"         ); //$NON-NLS-1$

    /**
     * Retrieves the {@linkplain BasementType} corresponding to the given
     * integer id, if it's valid (ie: in [0,7]).
     *
     * @see #getId()
     */
    public static Optional<BasementType> ofId(int id) {
        for (BasementType v : values()) {
            if (id == v.id) return Optional.of(v);
        }
        return Optional.empty();
    }

    /**
     * Same as {@link #ofId(int)}, but throws an exception on invalid ids
     */
    public static BasementType ofRequiredId(int id) throws IllegalArgumentException {
        return ofId(id).orElseThrow(() -> new IllegalArgumentException(Integer.toString(id)));
    }

    private BasementType(int id, int depth, String descMsgKey) {
        this.id = id;
        this.depth = depth;
        this.desc = Messages.getString(descMsgKey);
    }

    private final int id;
    private final int depth;

    private final String desc;

    public int getId() {
        return id;
    }

    public int getDepth() {
        return depth;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * Per page 179 of Total Warfare, this is the table used to determine
     * building's basement.
     */
    public static BasementType basementsTable(int roll2d6) {
        if (2 > roll2d6 || roll2d6 > 12) {
            throw new IllegalArgumentException("roll2d6 must be in [2,12]"); //$NON-NLS-1$
        }
        switch (roll2d6) {
            case 2:  return TWO_DEEP_FEET;
            case 3:  return ONE_DEEP_FEET;
            case 4:  return ONE_DEEP_NORMAL;
            // 5-8: no basement

            // This was never returned by Building.rollBasement() where this
            // code comes from. This may be just because of a lapse of memory,
            // but it may also be that returning this value causes some issue
            // elsewhere.
            //
            // LATER investigate why this wasn't used
            //
            // case 9:  return ONE_DEEP_NORMALINFONLY;

            case 10: return ONE_DEEP_NORMAL;
            case 11: return ONE_DEEP_HEAD;
            case 12: return TWO_DEEP_HEAD;
            default: return NONE;
        }
    }

}
