/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */

package megamek.client.bot.common;

import megamek.client.ratgenerator.MissionRole;
import megamek.common.Entity;
import megamek.common.IAero;
import megamek.logging.MMLogger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.function.Consumer;

/**
 * A struct of arrays for the units in the game so it is very fast.
 * @author Luana Coppio
 */
public class StructOfUnitArrays implements Iterable<Integer> {
    private static final MMLogger logger = MMLogger.create(StructOfUnitArrays.class);

    private int[] id;
    private int[] x;
    private int[] y;
    private int[] facing;
    private int[] ownerId;
    private int[] teamId;
    private int[] maxRange;
    private int[] role;
    private int[] armor;
    private int[] internal;
    private boolean[] ecm;
    private boolean[] vip;
    private boolean[] cargoTransport;
    private int[][] missionRole;
    private int length;

    /**
     * Create a new struct of unit arrays with the entities.
     * @param entities list of entities to create the struct with
     */
    public StructOfUnitArrays(List<Entity> entities) {
        this.length = entities.size();
        this.id = new int[length];
        this.x = new int[length];
        this.y = new int[length];
        this.facing = new int[length];
        this.ownerId = new int[length];
        this.teamId = new int[length];
        this.maxRange = new int[length];
        this.role = new int[length];
        this.armor = new int[length];
        this.internal = new int[length];
        this.ecm = new boolean[length];
        this.vip = new boolean[length];
        this.cargoTransport = new boolean[length];
        this.missionRole = new int[length][];
        update(entities);
        logger.debug("Created StructOfUnitArrays with {} entities", length);
    }

    /**
     * Update the struct with the entities, it resizes up the arrays if the entities list is bigger than the current
     * size, if the size is smaller then it will only update the elements that are in the list and will reduce the limit
     * @param entities list of entities to update the struct with
     */
    public void update(List<Entity> entities) {
        setup(entities.size());
        for (int i = 0; i < length; i++) {
            Entity entity = entities.get(i);
            this.id[i] = entity.getId();
            this.x[i] = entity.getPosition() == null ? 0 : entity.getPosition().getX();
            this.y[i] = entity.getPosition() == null ? 0 : entity.getPosition().getY();
            this.facing[i] = entity.getFacing();
            this.ownerId[i] = entity.getOwnerId();
            this.teamId[i] = entity.getOwner().getTeam();
            this.maxRange[i] = entity.getMaxWeaponRange();
            this.role[i] = entity.getRole().ordinal();
            this.armor[i] = Math.max(entity.getTotalArmor(), 0);
            this.internal[i] = (entity instanceof IAero aero) ?  Math.max(aero.getSI(), 0) :
                  Math.max(entity.getTotalInternal(), 0);
            this.ecm[i] = entity.hasECM();
            this.vip[i] = UnitClassifier.isVIP(entity);
            this.cargoTransport[i] = UnitClassifier.isTransport(entity);
            int[] missionRoles = UnitClassifier.determineUnitMissionRole(entity).stream().mapToInt(Enum::ordinal).toArray();
            this.missionRole[i] = missionRoles;
        }
        logger.debug("Updated StructOfUnitArrays with {} entities", length);
    }

    private void setup(int length) {
        if (length > this.length) {
            this.id = new int[length];
            this.x = new int[length];
            this.y = new int[length];
            this.facing = new int[length];
            this.ownerId = new int[length];
            this.teamId = new int[length];
            this.maxRange = new int[length];
            this.role = new int[length];
            this.armor = new int[length];
            this.internal = new int[length];
            this.ecm = new boolean[length];
            this.vip = new boolean[length];
            this.cargoTransport = new boolean[length];
            this.missionRole = new int[length][];
        }
        this.length = length;
    }

    /**
     * Get the number of entities in the struct.
     * @return the number of entities
     */
    public int size() {
        return length;
    }

    /**
     * Get the max weapon range of the entity at the given index.
     * @param index index of the unit
     * @return the max weapon range of the entity
     */
    public int getMaxWeaponRange(int index) {
        assertIndex(index);
        return maxRange[index];
    }

