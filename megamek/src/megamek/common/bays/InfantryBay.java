/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org).
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
 */
package megamek.common.bays;

import java.io.Serial;

import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.InfantryTransporter;

/**
 * Represents a volume of space set aside for carrying infantry platoons aboard large spacecraft and mobile structures.
 * Marines count as crew and should have at least steerage quarters.
 */
public final class InfantryBay extends Bay implements InfantryTransporter {
    @Serial
    private static final long serialVersionUID = 946578184870030662L;

    // This represents the "factory setting" of the bay, and is used primarily by the construction rules. In practice,
    // we support loading any type of infantry into the bay as long as there is room to avoid the headache of having
    // to do formal reconfigurations.
    private final PlatoonType platoonType;

    /**
     * Create a space for the given tonnage of troops. This is the total tonnage of the bay; the amount of space taken
     * up by a given unit depends on the PlatoonType.
     *
     * @param space     The number of platoons (or squads for mechanized) of the designated type this bay can carry.
     * @param bayNumber What bay this is in
     */
    public InfantryBay(double space, int doors, int bayNumber, PlatoonType bayType) {
        // We need to track by total tonnage rather than individual platoons
        totalSpace = space * bayType.getWeight();
        currentSpace = totalSpace;
        this.minDoors = 0;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        currentDoors = doors;
        this.platoonType = bayType;
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        StringBuilder sb = new StringBuilder();
        sb.append("Infantry Bay ")
              .append(numDoorsString())
              .append(" - ")
              .append(getUnusedSlots())
              .append(" ")
              .append(platoonType.toString());
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

    /**
     * Determines if this object can accept the given unit. The unit may not be of the appropriate type or there may be
     * no room for the unit.
     *
     * @param unit - the <code>Entity</code> to be loaded.
     *
     * @return <code>true</code> if the unit can be loaded, <code>false</code>
     *       otherwise.
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

        // Return our result.
        return result;
    }

    @Override
    public boolean canUnloadUnits() {
        // Infantry is only restricted by adjacency requirements (TW pp. 223 - 225)
        return super.canUnloadUnits() ||
                     troops.stream().map(unit -> game.getEntity(unit)).anyMatch(e -> (e != null && e.isInfantry()));
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

    @Override
    public double getUnusedSlots() {
        return currentSpace / platoonType.getWeight() - getBayDamage();
    }

    @Override
    public int getPersonnel(boolean clan) {
        return (int) (totalSpace / platoonType.getWeight()) *
                     (clan ? platoonType.getClanPersonnel() : platoonType.getISPersonnel());
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
        String bayType = "infantryBay";
        return this.bayString(bayType,
              (totalSpace / platoonType.getWeight()),
              doors,
              bayNumber,
              platoonType.toString(),
              Entity.LOC_NONE,
              0);
    }

    public PlatoonType getPlatoonType() {
        return platoonType;
    }

    @Override
    public long getCost() {
        // Based on the weight of the equipment (not capacity), rounded up to the whole ton
        return 15000L * (long) Math.ceil(getWeight());
    }

    @Override
    public BayType getBayType() {
        return switch (platoonType) {
            case JUMP -> BayType.INFANTRY_JUMP;
            case MOTORIZED -> BayType.INFANTRY_MOTORIZED;
            case MECHANIZED -> BayType.INFANTRY_MECHANIZED;
            default -> BayType.INFANTRY_FOOT;
        };
    }
}
