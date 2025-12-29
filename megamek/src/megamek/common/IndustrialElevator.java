/*
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
package megamek.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Represents a player-controlled industrial elevator within a building.
 * <p>
 * Industrial elevators have a cargo capacity, a shaft spanning multiple levels, and a platform that moves 1 level per
 * turn. Players can call the elevator during the End Phase, and units can ride it during the Movement Phase.
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
     * Terrain encoding:
     * <ul>
     *   <li>level = shaft bottom elevation</li>
     *   <li>exits = (shaftTop &lt;&lt; 8) | capacityTens</li>
     * </ul>
     *
     * @param location The hex location
     * @param level    The terrain level (shaft bottom)
     * @param exits    The terrain exits field (encoded shaft top and capacity)
     *
     * @return A new IndustrialElevator instance
     */
    public static IndustrialElevator fromTerrain(BoardLocation location, int level, int exits) {
        int shaftBottom = level;
        int shaftTop = (exits >> 8) & 0xFF;
        int capacityTens = exits & 0xFF;
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
     * @return true if the platform is at this level
     */
    public boolean isPlatformAt(int level) {
        return platformLevel == level;
    }

    /**
     * Checks if a given level is within the elevator shaft.
     *
     * @param level The level to check
     *
     * @return true if the level is within the shaft range
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
     * @param entity The entity to check
     *
     * @return true if the entity is on the platform
     */
    public boolean isEntityOnPlatform(Entity entity) {
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
     * @return true if current load is within capacity
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
     * @return true if the call was found and removed
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
     * @return The next call, or null if queue is empty
     */
    public ElevatorCall getNextCall() {
        return callQueue.isEmpty() ? null : callQueue.get(0);
    }

    /**
     * Sorts the call queue by distance from current platform position. Calls at the same distance maintain their
     * original order (FIFO for ties).
     */
    private void sortCallQueue() {
        // Stable sort - maintains order for equal distances
        Collections.sort(callQueue);
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
     * @return true if the platform moved
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
        return (shaftTop << 8) | (capacityTens & 0xFF);
    }

    @Override
    public String toString() {
        return String.format("IndustrialElevator[%s, shaft=%d-%d, platform=%d, capacity=%dt, %s]",
              location, shaftBottom, shaftTop, platformLevel, capacityTons,
              functional ? "functional" : "disabled");
    }

    // --- Inner Record: ElevatorCall ---

    /**
     * Represents a call to an industrial elevator.
     * <p>
     * Calls are sorted by distance from the platform (nearest first), with ties broken by turn called (earlier first),
     * then by initiative.
     */
    public static class ElevatorCall implements Comparable<ElevatorCall>, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

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
        public int compareTo(ElevatorCall other) {
            // Primary: distance (nearest first)
            int distanceCompare = Integer.compare(this.distanceFromPlatform, other.distanceFromPlatform);
            if (distanceCompare != 0) {
                return distanceCompare;
            }
            // Secondary: turn called (earlier first)
            int turnCompare = Integer.compare(this.turnCalled, other.turnCalled);
            if (turnCompare != 0) {
                return turnCompare;
            }
            // Tertiary: initiative (higher wins, so reverse order)
            return Integer.compare(other.initiativeBonus, this.initiativeBonus);
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