    /**
     * Get the id of the entity at the given index.
     * @param index index of the unit
     * @return the id of the entity
     */
    public int getId(int index) {
        assertIndex(index);
        return id[index];
    }

    /**
     * Get the x coordinate of the entity at the given index.
     * @param index index of the unit
     * @return the x coordinate of the entity
     */
    public int getX(int index) {
        assertIndex(index);
        return x[index];
    }

    /**
     * Get the y coordinate of the entity at the given index.
     * @param index index of the unit
     * @return the y coordinate of the entity
     */
    public int getY(int index) {
        assertIndex(index);
        return y[index];
    }

    /**
     * Get the x and y coordinates of the entity at the given index.
     * @param index index of the unit
     * @return an array with the x and y coordinates of the entity
     */
    public int[] getXY(int index) {
        assertIndex(index);
        int[] xy = new int[2];
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
        assertIndex(index);
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
        int[][] xyr = new int[length][3];
        getAllXYMaxRange(xyr);
        return xyr;
    }

    /**
     * Get the x and y coordinates of all the entities, it copies the positions in the array passed.
     * @param xyr array to copy the x, y and max range coordinates
     */
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
        int[][] xy = new int[length][2];
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
        assertIndex(index);
        int[] xyf = new int[3];
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
        assertIndex(index);
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
     * Get the facing of the unit at the given index.
     * @param index index of the unit
     * @return the facing of the unit
     */
    public int getFacing(int index) {
        assertIndex(index);
        return facing[index];
    }

    private void assertIndex(int index) {
        if (index >= length) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + " >= " + length);
        }
    }

    /**
     * Get the owner id of the unit at the given index.
     * @param index index of the unit
     * @return the owner id of the unit
     */
    public int getOwnerId(int index) {
        assertIndex(index);
        return ownerId[index];
    }

    /**
     * Get the team id of the unit at the given index.
     * @param index index of the unit
     * @return the team id of the unit
     */
    public int getTeamId(int index) {
        assertIndex(index);
        return teamId[index];
    }

    /**
     * Get the role of the unit at the given index.
     * @param index index of the unit
     * @return the role of the unit
     */
    public int getRole(int index) {
        assertIndex(index);
        return role[index];
    }

    /**
     * Get the armor remaining of the unit at the given index.
     * @param index index of the unit
     * @return the armor remaining of the unit
     */
    public int getArmor(int index) {
        assertIndex(index);
        return armor[index];
    }

    /**
     * Get the armor remaining of the unit at the given index.
     * @param index index of the unit
     * @return the armor remaining of the unit
     */
    public int getInternal(int index) {
        assertIndex(index);
        return internal[index];
    }

    /**
     * True if the unit has ECM, false otherwise.
     * @param index index of the unit
     * @return true if the unit has ECM, false otherwise
     */
    public boolean hasECM(int index) {
        assertIndex(index);
        return ecm[index];
    }

    /**
     * True if the unit is VIP, false otherwise.
     * @param index index of the unit
     * @return true if the unit is VIP, false otherwise
     */
    public boolean isVIP(int index) {
        assertIndex(index);
        return vip[index];
    }

    /**
     * True if the unit is cargo transport, false otherwise.
     * @param index index of the unit
     * @return true if the unit is cargo transport, false otherwise
     */
    public boolean isCargoTransport(int index) {
        assertIndex(index);
        return cargoTransport[index];
    }

    /**
     * True if the unit has the given mission role, false otherwise.
     * @param missionRole mission role to check
     * @return true if the unit has the given mission role, false otherwise
     */
    public boolean hasMissionRole(MissionRole missionRole) {
        for (int i = 0; i < length; i++) {
            for (int role : this.missionRole[i]) {
                if (role == missionRole.ordinal()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the mission roles of the unit at the given index.
     * @param index index of the unit
     * @return the mission roles of the unit
     */
    public Set<MissionRole> getMissionRole(int index) {
        assertIndex(index);
        Set<MissionRole> missionRoles = new HashSet<>();
        for (int role : this.missionRole[index]) {
            missionRoles.add(MissionRole.values()[role]);
        }
        return missionRoles;
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
