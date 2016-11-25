/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

/**
 * This class represents a single, possibly multi-hex building on the board.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour )
 * @version $Revision$
 */
public class Building implements Serializable {

    // Private attributes and helper functions.

    /**
     *
     */
    private static final long serialVersionUID = -8236017592012683793L;

    /**
     * The ID of this building.
     */
    private int id = Building.UNKNOWN;

    /**
     * The coordinates of every hex of this building.
     */
    private Vector<Coords> coordinates = new Vector<Coords>();

    /**
     * The construction type of the building.
     */
    private int type = Building.UNKNOWN;

    /**
     * The Basement type of the building.
     */
    private Map<Coords,BasementType> basement = new HashMap<Coords,BasementType>();
    /**
     * the class of the building
     */
    private int bldgClass = Building.STANDARD;

    private int collapsedHexes = 0;

    private int originalHexes = 0;

    /**
     * The current construction factor of the building hexes. Any damage
     * immediately updates this value.
     */
    private Map<Coords, Integer> currentCF = new HashMap<Coords, Integer>();
    /**
     * The construction factor of the building hexes at the start of this attack
     * phase. Damage that is received during the phase is applied at the end of
     * the phase.
     */
    private Map<Coords, Integer> phaseCF = new HashMap<Coords, Integer>();
    /**
     * The current armor of the building hexes.
     */
    private Map<Coords, Integer> armor = new HashMap<Coords, Integer>();

    /**
     * The current state of the basement.
     */
    private Map<Coords, Boolean> basementCollapsed = new HashMap<Coords, Boolean>();

    /**
     * The name of the building.
     */
    private String name = null;

    /**
     * Flag that indicates whether this building is burning
     */
    private Map<Coords, Boolean> burning = new HashMap<Coords, Boolean>();

