/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import megamek.common.annotations.Nullable;
import megamek.common.enums.BasementType;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hex represents a single hex on the board.
 * @author Ben
 */
public class Hex implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 82483704768044698L;
    private Coords coords;
    private int level;
    private Map<Integer, Terrain> terrains = new HashMap<>(1);
    private String theme;
    private String originalTheme;
    private int fireTurn;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Constructs a clear, plain hex at level 0.
     */
    public Hex() {
        this(0);
    }

    /**
     * Constructs a clean, plain hex at specified level.
     */
    public Hex(int level) {
        this(level, new Terrain[Terrains.SIZE], null, new Coords(0, 0));
    }

    public Hex(int level, Terrain[] terrains, String theme) {
        this(level, terrains, theme, new Coords(0, 0));
    }

    /**
     * Constructs a Hex with all parameters.
     */
    public Hex(int level, Terrain[] terrains, String theme, Coords c) {
        this.level = level;
        coords = c;
        for (final Terrain t : terrains) {
            if (t != null) {
                this.terrains.put(t.getType(), t);
            }
        }

        if ((theme == null) || !theme.isBlank()) {
            this.theme = theme;
        } else {
            this.theme = null;
        }
        originalTheme = this.theme;
    }

    public Hex(int level, String terrain, String theme) {
        this(level, terrain, theme, new Coords(0, 0));
    }

    /**
     * Constructs a Hex from a combined string terrains format
     */
    public Hex(int level, String terrain, String theme, Coords c) {
        this(level, new Terrain[Terrains.SIZE], theme, c);
        for (StringTokenizer st = new StringTokenizer(terrain, ";", false); st.hasMoreTokens();) {
            addTerrain(new Terrain(st.nextToken()));
        }
    }
    //endregion Constructors

    //region Getters/Setters
    /**
     * @return The level of a hex, as defined in TW. This refers to the height of the ground terrain.
     */
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * The theme is intended as a tag for the tileset file to indicate a special graphic for the hex
     * @return the theme name
     */
    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
    //endregion Getters/Setters

    /**
     * @return An array that contains an id for each terrain present in this hex.
     */
    public int[] getTerrainTypes() {
        return terrains.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Resets the theme to what was specified in the board file.
     */
    public void resetTheme() {
        setTheme(originalTheme);
    }

    /**
     * Clears the "exits" flag for all terrains in the hex where it is not manually specified.
     */
    public void clearExits() {
        for (Integer i : terrains.keySet()) {
            Terrain t = terrains.get(i);
            if ((t != null) && !t.hasExitsSpecified()) {
                t.setExits(0);
            }
        }
    }

    /**
     * Sets the "exits" flag appropriately for the neighbouring hex in the provided direction. Does
     * not reset connects in other directions. All <code>Terrain.ROAD</code>s will automatically
     * connect to <code>Terrain.PAVEMENT</code> hexes.
     *
     * @param other the neighbouring hex in the specified direction
     * @param direction the <code>int</code> direction of the exit. This value should be between 0
     *                  and 5 (inclusive).
     * @see Hex#setExits(Hex, int, boolean)
     */
    public void setExits(Hex other, int direction) {
        this.setExits(other, direction, true);
    }

    /**
     * Sets the "exits" flag appropriately for the neighbouring hex in the provided direction. Does
     * not reset connects in other directions.
     *
     * @param other the neighbouring hex in the specified direction
     * @param direction the <code>int</code> direction of the exit. This value should be between 0
     *                  and 5 (inclusive).
     * @param roadsAutoExit if to automatically exit onto Pavement hexes
     * @see Hex#setExits(Hex, int)
     */
    public void setExits(Hex other, int direction, boolean roadsAutoExit) {
        for (Integer i : terrains.keySet()) {
            Terrain cTerr = getTerrain(i);
            Terrain oTerr;

            if ((cTerr == null) || cTerr.hasExitsSpecified()) {
                continue;
            }

            if (other != null) {
                oTerr = other.getTerrain(i);
            } else {
                oTerr = null;
            }

            cTerr.setExit(direction, cTerr.exitsTo(oTerr));
            
            // Water gets a special treatment: Water at the board edge
            // (hex == null) should usually look like ocean and 
            // therefore always gets connection to outside the board 
            if ((cTerr.getType() == Terrains.WATER) && (other == null)) {
                cTerr.setExit(direction, true);
            }

            // Roads exit into pavement, too.
            if ((other != null) && roadsAutoExit && (cTerr.getType() == Terrains.ROAD)
                    && other.containsTerrain(Terrains.PAVEMENT)) {
                cTerr.setExit(direction, true);
            }

            // buildings must have the same building class
            if ((other != null) && (cTerr.getType() == Terrains.BUILDING)
                    && (terrainLevel(Terrains.BLDG_CLASS) != other.terrainLevel(Terrains.BLDG_CLASS))) {
                cTerr.setExit(direction, false);
            }

            // gun emplacements can only be single hex buildings
            if ((cTerr.getType() == Terrains.BUILDING)
                    && (terrainLevel(Terrains.BLDG_CLASS) == Building.GUN_EMPLACEMENT)) {
                cTerr.setExit(direction, false);
            }

        }
    }

    /**
     * Determine if this <code>Hex</code> contains the indicated terrain that exits in the specified
     * direction.
     *
     * @param terrType the <code>int</code> type of the terrain.
     * @param direction the <code>int</code> direction of the exit. This value should be between 0
     *                  and 5 (inclusive).
     * @return <code>true</code> if this Hex contains the indicated terrain that exits in the
     * specified direction, or <code>false</code> if bad input is supplied, if no such terrain
     * exists, or if it doesn't exit in that direction.
     * @see Hex#setExits(Hex, int, boolean)
     */
    public boolean containsTerrainExit(int terrType, int direction) {
        boolean result = false;
        final Terrain terr = getTerrain(terrType);

        // Do we have the given terrain that has exits?
        if ((direction >= 0) && (direction <= 5) && (terr != null)) {

            // See if we have an exit in the given direction.
            final int exits = terr.getExits();
            final int exitInDir = (int) Math.pow(2, direction);
            if ((exits & exitInDir) > 0) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Determines if this Hex contains any exists in the specified direction.
     *
     * @param direction the <code>int</code> direction of the exit. This value should be between 0
     *                  and 5 (inclusive).
     * @return <code>true</code> if this <code>Hex</code> contains any terrain that exits in the
     * specified direction. <code>false</code> if bad input is supplied or if no terrain exits in
     * that direction.
     * @see Hex#setExits(Hex, int, boolean)
     */
    public boolean containsExit(int direction) {
        boolean rv = false;
        for (Integer terrType : terrains.keySet()) {
            rv |= containsTerrainExit(terrType, direction);
        }
        return rv;
    }

    /**
     * @return if this hex contains a terrain type that can have exits
     */
    public boolean hasExitableTerrain() {
        boolean rv = false;
        for (Integer terrType : terrains.keySet()) {
            rv |= Terrains.exitableTerrain(terrType);
        }
        return rv;
    }

    /**
     * @return the highest level that features in this hex extend to. Above this level is assumed
     * to be air. This assumes a ground map.
     */
    public int ceiling() {
        return ceiling(false);
    }

    /**
     * @param inAtmosphere Determines if the ceiling should be determined for an atmospheric map
     *                     (eg, altitudes) or ground map (eg, levels)
     * @return the highest level or altitude (depending on flag) that features in this hex extend
     * to. Above this level is assumed to be air.
     */
    public int ceiling(boolean inAtmosphere) {
        return level + maxTerrainFeatureElevation(inAtmosphere);
    }

    /**
     * @param inAtmosphere Determines if the ceiling should be determined for an atmospheric map
     *                     (eg, altitudes) or ground map (eg, levels)
     * @return the elevation or altitude of the terrain feature that rises the highest above the
     * surface of the hex. For example, if the hex is on the ground map and contains woods, this
     * would return 2.
     */
    public int maxTerrainFeatureElevation(boolean inAtmosphere) {
        int maxFeature = 0;
        int featureElev;
        for (Integer terrainType : terrains.keySet()) {
            featureElev = terrains.get(terrainType).getTerrainElevation(inAtmosphere);
            if (featureElev > maxFeature) {
                maxFeature = featureElev;
            }
        }
        return maxFeature;
    }

    /**
     * Returns the lowest reachable point of this hex, used for terrain types
     * that can extend below the surface of the hex, such as water and
     * basements. Unrevealed basements will not affect this value.
     *
     * @return the lowest level that revealed features in this hex extend to.
     *         Below this level is assumed to be bedrock and/or basement.
     *         Unrevealed basements will not affect this value.
     */
    public int floor() {
        return level - depth();
    }

    /**
     * @return a level indicating how far features in this hex extends below the surface level.
     */
    public int depth() {
        return depth(false);
    }

    public int depth(boolean hidden) {
        int depth = 0;
        Terrain water = getTerrain(Terrains.WATER);
        Terrain basement = getTerrain(Terrains.BLDG_BASEMENT_TYPE);

        if (water != null) {
            depth += water.getLevel();
        }

        if (basement != null) {
            if (hidden) {
                depth += BasementType.getType(basement.getLevel()).getDepth();
            }
        }

        return depth;
    }

    /**
     * @return true if this hex has a terrain with a non-zero terrain factor
     */
    public boolean hasTerrainFactor() {
        for (int type : terrains.keySet()) {
            if (terrains.get(type).getTerrainFactor() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param type the terrain type to check
     * @return <code>true</code> if the specified terrain is represented in the hex at any level.
     * @see Hex#containsTerrain(int, int)
     * @see Hex#containsAllTerrainsOf(int...)
     * @see Hex#containsAnyTerrainOf(int...)
     */
    public boolean containsTerrain(int type) {
        return getTerrain(type) != null;
    }

    /**
     * @param type the terrain type to check
     * @param level level to check the presence of the given terrain at
     * @return if the specified terrain is represented in the hex at given level.
     * @see Hex#containsTerrain(int)
     * @see Hex#containsAllTerrainsOf(int...)
     * @see Hex#containsAnyTerrainOf(int...)
     */
    public boolean containsTerrain(int type, int level) {
        Terrain terrain = getTerrain(type);
        if (terrain != null) {
            return terrain.getLevel() == level;
        }
        return false;
    }

    /**
     * @param types the terrains to check
     * @return if at least one of the specified terrains are represented in the hex at any level.
     * @see Hex#containsTerrain(int, int)
     * @see Hex#containsTerrain(int)
     * @see Hex#containsAllTerrainsOf(int...)
     */
    public boolean containsAnyTerrainOf(int... types) {
        for (int type: types) {
            if (containsTerrain(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param types the terrains to check
     * @return <code>true</code> if all the specified terrains are represented in the hex at any level.
     * @see Hex#containsTerrain(int, int)
     * @see Hex#containsAllTerrainsOf(int...)
     * @see Hex#containsAnyTerrainOf(int...)
     */
    public boolean containsAllTerrainsOf(int... types) {
        for (int type: types) {
            if (!containsTerrain(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return if there is pavement, a road or a bridge in the hex.
     */
    public boolean hasPavement() {
        return containsTerrain(Terrains.PAVEMENT) || containsTerrain(Terrains.ROAD) || containsTerrain(Terrains.BRIDGE);
    }

    /**
     * @return the level of the terrain specified, or Terrain.LEVEL_NONE if the
     *         terrain is not present in the hex
     */
    public int terrainLevel(int type) {
        Terrain terrain = getTerrain(type);
        if (terrain != null) {
            return terrain.getLevel();
        }
        return Terrain.LEVEL_NONE;
    }

    /**
     * @param type the specified type
     * @return the terrain of the specified type, or <code>null</code> if the terrain is not present
     * in the hex
     */
    public @Nullable Terrain getTerrain(int type) {
        return terrains.get(type);
    }

    /**
     * @param type the preferred type to get
     * @param types the terrain types to check
     * @return Returns the Terrain for the preferred type, then an unspecified one of the given
     * types of Terrain, or null if none are preset.
     */
    public @Nullable Terrain getAnyTerrainOf(int type, int... types) {
        if (containsTerrain(type)) {
            return terrains.get(type);
        }

        for (int moreTypes : types) {
            if (containsTerrain(moreTypes)) {
                return terrains.get(moreTypes);
            }
        }

        return null;
    }

    /**
     * @param terrain the terrain to add to this hex
     */
    public void addTerrain(Terrain terrain) {
        terrains.put(terrain.getType(), terrain);
    }

    /**
     * @param type the terrain type to remove
     */
    public void removeTerrain(int type) {
        terrains.remove(type);
    }

    /**
     * Removes all Terrains from the hex.
     */
    public void removeAllTerrains() {
        terrains.clear();
    }

    /**
     * @return the number of terrain attributes present that are displayable in tooltips
     */
    public int displayableTerrainsPresent() {
        int present = 0;
        for (Integer i : terrains.keySet()) {
            if ((null != Terrains.getDisplayName(i, terrains.get(i).getLevel()))) {
                present++;
            }
        }
        return present;
    }

    /**
     * @return the number of terrain attributes present
     */
    public int terrainsPresent() {
        return terrains.size();
    }

    /**
     * FIXME : I should be a clone implementation, not this
     * @return new hex which is equal to this
     */
    public Hex duplicate() {
        Terrain[] tcopy = new Terrain[Terrains.SIZE];
        for (Integer i : terrains.keySet()) {
            tcopy[i] = new Terrain(terrains.get(i));
        }
        return new Hex(level, tcopy, theme, coords);
    }

    /**
     * Adds terrain modifiers to PSRs made in this hex
     */
    public void terrainPilotingModifier(EntityMovementMode moveMode, PilotingRollData roll,
                                        boolean enteringRubble) {
        for (Integer i : terrains.keySet()) {
            terrains.get(i).pilotingModifier(moveMode, roll, enteringRubble);
        }
    }

    /**
     * @return extra movement cost for entering the hex
     */
    public int movementCost(Entity entity) {
        int rv = 0;
        for (final Terrain terrain : terrains.values()) {
            rv += terrain.movementCost(entity);
        }
        return rv;
    }

    /**
     * @return the modifier to the roll to ignite this hex
     */
    public int getIgnitionModifier() {
        int mod = 0;
        for (final Terrain terrain : terrains.values()) {
            if (terrain != null) {
                mod += terrain.ignitionModifier();
            }
        }
        return mod;
    }

    /**
     * @return if this hex is ignitable
     */
    public boolean isIgnitable() {
        return (containsTerrain(Terrains.WOODS) || containsTerrain(Terrains.JUNGLE)
                || containsTerrain(Terrains.BUILDING) || containsTerrain(Terrains.FUEL_TANK)
                || containsTerrain(Terrains.FIELDS) || containsTerrain(Terrains.INDUSTRIAL));
    }

    public boolean isClearForTakeoff() {
        for (final Integer i : terrains.keySet()) {
            if (containsTerrain(i) && (i != Terrains.PAVEMENT) && (i != Terrains.ROAD) && (i != Terrains.FLUFF)
                    && (i != Terrains.ARMS) && (i != Terrains.LEGS) && (i != Terrains.SNOW) && (i != Terrains.MUD)
                    && (i != Terrains.SMOKE) && (i != Terrains.METAL_CONTENT)) {
                return false;
            }
        }
        return true;
    }

    public boolean isClearForLanding() {
        return !containsTerrain(Terrains.IMPASSABLE);
    }

    public int getFireTurn() {
        return fireTurn;
    }

    public void incrementFireTurn() {
        fireTurn = fireTurn + 1;
    }

    public void resetFireTurn() {
        fireTurn = 0;
    }

    /**
     * get any modifiers to a bog-down roll in this hex. Takes the worst
     * modifier. If there is no bog-down chance in this hex, then it returns
     * TargetRoll.AUTOMATIC_SUCCESS
     */
    public int getBogDownModifier(EntityMovementMode moveMode, boolean largeVee) {
        int mod = TargetRoll.AUTOMATIC_SUCCESS;
        for (final Terrain terrain : terrains.values()) {
            if ((terrain != null) && (mod < terrain.getBogDownModifier(moveMode, largeVee))) {
                mod = terrain.getBogDownModifier(moveMode, largeVee);
            }
        }
        return mod;
    }

    /**
     * get any modifiers to an unstuck roll in this hex.
     */
    public void getUnstuckModifier(int elev, PilotingRollData rollTarget) {
        for (final Terrain terrain : terrains.values()) {
            terrain.getUnstuckModifier(elev, rollTarget);
        }
    }
    
    /** 
     * True if this hex has a clifftop towards otherHex. This hex
     * must have the terrain CLIFF_TOP, it must have exits
     * specified (exits set to active) for the CLIFF_TOP terrain,
     * and must have an exit in the direction of otherHex.  
     */
    public boolean hasCliffTopTowards(Hex otherHex) {
        return containsTerrain(Terrains.CLIFF_TOP)
            && getTerrain(Terrains.CLIFF_TOP).hasExitsSpecified()
            && ((getTerrain(Terrains.CLIFF_TOP).getExits() & (1 << coords.direction(otherHex.getCoords()))) != 0);
    }

    /** Returns the position of this hex on the board. */
    public Coords getCoords() {
        return coords;
    }

    /** 
     * Sets the coords of this hex. DO NOT USE outside board.java!
     * WILL NOT MOVE THE HEX. Only the position of the hex in the 
     * board's data[] determines the actual location of the hex. 
     */
    public void setCoords(Coords c) {
        coords = c;
    }

    /**
     * @return if this hex is "clear", based on the absence of most terrain types.
     */
    public boolean isClearHex() {
        for (int t = 1; t <= Terrains.BLDG_BASE_COLLAPSED; t++) {
            // Ignore some terrain types
            if ((t == Terrains.FLUFF) || (t == Terrains.ARMS) || (t == Terrains.LEGS)) {
                continue;
            }

            if (containsTerrain(t)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Windchild Rework Me
     * Determines if the Hex is valid or not. <code>errBuff</code> can be used to return a report
     * of why the hex is valid.
     *
     * @param errBuff Buffer to contain error messages. If null, method returns on first failure.
     * @return if the hex is valid
     */
    public boolean isValid(@Nullable StringBuffer errBuff) {
        boolean valid = true;
        
        // When no StringBuffer is passed, use a dummy
        // to avoid numerous null checks
        if (errBuff == null) {
            errBuff = new StringBuffer();
        }
        
        // Check individual terrains for validity
        for (final Terrain terrain : terrains.values()) {
            if (terrain == null) {
                valid = false;
                errBuff.append("Hex contains a null terrain!\n");
                continue;
            }

            StringBuffer terrainErr = new StringBuffer();
            if (!terrain.isValid(terrainErr)) {
                valid = false;
                if (errBuff.length() > 0) {
                    errBuff.append("\n");
                }
                errBuff.append(terrainErr);
            }
        }

        // Rapids
        if ((containsTerrain(Terrains.RAPIDS))) {
            if (!containsTerrain(Terrains.WATER)) {
                valid = false;
                errBuff.append("Rapids must occur within water!\n");
            }

            if (depth() < 1) {
                valid = false;
                errBuff.append("Rapids must occur in depth 1 or greater!\n");
            }
        }

        // Foliage (Woods and Jungles)
        if (containsTerrain(Terrains.WOODS) && containsTerrain(Terrains.JUNGLE)) {
            valid = false;
            errBuff.append("Woods and Jungle cannot appear in the same hex!\n");
        }

        if ((containsTerrain(Terrains.WOODS) || containsTerrain(Terrains.JUNGLE))
                && containsTerrain(Terrains.FOLIAGE_ELEV)) {
            int wl = terrainLevel(Terrains.WOODS);
            int jl = terrainLevel(Terrains.JUNGLE);
            int el = terrainLevel(Terrains.FOLIAGE_ELEV);
            
            boolean isLightOrHeavy = wl == 1 || jl == 1 || wl == 2 || jl == 2;
            boolean isUltra = wl == 3 || jl == 3;
            
            if (! ((el == 1) || (isLightOrHeavy && el == 2) || (isUltra && el == 3))) {
                valid = false;
                errBuff.append("Foliage elevation is wrong, must be 1 or 2 for Light/Heavy and 1 or 3 for Ultra Woods/Jungle!\n");
            }
        }
        if (!(containsTerrain(Terrains.WOODS) || containsTerrain(Terrains.JUNGLE))
                && containsTerrain(Terrains.FOLIAGE_ELEV)) {
            valid = false;
            errBuff.append("Woods and Jungle elevation terrain present without Woods or Jungle!\n");
        }
        
        // Buildings must have at least BUILDING, BLDG_ELEV and BLDG_CF
        if (containsAnyTerrainOf(Terrains.BUILDING, Terrains.BLDG_ELEV, Terrains.BLDG_CF, Terrains.BLDG_FLUFF, 
                Terrains.BLDG_ARMOR, Terrains.BLDG_CLASS, Terrains.BLDG_BASE_COLLAPSED, Terrains.BLDG_BASEMENT_TYPE)
                && !containsAllTerrainsOf(Terrains.BUILDING, Terrains.BLDG_ELEV, Terrains.BLDG_CF)) {
            valid = false;
            errBuff.append("Incomplete Building! A hex with any building terrain must at least contain "
                    + "a building type, building elevation and building CF.\n");
        }

        // Bridges must have all of BRIDGE, BRIDGE_ELEV and BRIDGE_CF
        if (containsAnyTerrainOf(Terrains.BRIDGE, Terrains.BRIDGE_ELEV, Terrains.BRIDGE_CF)
                && !containsAllTerrainsOf(Terrains.BRIDGE, Terrains.BRIDGE_ELEV, Terrains.BRIDGE_CF)) {
            valid = false;
            errBuff.append("Incomplete Bridge! A hex with any bridge terrain must contain "
                    + "the bridge type, bridge elevation and the bridge CF.\n");
        }

        // Fuel Tanks must have all of FUEL_TANK, _ELEV, _CF and _MAGN
        if (containsAnyTerrainOf(Terrains.FUEL_TANK, Terrains.FUEL_TANK_CF, 
                Terrains.FUEL_TANK_ELEV, Terrains.FUEL_TANK_MAGN)
                && !containsAllTerrainsOf(Terrains.FUEL_TANK, Terrains.FUEL_TANK_CF, 
                        Terrains.FUEL_TANK_ELEV, Terrains.FUEL_TANK_MAGN)) {
            valid = false;
            errBuff.append("Incomplete Fuel Tank! A hex with any fuel tank terrain must contain "
                    + "the fuel tank type, elevation, CF and the fuel tank magnitude.\n");
        }
        
        if (containsAllTerrainsOf(Terrains.FUEL_TANK, Terrains.BUILDING)) {
            valid = false;
            errBuff.append("A Hex cannot have both a Building and a Fuel Tank.\n");
        }

        return valid;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Level: ").append(getLevel()).append("  Features: ");
        for (final Terrain terrain : terrains.values()) {
            if (terrain != null) {
                switch (terrain.getType()) {
                    case Terrains.WOODS:
                        if (terrain.getLevel() == 2) {
                            sb.append("Heavy Woods");
                        } else if (terrain.getLevel() == 1) {
                            sb.append("Light Woods");
                        } else {
                            sb.append("??? Woods");
                        }
                        break;
                    case Terrains.WATER:
                        sb.append("Water, depth: ").append(terrain.getLevel());
                        break;
                    case Terrains.ROAD:
                        sb.append("Road");
                        break;
                    case Terrains.ROUGH:
                        sb.append("Rough");
                        break;
                    case Terrains.RUBBLE:
                        sb.append("Rubble");
                        break;
                    case Terrains.SWAMP:
                        sb.append("Swamp");
                        break;
                    case Terrains.ARMS:
                        sb.append("Arm");
                        break;
                    case Terrains.LEGS:
                        sb.append("Leg");
                        break;
                    default:
                        sb.append(Terrains.getName(terrain.getType())).append("(")
                                .append(terrain.getLevel()).append(", ")
                                .append(terrain.getTerrainFactor()).append(")");
                }
                sb.append("; ");
            }
        }

        return sb.toString();
    }

    /**
     * Returns a string representation of this Hex to use for copy/paste actions. The string contains
     * the elevation, theme and terrains of this Hex except automatic terrains (such as inclines). The
     * generated string can be parsed to generate a copy of the hex using {@link #parseClipboardString(String)}.
     *
     * @return A string representation to use when copying a hex to the clipboard.
     */
    public String getClipboardString() {
        StringBuilder hexString = new StringBuilder("MegaMek Hex///");
        hexString.append("Level###").append(getLevel()).append("///");
        hexString.append("Theme###").append(getTheme()).append("///");
        hexString.append("Terrain###");
        List<String> terrains = Arrays.stream(getTerrainTypes())
                .filter(t -> !Terrains.AUTOMATIC.contains(t))
                .mapToObj(t -> getTerrain(t).toString()).collect(Collectors.toList());
        hexString.append(String.join(";", terrains));
        return hexString.toString();
    }

    /**
     * Returns a new Hex parsed from a clipboard string representation.
     * Returns null when the clipboard String is not created by {@link #getClipboardString()}
     * (i.e., when it does not at least start with "MegaMek Hex").
     *
     * @param clipboardString The string representation of the Hex to parse
     * @return A hex containing any features that could be parsed from clipboardString
     */
    public static @Nullable Hex parseClipboardString(String clipboardString) {
        StringTokenizer hexInfo = new StringTokenizer(clipboardString, "///");
        String theme = "";
        int hexLevel = 0;
        String terrainString = "";

        if (!clipboardString.startsWith("MegaMek Hex")) {
            return null;
        }

        while (hexInfo.hasMoreTokens()) {
            String info = hexInfo.nextToken();
            StringTokenizer subInfo = new StringTokenizer(info, "###");
            if (subInfo.hasMoreTokens()) {
                String infoType = subInfo.nextToken();
                switch (infoType) {
                    case "MegaMek Hex":
                        // This is just a header
                    case "Level":
                        if (subInfo.hasMoreTokens()) {
                            try {
                                hexLevel = Integer.parseInt(subInfo.nextToken());
                            } catch (NumberFormatException ignored) {
                                // hexLevel stays at 0
                            }
                        }
                        break;
                    case "Theme":
                        if (subInfo.hasMoreTokens()) {
                            theme = subInfo.nextToken();
                        }
                        break;
                    case "Terrain":
                        if (subInfo.hasMoreTokens()) {
                            terrainString = subInfo.nextToken();
                        }
                        break;
                }
            }
        }
        return new Hex(hexLevel, terrainString, theme, new Coords(0, 0));
    }

}