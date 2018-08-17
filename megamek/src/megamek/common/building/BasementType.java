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

import megamek.common.Messages;

// LEGAL (giorgiga) I'm not sure the above copyright is the correct one
//
// The code in this file was originally in Building.java, so I copied the
// license header from that file.

public enum BasementType {

    UNKNOWN               (0,0, "Building.BasementUnknown"             ), //$NON-NLS-1$
    NONE                  (1,0, "Building.BasementNone"                ), //$NON-NLS-1$
    TWO_DEEP_FEET         (2,2, "Building.BasementTwoDeepFeet"         ), //$NON-NLS-1$
    ONE_DEEP_FEET         (3,1, "Building.BasementOneDeepFeet"         ), //$NON-NLS-1$
    ONE_DEEP_NORMAL       (4,1, "Building.BasementOneDeepNormal"       ), //$NON-NLS-1$
    ONE_DEEP_NORMALINFONLY(5,1, "Building.BasementOneDeepNormalInfOnly"), //$NON-NLS-1$
    ONE_DEEP_HEAD         (6,1, "Building.BasementOneDeepHead"         ), //$NON-NLS-1$
    TWO_DEEP_HEAD         (7,2, "Building.BasementTwoDeepHead"         ); //$NON-NLS-1$

    BasementType(int value, int depth, String descMsgKey) {
        this.value = value;
        this.depth = depth;
        this.desc = Messages.getString(descMsgKey);
    }

    private int value;
    private int depth;
    private String desc;

    public int getValue() {
        return value;
    }

    public int getDepth() {
        return depth;
    }

    public String getDesc() {
        return desc;
    }

    public static BasementType getType(int value) {
        for (BasementType type : BasementType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return UNKNOWN;
    }

}
