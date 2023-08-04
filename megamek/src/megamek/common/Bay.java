/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2018-2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Represents a volume of space set aside for carrying cargo of some sort
 * aboard large spacecraft and mobile structures
 */
public class Bay implements Transporter, ITechnology {
    private static final long serialVersionUID = -9056450317468016272L;

    public static final String FIELD_SEPARATOR = ":";
    public static final String FACING_PREFIX = "f";

    /** Minimum number of doors for all bays (except infantry) is 1 **/
    int minDoors = 1;
    int doors = 1;
    int doorsNext = 1;
    int currentdoors = doors;
    protected int unloadedThisTurn = 0;
    protected int loadedThisTurn = 0;
    List<Integer> recoverySlots = new ArrayList<>();
    int bayNumber = 0;
    transient Game game = null;
    private double damage;

    /** The troops being carried. */
    Vector<Integer> troops = new Vector<>();

    /** The total amount of space available for troops. */
    double totalSpace;

    /** The current amount of space not occupied by troops or cargo. */
    double currentSpace;

    /**
     * The default constructor is only for serialization.
     */
    protected Bay() {
        totalSpace = 0;
        currentSpace = 0;
        damage = 0;
    }

    /**
     * Create a space for the given tonnage of troops. For this class, only the
     * weight of the troops (and their equipment) are considered; if you'd like
     * to think that they are stacked like lumber, be my guest.
     *
     * @param space The weight of troops (in tons) this space can carry.
     * @param doors      The number of bay doors
     * @param bayNumber  The id number for the bay
     */
    public Bay(double space, int doors, int bayNumber) {
        totalSpace = space;
        currentSpace = space;
        this.doors = doors;
        doorsNext = doors;
        this.currentdoors = doors;
        this.bayNumber = bayNumber;
        damage = 0;
    }

    /**
     * Bay damage to unit transport bays is tracked by number of cubicles/units. Damage
     * to cargo bays is tracked by cargo tonnage.
     *
     * @return The reduction of bay capacity due to damage.
     */
    public double getBayDamage() {
        return damage;
    }

    /**
     * Bay damage to unit transport bays is tracked by number of cubicles/units. Damage
     * to cargo bays is tracked by cargo tonnage.
     *
     * @param damage The total amount of bay capacity reduced due to damage.
     */
    public void setBayDamage(double damage) {
        this.damage = Math.min(damage, totalSpace);
    }

    /**
     * Method used by MHQ to update bay space when loading units in lobby. See Utilities.loadPlayerTransports
     * This ensures that consumed space is kept in sync between the MM client and MHQ game thread
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
        currentdoors = d;
    }

    public int getCurrentDoors() {
        // defense against invalid values
        return Math.min(doors, Math.max(currentdoors, 0));
    }

    public void setCurrentDoors(int d) {
        currentdoors = d;
    }

    // for setting doors after this launch
    public void setDoorsNext(int d) {
        doorsNext = d;
    }

    public int getDoorsNext() {
        return doorsNext;
    }

    public void resetDoors() {
        doorsNext = currentdoors;
    }

    public void resetCounts() {
        unloadedThisTurn = 0;
        loadedThisTurn = 0;
    }

    /**
     * Most bay types track space by individual units. Infantry bays have variable space requirements
     * and must track by cubicle tonnage.
     *
     * @param unit The unit to load/unload.
     * @return     The amount of bay space taken up by the unit.
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
        return (getUnused() >= spaceForUnit(unit)) && (currentdoors > loadedThisTurn);
    }

    /**
     * To unload units, a bay must have more doors available than units unloaded
     * this turn. Can't load, launch or recover into a damaged bay, but you can unload it
     *
     * @return True when further doors are available to unload units this turn
     */
    public boolean canUnloadUnits() {
        return currentdoors > unloadedThisTurn;
    }

