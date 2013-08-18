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

/**
 * This class represents a single, targetable hex of a building. The building
 * itself may occupy multiple hexex.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour )
 * @version $Revision$
 */
public class BuildingTarget implements Targetable {

    /**
     *
     */
    private static final long serialVersionUID = 6432766092407639630L;

    /**
     * The coordinates of the hex being targeted.
     */
    private Coords position = null;

    /**
     * The ID of the building being targeted.
     */
    private int id = Building.UNKNOWN;

    /**
     * The height of the building at the targeted position.
     */
    private int height = Building.UNKNOWN;

    /**
     * The elevation of the building at the targeted position.
     */
    private int elevation = Building.UNKNOWN;

    /**
     * The name of this hex of the building.
     */
    private String name = null;
    
    /**
     * The type of attack that is targeting this building.
     */
    private int type;

    /**
     * Initialize this object from the input.
     *
     * @param coords - the <code>Coords</code> of the hext being targeted.
     * @param board - the game's <code>Board</code> object.
     * @param ignite - a <code>boolean</code> flag that indicates whether the
     *            player is attempting to set the building on fire, or not.
     * @exception an <code>IllegalArgumentException</code> will be thrown if
     *                the given coordinates do not contain a building.
     */
    protected void init(Coords coords, IBoard board, int nType) {
        position = coords;
        type = nType;

        // Get the building at the given coordinates.
        Building bldg = board.getBuildingAt(position);
        if (bldg == null) {
            throw new IllegalArgumentException("The coordinates, "
                    + position.getBoardNum()
                    + ", do not contain a building.");
        }

        // Save the building's ID.
        id = BuildingTarget.coordsToId(coords);

        // Generate a name.
        StringBuffer buff = new StringBuffer();
        buff.append("Hex ").append(position.getBoardNum()).append(" of ")
                .append(bldg.getName());
        switch (nType){
            case Targetable.TYPE_BLDG_IGNITE:
                buff.append(Messages.getString("BuildingTarget.Ignite"));
                break;
            case Targetable.TYPE_BUILDING:
                buff.append(Messages.getString("BuildingTarget.Collapse"));
                break;
            case Targetable.TYPE_BLDG_TAG:
                buff.append(Messages.getString("BuildingTarget.Tag"));
                break;                    
        }

        name = buff.toString();

        // Bottom of building is at ground level, top of building is at
        // BLDG_ELEV.
        // Note that height of 0 is a single story building.
        // Bridges are always height 0, and the BRIDGE_ELEV indicates the
        // elevation
        IHex targetHex = board.getHex(position);
        elevation = Math.max(-targetHex.depth(), targetHex
                .terrainLevel(Terrains.BRIDGE_ELEV));
        height = targetHex.terrainLevel(Terrains.BLDG_ELEV);
        if (height <= 0) {
            height = 0;
        } else {
            height--;
        }
    }

    /**
     * Target a single hex of a building.
     *
     * @param coords - the <code>Coords</code> of the hext being targeted.
     * @param board - the game's <code>Board</code> object.
     * @param type - an <code>int</code> value that indicates whether the
     *            player is attempting to set the building on fire, or not.
     * @exception an <code>IllegalArgumentException</code> will be thrown if
     *                the given coordinates do not contain a building.
     */
    public BuildingTarget(Coords coords, IBoard board, int nType) {
        init(coords, board, nType);
    }

    /**
     * Target a single hex of a building.
     *
     * @param coords - the <code>Coords</code> of the hext being targeted.
     * @param board - the game's <code>Board</code> object.
     * @param ignite - a <code>boolean</code> flag that indicates whether the
     *            player is attempting to set the building on fire, or not.
     * @exception an <code>IllegalArgumentException</code> will be thrown if
     *                the given coordinates do not contain a building.
     */
    public BuildingTarget(Coords coords, IBoard board, boolean ignite) {
        init(coords, board, 
                ignite ? Targetable.TYPE_BLDG_IGNITE : Targetable.TYPE_BUILDING);
    }

    // Implementation of Targetable

    public int getTargetType() {
        return type;
    }

    public int getTargetId() {
        return id;
    }

    public Coords getPosition() {
        return position;
    }

    public int absHeight() {
        return getHeight() + getElevation();
    }

    public int getHeight() {
        return height;
    }

    public int getElevation() {
        return elevation;
    }

    public boolean isImmobile() {
        return true;
    }

    public String getDisplayName() {
        return name;
    }

    /**
     * Creates an id for this building based on its location as well as a 
     * building code.
     * The transformation encodes the y value in the top 5 decimal digits and
     * the x value in the bottom 5. Could more efficiently encode this by
     * partitioning the binary representation, but this is more human readable
     * and still allows for a 99999x99999 hex map.
     */
    public static int coordsToId(Coords c) {
        return Targetable.TYPE_BUILDING * 1000000 + c.y * 1000 + c.x;
    }

    // decode 1 number into 2
    public static Coords idToCoords(int id) {
        int idNoType =  id - Targetable.TYPE_BUILDING * 1000000;
        int y = (idNoType) / 1000;
        return new Coords(idNoType - (y * 1000), y);
    }

    public int sideTable(Coords src) {
        return ToHitData.SIDE_FRONT;
    }
    
    public int sideTable(Coords src, boolean usePrior) {
        return sideTable(src);
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Targetable#isOffBoard()
     */
    public boolean isOffBoard() {
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see megamek.common.Targetable#isAirborne()
     */
    public boolean isAirborne() {
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see megamek.common.Targetable#isAirborneVTOLorWIGE()
     */
    public boolean isAirborneVTOLorWIGE() {
        return false;
    }
    
    public int getAltitude() {
        return 0;
    }
}
