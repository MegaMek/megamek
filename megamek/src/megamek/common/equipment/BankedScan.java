/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.equipment;

import java.io.Serial;
import java.io.Serializable;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.units.Entity;

/**
 * One successful scan, banked on the unit that made it until that unit carries it home. A reading of a scan point
 * remembers the point's hex; a reading of an enemy unit in a Sensor Check mission remembers the unit. Readings are
 * worth nothing until the unit leaves over its home edge, and die with the unit, which is the whole of the
 * defender's counterplay. A class rather than a record because it rides in the unit's save game through XStream,
 * which cannot read records back.
 */
public final class BankedScan implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int gameRound;
    private final int targetEntityId;
    private final Coords objectivePosition;
    private final String description;

    private BankedScan(int gameRound, int targetEntityId, @Nullable Coords objectivePosition, String description) {
        this.gameRound = gameRound;
        this.targetEntityId = targetEntityId;
        this.objectivePosition = objectivePosition;
        this.description = description;
    }

    /**
     * @param gameRound         the round the scan succeeded in
     * @param objectivePosition the hex of the scan point that was read
     * @param description       the point's name, for reports and tooltips
     *
     * @return a banked reading of a scan point
     */
    public static BankedScan ofObjective(int gameRound, Coords objectivePosition, String description) {
        return new BankedScan(gameRound, Entity.NONE, objectivePosition, description);
    }

    /**
     * @param gameRound      the round the scan succeeded in
     * @param targetEntityId the enemy unit that was read
     * @param description    the unit's name, for reports and tooltips
     *
     * @return a banked reading of an enemy unit (the Sensor Check mission)
     */
    public static BankedScan ofEnemyUnit(int gameRound, int targetEntityId, String description) {
        return new BankedScan(gameRound, targetEntityId, null, description);
    }

    /** @return the round the scan succeeded in */
    public int getGameRound() {
        return gameRound;
    }

    /** @return the scanned enemy unit's id, or {@link Entity#NONE} for a reading of a scan point */
    public int getTargetEntityId() {
        return targetEntityId;
    }

    /** @return the scanned point's hex, or {@code null} for a reading of an enemy unit */
    public @Nullable Coords getObjectivePosition() {
        return objectivePosition;
    }

    /** @return {@code true} when this reading is of a scan point rather than an enemy unit */
    public boolean isObjectiveReading() {
        return objectivePosition != null;
    }

    /** @return what was scanned, in words */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "BankedScan[round " + gameRound + ", " + description + "]";
    }
}
