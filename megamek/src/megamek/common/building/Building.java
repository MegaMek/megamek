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
package megamek.common.building;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.Report;
import megamek.common.Terrains;

/**
 * Represents a single, possibly multi-hex building on the board.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour )
 */
public class Building implements Serializable {

    private static final long serialVersionUID = -8236017592012683793L;

    /** @deprecated magic values shall be removed */
    @Deprecated public static final int UNKNOWN = -1;

    /** @deprecated use {@link ConstructionType#LIGHT}    instead */ @Deprecated public static final int LIGHT = 1;
    /** @deprecated use {@link ConstructionType#MEDIUM}   instead */ @Deprecated public static final int MEDIUM = 2;
    /** @deprecated use {@link ConstructionType#HEAVY}    instead */ @Deprecated public static final int HEAVY = 3;
    /** @deprecated use {@link ConstructionType#HARDENED} instead */ @Deprecated public static final int HARDENED = 4;
    /** @deprecated use {@link ConstructionType#WALL}     instead */ @Deprecated public static final int WALL = 5;

    /** @deprecated use {@link BuildingClass#STANDARD}        instead */ @Deprecated public static final int STANDARD = 0;
    /** @deprecated use {@link BuildingClass#HANGAR}          instead */ @Deprecated public static final int HANGAR = 1;
    /** @deprecated use {@link BuildingClass#FORTRESS}        instead */ @Deprecated public static final int FORTRESS = 2;
    /** @deprecated use {@link BuildingClass#GUN_EMPLACEMENT} instead */ @Deprecated public static final int GUN_EMPLACEMENT = 3;

