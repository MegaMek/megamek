/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org).
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

/**
 * Represents a volume of space set aside for carrying infantry platoons aboard large spacecraft
 * and mobile structures. Marines count as crew and should have at least steerage quarters.
 */
public final class InfantryBay extends Bay {
    private static final long serialVersionUID = 946578184870030662L;

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

    // This represents the "factory setting" of the bay, and is used primarily by the construction rules.
    // In practice we support loading any type of infantry into the bay as long as there is room to avoid
    // the headache of having to do formal reconfigurations.
    private PlatoonType platoonType = PlatoonType.FOOT;

    /**
     * The default constructor is only for serialization.
     */
    private InfantryBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a space for the given tonnage of troops. This is the total tonnage of the bay;
     * the amount of space taken up by a given unit depends on the PlatoonType.
     *
     * @param space
     *            - The number of platoons (or squads for mechanized) of the designated type this
     *              bay can carry.
     * @param bayNumber
     */
    public InfantryBay(double space, int doors, int bayNumber, PlatoonType bayType) {
        // We need to track by total tonnage rather than individual platoons
        totalSpace = space * bayType.getWeight();
        currentSpace = totalSpace;
        this.minDoors = 0;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        currentdoors = doors;
        this.platoonType = bayType;
    }

    @Override
    public double spaceForUnit(Entity unit) {
        PlatoonType type = PlatoonType.getPlatoonType(unit);
        if ((unit instanceof Infantry) && (type == PlatoonType.MECHANIZED)) {
            return type.getWeight() * ((Infantry) unit).getSquadCount();
        } else {
            return type.getWeight();
        }
    }

    /**
     * Determines if this object can accept the given unit. The unit may not be
     * of the appropriate type or there may be no room for the unit.
     *
     * @param unit
     *            - the <code>Entity</code> to be loaded.
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *         otherwise.
     */
    @Override
    public boolean canLoad(Entity unit) {
        // Only infantry
        boolean result = unit.hasETypeFlag(Entity.ETYPE_INFANTRY);

        // We must have enough space for the new troops.
        // POSSIBLE BUG: we may have to take the Math.ceil() of the weight.
        if (getUnused() < spaceForUnit(unit)) {
            result = false;
        }

        // is the door functional
        if (currentdoors < loadedThisTurn) {
            result = false;
        }

        // Return our result.
        return result;
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        StringBuilder sb = new StringBuilder();
        sb.append("Infantry Bay ").append(numDoorsString()).append(" - ")
                .append(getUnusedSlots())
            .append(" ").append(platoonType.toString());
        if (platoonType != PlatoonType.MECHANIZED) {
            sb.append(" platoon");
        } else {
            sb.append(" squad");
        }
        if (getUnusedSlots() != 1) {
            sb.append("s");
        }
        return sb.toString();
    }

    @Override
    public double getUnusedSlots() {
        return currentSpace / platoonType.getWeight() - getBayDamage();
    }

    @Override
    public int getPersonnel(boolean clan) {
        return (int) (totalSpace / platoonType.getWeight())
                * (clan ? platoonType.getClanPersonnel() : platoonType.getISPersonnel());
    }

    @Override
    public String getDefaultSlotDescription() {
        return " (" + platoonType.toString() + ")";
    }

    @Override
    public String getType() {
        return "Infantry (" + platoonType.toString() + ")";
    }

    @Override
    public String toString() {
        String bayType = "infantrybay";
        return this.bayString(
                bayType,
                (totalSpace / platoonType.getWeight()),
                doors,
                bayNumber,
                platoonType.toString(),
                Entity.LOC_NONE,
                0
        );
    }

    public PlatoonType getPlatoonType() {
        return platoonType;
    }

    @Override
    public long getCost() {
        // Based on the weight of the equipment (not capacity), rounded up to the whole ton
        return 15000L * (long) Math.ceil(getWeight());
    }
}
