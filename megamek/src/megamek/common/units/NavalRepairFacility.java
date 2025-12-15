/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.units;

import java.io.Serial;
import java.text.DecimalFormat;

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.bays.Bay;
import megamek.common.bays.UnitBay;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.util.RoundWeight;

/**
 * Standard naval repair facilities for space stations (jumpships and warships can also carry a single facility). See
 * TacOps 334-5 for rules.
 *
 * @author Neoancient
 */
public class NavalRepairFacility extends UnitBay {

    @Serial
    private static final long serialVersionUID = -5983197195382933671L;

    // No more than one bay is allowed per armor facing
    private int facing = Entity.LOC_NONE;
    private boolean pressurized = false;
    private final boolean arts;

    /**
     * The default constructor is only for serialization.
     */

    protected NavalRepairFacility() {
        totalSpace = 0;
        currentSpace = 0;
        arts = false;
    }

    /**
     * Create a new repair facility
     *
     * @param size        Maximum capacity in tons
     * @param doors       The number of bay doors
     * @param bayNumber   The id number for the bay
     * @param facing      The armor facing of the facility
     * @param pressurized Whether the bay is pressurized
     */
    public NavalRepairFacility(double size, int doors, int bayNumber, int facing, boolean pressurized) {
        this(size, doors, bayNumber, facing, pressurized, false);
    }

    /**
     * Create a new repair facility
     *
     * @param size        Maximum capacity in tons
     * @param doors       The number of bay doors
     * @param bayNumber   The id number for the bay
     * @param facing      The armor facing of the facility
     * @param pressurized Whether the bay is pressurized
     * @param arts        Whether the bay has the advanced robotic transport system
     */
    public NavalRepairFacility(double size, int doors, int bayNumber, int facing,
          boolean pressurized, boolean arts) {
        totalSpace = size;
        currentSpace = size;
        this.doors = doors;
        doorsNext = doors;
        this.bayNumber = bayNumber;
        currentDoors = doors;
        this.facing = facing;
        this.pressurized = pressurized;
        this.arts = arts;
    }

    /**
     * Pressurized facility allows crew to work without encumbrance of life support gear. No game effect that I could
     * find.
     *
     * @return Whether the facility is pressurized.
     */
    public boolean isPressurized() {
        return pressurized;
    }

    /**
     * Advanced Robotic Transport System (ARTS). See IO, p. 147
     *
     * @return Whether the bay has the ARTS automated system
     */
    public boolean hasARTS() {
        return arts;
    }

    public void setPressurized(boolean pressurized) {
        this.pressurized = pressurized;
    }

    @Override
    public String getTransporterType() {
        StringBuilder sb = new StringBuilder();
        if (arts) {
            sb.append("ARTS ");
        }
        sb.append("Naval Repair Facility ");
        if (pressurized) {
            sb.append("(Pressurized)");
        } else {
            sb.append("Unpressurized");
        }
        return sb.toString();
    }

    @Override
    public boolean canLoad(Entity unit) {
        if (unit.getWeight() > currentSpace) {
            return false;
        }
        // We can carry two dropships or one JS/WS/SS.
        if (unit.hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
            if (troops.isEmpty()) {
                return true;
            }

            Entity troop = unit.getGame().getEntity(troops.get(0));

            return ((troops.size() == 1)
                  && (null != troop) && (troop.hasETypeFlag(Entity.ETYPE_DROPSHIP)));
        } else if (unit.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            return troops.isEmpty();
        } else {
            return false;
        }
    }

    @Override
    public double getWeight() {
        double factor;
        if (pressurized) {
            factor = 0.075;
        } else {
            factor = 0.025;
        }
        if (arts) {
            factor *= 1.25;
        }
        return RoundWeight.nextHalfTon(totalSpace * factor);
    }

    @Override
    public int getFacing() {
        return facing;
    }

    /**
     * Sets the bay location
     *
     * @param facing The armor facing (location) of the bay
     */
    @Override
    public void setFacing(int facing) {
        this.facing = facing;
    }

    @Override
    public String toString() {
        return (arts ? "artsnavalrepair" : "navalrepair")
              + (pressurized ? "pressurized:" : "unpressurized:")
              + totalSpace + FIELD_SEPARATOR
              + doors + FIELD_SEPARATOR
              + bayNumber + FIELD_SEPARATOR
              + FACING_PREFIX + getFacing();
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        StringBuilder sb = new StringBuilder("Standard ");
        if (pressurized) {
            sb.append("Pressurized");
        } else {
            sb.append("Unpressurized");
        }
        if (arts) {
            sb.append(" ARTS");
        }
        sb.append(" Naval Repair Facility: ");
        sb.append(DecimalFormat.getInstance().format(totalSpace)).append(" tons");
        return sb.toString();
    }

    @Override
    public int hardpointCost() {
        return 2;
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TechBase.ALL).setAdvancement(DATE_ES, DATE_ES, DATE_ES)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        if (arts) {
            return Bay.artsTechAdvancement();
        } else {
            return NavalRepairFacility.techAdvancement();
        }
    }

    @Override
    public long getCost() {
        long cost = (long) totalSpace * 5000L;
        if (pressurized) {
            cost *= 2;
        }
        if (arts) {
            cost += 1000000L;
        }
        return cost;
    }

    /**
     * Most bay types track space by individual units. Infantry bays have variable space requirements and must track by
     * cubicle tonnage.
     *
     * @param unit The unit to load/unload.
     *
     * @return The amount of bay space taken up by the unit.
     */
    @Override
    public double spaceForUnit(Entity unit) {
        return unit.getTonnage();
    }

}