    /**
     * Constructs a new building at the given coordinates, fetching info from the given board 
     */
    public static Building newBuildingAt(Coords coords, IBoard board) {
        IHex curHex = board.getHex(coords);
        BasementType basementType = BasementType.getType(curHex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE));
        return new Building(coords, board, Terrains.BUILDING, basementType);
    }

    /**
     * Constructs a new bridge at the given coordinates, fetching info from the given board 
     */
    public static Building newBridgeAt(Coords coords, IBoard board) {
        return new Building(coords, board, Terrains.BRIDGE, BasementType.NONE);
    }

    /**
     * Constructs a new fuel tank at the given coordinates, fetching info from the given board 
     */
    public static FuelTank newFuelTankAt(Coords coords, IBoard board) {
        IHex curHex = board.getHex(coords);
        int magnitude = curHex.getTerrain(Terrains.FUEL_TANK_MAGN).getLevel();
        return new FuelTank(coords, board, Terrains.FUEL_TANK, magnitude);
    }

    /**
     * Construct a building for the given coordinates from the board's
     * information. If the building covers multiple hexes, every hex will be
     * included in the building.
     *
     * @param coords
     *        the <code>Coords</code> of a hex of the building. If the
     *        building covers multiple hexes, this constructor will include
     *        them all in this building automatically.
     * @param board
     *        the game's <code>Board</code> object.
     *
     * @throws IllegalArgumentException
     *        if the given coordinates do not contain a building, or if the
     *        building covers multiple hexes with different CFs.
     */
    protected Building(Coords coords, IBoard board, int structureType, BasementType basementType) {

        // FIXME This is an unlucky idea, especially considering that id is used
        //       as the only factor to check for equality
        id = coords.hashCode();

        // The building occupies the given coords, at least.
        coordinates.add(coords);
        originalHexes++;

        burning.put(coords, false);

        // Get the Hex for those coords.
        IHex startHex = board.getHex(coords);

        // Read our construction type from the hex.
        if (!startHex.containsTerrain(structureType)) {
            throw new IllegalArgumentException("The coordinates, " //$NON-NLS-1$
                    + coords.getBoardNum() + ", do not contain a building."); //$NON-NLS-1$
        }
        type = startHex.terrainLevel(structureType);
        bldgClass = startHex.terrainLevel(Terrains.BLDG_CLASS);

        // Insure that we've got a good type (and initialize our CF).
        currentCF.put(coords, ConstructionType.ofRequiredId(type).getDefaultCF());

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

    }

    private final int id;
    /** @deprecated this is being refactored out and  the int replaced with a ConstructionType */
    @Deprecated private final int type;
    private final int bldgClass;
    private final String name;
    
    private int collapsedHexes = 0;
    private int originalHexes = 0;
    private List<DemolitionCharge> demolitionCharges = new ArrayList<>();

    private List<Coords> coordinates = new ArrayList<>();
    private final Map<Coords,BasementType> basement = new HashMap<>();
    private Map<Coords, Integer> currentCF = new HashMap<>(); // any damage immediately updates this value
    private Map<Coords, Integer> phaseCF = new HashMap<>(); // cf at start of phase - damage is applied at the end of the phase it was received in
    private Map<Coords, Integer> armor = new HashMap<>();
    private Map<Coords, Boolean> basementCollapsed = new HashMap<>();
    private Map<Coords, Boolean> burning = new HashMap<>();


    // TODO: leaving out Castles Brian until issues with damage scaling are
    // resolved
    // public static final int CASTLE_BRIAN = 3;

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

    /** @deprecated use {@link #iterateCoords()} instead */
    @Deprecated public Enumeration<Coords> getCoords() {
        return Collections.enumeration(coordinates);
    }

    public Iterator<Coords> iterateCoords() {
        return Collections.unmodifiableList(coordinates).iterator();
    }

    public Optional<ConstructionType> getConstructionType() {
        return ConstructionType.ofId(getType());
    } 

    /** @deprecated use {@link #getConstructionType()} instead */
    @Deprecated public int getType() { return type; }

    public Optional<BuildingClass> getBuildingClass() {
        return BuildingClass.ofId(getBldgClass());
    } 

    /** @deprecated use {@link #getBuildingClass()} instead */
    @Deprecated public int getBldgClass() { return bldgClass; }

    /**
     * Get the building basement, per TacOps rules.
     *
     * @return the <code>int</code> code of the buildingbasement type.
     */
    public boolean getBasementCollapsed(Coords coords) {
        return basementCollapsed.get(coords);
    }

    public void collapseBasement(Coords coords, IBoard board, List<Report> vPhaseReport) {
        if ((basement.get(coords) == BasementType.NONE) || (basement.get(coords) == BasementType.ONE_DEEP_NORMALINFONLY)) {
            System.err.println("hex has no basement to collapse"); //$NON-NLS-1$
            return;
        }
        if (basementCollapsed.get(coords)) {
            System.err.println("hex has basement that already collapsed"); //$NON-NLS-1$
            return;
        }
        Report r = new Report(2112, Report.PUBLIC);
        r.add(getName());
        r.add(coords.getBoardNum());
        vPhaseReport.add(r);
        System.err.println("basement " + basement + "is collapsing, hex:" //$NON-NLS-1$ //$NON-NLS-2$
                + coords.toString() + " set terrain!"); //$NON-NLS-1$
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
    public boolean rollBasement(Coords coords, IBoard board, List<Report> vPhaseReport) {
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
            r.add(BasementType.getType(hex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)).getDesc());
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
     *        the <code>Coords> of the hex in question
     * @param cf
     *        the <code>int</code> value of the building hex's current
     *        construction factor. This value must be greater than or equal
     *        to zero.
     * @throws IllegalArgumentException
     *         if the passed value is less than zero, an
     *          <code>IllegalArgumentException</code> is thrown.
     */
    public void setCurrentCF(int cf, Coords coords) {
        if (cf < 0) {
            throw new IllegalArgumentException(
                    "Invalid value for Construction Factor: " + cf); //$NON-NLS-1$
        }

        currentCF.put(coords, cf);
    }

    /**
     * Set the construction factor of the building hex for the start of the next
     * phase. Call this method at the end of the phase to apply damage sustained
     * by the building during the phase.
     *
     * @param coords
     *        the <code>Coords> of the hex in question
     * @param cf
     *        the <code>int</code> value of the building's current
     *        construction factor. This value must be greater than or equal
     *        to zero.
     *
     * @throws IllegalArgumentException
     *         if the passed value is less than zero, an
     *         <code>IllegalArgumentException</code> is thrown.
     */
    public void setPhaseCF(int cf, Coords coords) {
        if (cf < 0) {
            throw new IllegalArgumentException(
                    "Invalid value for Construction Factor: " + cf); //$NON-NLS-1$
        }

        phaseCF.put(coords, cf);
    }

    public void setArmor(int a, Coords coords) {
        if (a < 0) {
            throw new IllegalArgumentException("Invalid value for armor: " + a); //$NON-NLS-1$
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
     *
     * @deprecated use {@link ConstructionType} instead
     */
    @Deprecated public static int getDefaultCF(int type) {
        return ConstructionType.ofId(type).map(ConstructionType::getDefaultCF)
                                          .orElse(Building.UNKNOWN);
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
     * @deprecated use {@link #getConstructionType()} instead
     */
    @Deprecated public double getInfDmgFromInside() {
        return getConstructionType().map(ConstructionType::getDamageReductionFromInside)
                                    .orElse(0f);
    }

    /**
     * Per page 172 of Total Warfare, this is the fraction of a weapon's damage that
     * passes through to infantry inside the building.
     * @return Damage fraction.
     * 
     * @deprecated use {@link #getConstructionType()} instead
     */
    @Deprecated public float getDamageReductionFromOutside() {
        return getConstructionType().map(ConstructionType::getDamageReductionFromOutside)
                                    .orElse(0f);
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
    private void include(Coords coords, IBoard board, int structureType) {

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
                throw new IllegalArgumentException("The coordinates, " //$NON-NLS-1$
                        + coords.getBoardNum()
                        + ", should contain the same type of building as " //$NON-NLS-1$
                        + coordinates.get(0).getBoardNum());
            }
            if (bldgClass != nextHex.terrainLevel(Terrains.BLDG_CLASS)) {
                throw new IllegalArgumentException("The coordinates, " //$NON-NLS-1$
                        + coords.getBoardNum()
                        + ", should contain the same class of building as " //$NON-NLS-1$
                        + coordinates.get(0).getBoardNum());
            }

        }
        // We passed our tests, add the next hex to this building.
        coordinates.add(coords);
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

    // LATER fix equals/hashCode
    //
    // Basing equality on id equality does not make sense on a mutable class.
    // This will need to be addressed, but to do so one must check all places
    // where equality is used (eg: calls to equals() and use in collections).
    //
    // Also note the comment "True until we're talking about more than one
    // Board per Game" below, which seems to imply that building ids are not
    // necessarily unique in multi-board setups.

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((null == obj) || (getClass() != obj.getClass())) {
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

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();

        getConstructionType().ifPresent(ct -> {
            switch (ct) {
            case LIGHT:    buf.append("Light ");    break;
            case MEDIUM:   buf.append("Medium ");   break;
            case HEAVY:    buf.append("Heavy ");    break;
            case HARDENED: buf.append("Hardened "); break;
            case WALL:  // fall-through
            default:    // do nothing
            }
        });

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

}