    public class DemolitionCharge implements Serializable {
        /**
         *
         */
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
                return uuid.equals(((DemolitionCharge)o).uuid);
            }
            return false;
        }
     }

    private List<DemolitionCharge> demolitionCharges = new ArrayList<>();

    // Public and Protected constants, constructors, and methods.

    /**
     * Update this building to include the new hex (and all hexes off the new
     * hex, which aren't already included).
     *
     * @param coords
     *            - the <code>Coords</code> of the new hex.
     * @param board
     *            - the game's <code>IBoard</code> object.
     * @exception an
     *                <code>IllegalArgumentException</code> will be thrown if
     *                the given coordinates do not contain a building, or if the
     *                building covers multiple hexes with different CF.
     */
    protected void include(Coords coords, IBoard board, int structureType) {

        // If the hex is already in the building, we've covered it before.
        if (isIn(coords)) {
            return;
        }

        // Get the nextHex hex.
        IHex nextHex = board.getHex(coords);
        if ((null == nextHex) || !(nextHex.containsTerrain(structureType))) {
            return;
        }

        if (structureType == Terrains.BUILDING) {
            // Error off if the building type, or CF is off.
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

    } // End void protected include( Coords, Board )

    /**
     * Generic flag for uninitialized values.
     */
    protected static final int UNKNOWN = -1;

    /**
     * Basement handlers
     */
    public enum BasementType {
        UNKNOWN(0,0, Messages.getString("Building.BasementUnknown")),
        NONE(1,0, Messages.getString("Building.BasementNone")),
        TWO_DEEP_FEET(2,2,Messages.getString("Building.BasementTwoDeepFeet")),
        ONE_DEEP_FEET(3,1,Messages.getString("Building.BasementOneDeepFeet")),
        ONE_DEEP_NORMAL(4,1,Messages.getString("Building.BasementOneDeepNormal")),
        ONE_DEEP_NORMALINFONLY(5,1,Messages.getString("Building.BasementOneDeepNormalInfOnly")),
        ONE_DEEP_HEAD(6,1,Messages.getString("Building.BasementOneDeepHead")),
        TWO_DEEP_HEAD(7,2,Messages.getString("Building.BasementTwoDeepHead"));

        private int value;
        private int depth;
        private String desc;

        BasementType(int type, int depth, String desc) {
            value = type;
            this.depth = depth;
            this.desc = desc;
        }

        public int getValue() {
            return value;
        }

        public int getDepth() {
            return depth;
        }

        public String getDesc() {
            return desc;
        }

        public static BasementType getType(int value) {
            for (BasementType type : BasementType.values()) {
                if (type.getValue() == value) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * Various construction types.
     */
    public static final int LIGHT = 1;
    public static final int MEDIUM = 2;
    public static final int HEAVY = 3;
    public static final int HARDENED = 4;
    public static final int WALL = 5;

    /**
     * Various building types
     */
    public static final int STANDARD = 0;
    public static final int HANGAR = 1;
    public static final int FORTRESS = 2;
    public static final int GUN_EMPLACEMENT = 3;

    // TODO: leaving out Castles Brian until issues with damage scaling are
    // resolved
    // public static final int CASTLE_BRIAN = 3;

    /**
     * Construct a building for the given coordinates from the board's
     * information. If the building covers multiple hexes, every hex will be
     * included in the building.
     *
     * @param coords
     *            - the <code>Coords</code> of a hex of the building. If the
     *            building covers multiple hexes, this constructor will include
     *            them all in this building automatically.
     * @param board
     *            - the game's <code>Board</code> object.
     * @exception an
     *                <code>IllegalArgumentException</code> will be thrown if
     *                the given coordinates do not contain a building, or if the
     *                building covers multiple hexes with different CFs.
     */
    public Building(Coords coords, IBoard board, int structureType, BasementType basementType) {

        // The ID of the building will be the hashcode of the coords.
        // ASSUMPTION: this will be unique ID across ALL the building's
        // hexes for ALL the clients of this board.
        id = coords.hashCode();

        // The building occupies the given coords, at least.
        coordinates.addElement(coords);
        originalHexes++;

        burning.put(coords, false);

        // Get the Hex for those coords.
        IHex startHex = board.getHex(coords);

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
        StringBuffer buffer = new StringBuffer();
        if (structureType == Terrains.FUEL_TANK) {
            buffer.append("Fuel Tank #");
        } else if (getType() == Building.WALL) {
            buffer.append("Wall #");
        } else if (structureType == Terrains.BUILDING) {
            buffer.append("Building #");
        } else if (structureType == Terrains.BRIDGE) {
            buffer.append("Bridge #");
        } else {
            buffer.append("Structure #");
        }
        buffer.append(id);
        name = buffer.toString();

    } // End public Building( Coords, Board )

    /**
     * Creates a new building of the specified type, name, ID, and coordinates.
     * Do *not* use this method unless you have carefully examined this class.
     * The construction factors for the building will be based on the type.
     *
     * @param type
     *            The <code>int</code> type of the building.
     * @param id
     *            The <code>int</code> ID of this building.
     * @param name
     *            The <code>String</code> name of this building.
     * @param coords
     *            The <code>Vector</code> of <code>Coords<code>
     *                  for this building.  This object is used directly
     *                  without being copied.
     * @exception an
     *                <code>IllegalArgumentException</code> will be thrown if
     *                the given coordinates do not contain a building, or if the
     *                building covers multiple hexes with different CFs.
     */
    public Building(int bldgClass, int type, int id, String name,
            Vector<Coords> coords) {
        this.bldgClass = bldgClass;
        this.type = type;
        this.id = id;
        this.name = name;
        coordinates = coords;
        // Insure that we've got a good type (and initialize our CF).
        for (Coords coord : coordinates) {
            currentCF.put(coord, getDefaultCF(this.type));
            phaseCF.putAll(currentCF);
            armor.put(coord, 0);
            if (getDefaultCF(this.type) == Building.UNKNOWN) {
                throw new IllegalArgumentException(
                        "Invalid construction type: " + this.type + ".");
            }
            basement.put(coord, BasementType.UNKNOWN);
            basementCollapsed.put(coord, false);
        }
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

    public void collapseBasement(Coords coords, IBoard board,
            Vector<Report> vPhaseReport) {
        if ((basement.get(coords) == BasementType.NONE) || (basement.get(coords) == BasementType.ONE_DEEP_NORMALINFONLY)) {
            System.err.println("hex has no basement to collapse");
            return;
        }
        if (basementCollapsed.get(coords)) {
            System.err.println("hex has basement that already collapsed");
            return;
        }
        Report r = new Report(2112, Report.PUBLIC);
        r.add(getName());
        r.add(coords.getBoardNum());
        vPhaseReport.add(r);
        System.err.println("basement " + basement + "is collapsing, hex:"
                + coords.toString() + " set terrain!");
        board.getHex(coords).addTerrain(Terrains.getTerrainFactory().createTerrain(
                Terrains.BLDG_BASE_COLLAPSED, 1));
        basementCollapsed.put(coords, true);

    }

    /**
     * Roll what kind of basement this building has
     * @param coords the <code>Coords</code> of theb building to roll for
     * @param vPhaseReport the <code>Vector<Report></code> containing the phasereport
     * @return a <code>boolean</code> indicating wether the hex and building was changed or not
     */
    public boolean rollBasement(Coords coords, IBoard board, Vector<Report> vPhaseReport) {
        if (basement.get(coords) == BasementType.UNKNOWN) {
            IHex hex = board.getHex(coords);
            Report r = new Report(2111, Report.PUBLIC);
            r.add(getName());
            r.add(coords.getBoardNum());
            int basementRoll = Compute.d6(2);
            r.add(basementRoll);
            if (basementRoll == 2) {
                basement.put(coords, BasementType.TWO_DEEP_FEET);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(
                        Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).getValue()));
            } else if (basementRoll == 3) {
                basement.put(coords, BasementType.ONE_DEEP_FEET);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(
                        Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).getValue()));
            } else if (basementRoll == 4) {
                basement.put(coords, BasementType.ONE_DEEP_NORMAL);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(
                        Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).getValue()));
            } else if (basementRoll == 10) {
                basement.put(coords, BasementType.ONE_DEEP_NORMAL);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(
                        Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).getValue()));
            } else if (basementRoll == 11) {
                basement.put(coords, BasementType.ONE_DEEP_HEAD);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(
                        Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).getValue()));
            } else if (basementRoll == 12) {
                basement.put(coords, BasementType.TWO_DEEP_HEAD);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(
                        Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).getValue()));
            } else {
                basement.put(coords, BasementType.NONE);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(
                        Terrains.BLDG_BASEMENT_TYPE, basement.get(coords).getValue()));
            }
            r.add(BasementType.getType(hex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)).desc);
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
     * @param coords
     *            - the <code>Coords> of the hex in question
     * @param cf
     *            - the <code>int</code> value of the building hex's current
     *            construction factor. This value must be greater than or equal
     *            to zero.
     * @exception If
     *                the passed value is less than zero, an
     *                <code>IllegalArgumentException</code> is thrown.
     */
    public void setCurrentCF(int cf, Coords coords) {
        if (cf < 0) {
            throw new IllegalArgumentException(
                    "Invalid value for Construction Factor: " + cf);
        }

        currentCF.put(coords, cf);
    }

    /**
     * Set the construction factor of the building hex for the start of the next
     * phase. Call this method at the end of the phase to apply damage sustained
     * by the building during the phase.
     *
     * @param coords
     *            - the <code>Coords> of the hex in question
     * @param cf
     *            - the <code>int</code> value of the building's current
     *            construction factor. This value must be greater than or equal
     *            to zero.
     * @exception If
     *                the passed value is less than zero, an
     *                <code>IllegalArgumentException</code> is thrown.
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
     * @param other
     *            - the other <code>Object</code> to compare to this
     *            <code>Building</code>.
     * @return <code>true</code> if the other object is the same as this
     *         <code>Building</code>. The value <code>false</code> will be
     *         returned if the other object is <code>null</code>, is not a
     *         <code>Buildig</code>, or if it is not the same as this
     *         <code>Building</code>.
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((null == obj) || (getClass() != obj.getClass())) {
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

    /**
     * Get a String for this building.
     */
    @Override
    public String toString() {

        // Assemble the string in pieces.
        StringBuffer buf = new StringBuffer();

        // Add the building type to the buffer.
        switch (getType()) {
            case Building.LIGHT:
                buf.append("Light ");
                break;
            case Building.MEDIUM:
                buf.append("Medium ");
                break;
            case Building.HEAVY:
                buf.append("Heavy ");
                break;
            case Building.HARDENED:
                buf.append("Hardened ");
                break;
            case Building.WALL:
                buf.append("");
                break;
        }

        switch (getBldgClass()) {
            case Building.HANGAR:
                buf.append("Hangar ");
                break;
            case Building.FORTRESS:
                buf.append("Fortress ");
                break;
            case Building.GUN_EMPLACEMENT:
                buf.append("Gun Emplacement");
                break;
            // case Building.CASTLE_BRIAN:
            // buf.append("Castle Brian ");
            // break;
            default:
                buf.append("Standard ");
        }

        // Add the building's name.
        buf.append(name);

        // Return the string.
        return buf.toString();
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
        // if(getBldgClass() == Building.CASTLE_BRIAN) {
        // return (int) Math.ceil(getPhaseCF(pos));
        // }
        return (int) Math.ceil(getPhaseCF(pos) / 10.0);
    }

    /**
     * Returns the percentage of damage done to the building for attacks against
     * infantry in the building from other units within the building.  TW pg175.
     *
     * @param pos
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

    public BasementType getBasement(Coords coords) {
        return basement.get(coords);
    }

    public void setBasement(Coords coords, BasementType basement) {
        this.basement.put(coords, basement);
    }

    public void setBasementCollapsed(Coords coords, boolean collapsed) {
        basementCollapsed.put(coords, collapsed);
    }



} // End public class Building implements Serializable
