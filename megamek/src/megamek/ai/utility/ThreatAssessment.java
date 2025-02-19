/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.ai.utility;


import megamek.common.Coords;
import megamek.common.Targetable;
import megamek.common.UnitRole;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Interface for assessing threat levels at specific coordinates.
 */
public interface ThreatAssessment {
    /**
     * Gets the enemy threat level at the specified coordinates.
     *
     * @param position The coordinates to assess.
     * @return The enemy threat level.
     */
    double getEnemyThreat(Coords position);

    /**
     * Gets a list of friendly units within a specified range of a position.
     *
     * @param position The coordinates to check.
     * @param range The range within which to find friendly units.
     * @return A list of friendly units within the specified range.
     */
    default List<Targetable> getFriendliesWithinRange(Coords position, int range) {
        var friendlies = getEntities(getUnitIdsAtRange(getStructOfAlliesArrays(), position.getX(), position.getY(), range));
        friendlies.addAll(getEntities(getUnitIdsAtRange(getStructOfOwnUnitsArrays(), position.getX(), position.getY(), range)));
        return friendlies;
    }

    /**
     * Gets a list of enemy units within a specified range of a position.
     *
     * @param position The coordinates to check.
     * @return A list of enemy units within the specified range.
     */
    default List<Targetable> getEnemiesWithinRange(Coords position, int range) {
        return getEntities(getUnitIdsAtRange(getStructOfEnemiesArrays(), position.getX(), position.getY(), range));
    }

    /**
     * Calculate the maximum damage a unit can inflict at a given range.
     *
     * @param unit The unit for which to calculate the damage.
     * @param enemyRange The range at which the damage is calculated.
     * @return The maximum damage the unit can inflict at the given range.
     */
    double calculateUnitMaxDamageAtRange(Targetable unit, int enemyRange);

    /**
     * Calculate the maximum damage all friends in range can cause to target coordinate
     *
     * @param targetCoords The target coordinates
     * @param range The range to consider
     * @return The maximum damage all friends in range can cause to target coordinate
     */
    double maxAmountOfDamageFromFriendsInRange(Coords targetCoords, int range);

    /**
     * Get the number of friends within a certain range of a position.
     *
     * @param coords The position to check.
     * @param range The range within which to check.
     * @return The number of friends within the specified range.
     */
    default int getNumberOfFriendsInRange(Coords coords, int range) {
        var x = coords.getX();
        var y = coords.getY();

        var allXY = new int[Math.max(getStructOfAlliesArrays().length, getStructOfAlliesArrays().length)][2];
        getStructOfAlliesArrays().getAllXY(allXY);
        int units = getUnitCount(range, x, y, allXY, getStructOfAlliesArrays().length);
        getStructOfOwnUnitsArrays().getAllXY(allXY);
        units += getUnitCount(range, x, y, allXY, getStructOfOwnUnitsArrays().length);
        return units;
    }

    /**
     * Get the number of enemies within a certain range of a position.
     *
     * @param coords The position to check.
     * @param range The range within which to check.
     * @return The number of enemies within the specified range.
     */
    default int getNumberOfEnemiesInRange(Coords coords, int range) {
        return getUnitCount(range, coords.getX(), coords.getY(), getStructOfEnemiesArrays().getAllXY(), getStructOfEnemiesArrays().length);
    }

    private int getUnitCount(int range, int x, int y, int[][] allXY, int length) {
        double dist;
        int units = 0;
        for (int i = 0; i < length; i++) {
            dist = Coords.distance(x, y, allXY[i][0], allXY[i][1]);
            if (dist <= range) {
                units++;
            }
        }
        return units;
    }

    /**
     * Get the IDs of friends within a certain range of a position.
     *
     * @param coords The position to check.
     * @param range The range within which to check.
     * @return The IDs of friends within the specified range.
     */
    default List<Integer> getFriendIDsAtRange(Coords coords, int range) {
        var x = coords.getX();
        var y = coords.getY();
        var ids = getUnitIdsAtRange(getStructOfAlliesArrays(), x, y, range);
        ids.addAll(getUnitIdsAtRange(getStructOfOwnUnitsArrays(), x, y, range));
        return ids;
    }

    /**
     * Get the IDs of enemies within a certain range of a position.
     *
     * @param coords The position to check.
     * @param range The range within which to check.
     * @return The IDs of enemies within the specified range.
     */
    default List<Integer> getEnemyIDsAtRange(Coords coords, int range) {
        var x = coords.getX();
        var y = coords.getY();
        return getUnitIdsAtRange(getStructOfEnemiesArrays(), x, y, range);
    }

    /**
     * Get the N closest enemy positions to a specified position.
     * @param coords The position to check.
     * @param numberOfEnemies The number of enemies to return.
     * @return The N closest enemy positions.
     */
    default List<Coords> getNClosestEnemiesPositions(Coords coords, int numberOfEnemies) {
        var allXY = getStructOfEnemiesArrays().getAllXY();
        int x = coords.getX();
        int y = coords.getY();
        PriorityQueue<Coords> closestEnemies = new PriorityQueue<>(Comparator.comparingDouble(c -> Coords.distance(x, y, c.getX(), c.getY())));
        for (int[] xy : allXY) {
            closestEnemies.add(new Coords(xy[0], xy[1]));
        }
        return new ArrayList<>(closestEnemies).subList(0, numberOfEnemies);
    }

