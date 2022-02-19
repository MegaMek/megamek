/*
* MegaMek -
* Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
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

import megamek.common.enums.BasementType;
import org.apache.logging.log4j.LogManager;

import java.io.Serializable;
import java.util.*;

/**
 * This class represents a single, possibly multi-hex building on the board.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour)
 */
public class Building implements Serializable {
    private static final long serialVersionUID = -8236017592012683793L;

    /**
     * Generic flag for uninitialized values.
     */
    protected static final int UNKNOWN = -1;
    
    // The Building Types
    public static final int LIGHT = 1;
    public static final int MEDIUM = 2;
    public static final int HEAVY = 3;
    public static final int HARDENED = 4;
    public static final int WALL = 5;
    
    /**
     * The Building Type of the building; equal to the terrain elevation of the BUILDING terrain of a hex.
     */
    private int type = Building.UNKNOWN;
    
    // The Building Classes
    public static final int STANDARD = 0;
    public static final int HANGAR = 1;
    public static final int FORTRESS = 2;
    public static final int GUN_EMPLACEMENT = 3;
    // TODO: leaving out Castles Brian until issues with damage scaling are resolved
    // public static final int CASTLE_BRIAN = 3;
    
    /**
     * The Building Class of the building; equal to the terrain elevation of the BUILDING CLASS terrain of a hex.
     */
    private int bldgClass = Building.STANDARD;
    
    /**
     * The ID of this building.
     */
    private int id = Building.UNKNOWN;

    /**
     * The coordinates of every hex of this building.
     */
    private Vector<Coords> coordinates = new Vector<>();

    /**
     * The Basement type of the building.
     */
    private Map<Coords, BasementType> basement = new HashMap<>();

    private int collapsedHexes = 0;

    private int originalHexes = 0;

    /**
     * The current construction factor of the building hexes. Any damage
     * immediately updates this value.
     */
    private Map<Coords, Integer> currentCF = new HashMap<>();
    
    /**
     * The construction factor of the building hexes at the start of this attack
     * phase. Damage that is received during the phase is applied at the end of
     * the phase.
     */
    private Map<Coords, Integer> phaseCF = new HashMap<>();
    
    /**
     * The current armor of the building hexes.
     */
    private Map<Coords, Integer> armor = new HashMap<>();

    /**
     * The current state of the basement.
     */
    private Map<Coords, Boolean> basementCollapsed = new HashMap<>();

    /**
     * The name of the building.
     */
    private String name = null;

    /**
     * Flag that indicates whether this building is burning
     */
    private Map<Coords, Boolean> burning = new HashMap<>();

    public static class DemolitionCharge implements Serializable {
        private static final long serialVersionUID = -6655782801564155668L;
        public int damage;
        public int playerId;
        public Coords pos;
        /**
         * A UUID to keep track of the identify of this demolition charge.
         * Since we could have multiple charges in the same building hex, we
         * can't track identity based upon owner and damage.  Additionally,
         * since we pass objects across the network, we need a mechanism to
         * track identify other than memory address.
         */
        public UUID uuid = UUID.randomUUID();

        public DemolitionCharge(int playerId, int damage, Coords p) {
            this.damage = damage;
            this.playerId = playerId;
            this.pos = p;
        }

