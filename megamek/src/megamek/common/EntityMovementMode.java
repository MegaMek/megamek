/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

/**
 * This interface represents the Entity Movement Types
 */
public enum EntityMovementMode {

    NONE ("none", "building"), // Future expansion. Turrets?
    BIPED ("biped"),
    TRIPOD ("tripod"),
    QUAD ("quad"),
    TRACKED ("tracked"),
    WHEELED ("wheeled"),
    HOVER ("hover"),
    VTOL ("vtol"),
    NAVAL ("naval"),
    HYDROFOIL ("hydrofoil"),
    SUBMARINE ("submarine"),
    INF_LEG ("inf_leg", "leg"),
    INF_MOTORIZED("inf_motorized", "motorized"),
    INF_JUMP ("inf_jump", "jump"),
    BIPED_SWIM,
    QUAD_SWIM,
    WIGE ("wige", "glider"),
    AERODYNE ("aerodyne"),
    SPHEROID ("spheroid"),
    INF_UMU ("umu"),
    AIRMECH,
    AEROSPACE, // this might be a synonym for AERODYNE.
    RAIL ("rail"),
    MAGLEV ("maglev");

    private String[] aliases;

    EntityMovementMode(String... aliases) {
        this.aliases = aliases;
    }

    private boolean isAlias(String str) {
        for (String alias : aliases) {
            if (alias.trim().equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    public static EntityMovementMode getMode(String str) {
        for (EntityMovementMode mode: EntityMovementMode.values()) {
            if (mode.isAlias(str)) {
                return mode;
            }
        }
        return NONE;
    }

    public static EntityMovementMode type(String token)
    {
        return EntityMovementMode.valueOf(token);
    }
    public static String token(EntityMovementMode t)
    {
        return t.name();
    }
}
