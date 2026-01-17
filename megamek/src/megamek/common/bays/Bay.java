/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.bays;

import static java.util.stream.Collectors.toList;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Era;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Transporter;
import megamek.common.game.Game;
import megamek.common.interfaces.ITechnology;
import megamek.common.units.Entity;

/**
 * Represents a volume of space set aside for carrying cargo of some sort aboard large spacecraft and mobile structures
 */
public class Bay implements Transporter, ITechnology {
    @Serial
    private static final long serialVersionUID = -9056450317468016272L;

    public static final int UNSET_BAY = -1;
    public static final String FIELD_SEPARATOR = ":";
    public static final String FACING_PREFIX = "f";

    // Minimum number of doors for all unit bays (except infantry) is 1
    // Minimum number of doors for all other bays is 0
    protected int minDoors = 0;
    protected int doors = 1;
    protected int doorsNext = 1;
    protected int currentDoors = doors;
    protected int unloadedThisTurn = 0;
    protected int loadedThisTurn = 0;
    protected List<Integer> recoverySlots = new ArrayList<>();
    protected int bayNumber = 0;
    protected transient Game game = null;
    protected double damage;

    /** The troops being carried. */
    protected Vector<Integer> troops = new Vector<>();

    /** The total amount of space available for troops. */
    protected double totalSpace;

    /** The current amount of space not occupied by troops or cargo. */
    protected double currentSpace;

    /**
     * The default constructor is only for serialization.
     */
    protected Bay() {
        totalSpace = 0;
        currentSpace = 0;
        damage = 0;
    }

    /**
     * Create a space for the given tonnage of troops. For this class, only the weight of the troops (and their
     * equipment) are considered; if you'd like to think that they are stacked like lumber, be my guest.
     *
     * @param space     The weight of troops (in tons) this space can carry.
     * @param doors     The number of bay doors
     * @param bayNumber The id number for the bay
     */
    public Bay(double space, int doors, int bayNumber) {
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        doorsNext = doors;
        this.currentDoors = doors;
        this.bayNumber = bayNumber;
        damage = 0;
    }

    /**
     * Bay damage to unit transport bays is tracked by number of cubicles/units. Damage to cargo bays is tracked by
     * cargo tonnage.
     *
     * @return The reduction of bay capacity due to damage.
     */
    public double getBayDamage() {
        return damage;
    }

    /**
     * Bay damage to unit transport bays is tracked by number of cubicles/units. Damage to cargo bays is tracked by
     * cargo tonnage.
     *
     * @param damage The total amount of bay capacity reduced due to damage.
     */
    public void setBayDamage(double damage) {
        this.damage = Math.min(damage, totalSpace);
    }

    /**
     * Method used by MHQ to update bay space when loading units in lobby. See Utilities.loadPlayerTransports This
     * ensures that consumed space is kept in sync between the MM client and MHQ game thread
     *
     * @param space - double representing space consumed by the unit being loaded. 1 except in the case of infantry
     */
    public void setCurrentSpace(double space) {
        this.currentSpace = Math.max(0, (currentSpace - space));
    }

    // the starting number of doors for the bay.
    public int getDoors() {
        return doors;
    }

    // the required minimum number of doors for the bay.
    public int getMinDoors() {
        return minDoors;
    }

    public void setDoors(int d) {
        doors = d;
        doorsNext = d;
        currentDoors = d;
    }

    public Vector<Integer> getTroops() {
        return troops;
    }

    public int getCurrentDoors() {
        // defense against invalid values
        return Math.min(doors, Math.max(currentDoors, 0));
    }

    public void setCurrentDoors(int d) {
        currentDoors = d;
    }

    // for setting doors after this launch
    public void setDoorsNext(int d) {
        doorsNext = d;
    }

    public int getDoorsNext() {
        return doorsNext;
    }

    public void resetDoors() {
        doorsNext = currentDoors;
    }

    public void resetCounts() {
        unloadedThisTurn = 0;
        loadedThisTurn = 0;
    }

    /**
     * Most bay types track space by individual units. Infantry bays have variable space requirements and must track by
     * cubicle tonnage.
     *
     * @param unit The unit to load/unload.
     *
     * @return The amount of bay space taken up by the unit.
     */
    public double spaceForUnit(Entity unit) {
        return 1;
    }

    @Override
    public void resetTransporter() {
        troops = new Vector<>();
        currentSpace = totalSpace;
        resetCounts();
    }

    @Override
    public boolean canLoad(Entity unit) {
        return (getUnused() >= spaceForUnit(unit)) && (currentDoors > loadedThisTurn);
    }

    /**
     * @return True when further doors are available to unload units this turn. This method checks only the state of bay
     *       doors, not if it has units left to unload or the status of those.
     */
    public boolean canUnloadUnits() {
        return currentDoors > unloadedThisTurn;
    }