        @Override
        public int hashCode() {
            return uuid.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DemolitionCharge) {
                return uuid.equals(((DemolitionCharge) o).uuid);
            }
            return false;
        }
    }

    private List<DemolitionCharge> demolitionCharges = new ArrayList<>();

    /**
     * Update this building to include the new hex (and all hexes off the new
     * hex, which aren't already included).
     *
     * @param coords tthe <code>Coords</code> of the new hex.
     * @param board the game's <code>Board</code> object.
     * @exception IllegalArgumentException will be thrown if the given coordinates do not contain a
     * building, or if the building covers multiple hexes with different CF.
     */
    protected void include(Coords coords, Board board, int structureType) {

        // If the hex is already in the building, we've covered it before.
        if (isIn(coords)) {
            return;
        }

        // Get the nextHex hex.
        Hex nextHex = board.getHex(coords);
        if ((null == nextHex) || !(nextHex.containsTerrain(structureType))) {
            return;
        }

        if (structureType == Terrains.BUILDING) {
            // Error if the Building Type (Light, Medium...) or Building Class (Standard, Hangar...) is off.
            if (type != nextHex.terrainLevel(Terrains.BUILDING)) {
                throw new IllegalArgumentException("The coordinates, "
                        + coords.getBoardNum()
                        + ", should contain the same type of building as "
                        + coordinates.elementAt(0).getBoardNum());
            }
            if (bldgClass != nextHex.terrainLevel(Terrains.BLDG_CLASS)) {
                throw new IllegalArgumentException("The coordinates, "
                        + coords.getBoardNum()
                        + ", should contain the same class of building as "
                        + coordinates.elementAt(0).getBoardNum());
            }

        }
        // We passed our tests, add the next hex to this building.
        coordinates.addElement(coords);
        originalHexes++;
        currentCF.put(coords, nextHex.terrainLevel(Terrains.BLDG_CF));
        phaseCF.put(coords, nextHex.terrainLevel(Terrains.BLDG_CF));
        basement.put(coords, BasementType.getType(nextHex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)));
        basementCollapsed.put(coords, nextHex.terrainLevel(Terrains.BLDG_BASE_COLLAPSED) == 1);
        if (structureType == Terrains.BRIDGE) {
            currentCF.put(coords, nextHex.terrainLevel(Terrains.BRIDGE_CF));
            phaseCF.put(coords, nextHex.terrainLevel(Terrains.BRIDGE_CF));
        }
        if (structureType == Terrains.FUEL_TANK) {
            currentCF.put(coords, nextHex.terrainLevel(Terrains.FUEL_TANK_CF));
            phaseCF.put(coords, nextHex.terrainLevel(Terrains.FUEL_TANK_CF));
        }
        if (nextHex.containsTerrain(Terrains.BLDG_ARMOR)) {
            armor.put(coords, nextHex.terrainLevel(Terrains.BLDG_ARMOR));
        } else {
            armor.put(coords, 0);
        }

        burning.put(coords, false);

        // Walk through the exit directions and
        // identify all hexes in this building.
        for (int dir = 0; dir < 6; dir++) {

            // Does the building exit in this direction?
            if (nextHex.containsTerrainExit(structureType, dir)) {
                include(coords.translated(dir), board, structureType);
            }

        }

    }

    /**
     * Construct a building for the given coordinates from the board's
     * information. If the building covers multiple hexes, every hex will be
     * included in the building.
     *
     * @param coords the <code>Coords</code> of a hex of the building. If the building covers
     *               multiple hexes, this constructor will include them all in this building
     *               automatically.
     * @param board the game's <code>Board</code> object.
     * @exception IllegalArgumentException will be thrown if the given coordinates do not contain a
     * building, or if the building covers multiple hexes with different CFs.
     */
    public Building(Coords coords, Board board, int structureType, BasementType basementType) {

        // The ID of the building will be deterministic based on the
        // position of its first hex. 9,999 hexes in the Y direction
        // ought to be enough for anyone.
        //
        // ASSUMPTION: this will be unique ID across ALL the building's
        // hexes for ALL the clients of this board.
        id = coords.getX() * 10000 + coords.getY();

        // The building occupies the given coords, at least.
        coordinates.addElement(coords);
        originalHexes++;

        burning.put(coords, false);

        // Get the Hex for those coords.
        Hex startHex = board.getHex(coords);

        // Read our construction type from the hex.
        if (!startHex.containsTerrain(structureType)) {
            throw new IllegalArgumentException("The coordinates, "
                    + coords.getBoardNum() + ", do not contain a building.");
        }
        type = startHex.terrainLevel(structureType);
        bldgClass = startHex.terrainLevel(Terrains.BLDG_CLASS);

        // Insure that we've got a good type (and initialize our CF).
        currentCF.put(coords, getDefaultCF(type));
        if (currentCF.get(coords) == Building.UNKNOWN) {
            throw new IllegalArgumentException("Unknown construction type: "
                    + type + ".  The board is invalid.");
        }

        // Now read the *real* CF, if the board specifies one.
        if ((structureType == Terrains.BUILDING)
                && startHex.containsTerrain(Terrains.BLDG_CF)) {
            currentCF.put(coords, startHex.terrainLevel(Terrains.BLDG_CF));
        }
        if ((structureType == Terrains.BRIDGE)
                && startHex.containsTerrain(Terrains.BRIDGE_CF)) {
            currentCF.put(coords, startHex.terrainLevel(Terrains.BRIDGE_CF));
        }
        if ((structureType == Terrains.FUEL_TANK)
                && startHex.containsTerrain(Terrains.FUEL_TANK_CF)) {
            currentCF.put(coords, startHex.terrainLevel(Terrains.FUEL_TANK_CF));
        }
        if (startHex.containsTerrain(Terrains.BLDG_ARMOR)) {
            armor.put(coords, startHex.terrainLevel(Terrains.BLDG_ARMOR));
        } else {
            armor.put(coords, 0);
        }

        phaseCF.putAll(currentCF);

        basement.put(coords, basementType);
        basementCollapsed.put(coords, startHex.terrainLevel(Terrains.BLDG_BASE_COLLAPSED) == 1);

        // Walk through the exit directions and
        // identify all hexes in this building.
        for (int dir = 0; dir < 6; dir++) {

            // Does the building exit in this direction?
            if (startHex.containsTerrainExit(structureType, dir)) {
                include(coords.translated(dir), board, structureType);
            }

        }

        // Set the building's name.
        StringBuilder sb = new StringBuilder();
        if (structureType == Terrains.FUEL_TANK) {
            sb.append("Fuel Tank #");
        } else if (getType() == Building.WALL) {
            sb.append("Wall #");
        } else if (structureType == Terrains.BUILDING) {
            sb.append("Building #");
        } else if (structureType == Terrains.BRIDGE) {
            sb.append("Bridge #");
        } else {
            sb.append("Structure #");
        }
        sb.append(id);
        name = sb.toString();
    }

    /**
     * Get the ID of this building. The same ID applies to all hexes.
     *
     * @return the <code>int</code> ID of the building.
     */
    public int getId() {
        return id;
    }

    /**
     * Determine if the building occupies given coordinates. Multi-hex buildings
     * will occupy multiple coordinates. Only one building per hex.
     *
     * @param coords
     *            - the <code>Coords</code> being examined.
     * @return <code>true</code> if the building occupies the coordinates.
     *         <code>false</code> otherwise.
     */
    public boolean isIn(Coords coords) {
        return coordinates.contains(coords);
    }

    /**
     * Determins if the coord exist in the currentCF has.
     *
     * @param coords
     *            - the <code>Coords</code> being examined.
     * @return <code>true</code> if the building has CF at the coordinates.
     *         <code>false</code> otherwise.
     */
    public boolean hasCFIn(Coords coords) {
        return currentCF.containsKey(coords);

    }

    /**
     * Get the coordinates that the building occupies.
     *
     * @return an <code>Enumeration</code> of the <code>Coord</code> objects.
     */
    public Enumeration<Coords> getCoords() {
        return coordinates.elements();
    }
    
    /** Returns a list of this Building's coords. The list is unmodifiable. */
    public List<Coords> getCoordsList() {
        return Collections.unmodifiableList(coordinates);
    }

    /**
     * Get the construction type of the building. This value will be one of the
     * constants, LIGHT, MEDIUM, HEAVY, or HARDENED.
     *
     * @return the <code>int</code> code of the building's construction type.
     */
    public int getType() {
        return type;
    }

    /**
     * Get the building class, per TacOps rules.
     *
     * @return the <code>int</code> code of the building's classification.
     */
    public int getBldgClass() {
        return bldgClass;
    }

    /**
     * Get the building basement, per TacOps rules.
     *
     * @return the <code>int</code> code of the buildingbasement type.
     */
    public boolean getBasementCollapsed(Coords coords) {
        return basementCollapsed.get(coords);
    }

    public void collapseBasement(Coords coords, Board board, Vector<Report> vPhaseReport) {
        if (basement.get(coords).isNone() || basement.get(coords).isOneDeepNormalInfantryOnly()) {
            LogManager.getLogger().error("Hex has no basement to collapse");
            return;
        } else if (basementCollapsed.get(coords)) {
            LogManager.getLogger().error("Hex has basement that already collapsed");
            return;
        }
        Report r = new Report(2112, Report.PUBLIC);
        r.add(getName());
        r.add(coords.getBoardNum());
        vPhaseReport.add(r);
        LogManager.getLogger().error("basement " + basement + "is collapsing, hex:" + coords + " set terrain!");
        board.getHex(coords).addTerrain(new Terrain(Terrains.BLDG_BASE_COLLAPSED, 1));
        basementCollapsed.put(coords, true);

    }

    /**
     * Roll what kind of basement this building has
     * @param coords the <code>Coords</code> of the building to roll for
     * @param vPhaseReport the <code>Vector<Report></code> containing the phasereport
     * @return a <code>boolean</code> indicating weather the hex and building was changed or not
     */
    public boolean rollBasement(Coords coords, Board board, Vector<Report> vPhaseReport) {
        if (basement.get(coords).isUnknown()) {
            Hex hex = board.getHex(coords);
            Report r = new Report(2111, Report.PUBLIC);
            r.add(getName());
            r.add(coords.getBoardNum());
            int basementRoll = Compute.d6(2);
            r.add(basementRoll);
            if (basementRoll == 2) {
                basement.put(coords, BasementType.TWO_DEEP_FEET);
                hex.addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).ordinal()));
            } else if (basementRoll == 3) {
                basement.put(coords, BasementType.ONE_DEEP_FEET);
                hex.addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).ordinal()));
            } else if (basementRoll == 4) {
                basement.put(coords, BasementType.ONE_DEEP_NORMAL);
                hex.addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).ordinal()));
            } else if (basementRoll == 10) {
                basement.put(coords, BasementType.ONE_DEEP_NORMAL);
                hex.addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).ordinal()));
            } else if (basementRoll == 11) {
                basement.put(coords, BasementType.ONE_DEEP_HEAD);
                hex.addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).ordinal()));
            } else if (basementRoll == 12) {
                basement.put(coords, BasementType.TWO_DEEP_HEAD);
                hex.addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).ordinal()));
            } else {
                basement.put(coords, BasementType.NONE);
                hex.addTerrain(new Terrain(Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).ordinal()));
            }

            r.add(BasementType.getType(hex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)).toString());
            vPhaseReport.add(r);
            return true;
        }

        return false;
    }

    /**
     * Get the current construction factor of the building hex at the passed
     * coords. Any damage immediately updates this value.
     *
     * @param coords
     *            - the <code>Coords> of the hex in question
     *
     * @return the <code>int</code> value of the building hex's current
     *         construction factor. This value will be greater than or equal to
     *         zero.
     */
    public int getCurrentCF(Coords coords) {
        return currentCF.get(coords);
    }

    /**
     * Get the construction factor of the building hex at the passed coords at
     * the start of the current phase. Damage that is received during the phase
     * is applied at the end of the phase.
     *
     * @param coords
     *            - the <code>Coords> of the hex in question
     * @return the <code>int</code> value of the building's construction factor
     *         at the start of this phase. This value will be greater than or
     *         equal to zero.
     */
    public int getPhaseCF(Coords coords) {
        return phaseCF.get(coords);
    }

    public int getArmor(Coords coords) {
        return armor.get(coords);
    }

    /**
     * Set the current construction factor of the building hex. Call this method
     * immediately when the building sustains any damage.
     *
     * @param coords the <code>Coords> of the hex in question
     * @param cf the <code>int</code> value of the building hex's current construction factor. This
     *           value must be greater than or equal to zero.
     * @exception IllegalArgumentException if the passed value is less than zero
     */
    public void setCurrentCF(int cf, Coords coords) {
        if (cf < 0) {
            throw new IllegalArgumentException("Invalid value for Construction Factor: " + cf);
        }

        currentCF.put(coords, cf);
    }

    /**
     * Set the construction factor of the building hex for the start of the next
     * phase. Call this method at the end of the phase to apply damage sustained
     * by the building during the phase.
     *
     * @param coords the <code>Coords> of the hex in question
     * @param cf the <code>int</code> value of the building hex's current construction factor. This
     *           value must be greater than or equal to zero.
     * @exception IllegalArgumentException if the passed value is less than zero
     */
    public void setPhaseCF(int cf, Coords coords) {
        if (cf < 0) {
            throw new IllegalArgumentException(
                    "Invalid value for Construction Factor: " + cf);
        }

        phaseCF.put(coords, cf);
    }

    public void setArmor(int a, Coords coords) {
        if (a < 0) {
            throw new IllegalArgumentException("Invalid value for armor: " + a);
        }

        armor.put(coords, a);
    }

    /**
     * Get the name of this building.
     *
     * @return the <code>String</code> name of this building.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the default construction factor for the given type of building.
     *
     * @param type
     *            - the <code>int</code> construction type of the building.
     * @return the <code>int</code> default construction factor for that type of
     *         building. If a bad type value is passed, the constant
     *         <code>Building.UNKNOWN</code> will be returned instead.
     */
    public static int getDefaultCF(int type) {
        int retval = Building.UNKNOWN;
        switch (type) {
            case Building.LIGHT:
                retval = 15;
                break;
            case Building.MEDIUM:
                retval = 40;
                break;
            case Building.HEAVY:
                retval = 90;
                break;
            case Building.HARDENED:
            case Building.WALL:
                retval = 120;
                break;
        }
        return retval;
    }

    /**
     * Override <code>Object#equals(Object)</code>.
     *
     * @param obj
     *            - the other <code>Object</code> to compare to this
     *            <code>Building</code>.
     * @return <code>true</code> if the other object is the same as this
     *         <code>Building</code>. The value <code>false</code> will be
     *         returned if the other object is <code>null</code>, is not a
     *         <code>Building</code>, or if it is not the same as this
     *         <code>Building</code>.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Building)) {
            return false;
        }
        // True until we're talking about more than one Board per Game.
        final Building other = (Building) obj;
        return (id == other.id);
    }

    @Override
    public int hashCode() {
        return id;
    }
    
    /** Returns a string representation of the given building type, e.g. "Hardened". */
    public static String typeName(int type) {
        switch (type) {
            case Building.LIGHT:
                return "Light";
            case Building.MEDIUM:
                return "Medium";
            case Building.HEAVY:
                return "Heavy";
            case Building.HARDENED:
                return "Hardened";
            default:
                return "Unknown";
        }
    }
    
    /** Returns a string representation of the given building class, e.g. "Hangar". */
    public static String className(int bldgClass) {
        switch (bldgClass) {
            case Building.HANGAR:
                return "Hangar";
            case Building.FORTRESS:
                return "Fortress";
            case Building.GUN_EMPLACEMENT:
                return "Gun Emplacement";
            default:
                return "Building";
        }
    }

    @Override
    public String toString() {
        return typeName(getType()) + " " + className(getBldgClass()) + " " + name;
    }

    /**
     * Determine if this building is on fire.
     *
     * @return <code>true</code> if the building is on fire.
     */
    public boolean isBurning(Coords coords) {
        return burning.get(coords);
    }

    /**
     * Set the flag that indicates that this building is on fire.
     *
     * @param onFire
     *            - a <code>boolean</code> value that indicates whether this
     *            building is on fire.
     */
    public void setBurning(boolean onFire, Coords coords) {
        burning.put(coords, onFire);
    }

    public void addDemolitionCharge(int playerId, int damage, Coords pos) {
        DemolitionCharge charge = new DemolitionCharge(playerId, damage, pos);
        demolitionCharges.add(charge);
    }

    public void removeDemolitionCharge(DemolitionCharge charge) {
        demolitionCharges.remove(charge);
    }

    public List<DemolitionCharge> getDemolitionCharges() {
        return demolitionCharges;
    }

    public void setDemolitionCharges(List<DemolitionCharge> charges) {
        demolitionCharges = charges;
    }

    /**
     * Remove one building hex from the building
     *
     * @param coords
     *            - the <code>Coords</code> of the hex to be removed
     */
    public void removeHex(Coords coords) {
        coordinates.remove(coords);
        currentCF.remove(coords);
        phaseCF.remove(coords);
        collapsedHexes++;
    }

    public int getOriginalHexCount() {
        return originalHexes;
    }

    public int getCollapsedHexCount() {
        return collapsedHexes;
    }

    /**
     *
     * @return the damage scale multiplier for units passing through this
     *         building
     */
    public double getDamageFromScale() {
        switch (getBldgClass()) {
            case Building.HANGAR:
                return 0.5;
            case Building.FORTRESS:
            case Building.GUN_EMPLACEMENT:
                return 2.0;
                // case Building.CASTLE_BRIAN:
                // return 10.0;
            default:
                return 1.0;
        }
    }

    /**
     *
     * @return the damage scale multiplier for damage applied to this building
     *         (and occupants)
     */
    public double getDamageToScale() {
        switch (getBldgClass()) {
            case Building.FORTRESS:
            case Building.GUN_EMPLACEMENT:
                return 0.5;
                // case Building.CASTLE_BRIAN:
                // return 0.1;
            default:
                return 1.0;
        }
    }

    /**
     *
     * @return the amount of damage the building absorbs
     */
    public int getAbsorbtion(Coords pos) {
        // if (getBldgClass() == Building.CASTLE_BRIAN) {
        // return (int) Math.ceil(getPhaseCF(pos));
        // }
        return (int) Math.ceil(getPhaseCF(pos) / 10.0);
    }

    /**
     * Returns the percentage of damage done to the building for attacks against
     * infantry in the building from other units within the building.  TW pg175.
     *
     * @return
     */
    public double getInfDmgFromInside() {
         switch (getType()) {
            case Building.LIGHT:
            case Building.MEDIUM:
                return 0.0;
            case Building.HEAVY:
                return 0.5;
            case Building.HARDENED:
                return 0.75;
            default:
                return 0;
        }
    }

    /**
     * Per page 172 of Total Warfare, this is the fraction of a weapon's damage that
     * passes through to infantry inside the building.
     * @return Damage fraction.
     */
    public float getDamageReductionFromOutside() {
        switch (getType()) {
            case Building.LIGHT:
                return 0.75f;
            case Building.MEDIUM:
                return 0.5f;
            case Building.HEAVY:
                return 0.25f;
            default:
                return 0f;
        }
    }

    public BasementType getBasement(Coords coords) {
        return basement.get(coords);
    }

    public void setBasement(Coords coords, BasementType basement) {
        this.basement.put(coords, basement);
    }

    public void setBasementCollapsed(Coords coords, boolean collapsed) {
        basementCollapsed.put(coords, collapsed);
    }
}
