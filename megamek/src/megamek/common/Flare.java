/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common;

import java.io.Serial;
import java.io.Serializable;

/**
 * This class represents parachute flares deployed by illumination artillery or mek mortars.
 */
public class Flare implements Serializable {
    @Serial
    private static final long serialVersionUID = 451911245389504483L;

    public Coords position;
    private final int boardId;
    public int turnsToBurn;
    public final int radius;
    public int flags;

    public static int F_IGNITED = 1;
    public static int F_DRIFTING = 2;

    public Flare(Coords position, int boardId, int turnsToBurn, int radius, int flags) {
        this.position = position;
        this.boardId = boardId;
        this.turnsToBurn = turnsToBurn;
        this.radius = radius;
        this.flags = flags;
    }

    public int getBoardId() {
        return boardId;
    }

    public boolean illuminates(Coords c) {
        // LEGACY replace with boardId version
        return illuminates(c, 0);
    }

    public boolean illuminates(Coords c, int boardId) {
        return isIgnited() && (this.boardId == boardId) && (position.distance(c) <= radius);
    }

    public boolean isIgnited() {
        return (flags & F_IGNITED) != 0;
    }

    public boolean isDrifting() {
        return (flags & F_DRIFTING) != 0;
    }

    public void ignite() {
        flags |= Flare.F_IGNITED;
    }
}
