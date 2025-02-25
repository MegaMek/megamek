/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/**
 * Classes that implement this interface have the ability to load, carry, and
 * unload units in the game. It is anticipated that classes will exist for
 * passenger compartments, battle armor steps, Mek bays, Aerospace hangers, and
 * vehicle garages. Other possible classes include cargo bays and Dropship
 * docks.
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

    default public double spaceForUnit(Entity unit) {
        PlatoonType type = PlatoonType.getPlatoonType(unit);
        if ((unit instanceof Infantry) && (type == PlatoonType.MECHANIZED)) {
            return type.getWeight() * ((Infantry) unit).getSquadCount();
        } else {
            return type.getWeight();
        }
    }

    /**
     * @return the number of unused spaces in this transporter.
     */
    double getUnused();
    int getCurrentDoors();
    int getNumberLoadedThisTurn();

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *         otherwise.
     */
    default public boolean canLoad(Entity unit) {
        // Only infantry
        boolean result = unit.hasETypeFlag(Entity.ETYPE_INFANTRY);

        // We must have enough space for the new troops.
        // POSSIBLE BUG: we may have to take the Math.ceil() of the weight.
        if (getUnused() < spaceForUnit(unit)) {
            result = false;
        }

        // is the door functional
        if (getCurrentDoors() < getNumberLoadedThisTurn()) {
            result = false;
        }

        // Return our result.
        return result;
    }

}
