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
package megamek.common.units;

import java.io.Serial;
import java.io.Serializable;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;

/**
 * One unit being loaded into, or unloaded from, a grounded Small Craft or DropShip by its cranes (TW p.90-91). VTOLs,
 * Small Craft and fighters cannot mount or dismount under their own power; the carrier's cranes move them over several
 * turns instead. The carrier keeps one of these per unit in progress.
 * <p>
 * The operation is declared during the Movement Phase and starts in that turn's End Phase. Each later End Phase banks a
 * turn; loading completes in the End Phase of the fourth turn after the declaration, unloading in the End Phase of the
 * third.
 * </p>
 * <p>
 * Deliberately a plain class rather than a record: carrier state is saved with XStream, which cannot rebuild records
 * without a custom converter.
 * </p>
 */
public final class CraneOperation implements Serializable {

    @Serial
    private static final long serialVersionUID = 3485207165214439721L;

    /** Turns of crane work after the declaring turn before a unit is loaded (TW p.90). */
    public static final int LOAD_TURNS = 4;

    /** Turns of crane work after the declaring turn before a unit is unloaded (TW p.91). */
    public static final int UNLOAD_TURNS = 3;

    /** Whether the cranes are putting a unit aboard or taking it off. */
    public enum Direction {
        LOAD,
        UNLOAD
    }

    private final Direction direction;
    private final int unitId;
    private final Coords unitPosition;
    private final int unloadFacing;
    private boolean isStarted;
    private int turnsCompleted;

    private CraneOperation(Direction direction, int unitId, Coords unitPosition, int unloadFacing) {
        this.direction = direction;
        this.unitId = unitId;
        this.unitPosition = unitPosition;
        this.unloadFacing = unloadFacing;
    }

    /**
     * Creates a crane loading operation for a unit waiting beside the carrier.
     *
     * @param unitId       the id of the unit to load
     * @param unitPosition the hex the unit waits in; it must still be there in every End Phase
     *
     * @return the new, not yet started, operation
     */
    public static CraneOperation load(int unitId, Coords unitPosition) {
        return new CraneOperation(Direction.LOAD, unitId, unitPosition, 0);
    }

    /**
     * Creates a crane unloading operation for a unit carried by the carrier.
     *
     * @param unitId         the id of the carried unit to unload
     * @param unloadPosition the adjacent hex the unit will be placed in
     * @param unloadFacing   the facing the unit will be placed with
     *
     * @return the new, not yet started, operation
     */
    public static CraneOperation unload(int unitId, Coords unloadPosition, int unloadFacing) {
        return new CraneOperation(Direction.UNLOAD, unitId, unloadPosition, unloadFacing);
    }

    /** @return whether this operation loads or unloads its unit */
    public Direction getDirection() {
        return direction;
    }

    /** @return {@code true} if the cranes are loading the unit */
    public boolean isLoading() {
        return direction == Direction.LOAD;
    }

    /** @return the id of the unit being moved by the cranes */
    public int getUnitId() {
        return unitId;
    }

    /**
     * @return for loading, the hex the unit waits in; for unloading, the hex the unit will be placed in
     */
    public Coords getUnitPosition() {
        return unitPosition;
    }

    /** @return the facing an unloaded unit is placed with; not used when loading */
    public int getUnloadFacing() {
        return unloadFacing;
    }

    /** @return {@code true} once the declaring turn's End Phase has confirmed the operation */
    public boolean isStarted() {
        return isStarted;
    }

    /** Marks the operation as confirmed in the declaring turn's End Phase; no turn of work is banked yet. */
    public void start() {
        isStarted = true;
    }

    /** @return the number of turns of crane work banked after the declaring turn */
    public int getTurnsCompleted() {
        return turnsCompleted;
    }

    /** @return the number of turns of crane work this operation needs: {@value #LOAD_TURNS} or {@value #UNLOAD_TURNS} */
    public int getTurnsRequired() {
        return isLoading() ? LOAD_TURNS : UNLOAD_TURNS;
    }

    /**
     * Banks one turn of crane work, without going past the number of turns required.
     *
     * @return the number of turns banked so far, including this one
     */
    public int bankTurn() {
        turnsCompleted = Math.min(turnsCompleted + 1, getTurnsRequired());
        return turnsCompleted;
    }

    /** @return {@code true} when the cranes have worked the required number of turns */
    public boolean isComplete() {
        return turnsCompleted >= getTurnsRequired();
    }

    /**
     * @return a short description for logging
     */
    @Override
    public String toString() {
        return direction + " unit " + unitId + " at " + unitPosition + " (started " + isStarted + ", turn "
              + turnsCompleted + " of " + getTurnsRequired() + ")";
    }

    /**
     * @return the hex an unloaded unit is placed in, or {@code null} for a loading operation
     */
    public @Nullable Coords getUnloadPosition() {
        return isLoading() ? null : unitPosition;
    }
}
