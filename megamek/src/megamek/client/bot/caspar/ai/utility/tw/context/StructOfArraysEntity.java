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

package megamek.client.bot.caspar.ai.utility.tw.context;

import megamek.common.Entity;
import megamek.common.UnitRole;

import java.util.*;
import java.util.function.Consumer;

public class StructOfArraysEntity implements Iterable<Integer> {

    private final int[] id;
    private final int[] x;
    private final int[] y;
    private final int[] facing;
    private final int[] ownerId;
    private final int[] teamId;
    private final UnitRole[] role;
    public final int length;

    public StructOfArraysEntity(List<Entity> entities) {
        entities.sort(Comparator.comparingInt(Entity::getId));
        length = entities.size();
        id = new int[length];
        x = new int[length];
        y = new int[length];
        facing = new int[length];
        ownerId = new int[length];
        teamId = new int[length];
        role = new UnitRole[length];

        for (int i = 0; i < length; i++) {
            Entity entity = entities.get(i);
            id[i] = entity.getId();
            if (entity.getPosition() == null) {
                x[i] = Integer.MAX_VALUE;
                y[i] = Integer.MAX_VALUE;
            } else {
                x[i] = entity.getPosition().getX();
                y[i] = entity.getPosition().getY();
            }
            facing[i] = entity.getFacing();
            ownerId[i] = entity.getOwner().getId();
            teamId[i] = entity.getOwner().getTeam();
            role[i] = entity.getRole();
        }
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
    public UnitRole getRole(int index) {
        return role[index];
    }

    /**
     * Get the id, x, y, facing, owner id, team id and role (ordinal) of the entity at the given index.
     * @return an array with the id, x, y, facing, owner id, team id and role (ordinal) of all entities
     */
    public int[][] toArray() {
        int[][] array = new int[length][7];
        for (int i = 0; i < length; i++) {
            array[i][0] = id[i];
            array[i][1] = x[i];
            array[i][2] = y[i];
            array[i][3] = facing[i];
            array[i][4] = ownerId[i];
            array[i][5] = teamId[i];
            array[i][6] = role[i].ordinal();
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
        return new StringJoiner(", ", StructOfArraysEntity.class.getSimpleName() + "[", "]")
            .add("length=" + length)
            .toString();
    }
}
