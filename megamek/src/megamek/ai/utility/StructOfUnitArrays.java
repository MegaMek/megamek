/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.ai.utility;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.function.Consumer;

public abstract class StructOfUnitArrays implements Iterable<Integer> {
    protected final int[] id;
    protected final int[] x;
    protected final int[] y;
    protected final int[] facing;
    protected final int[] ownerId;
    protected final int[] teamId;
    protected final int[] maxRange;
    protected final int[] role;
    public final int length;

    protected StructOfUnitArrays(int length) {
        this.id = new int[length];
        this.x = new int[length];
        this.y = new int[length];
        this.facing = new int[length];
        this.ownerId = new int[length];
        this.teamId = new int[length];
        this.maxRange = new int[length];
        this.role = new int[length];
        this.length = length;
    }

    public int getMaxWeaponRange(int index) {
        return maxRange[index];
    }

    public int getId(int index) {
        return id[index];
    }

    public int getX(int index) {
        return x[index];
    }

    public int getY(int index) {
        return y[index];
    }

    public int[] getXY(int index) {
        var xy = new int[2];
        getXY(index, xy);
        return xy;
    }

    /**
     * Get the x and y coordinates of the entity at the given index, it copies the position in the array passed.
     * It will copy the x in the position 0 and the y in the position 1, the array must be at least 2 elements long.
     * @param index index of the unit
     * @param xy array to copy the x and y coordinates
     */
    public void getXY(int index, int[] xy) {
        if (xy.length < 2) {
            throw new IllegalArgumentException("xy length must be at least 2");
        }
        xy[0] = x[index];
        xy[1] = y[index];
    }


    /**
     * Get the x and y coordinates of all the entities.
     * Each element in the array will be an x,y coordinate pair.
     * @return an array with the x and y coordinates of all the entities
     */
    public int[][] getAllXYMaxRange() {
        var xyr = new int[length][3];
        getAllXYMaxRange(xyr);
        return xyr;
    }

    public void getAllXYMaxRange(int[][] xyr) {
        if (xyr.length < length) {
            throw new IllegalArgumentException("xy must be the same length as the number of entities");
        } else if (xyr[0].length < 3) {
            throw new IllegalArgumentException("xy must have 2 columns");
        }
        for (int i = 0; i < length; i++) {
            xyr[i][0] = x[i];
            xyr[i][1] = y[i];
            xyr[i][2] = maxRange[i];
        }
    }

    /**
     * Get the x and y coordinates of all the entities.
     * Each element in the array will be an x,y coordinate pair.
     * @return an array with the x and y coordinates of all the entities
     */
    public int[][] getAllXY() {
        var xy = new int[length][2];
        getAllXY(xy);
        return xy;
    }

    /**
     * Get the x and y coordinates of all the entities, it copies the positions in the array passed.
     * The array must be at least the same length as the number of entities and each element must be at least 2 elements long.
     *
     * @param xy array to copy the x and y coordinates
     */
    public void getAllXY(int[][] xy) {
        if (xy.length < length) {
            throw new IllegalArgumentException("xy must be the same length as the number of entities");
        } else if (xy[0].length < 2) {
            throw new IllegalArgumentException("xy must have 2 columns");
        }
        for (int i = 0; i < length; i++) {
            xy[i][0] = x[i];
            xy[i][1] = y[i];
        }
    }

    /**
     * Get the x and y coordinates and facing of the entity at the given index.
     * It will return an array with the x in the position 0, the y in the position 1 and the facing in the position 2.
     *
     * @param index index of the unit
     * @return an array with the x, y and facing of the entity
     */
    public int[] getXYFacing(int index) {
        var xyf = new int[3];
        getXYFacing(index, xyf);
        return xyf;
    }

    /**
     * Get the x and y coordinates and facing of the entity at the given index, it copies the position in the array passed.
     * It will copy the x in the position 0, the y in the position 1 and the facing in the position 2, the array must be at least 3 elements long.
     *
     * @param index index of the unit
     * @param xyf array to copy the x, y and facing
     */
    public void getXYFacing(int index, int[] xyf) {
        if (xyf.length < 3) {
            throw new IllegalArgumentException("xyf length must be at least 3");
        }
        xyf[0] = x[index];
        xyf[1] = y[index];
        xyf[2] = facing[index];
    }

    /**
     * Get the x and y coordinates and facing of all the entities.
     * Each element in the array will be an x,y,facing coordinate triplet.
     * @return an array with the x, y and facing of all the entities
     */
    public int[][] getAllXYFacing() {
        var xyf = new int[length][3];
        getAllXYFacing(new int[length][3]);
        return xyf;
    }

    /**
     * Get the x and y coordinates and facing of all the entities, it copies the positions in the array passed.
     * The array must be at least the same length as the number of entities and each element must be at least 3 elements long.
     * Each element in the array will be an x,y,facing coordinate triplet.
     * @param xyf array to copy the x, y and facing coordinates
     */
    public void getAllXYFacing(int[][] xyf) {
        if (xyf.length < length) {
            throw new IllegalArgumentException("xyf must be the same length as the number of entities");
        } else if (xyf[0].length < 3) {
            throw new IllegalArgumentException("xyf must have 3 columns");
        }
        for (int i = 0; i < length; i++) {
            xyf[i][0] = x[i];
            xyf[i][1] = y[i];
            xyf[i][2] = facing[i];
        }
    }

    /**
     * Get the facing of the entity at the given index.
     * @param index index of the unit
     * @return the facing of the entity
     */
    public int getFacing(int index) {
        return facing[index];
    }

    /**
     * Get the owner id of the entity at the given index.
     * @param index index of the unit
     * @return the owner id of the entity
     */
    public int getOwnerId(int index) {
        return ownerId[index];
    }

    /**
     * Get the team id of the entity at the given index.
     * @param index index of the unit
     * @return the team id of the entity
     */
    public int getTeamId(int index) {
        return teamId[index];
    }

    /**
     * Get the role of the entity at the given index.
     * @param index index of the unit
     * @return the role of the entity
     */
    public int getRole(int index) {
        return role[index];
    }


    public enum Index {
        ID,
        X,
        Y,
        FACING,
        OWNER_ID,
        TEAM_ID,
        ROLE,
        MAX_RANGE;
    }

    /**
     * Get the id, x, y, facing, owner id, team id and role (ordinal) of the entity at the given index.
     * @return an array with the id, x, y, facing, owner id, team id and role (ordinal) of all entities
     */
    public int[][] toArray() {
        int[][] array = new int[length][7];
        for (int i = 0; i < length; i++) {
            array[i][Index.ID.ordinal()] = id[i];
            array[i][Index.X.ordinal()] = x[i];
            array[i][Index.Y.ordinal()] = y[i];
            array[i][Index.FACING.ordinal()] = facing[i];
            array[i][Index.OWNER_ID.ordinal()] = ownerId[i];
            array[i][Index.TEAM_ID.ordinal()] = teamId[i];
            array[i][Index.ROLE.ordinal()] = role[i];
            array[i][Index.MAX_RANGE.ordinal()] = maxRange[i];
        }
        return array;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < length;
            }

            @Override
            public Integer next() {
                return index++;
            }
        };
    }

    @Override
    public void forEach(Consumer<? super Integer> action) {
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<Integer> spliterator() {
        return Iterable.super.spliterator();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", StructOfUnitArrays.class.getSimpleName() + "[", "]")
            .add("length=" + length)
            .toString();
    }
}
