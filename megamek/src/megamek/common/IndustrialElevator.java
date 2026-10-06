/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import megamek.common.annotations.Nullable;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;

/**
 * Represents a player-controlled industrial elevator within a building.
 * <p>
 * Industrial elevators have a cargo capacity, a shaft spanning multiple levels, and a platform that moves 1 level per
 * turn. Players can call the elevator during the End Phase, and units can ride it during the Movement Phase.
 * <p>
 * All shaft and platform levels are <b>relative to the hex surface</b>, matching {@code Entity.getElevation()}:
 * level 0 is the hex surface, negative levels are below it (basement shaft), positive levels are above it (building
 * floors). The same terrain encoding therefore works on a hex of any board level.
 * <p>
 * Key rules:
 * <ul>
 *   <li>Maximum cargo capacity - elevator won't move if exceeded</li>
 *   <li>Platform moves 1 level per turn toward the nearest caller</li>
 *   <li>Movement costs 1 Walking/Cruising MP per level</li>
 *   <li>Only Walking/Cruising movement allowed (no running/jumping)</li>
 *   <li>Elevator disabled if any shaft level is breached/flooded/collapsed</li>
 *   <li>Entering shaft without platform causes fall</li>
 * </ul>
 */
public class IndustrialElevator implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Multiplier for capacity encoding (capacity stored as capacityTens * 10 = actual tons) */
    public static final int CAPACITY_MULTIPLIER = 10;

    /** Bit shift for the shaft-top value within the terrain exits field: exits = (shaftTop &lt;&lt; 8) | capacityTens */
    public static final int SHAFT_TOP_SHIFT = 8;

    /** Bit mask for the capacity (in tens of tons) packed into the low byte of the terrain exits field */
    public static final int CAPACITY_MASK = 0xFF;

    /** The hex location of this elevator shaft */
    private final BoardLocation location;

    /** The lowest level the elevator platform can reach */
    private final int shaftBottom;

    /** The highest level the elevator platform can reach */
    private final int shaftTop;

    /** Maximum cargo capacity in tons */
    private final int capacityTons;

    /** Current level of the elevator platform within the shaft */
    private int platformLevel;

    /** Whether the elevator is functional (false if shaft is damaged) */
    private boolean functional;

    /** Queue of pending elevator calls, sorted by priority */
    private final List<ElevatorCall> callQueue;

    /**
     * Creates a new industrial elevator.
     *
     * @param location     The hex location of the elevator shaft
     * @param shaftBottom  The lowest level the platform can reach
     * @param shaftTop     The highest level the platform can reach
     * @param capacityTons Maximum cargo capacity in tons
     */
    public IndustrialElevator(BoardLocation location, int shaftBottom, int shaftTop, int capacityTons) {
        if (shaftBottom > shaftTop) {
            throw new IllegalArgumentException(
                  "shaftBottom (" + shaftBottom + ") must be <= shaftTop (" + shaftTop + ")");
        }
        this.location = location;
        this.shaftBottom = shaftBottom;
        this.shaftTop = shaftTop;
        this.capacityTons = capacityTons;
        this.platformLevel = shaftTop; // Platform starts at top (entry level)
        this.functional = true;
        this.callQueue = new CopyOnWriteArrayList<>();
    }

    /**
     * Creates an industrial elevator from terrain data.
     * <p>
     * Terrain encoding (levels relative to the hex surface, see the class doc):
     * <ul>
     *   <li>level = shaft bottom, e.g. {@code -3} for a shaft reaching 3 levels below the surface</li>
     *   <li>exits = (shaftTop &lt;&lt; 8) | capacityTens, with shaft top {@code 0} meaning the hex surface</li>
     * </ul>
     *
     * @param location The hex location
     * @param level    The terrain level (shaft bottom, relative to the hex surface)
     * @param exits    The terrain exits field (encoded shaft top and capacity)
     *
     * @return A new IndustrialElevator instance
     */
    public static IndustrialElevator fromTerrain(BoardLocation location, int level, int exits) {
        int shaftBottom = level;
        int shaftTop = (exits >> SHAFT_TOP_SHIFT) & CAPACITY_MASK;
        int capacityTens = exits & CAPACITY_MASK;
        int capacityTons = capacityTens * CAPACITY_MULTIPLIER;
        return new IndustrialElevator(location, shaftBottom, shaftTop, capacityTons);
    }

    // --- Getters ---

    public BoardLocation getLocation() {
        return location;
    }

    public Coords getCoords() {
        return location.coords();
    }

    public int getBoardId() {
        return location.boardId();
    }

    public int getShaftBottom() {
        return shaftBottom;
    }

    public int getShaftTop() {
        return shaftTop;
    }

    public int getCapacityTons() {
        return capacityTons;
    }

    public int getPlatformLevel() {
        return platformLevel;
    }

    public boolean isFunctional() {
        return functional;
    }

    public List<ElevatorCall> getCallQueue() {
        return Collections.unmodifiableList(callQueue);
    }

    // --- Setters ---

    public void setPlatformLevel(int level) {
        if ((level < shaftBottom) || (level > shaftTop)) {
            throw new IllegalArgumentException(
                  "Platform level " + level + " outside shaft range [" + shaftBottom + ", " + shaftTop + "]");
        }
        this.platformLevel = level;
    }

    public void setFunctional(boolean functional) {
        this.functional = functional;
    }

    // --- Platform Position Queries ---

    /**
     * Checks if the platform is currently at the specified level.
     *
     * @param level The level to check
     *
     * @return {@code true} if the platform is at this level
     */
    public boolean isPlatformAt(int level) {
        return platformLevel == level;
    }

    /**
     * Checks if a given level is within the elevator shaft.
     *
     * @param level The level to check
     *
     * @return {@code true} if the level is within the shaft range
     */
    public boolean isWithinShaft(int level) {
        return (level >= shaftBottom) && (level <= shaftTop);
    }

    /**
     * Gets the number of levels in the shaft.
     *
     * @return The shaft height in levels
     */
    public int getShaftHeight() {
        return shaftTop - shaftBottom + 1;
    }

    // --- Capacity Checking ---

    /**
     * Calculates the total tonnage of entities currently on the elevator platform.
     *
     * @param game The game instance to query for entities
     *
     * @return The total tonnage of entities on the platform
     */
    public double getCurrentLoad(Game game) {
        double totalTonnage = 0;
        for (Entity entity : game.getEntitiesVector()) {
            if (isEntityOnPlatform(entity)) {
                totalTonnage += entity.getWeight();
            }
        }
        return totalTonnage;
    }

    /**
     * Checks if an entity is currently on the elevator platform.
     *
     * @param entity The entity to check, or {@code null} (never on the platform)
     *
     * @return {@code true} if the entity is on the platform
     */
    public boolean isEntityOnPlatform(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        Coords entityCoords = entity.getPosition();
        if (entityCoords == null) {
            return false;
        }
        return entityCoords.equals(location.coords())
              && (entity.getBoardId() == location.boardId())
              && (entity.getElevation() == platformLevel);
    }

    /**
     * Checks if the elevator can move (not overloaded).
     *
     * @param game The game instance
     *
     * @return {@code true} if current load is within capacity
     */
    public boolean canMove(Game game) {
        return functional && (getCurrentLoad(game) <= capacityTons);
    }

    // --- Call Queue Management ---

    /**
     * Adds a call to the elevator queue.
     *
     * @param call The elevator call to add
     */
    public void addCall(ElevatorCall call) {
        callQueue.add(call);
        sortCallQueue();
    }

    /**
     * Removes a call from the queue.
     *
     * @param call The call to remove
     *
     * @return {@code true} if the call was found and removed
     */
    public boolean removeCall(ElevatorCall call) {
        return callQueue.remove(call);
    }

    /**
     * Clears all calls from the queue.
     */
    public void clearCallQueue() {
        callQueue.clear();
    }

    /**
     * Gets the next call the elevator should respond to (nearest caller).
     *
     * @return The next call, or {@code null} if queue is empty
     */
    public @Nullable ElevatorCall getNextCall() {
        return callQueue.isEmpty() ? null : callQueue.getFirst();
    }

    /**
     * Sorts the call queue by priority ({@link ElevatorCall#CALL_PRIORITY_ORDER}). The sort is stable, so calls that
     * compare equal keep their arrival order (FIFO).
     */
    private void sortCallQueue() {
        callQueue.sort(ElevatorCall.CALL_PRIORITY_ORDER);
    }

    /**
     * Recalculates distances for all calls based on current platform position. Should be called after the platform
     * moves.
     */
    public void recalculateCallDistances() {
        for (ElevatorCall call : callQueue) {
            call.updateDistance(platformLevel);
        }
        sortCallQueue();
    }

    // --- Platform Movement ---

    /**
     * Moves the platform one level toward the target level.
     *
     * @param targetLevel The level to move toward
     *
     * @return The new platform level after moving
     */
    public int movePlatformToward(int targetLevel) {
        if (!functional) {
            return platformLevel;
        }
        if (targetLevel > platformLevel) {
            platformLevel = Math.min(platformLevel + 1, shaftTop);
        } else if (targetLevel < platformLevel) {
            platformLevel = Math.max(platformLevel - 1, shaftBottom);
        }
        return platformLevel;
    }

    /**
     * Processes the elevator's end phase movement. Moves the platform 1 level toward the nearest caller if any.
     *
     * @param game The game instance (for capacity checking)
     *
     * @return {@code true} if the platform moved
     */
    public boolean processEndPhaseMovement(Game game) {
        if (!functional) {
            return false;
        }
        if (!canMove(game)) {
            return false; // Overloaded
        }

        ElevatorCall nextCall = getNextCall();
        if (nextCall == null) {
            return false; // No pending calls
        }

        int targetLevel = nextCall.getTargetLevel();
        int previousLevel = platformLevel;
        movePlatformToward(targetLevel);

        // If we arrived at the target level, remove this call from queue
        if (platformLevel == targetLevel) {
            callQueue.remove(nextCall);
        }

        // Recalculate distances for remaining calls
        if (!callQueue.isEmpty()) {
            recalculateCallDistances();
        }

        return platformLevel != previousLevel;
    }

    // --- Terrain Encoding ---

    /**
     * Encodes the elevator data for terrain storage.
     *
     * @return The exits value encoding shaft top and capacity
     */
    public int encodeExits() {
        int capacityTens = capacityTons / CAPACITY_MULTIPLIER;
        return (shaftTop << SHAFT_TOP_SHIFT) | (capacityTens & CAPACITY_MASK);
    }

    /**
     * Rewrites this elevator's hex so the board tileset renders the platform at its current level. After initialization
     * the shaft geometry is held in this object, so the terrain's level field is free to carry the platform position
     * for display. Callers must notify clients of the hex change (for example {@code gameManager.sendChangedHex} or
     * {@code markHexUpdate}).
     *
     * @param game the game whose board holds this elevator's hex
     *
     * @return {@code true} if the displayed level changed and clients need to be notified
     */
    public boolean syncDisplayLevel(Game game) {
        Hex hex = game.getBoard(getBoardId()).getHex(getCoords());
        if (hex == null) {
            return false;
        }
        Terrain terrain = hex.getTerrain(Terrains.INDUSTRIAL_ELEVATOR);
        if ((terrain == null) || (terrain.getLevel() == platformLevel)) {
            return false;
        }
        hex.removeTerrain(Terrains.INDUSTRIAL_ELEVATOR);
        hex.addTerrain(new Terrain(Terrains.INDUSTRIAL_ELEVATOR, platformLevel, true, terrain.getExits()));
        return true;
    }

    @Override
    public String toString() {
        return String.format("IndustrialElevator[%s, shaft=%d-%d, platform=%d, capacity=%dt, %s]",
              location, shaftBottom, shaftTop, platformLevel, capacityTons,
              functional ? "functional" : "disabled");
    }

    // --- Inner Class: ElevatorCall ---

    /**
     * Represents a call to an industrial elevator.
     * <p>
     * Calls are queued in {@link #CALL_PRIORITY_ORDER}: distance from the platform (nearest first), with ties broken
     * by turn called (earlier first), then by initiative (higher first). The ordering is intentionally a separate
     * {@link Comparator} rather than {@code Comparable}: it deliberately ignores fields such as the calling player,
     * so it would be inconsistent with {@link #equals(Object)}.
     */
    public static class ElevatorCall implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Queue priority: nearest to the platform first, then earliest turn called, then highest initiative.
         */
        public static final Comparator<ElevatorCall> CALL_PRIORITY_ORDER = Comparator
              .comparingInt(ElevatorCall::getDistanceFromPlatform)
              .thenComparingInt(ElevatorCall::getTurnCalled)
              .thenComparing(Comparator.comparingInt(ElevatorCall::getInitiativeBonus).reversed());

        private final int playerId;
        private final Coords callerPosition;
        private final int targetLevel;
        private final int turnCalled;
        private final int initiativeBonus; // For tie-breaking (higher = priority)
        private int distanceFromPlatform;

        /**
         * Creates a new elevator call.
         *
         * @param playerId             The ID of the calling player
         * @param callerPosition       The position of the calling unit
         * @param targetLevel          The level the caller wants the elevator at
         * @param turnCalled           The game turn when the call was made
         * @param initiativeBonus      The player's initiative for tie-breaking
         * @param currentPlatformLevel The current platform level (for distance calculation)
         */
        public ElevatorCall(int playerId, Coords callerPosition, int targetLevel,
              int turnCalled, int initiativeBonus, int currentPlatformLevel) {
            this.playerId = playerId;
            this.callerPosition = callerPosition;
            this.targetLevel = targetLevel;
            this.turnCalled = turnCalled;
            this.initiativeBonus = initiativeBonus;
            this.distanceFromPlatform = Math.abs(targetLevel - currentPlatformLevel);
        }

        public int getPlayerId() {
            return playerId;
        }

        public Coords getCallerPosition() {
            return callerPosition;
        }

        public int getTargetLevel() {
            return targetLevel;
        }

        public int getTurnCalled() {
            return turnCalled;
        }

        public int getInitiativeBonus() {
            return initiativeBonus;
        }

        public int getDistanceFromPlatform() {
            return distanceFromPlatform;
        }

        /**
         * Updates the distance from the current platform position.
         *
         * @param currentPlatformLevel The current platform level
         */
        public void updateDistance(int currentPlatformLevel) {
            this.distanceFromPlatform = Math.abs(targetLevel - currentPlatformLevel);
        }

        @Override
        public String toString() {
            return String.format("ElevatorCall[player=%d, level=%d, turn=%d, distance=%d]",
                  playerId, targetLevel, turnCalled, distanceFromPlatform);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ElevatorCall other)) {
                return false;
            }
            return playerId == other.playerId
                  && targetLevel == other.targetLevel
                  && turnCalled == other.turnCalled
                  && initiativeBonus == other.initiativeBonus
                  && distanceFromPlatform == other.distanceFromPlatform;
        }

        @Override
        public int hashCode() {
            int result = playerId;
            result = 31 * result + targetLevel;
            result = 31 * result + turnCalled;
            result = 31 * result + initiativeBonus;
            result = 31 * result + distanceFromPlatform;
            return result;
        }
    }
}
