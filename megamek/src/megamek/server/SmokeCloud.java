/*

 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import megamek.common.Coords;

public class SmokeCloud implements Serializable {
    @Serial
    private static final long serialVersionUID = -8937331680271675046L;

    public static final int SMOKE_NONE = 0;
    public static final int SMOKE_LIGHT = 1;
    public static final int SMOKE_HEAVY = 2;
    public static final int SMOKE_LI_LIGHT = 3;
    public static final int SMOKE_LI_HEAVY = 4;
    public static final int SMOKE_CHAFF_LIGHT = 5;
    public static final int SMOKE_GREEN = 6; // Anti-TSM smoke

    private int smokeDuration;
    private final int boardId;
    private final List<Coords> smokeHexList = new ArrayList<>();
    private int smokeLevel;
    private boolean didDrift = false;
    private final int roundOfGeneration;

    public SmokeCloud(List<Coords> coords, int boardId, int level, int duration, int roundOfGeneration) {
        smokeDuration = duration;
        smokeLevel = level;
        smokeHexList.addAll(coords);
        this.boardId = boardId;
        this.roundOfGeneration = roundOfGeneration;
    }

    public void setSmokeLevel(int level) {
        smokeLevel = Math.min(6, level);
    }

    /**
     * Reduces the level of smoke, heavy goes to light, LI heavy goes to LI light.
     */
    public void reduceSmokeLevel() {
        smokeLevel = switch (smokeLevel) {
            case SMOKE_HEAVY -> SMOKE_LIGHT;
            case SMOKE_LI_HEAVY -> SMOKE_LI_LIGHT;
            default -> SMOKE_NONE;
        };
    }

    /**
     * Returns the level of smoke, odd levels will correspond to light smoke while even levels will be heavy smoke.
     *
     * @return The smoke level
     */
    public int getSmokeLevel() {
        return smokeLevel;
    }

    /** @return True when this SmokeCloud is at a smoke level of SMOKE_NONE (= 0). */
    public boolean isCompletelyDissipated() {
        return smokeLevel == SMOKE_NONE;
    }

    public List<Coords> getCoordsList() {
        return smokeHexList;
    }

    /** Removes all the previously stored Coords of this SmokeCloud and stores the given Coords instead. */
    public void replaceCoords(Collection<Coords> newCoords) {
        smokeHexList.clear();
        smokeHexList.addAll(newCoords);
    }

    /** @return True when this SmokeCloud has no remaining smoke hex coordinates. */
    public boolean hasNoHexes() {
        return smokeHexList.isEmpty();
    }

    public void setDuration(int duration) {
        smokeDuration = duration;
    }

    public int getDuration() {
        return smokeDuration;
    }

    public void setDrift(boolean drift) {
        didDrift = drift;
    }

    public boolean didDrift() {
        return didDrift;
    }

    public int getRoundOfGeneration() {
        return roundOfGeneration;
    }

    public int getBoardId() {
        return boardId;
    }
}
