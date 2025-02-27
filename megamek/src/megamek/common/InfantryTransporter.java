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
 */
package megamek.common;

import java.io.Serializable;
import java.util.Vector;

/**
 * Classes that implement this interface have the ability to load, carry, drop, and
 * unload Infantry units in the game.
 * This interface includes a subset of the Bay.java class methods and Transporter interface.
 * @see megamek.common.Bay
 * @see megamek.common.Transporter
 */
public interface InfantryTransporter extends Serializable {

    /** The amount of space taken up by an infantry unit in a transport bay differs from the space
     * in an infantry compartment (used in APCs) due to quarters, equipment storage, and maintenance
     * equipment. A single cubicle holds a platoon, except in the case of mechanized which requires
     * a cubicle per squad. */

    public enum PlatoonType {
        FOOT (5, 28, 25),
        JUMP (6, 21, 20),
        MOTORIZED (7, 28, 25),
        MECHANIZED (8, 7, 5);

        private int weight;
        private int isPersonnel;
        private int clanPersonnel;

        PlatoonType(int weight, int isPersonnel, int clanPersonnel) {
            this.weight = weight;
            this.isPersonnel = isPersonnel;
            this.clanPersonnel = clanPersonnel;
        }

        public int getWeight() {
            return weight;
        }

        public int getISPersonnel() {
            return isPersonnel;
        }

        public int getClanPersonnel() {
            return clanPersonnel;
        }

        @Override
        public String toString() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }

        public static PlatoonType getPlatoonType(Entity en) {
            switch (en.getMovementMode()) {
                case TRACKED:
                case WHEELED:
                case HOVER:
                case VTOL:
                case SUBMARINE:
                    return MECHANIZED;
                case INF_MOTORIZED:
                    return MOTORIZED;
                case INF_JUMP:
                    return JUMP;
                default:
                    return FOOT;
            }
        }
    }

    /**
     * These methods are required for Infantry unit loading/unloading.
     * In practice, all are provided by Bay() or overridden in transports
     * that can carry infantry.
     */
    double getUnused();
    double getUnusedSlots();
    int getCurrentDoors();
    Vector<Entity> getLoadedUnits();
    double spaceForUnit(Entity unit);
    boolean canLoad(Entity unit);

}