    /**
     * Get the distance to the closest enemy at the final move path position.
     * @return The distance to the closest enemy.
     */
    default OptionalInt getDistanceToClosestEnemyAtFinalMovePathPosition(Coords finalPosition) {
        var dist = Integer.MAX_VALUE;
        var x1 = finalPosition.getX();
        var y1 = finalPosition.getY();

        for (var xy : getStructOfEnemiesArrays().getAllXY()) {
            dist = Math.min(Coords.distance(x1, y1, xy[0], xy[1]), dist);
        }
        if (dist == Integer.MAX_VALUE) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(dist);
    }

    private List<Integer> getUnitIdsAtRange(StructOfUnitArrays structOfArraysEntity, int x, int y, int range) {
        double dist;
        var allXY = structOfArraysEntity.getAllXY();
        var ids = new ArrayList<Integer>(allXY.length);
        for (int i = 0; i < structOfArraysEntity.length; i++) {
            dist = Coords.distance(x, y, allXY[i][0], allXY[i][1]);
            if (dist <= range) {
                ids.add(structOfArraysEntity.getId(i));
            }
        }
        return ids;
    }

    /**
     *  Get the distance to the closest enemy with a specific role.
     *
     * @param role The role of the enemy.
     * @return The distance to the closest enemy with the specified role.
     */
    default OptionalInt getDistanceToClosestEnemyWithRole(Coords finalPosition, UnitRole role) {
        var dist = Integer.MAX_VALUE;
        var x1 = finalPosition.getX();
        var y1 = finalPosition.getY();
        var roleOrdinal = role.ordinal();
        var allXY = getStructOfEnemiesArrays().getAllXY();
        for (int i = 0; i < allXY.length; i++) {
            if (getStructOfEnemiesArrays().getRole(i) == roleOrdinal) {
                dist = Math.min(Coords.distance(x1, y1, allXY[i][0], allXY[i][1]), dist);
            }
        }
        if (dist == Integer.MAX_VALUE) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(dist);
    }

    /**
     * Get the coordinates of the enemies that are threatening a specified position.
     * @param coords The position to check.
     * @return The coordinates of the threatening enemies.
     */
    default List<Coords> getThreateningEnemies(Coords coords) {
        var x1 = coords.getX();
        var y1 = coords.getY();
        int currentDistance;
        var allUnits = getStructOfEnemiesArrays().toArray();
        List<Coords> positions = new ArrayList<>();
        for (int[] allUnit : allUnits) {
            currentDistance = Coords.distance(x1, y1, allUnit[StructOfUnitArrays.Index.X.ordinal()], allUnit[StructOfUnitArrays.Index.Y.ordinal()]);
            if (currentDistance <= allUnit[StructOfUnitArrays.Index.MAX_RANGE.ordinal()]) {
                positions.add(new Coords(allUnit[StructOfUnitArrays.Index.X.ordinal()], allUnit[StructOfUnitArrays.Index.Y.ordinal()]));
            }
        }
        return positions;
    }

    /**
     * Get the structure of arrays of enemy units.
     * @return The structure of arrays of enemy units.
     */
    StructOfUnitArrays getStructOfEnemiesArrays();

    /**
     * Get the structure of arrays of friendly units.
     * @return The structure of arrays of friendly units.
     */
    StructOfUnitArrays getStructOfAlliesArrays();


    /**
     * Get the structure of arrays of your own units.
     * @return The structure of arrays of your own units.
     */
    StructOfUnitArrays getStructOfOwnUnitsArrays();


    /**
     * Get the incoming friendly artillery damage at specific coordinates.
     * @return The incoming friendly artillery damage.
     */
    Map<Coords, Double> getIncomingFriendlyArtilleryDamage();

    /**
     * Gets the closest VIP unit.
     *
     * @return The closest VIP unit, returns empty if there is none.
     */
    Optional<Targetable> getClosestVIP(Coords coords);

    /**
     * Gets the closest enemy unit.
     *
     * @return The closest enemy unit, returns empty if there is none.
     */
    default Optional<Targetable> getClosestEnemy(Coords finalPosition) {
        var x1 = finalPosition.getX();
        var y1 = finalPosition.getY();
        var allUnits = getStructOfEnemiesArrays().toArray();
        int currentDistance;
        int closestIdEntity = -1;
        int closestDistance = Integer.MAX_VALUE;
        for (int[] allUnit : allUnits) {
            currentDistance = Coords.distance(x1, y1, allUnit[StructOfUnitArrays.Index.X.ordinal()], allUnit[StructOfUnitArrays.Index.Y.ordinal()]);
            if (currentDistance <= closestDistance) {
                closestDistance = currentDistance;
                closestIdEntity = allUnit[StructOfUnitArrays.Index.ID.ordinal()];
            }
        }
        int finalClosestIdEntity = closestIdEntity;
        return getEnemiesList().stream().filter(e -> e.getId() == finalClosestIdEntity).findFirst();
    }

    /**
     * Gets a list of entities from a list of IDs.
     * @param ids The list of IDs.
     * @return The list of entities.
     */
    default List<Targetable> getEntities(List<Integer> ids) {
        List<Targetable> entities = getEnemiesList().stream().filter(f -> ids.contains(f.getId())).collect(Collectors.toList());
        getAlliesList().stream().filter(e -> ids.contains(e.getId())).forEach(entities::add);
        getOwnUnitsList().stream().filter(e -> ids.contains(e.getId())).forEach(entities::add);
        return entities;
    }

    /**
     * Gets the enemies list.
     *
     * @return The enemies list.
     */
    List<Targetable> getEnemiesList();

    /**
     * Gets the enemies list.
     *
     * @return The enemies list.
     */
    List<Targetable> getAlliesList();

    /**
     * Gets the enemies list.
     *
     * @return The enemies list.
     */
    List<Targetable> getOwnUnitsList();

    double getArmorRemainingPercent(Targetable target);
}
