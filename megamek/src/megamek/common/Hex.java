/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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
import java.util.HashMap;
import java.util.StringTokenizer;

import megamek.common.Building.BasementType;
import megamek.common.annotations.Nullable;

/**
 * Hex represents a single hex on the board.
 *
 * @author Ben
 */
public class Hex implements IHex, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 82483704768044698L;
    private int level;
    /**
     * A HashMap to <code>terrains</code>. It contains the exact same terrain
     * types that the old <code>terrains</code> did, however it allows an
     * efficient way to access all present terrains.
     *
     */
    private HashMap<Integer, ITerrain> terrains = new HashMap<Integer, ITerrain>(1);
    private String theme;
    private String originalTheme;
    private int fireTurn;
    private Coords coords;

    /** Constructs clear, plain hex at level 0. */
    public Hex() {
        this(0);
    }

    /** Constructs clean, plain hex at specified level. */
    public Hex(int level) {
        this(level, new ITerrain[Terrains.SIZE], null, new Coords(0, 0));
    }

    public Hex(int level, ITerrain[] terrains, String theme) {
        this(level, terrains, theme, new Coords(0, 0));
    }

    /** Constructs hex with all parameters. */
    public Hex(int level, ITerrain[] terrains, String theme, Coords c) {
        this.level = level;
        coords = c;
        for (ITerrain t : terrains) {
            if (t != null)
                this.terrains.put(t.getType(), t);
        }

        if ((theme == null) || (theme.length() > 0)) {
            this.theme = theme;
        } else {
            this.theme = null;
        }
        originalTheme = this.theme;
    }

    public Hex(int level, String terrain, String theme) {
        this(level, terrain, theme, new Coords(0, 0));
    }

    /** Contructs hex with string terrain info */
    public Hex(int level, String terrain, String theme, Coords c) {
        this(level, new ITerrain[Terrains.SIZE], theme, c);
        for (StringTokenizer st = new StringTokenizer(terrain, ";", false); st.hasMoreTokens();) {
            addTerrain(Terrains.getTerrainFactory().createTerrain(st.nextToken()));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#getTerrainTypes()
     */
    public int[] getTerrainTypes() {
        return terrains.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#getLevel()
     */
    public int getLevel() {
        return level;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#setLevel(int)
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#getTheme()
     */
    public String getTheme() {
        return theme;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#setTheme(java.lang.String)
     */
    public void setTheme(String theme) {
        this.theme = theme;
    }

    /** Resets the theme to what was specified in the board file. */
    public void resetTheme() {
        setTheme(originalTheme);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#clearExits()
     */
    public void clearExits() {
        for (Integer i : terrains.keySet()) {
            ITerrain t = terrains.get(i);
            if ((t != null) && !t.hasExitsSpecified()) {
                t.setExits(0);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#setExits(megamek.common.IHex, int)
     */
    public void setExits(IHex other, int direction) {
        this.setExits(other, direction, true);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#setExits(megamek.common.IHex, int, boolean)
     */
    public void setExits(IHex other, int direction, boolean roadsAutoExit) {
        for (Integer i : terrains.keySet()) {
            ITerrain cTerr = getTerrain(i);
            ITerrain oTerr;

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

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#containsTerrainExit(int, int)
     */
    public boolean containsTerrainExit(int terrType, int direction) {
        boolean result = false;
        final ITerrain terr = getTerrain(terrType);

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

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#containsExit(int)
     */
    public boolean containsExit(int direction) {
        boolean rv = false;
        for (Integer terrType : terrains.keySet()) {
            rv |= containsTerrainExit(terrType, direction);
        }
        return rv;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#hasExitableTerrain()
     */
    public boolean hasExitableTerrain() {
        boolean rv = false;
        for (Integer terrType : terrains.keySet()) {
            rv |= Terrains.exitableTerrain(terrType);
        }
        return rv;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#ceiling()
     */
    public int ceiling() {
        return ceiling(false);
    }

    public int ceiling(boolean inAtmosphere) {
        return level + maxTerrainFeatureElevation(inAtmosphere);
    }

    public int maxTerrainFeatureElevation(boolean inAtmo) {
        int maxFeature = 0;
        int featureElev;
        for (Integer terrainType : terrains.keySet()) {
            featureElev = terrains.get(terrainType).getTerrainElevation(inAtmo);
            if (featureElev > maxFeature) {
                maxFeature = featureElev;
            }
        }
        return maxFeature;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#surface()
     */
    public int surface() {
        return level;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#floor()
     */
    public int floor() {
        return level - depth();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#depth()
     */
    public int depth() {
        return depth(false);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#depth( boolean hidden)
     */
    public int depth(boolean hidden) {
        int depth = 0;
        ITerrain water = getTerrain(Terrains.WATER);
        ITerrain basement = getTerrain(Terrains.BLDG_BASEMENT_TYPE);

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
     * Returns true if this hex has a terrain with a non-zero terrain factor
     * 
     * @return
     */
    public boolean hasTerrainfactor() {
        for (int type : terrains.keySet()) {
            if (terrains.get(type).getTerrainFactor() > 0) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#contains(int)
     */
    public boolean containsTerrain(int type) {
        return getTerrain(type) != null;
    }
    
    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#containsAnyTerrainOf(int...)
     */
    public boolean containsAnyTerrainOf(int... types) {
        for (int type: types) {
            if (containsTerrain(type)) {
                return true;
            }
        }
        return false;
    }
    
    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#containsAllTerrainsOf(int...)
     */
    public boolean containsAllTerrainsOf(int... types) {
        for (int type: types) {
            if (!containsTerrain(type)) {
                return false;
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#contains(int, int)
     */
    public boolean containsTerrain(int type, int level) {
        ITerrain terrain = getTerrain(type);
        if (terrain != null) {
            return terrain.getLevel() == level;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#hasPavement()
     */
    public boolean hasPavement() {
        return containsTerrain(Terrains.PAVEMENT) || containsTerrain(Terrains.ROAD) || containsTerrain(Terrains.BRIDGE);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#levelOf(int)
     */
    public int terrainLevel(int type) {
        ITerrain terrain = getTerrain(type);
        if (terrain != null) {
            return terrain.getLevel();
        }
        return ITerrain.LEVEL_NONE;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#getTerrain(int)
     */
    public ITerrain getTerrain(int type) {
        return terrains.get(type);
    }
    
    @Override
    public ITerrain getAnyTerrainOf(int type, int... types) {
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

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#addTerrain(megamek.common.Terrain)
     */
    public void addTerrain(ITerrain terrain) {
        terrains.put(terrain.getType(), terrain);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#removeTerrain(int)
     */
    public void removeTerrain(int type) {
        terrains.remove(type);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#removeAllTerrains()
     */
    public void removeAllTerrains() {
        terrains.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#terrainsPresent()
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

    /*
     * report the number of terrains present for the tooltips.
     */
    public int terrainsPresent() {
        return terrains.size();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#duplicate
     */
    public IHex duplicate() {
        ITerrain[] tcopy = new ITerrain[Terrains.SIZE];
        ITerrainFactory f = Terrains.getTerrainFactory();
        for (Integer i : terrains.keySet()) {
            tcopy[i] = f.createTerrain(terrains.get(i));
        }
        return new Hex(level, tcopy, theme, coords);
    }

    public void terrainPilotingModifier(EntityMovementMode moveMode, PilotingRollData roll, boolean enteringRubble) {
        for (Integer i : terrains.keySet()) {
            terrains.get(i).pilotingModifier(moveMode, roll, enteringRubble);
        }
    }

    public int movementCost(Entity entity) {
        int rv = 0;
        for (ITerrain terrain : terrains.values()) {
            rv += terrain.movementCost(entity);
        }
        return rv;
    }

    @Override
    public String toString() {
        String temp;
        temp = "Level: " + getLevel();
        temp = temp + "  Features: ";
        for (ITerrain terrain : terrains.values()) {
            if (terrain != null) {
                switch (terrain.getType()) {
                case Terrains.WOODS:
                    if (terrain.getLevel() == 2) {
                        temp = temp + "Heavy Woods";
                    } else if (terrain.getLevel() == 1) {
                        temp = temp + "Light Woods";
                    } else {
                        temp = temp + "??? Woods";
                    }
                    break;
                case Terrains.WATER:
                    temp = temp + "Water, depth: " + terrain.getLevel();
                    break;
                case Terrains.ROAD:
                    temp = temp + "Road";
                    break;
                case Terrains.ROUGH:
                    temp = temp + "Rough";
                    break;
                case Terrains.RUBBLE:
                    temp = temp + "Rubble";
                    break;
                case Terrains.SWAMP:
                    temp = temp + "Swamp";
                    break;
                case Terrains.ARMS:
                    temp = temp + "Arm";
                    break;
                case Terrains.LEGS:
                    temp = temp + "Leg";
                    break;
                default:
                    temp = temp + Terrains.getName(terrain.getType()) + "(" + terrain.getLevel() + ", "
                            + terrain.getTerrainFactor() + ")";
                }
                temp = temp + "; ";
            }
        }
        return temp;
    }

    /*
     * Get the fire ignition modifier for this hex, based on its terrain
     */
    public int getIgnitionModifier() {
        int mod = 0;
        for (ITerrain terrain : terrains.values()) {
            if (terrain != null) {
                mod += terrain.ignitionModifier();
            }
        }
        return mod;
    }

    /**
     * Is this hex ignitable?
     */
    public boolean isIgnitable() {
        return (containsTerrain(Terrains.WOODS) || containsTerrain(Terrains.JUNGLE)
                || containsTerrain(Terrains.BUILDING) || containsTerrain(Terrains.FUEL_TANK)
                || containsTerrain(Terrains.FIELDS) || containsTerrain(Terrains.INDUSTRIAL));

    }

    public boolean isClearForTakeoff() {
        for (Integer i : terrains.keySet()) {
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
        for (ITerrain terrain : terrains.values()) {
            if ((terrain != null) && (mod < terrain.getBogDownModifier(moveMode, largeVee))) {
                mod = terrain.getBogDownModifier(moveMode, largeVee);
            }
        }
        return mod;
    }

    /**
     * get any modifiers to a an unstuck roll in this hex.
     */
    public void getUnstuckModifier(int elev, PilotingRollData rollTarget) {
        for (ITerrain terrain : terrains.values()) {
            terrain.getUnstuckModifier(elev, rollTarget);
        }
    }
    
    /** 
     * True if this hex has a clifftop towards otherHex. This hex
     * must have the terrain CLIFF_TOP, it must have exits
     * specified (exits set to active) for the CLIFF_TOP terrain,
     * and must have an exit in the direction of otherHex.  
     */
    public boolean hasCliffTopTowards(IHex otherHex) {
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

    @Override
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

    @Override 
    public boolean isValid(@Nullable StringBuffer errBuff) {
        boolean valid = true;
        
        // When no StringBuffer is passed, use a dummy
        // to avoid numerous null checks
        if (errBuff == null) {
            errBuff = new StringBuffer();
        }
        
        // Check individual terrains for validity
        for (ITerrain terrain : terrains.values()) {
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
            if (this.depth() <1) {
                valid = false;
                errBuff.append("Rapids must occurr in depth 1 or greater!\n");
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

}