    @Override
    public void load(Entity unit) throws IllegalArgumentException {
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load " + unit.getShortName() + " into this bay. " + getUnused());
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
     * Generate a raw list of the Ids stored in troops.
     * Used by MHQ in cases where we can't get the entities via Game
     *
     * @return A list of unit IDs of loaded units
     */
    public List<Integer> getLoadedUnitIds() {
        // Return a copy of our list of troops.
        return new ArrayList<>(troops);
    }

    /** @return A (possibly empty) list of units from this bay that can be launched. Units in recovery cannot launch. */
    public List<Entity> getLaunchableUnits() {
        return troops.stream().map(game::getEntity).filter(Objects::nonNull).filter(e -> e.getRecoveryTurn() == 0).collect(toList());
    }

    /** @return A (possibly empty) list of units from this bay that can be assault-dropped. */
    public List<Entity> getDroppableUnits() {
        return troops.stream().map(game::getEntity).filter(Objects::nonNull).filter(Entity::canAssaultDrop).collect(toList());
    }

    /** @return A (possibly empty) list of units from this bay that can be unloaded on the ground. */
    public List<Entity> getUnloadableUnits() {
        // TODO: we need to handle aeros and VTOLs differently
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
     * @return The amount of unused space in the bay expressed in slots. For most bays this is the
     *         same as the unused space, but bays for units that can take up a variable amount
     *         of space (such as infantry bays) this calculates the number of the default unit size
     *         that can fit into the remaining space.
     */
    public double getUnusedSlots() {
        return currentSpace;
    }

    /**
     * @return A String that describes the default slot type. Only meaningful for bays with variable
     *         space requirements (like infantry).
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

    public String getType() {
        return "Unknown";
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

    public int getNumberUnloadedThisTurn() {
        return unloadedThisTurn;
    }

    public int getNumberLoadedThisTurn() {
        return unloadedThisTurn;
    }

    /**
     * @return the tonnage of the bay, not the actual mass or weight
     */
    public double getWeight() {
        return totalSpace;
    }

    /**
     * @param clan  Whether the bay is installed in a Clan unit. Needed for infantry bays.
     * @return      The number of additional crew provided by the bay. This includes transport bays only;
     *              crew quarters are already accounted for in the crew total.
     */
    public int getPersonnel(boolean clan) {
        return 0;
    }

    @Override
    public String toString() {
        return "bay:" + totalSpace + ":" + doors + ":"+ bayNumber;
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
     * Some bays (dropshuttle and repair facility) have a maximum number per armor facing.
     * @return The facing of the bay, or Entity.LOC_NONE if the bay does not require a facing.
     */
    public int getFacing() {
        return Entity.LOC_NONE;
    }

    /**
     * Sets the armor facing for the bay, if the bay type requires it. If not required by the bay
     * type, does nothing.
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
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
                .setTechRating(RATING_A)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    /**
     * Shared by several types of bays
     * @return Tech advancement for advanced robotic transport system.
     */
    public static TechAdvancement artsTechAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(2600, 2609, DATE_NONE, 2804, 3068)
                .setApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_WB)
                .setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_E, RATING_E, RATING_E)
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
    public int getTechBase() {
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
    public int getTechRating() {
        return getTechAdvancement().getTechRating();
    }

    @Override
    public int getBaseAvailability(int era) {
        return getTechAdvancement().getBaseAvailability(era);
    }

    @Override
    public int getIntroductionDate(boolean clan, int faction) {
        return getTechAdvancement().getIntroductionDate(clan, faction);
    }

    @Override
    public int getPrototypeDate(boolean clan, int faction) {
        return getTechAdvancement().getPrototypeDate(clan, faction);
    }

    @Override
    public int getProductionDate(boolean clan, int faction) {
        return getTechAdvancement().getProductionDate(clan, faction);
    }

    @Override
    public int getExtinctionDate(boolean clan, int faction) {
        return getTechAdvancement().getExtinctionDate(clan, faction);
    }

    @Override
    public int getReintroductionDate(boolean clan, int faction) {
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