    @Override
    public void load(Entity unit) throws IllegalArgumentException {
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load "
                  + unit.getShortName()
                  + " into this bay. "
                  + getUnused());
        }
        currentSpace -= spaceForUnit(unit);
        if (!unit.getGame().getPhase().isDeployment() && !unit.getGame().getPhase().isLounge()) {
            loadedThisTurn += 1;
        }
        troops.addElement(unit.getId());
    }

    @Override
    public Vector<Entity> getLoadedUnits() {
        return troops.stream()
              .map(unit -> game.getEntity(unit))
              .filter(Objects::nonNull)
              .collect(Collectors.toCollection(Vector::new));
    }

    /**
     * Generate a raw list of the Ids stored in troops. Used by MHQ in cases where we can't get the entities via Game
     *
     * @return A list of unit IDs of loaded units
     */
    public List<Integer> getLoadedUnitIds() {
        // Return a copy of our list of troops.
        return new ArrayList<>(troops);
    }

    /** @return A (possibly empty) list of units from this bay that can be launched. Units in recovery cannot launch. */
    public List<Entity> getLaunchableUnits() {
        return troops.stream()
              .map(game::getEntity)
              .filter(Objects::nonNull)
              .filter(e -> e.getRecoveryTurn() == 0)
              .collect(toList());
    }

    /** @return A (possibly empty) list of units from this bay that can be assault-dropped. */
    public List<Entity> getDroppableUnits() {
        return troops.stream()
              .map(game::getEntity)
              .filter(Objects::nonNull)
              .filter(Entity::canAssaultDrop)
              .collect(toList());
    }

    /** @return A (possibly empty) list of units from this bay that can be unloaded on the ground. */
    public List<Entity> getUnloadableUnits() {
        // TODO: we need to handle aerospace and VTOLs differently
        // TODO: shouldn't this check the entity state like wasLoadedThisTurn()? It is equal to getLoadedUnits()
        return troops.stream().map(game::getEntity).filter(Objects::nonNull).collect(toList());
    }

    @Override
    public boolean unload(Entity unit) {
        boolean wasCarried = troops.removeElement(unit.getId());
        if (wasCarried) {
            currentSpace += spaceForUnit(unit);
            unloadedThisTurn += 1;
        }
        return wasCarried;
    }

    /**
     * Return a string that identifies the unused capacity of this transporter.
     *
     * @return A <code>String</code> meant for a human.
     */
    public String getUnusedString(boolean showRecovery) {
        return numDoorsString() + "  - " + getUnused() + ((getUnused() > 1) ? " units" : " unit");
    }

    protected String numDoorsString() {
        return "(" + getCurrentDoors() + ((getCurrentDoors() == 1) ? " door" : " doors") + ")";
    }

    @Override
    public String getUnusedString() {
        return getUnusedString(true);
    }

    @Override
    public double getUnused() {
        return currentSpace - damage;
    }

    /**
     * @return The amount of unused space in the bay expressed in slots. For most bays this is the same as the unused
     *       space, but bays for units that can take up a variable amount of space (such as infantry bays) this
     *       calculates the number of the default unit size that can fit into the remaining space.
     */
    public double getUnusedSlots() {
        return currentSpace;
    }

    /**
     * @return A String that describes the default slot type. Only meaningful for bays with variable space requirements
     *       (like infantry).
     */
    public String getDefaultSlotDescription() {
        return "";
    }

    @Override
    public boolean isWeaponBlockedAt(int loc, boolean isRear) {
        return false;
    }

    @Override
    public Entity getExteriorUnitAt(int loc, boolean isRear) {
        return null;
    }

    @Override
    public final List<Entity> getExternalUnits() {
        return new ArrayList<>(1);
    }

    @Override
    public int getCargoMpReduction(Entity carrier) {
        return 0;
    }

    /** Destroys a door for next turn. */
    public void destroyDoorNext() {
        if (getDoorsNext() > 0) {
            setDoorsNext(getDoorsNext() - 1);
        }
    }

    // destroy a door
    public void destroyDoor() {
        if (getCurrentDoors() > 0) {
            setCurrentDoors(getCurrentDoors() - 1);
        }
    }

    // restore a door
    public void restoreDoor() {
        if (getCurrentDoors() < getDoors()) {
            setCurrentDoors(getCurrentDoors() + 1);
        }
    }

    // restore all doors
    public void restoreAllDoors() {
        setCurrentDoors(getDoors());
    }

    @Override
    public int getNumberUnloadedThisTurn() {
        return unloadedThisTurn;
    }

    @Override
    public int getNumberLoadedThisTurn() {
        return loadedThisTurn;
    }

    /**
     * @return the tonnage of the bay, not the actual mass or weight
     */
    public double getWeight() {
        return totalSpace;
    }

    /**
     * @param clan Whether the bay is installed in a Clan unit. Needed for infantry bays.
     *
     * @return The number of additional crew provided by the bay. This includes transport bays only; crew quarters are
     *       already accounted for in the crew total.
     */
    public int getPersonnel(boolean clan) {
        return 0;
    }

    /**
     * Updated toString() and helpers to normalize bay string output To match new 6-field format: type:space(current or
     * total):doors:bayNumber:infantryType:facing:status bitmap See BLKFile.java:BLKFile constants
     */
    @Override
    public String toString() {
        return this.bayString("bay", totalSpace, doors, bayNumber, "", Entity.LOC_NONE, 0);
    }

    public String bayString(String bayType, double space, int doors, int bayNumber, String infType, int facing,
          int bitmap) {
        return String.format("%s:%s:%s:%s:%s:%s:%s", bayType, space, doors, bayNumber, infType, facing, bitmap);
    }

    public String bayString(String bayType, double space, int doors, int bayNumber) {
        return String.format("%s:%s:%s:%s:%s:%s:%s", bayType, space, doors, bayNumber, "", Entity.LOC_NONE, 0);
    }

    public String bayString(String bayType, double space, int doors) {
        return String.format("%s:%s:%s:%s:%s:%s:%s", bayType, space, doors, UNSET_BAY, "", Entity.LOC_NONE, 0);
    }

    /**
     * @return The total size of the bay.
     */
    public double getCapacity() {
        return totalSpace;
    }

    public int getBayNumber() {
        return bayNumber;
    }

    /**
     * Some bays (drop-shuttle and repair facility) have a maximum number per armor facing.
     *
     * @return The facing of the bay, or Entity.LOC_NONE if the bay does not require a facing.
     */
    public int getFacing() {
        return Entity.LOC_NONE;
    }

    /**
     * Sets the armor facing for the bay, if the bay type requires it. If not required by the bay type, does nothing.
     *
     * @param facing The location to use for the facing.
     */
    public void setFacing(int facing) {
        // do nothing by default
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    // Use cargo/infantry for default tech advancement
    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TechBase.ALL)
              .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
              .setTechRating(TechRating.A)
              .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    /**
     * Shared by several types of bays
     *
     * @return Tech advancement for advanced robotic transport system.
     */
    public static TechAdvancement artsTechAdvancement() {
        return new TechAdvancement(TechBase.ALL)
              .setAdvancement(2600, 2609, DATE_NONE, 2804, 3068)
              .setApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.WB)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.E)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    public TechAdvancement getTechAdvancement() {
        return Bay.techAdvancement();
    }

    @Override
    public boolean isClan() {
        return getTechAdvancement().isClan();
    }

    @Override
    public boolean isMixedTech() {
        return getTechAdvancement().isMixedTech();
    }

    @Override
    public TechBase getTechBase() {
        return getTechAdvancement().getTechBase();
    }

    @Override
    public int getIntroductionDate() {
        return getTechAdvancement().getIntroductionDate();
    }

    @Override
    public int getPrototypeDate() {
        return getTechAdvancement().getPrototypeDate();
    }

    @Override
    public int getProductionDate() {
        return getTechAdvancement().getProductionDate();
    }

    @Override
    public int getCommonDate() {
        return getTechAdvancement().getCommonDate();
    }

    @Override
    public int getExtinctionDate() {
        return getTechAdvancement().getExtinctionDate();
    }

    @Override
    public int getReintroductionDate() {
        return getTechAdvancement().getReintroductionDate();
    }

    @Override
    public TechRating getTechRating() {
        return getTechAdvancement().getTechRating();
    }

    @Override
    public AvailabilityValue getBaseAvailability(Era era) {
        return getTechAdvancement().getBaseAvailability(era);
    }

    @Override
    public int getIntroductionDate(boolean clan, Faction faction) {
        return getTechAdvancement().getIntroductionDate(clan, faction);
    }

    @Override
    public int getPrototypeDate(boolean clan, Faction faction) {
        return getTechAdvancement().getPrototypeDate(clan, faction);
    }

    @Override
    public int getProductionDate(boolean clan, Faction faction) {
        return getTechAdvancement().getProductionDate(clan, faction);
    }

    @Override
    public int getExtinctionDate(boolean clan, Faction faction) {
        return getTechAdvancement().getExtinctionDate(clan, faction);
    }

    @Override
    public int getReintroductionDate(boolean clan, Faction faction) {
        return getTechAdvancement().getReintroductionDate(clan, faction);
    }

    @Override
    public SimpleTechLevel getStaticTechLevel() {
        return getTechAdvancement().getStaticTechLevel();
    }

    /** @return true if this bay represents crew quarters or seating rather than a unit transport bay. */
    public boolean isQuarters() {
        return false;
    }

    /** @return true if this bay represents cargo capacity rather than unit transport or crew quarters. */
    public boolean isCargo() {
        return false;
    }

    /** @return The cost of the bay in C-bills */
    public long getCost() {
        return 0;
    }

    /** @return The safe launch rate for this particular bay: # of intact doors x 2 */
    public int getSafeLaunchRate() {
        return getCurrentDoors() * 2;
    }
}
