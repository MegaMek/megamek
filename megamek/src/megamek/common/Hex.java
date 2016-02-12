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
import java.util.HashSet;
import java.util.StringTokenizer;

import megamek.common.Building.BasementType;

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
     * An array to store the terrain types present in this hex.  The array
     * allows for constant access to the terrain type given a terrain id.
     */
    private ITerrain[] terrains;
    /**
     * A HashMap analog to <code>terrains</code>.  It contains the exact
     *  same terrain types as <code>terrains</code>, however it allows an
     *  efficient way to access all present terrains.
     *
     */
    private HashSet<Integer> hsTerrains;
    private String theme;
    private int fireTurn;
    private Coords coords;
    /** Constructs clear, plain hex at level 0. */
    public Hex() {
        this(0);
    }

    /** Constructs clean, plain hex at specified level. */
    public Hex(int level) {
        this(level, new ITerrain[Terrains.SIZE], null, new Coords(0,0));
    }

    public Hex(int level, ITerrain[] terrains, String theme) {
        this(level, terrains, theme, new Coords(0,0));
    }

    /** Constructs hex with all parameters. */
    public Hex(int level, ITerrain[] terrains, String theme, Coords c) {
        this.level = level;
        coords = c;
        this.terrains = terrains;
        // Creates a HM with a capacity that is 25% larger than the number of
        //  terrain types.  This should prevent us from every having to resize
        hsTerrains = new HashSet<Integer>(
                (int)((Terrains.SIZE * 1.25) + 0.5), 0.75f);

        for (int i = 0; i < terrains.length; i++){
            if (terrains[i] != null){
                hsTerrains.add(i);
            }
        }

        if ((theme == null) || (theme.length() > 0)) {
            this.theme = theme;
        } else {
            this.theme = null;
        }
    }

    public Hex(int level, String terrain, String theme) {
        this(level,terrain,theme,new Coords(0,0));
    }

    /** Contructs hex with string terrain info */
    public Hex(int level, String terrain, String theme, Coords c) {
        this(level, new ITerrain[Terrains.SIZE], theme, c);
        for (StringTokenizer st = new StringTokenizer(terrain, ";", false); st
                .hasMoreTokens();) {
            addTerrain(Terrains.getTerrainFactory().createTerrain(
                    st.nextToken()));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#getTerrainTypes()
     */
    public int[] getTerrainTypes(){
        int []rv = new int[hsTerrains.size()];
        int tdx = 0;
        for (Integer i : hsTerrains){
            rv[tdx++] = i;
        }
        return rv;
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

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#clearExits()
     */
    public void clearExits() {
        for (Integer i : hsTerrains){
            ITerrain t = terrains[i];
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
        for (Integer i : hsTerrains){
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

            // Roads exit into pavement, too.
            if ((other != null) && roadsAutoExit
                    && (cTerr.getType() == Terrains.ROAD)
                    && other.containsTerrain(Terrains.PAVEMENT)) {
                cTerr.setExit(direction, true);
            }

            //buildings must have the same building class
            if((other != null) && (cTerr.getType() == Terrains.BUILDING)
                    && (terrainLevel(Terrains.BLDG_CLASS) !=
                        other.terrainLevel(Terrains.BLDG_CLASS))) {
                cTerr.setExit(direction, false);
            }

            //gun emplacements can only be single hex buildings
            if((cTerr.getType() == Terrains.BUILDING)
                    && (terrainLevel(Terrains.BLDG_CLASS) ==
                        Building.GUN_EMPLACEMENT)) {
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
    public boolean containsExit(int direction){
        boolean rv = false;
        for (Integer terrType : hsTerrains){
            rv |= containsTerrainExit(terrType, direction);           
        }
        return rv;
    }
    
    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#hasExitableTerrain()
     */
    public boolean hasExitableTerrain(){
        boolean rv = false;
        for (Integer terrType : hsTerrains){
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
        for (Integer terrainType : hsTerrains) {
            featureElev = terrains[terrainType].getTerrainElevation(inAtmo);
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
     * @return
     */
    public boolean hasTerrainfactor() {
        for (int type : hsTerrains) {
            if (terrains[type].getTerrainFactor() > 0) {
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
        return containsTerrain(Terrains.PAVEMENT)
                || containsTerrain(Terrains.ROAD)
                || containsTerrain(Terrains.BRIDGE);
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
        return terrains[type];
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#addTerrain(megamek.common.Terrain)
     */
    public void addTerrain(ITerrain terrain) {
        terrains[terrain.getType()] = terrain;
        hsTerrains.add(terrain.getType());
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#removeTerrain(int)
     */
    public void removeTerrain(int type) {
        terrains[type] = null;
        hsTerrains.remove(type);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#removeAllTerrains()
     */
    public void removeAllTerrains() {
        for (int i = 0; i < terrains.length; i++) {
            terrains[i] = null;
        }
        hsTerrains.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#terrainsPresent()
     */
    public int displayableTerrainsPresent() {
        int present = 0;
        for (Integer i : hsTerrains){
            if ((null != terrains[i]) &&
                    (null != Terrains.getDisplayName(i,terrains[i].getLevel()))){
                present++;
            }
        }
        return present;
    }

    /*
     * report the number of terrains present for the tooltips.
     */
    public int terrainsPresent() {
        return hsTerrains.size();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IHex#duplicate
     */
    public IHex duplicate() {
        ITerrain[] tcopy = new ITerrain[terrains.length];
        ITerrainFactory f = Terrains.getTerrainFactory();
        for (Integer i : hsTerrains){
            if (terrains[i] != null) {
                tcopy[i] = f.createTerrain(terrains[i]);
            }
        }
        return new Hex(level, tcopy, theme, coords);
    }

    public int terrainPilotingModifier(EntityMovementMode moveMode) {
        int rv = 0;
        for (Integer i : hsTerrains){
            if (terrains[i] != null) {
                rv += terrains[i].pilotingModifier(moveMode);
            }
        }
        return rv;
    }

    public int movementCost(Entity entity) {
        int rv = 0;
        for (Integer i : hsTerrains){
            if (terrains[i] != null) {
                rv += terrains[i].movementCost(entity);
            }
        }
        return rv;
    }

    @Override
    public String toString() {
        String temp;
        temp = "Level: " + getLevel();
        temp = temp + "  Features: ";
        for (Integer i : hsTerrains) {
            ITerrain terrain = terrains[i];
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
                        temp = temp + Terrains.getName(terrain.getType()) + "("
                                + terrain.getLevel() + ", "
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
        for (Integer i : hsTerrains){
            if (terrains[i] != null) {
                mod += terrains[i].ignitionModifier();
            }
        }
        return mod;
    }

    /**
     * Is this hex ignitable?
     */
    public boolean isIgnitable() {
        return (containsTerrain(Terrains.WOODS)
                || containsTerrain(Terrains.JUNGLE)
                || containsTerrain(Terrains.BUILDING)
                || containsTerrain(Terrains.FUEL_TANK)
                || containsTerrain(Terrains.FIELDS)
                || containsTerrain(Terrains.INDUSTRIAL));

    }

    public boolean isClearForTakeoff() {
        for (Integer i : hsTerrains){
            if(containsTerrain(i) && (i != Terrains.PAVEMENT) &&
                    (i!=Terrains.ROAD)  && (i != Terrains.FLUFF) &&
                    (i != Terrains.ARMS) && (i != Terrains.LEGS)
                    && (i != Terrains.SNOW) && (i != Terrains.MUD)
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
     * modifier.
     * If there is no bog-down chance in this hex, then it returns
     * TargetRoll.AUTOMATIC_SUCCESS
     */
    public int getBogDownModifier(EntityMovementMode moveMode,
            boolean largeVee) {
        int mod = TargetRoll.AUTOMATIC_SUCCESS;
        for (Integer i : hsTerrains){
            if ((terrains[i] != null) &&
                    (mod < terrains[i].getBogDownModifier(moveMode,largeVee))){
                mod = terrains[i].getBogDownModifier(moveMode, largeVee);
            }
        }
        return mod;
    }

    /**
     * get any modifiers to a an unstuck roll in this hex.
     */
    public int getUnstuckModifier(int elev) {
        int mod = 0;
        for (Integer i : hsTerrains){
            if (terrains[i] != null) {
                mod += terrains[i].getUnstuckModifier(elev);
            }
        }
        return mod;
    }

    /** The notional position of this {@code Hex}, as set upon creation.
     *
     * @return the {@code Coords} object representing the coordinates this {@code
     *      Hex} was created with. NOTE: this is only used so that a certain hex
     *      will always use the same image to represent terrain. DO NOT USE FOR
     *      OTHER PURPOSES
     */
    public Coords getCoords() {
        return coords;
    }
    
    public void setCoords(Coords c) {
        coords = c;
    }

}
