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
 * This class represents a single, targetable hex of a building.  The building
 * itself may occupy multiple hexex.
 *
 * @author  Suvarov454@sourceforge.net (James A. Damour )
 * @version $Revision$
 */
public class BuildingTarget implements Targetable {

    /**
     * The coordinates of the hex being targeted.
     */
    private Coords      position = null;

    /**
     * Flag that indicates an attempt to ignite the building.
     */
    private boolean     isIgnite  = false;

    /**
     * The ID of the building being targeted.
     */
    private int         id = Building.UNKNOWN;

    /**
     * The height of the building at the targeted position.
     */
    private int         height = Building.UNKNOWN;

    /**
     * The elevation of the building at the targeted position.
     */
    private int         elevation = Building.UNKNOWN;

    /**
     * The name of this hex of the building.
     */
    private String      name = null;

    /**
     * Initialize this object from the input.
     *
     * @param   coords - the <code>Coords</code> of the hext being targeted.
     * @param   board - the game's <code>Board</code> object.
     * @param   ignite - a <code>boolean</code> flag that indicates whether
     *          the player is attempting to set the building on fire, or not.
     * @exception an <code>IllegalArgumentException</code> will be thrown if
     *          the given coordinates do not contain a building.
     */
    protected void init( Coords coords, IBoard board, boolean ignite ) {
        this.position = coords;
        this.isIgnite = ignite;

        // Get the building at the given coordinates.
        Building bldg = board.getBuildingAt( this.position );
        if ( bldg == null ) {
            throw new IllegalArgumentException( "The coordinates, " +
                                                this.position.getBoardNum() +
                                                ", do not contain a building."
                                                );
        }

        // Save the building's ID.
        this.id = coordsToId( coords );

        // Generate a name.
        StringBuffer buff = new StringBuffer();
        buff.append( "Hex " )
            .append( this.position.getBoardNum() )
            .append( " of " )
            .append( bldg.getName() );
        if ( this.isIgnite ) {
            buff.append( " (Ignite)" );
        } else {
            buff.append( " (Collapse)" );
        }
        this.name = buff.toString();

        // Get the height of the hex.
        // Note, this doesn't equal "ceiling()" for
        // one Level high buildings in a woods hex.
        IHex targetHex = board.getHex( this.position );
        this.elevation = targetHex.getElevation();
        this.height = targetHex.terrainLevel( Terrains.BLDG_ELEV );
    }


    /**
     * Target a single hex of a building.
     *
     * @param   coords - the <code>Coords</code> of the hext being targeted.
     * @param   board - the game's <code>Board</code> object.
     * @param   type - an <code>int</code> value that indicates whether
     *          the player is attempting to set the building on fire, or not.
     * @exception an <code>IllegalArgumentException</code> will be thrown if
     *          the given coordinates do not contain a building.
     */
    public BuildingTarget( Coords coords, IBoard board, int nType ) {
        boolean ignite = (nType == Targetable.TYPE_BLDG_IGNITE);
        this.init( coords, board, ignite );
    }

    /**
     * Target a single hex of a building.
     *
     * @param   coords - the <code>Coords</code> of the hext being targeted.
     * @param   board - the game's <code>Board</code> object.
     * @param   ignite - a <code>boolean</code> flag that indicates whether
     *          the player is attempting to set the building on fire, or not.
     * @exception an <code>IllegalArgumentException</code> will be thrown if
     *          the given coordinates do not contain a building.
     */
    public BuildingTarget( Coords coords, IBoard board, boolean ignite ) {
        this.init( coords, board, ignite );
    }

    // Implementation of Targetable

    public int getTargetType() {
        int retval = Targetable.TYPE_BUILDING;
        if ( this.isIgnite ) {
            retval = Targetable.TYPE_BLDG_IGNITE;
        }
        return retval;
    }

    public int getTargetId() {
        return this.id;
    }

    public Coords getPosition() {
        return this.position;
    }

    public int absHeight() {
        return getHeight() + getElevation();
    }

    public int getHeight() {
        return this.height;
    }

    public int getElevation() {
        return this.elevation;
    }

    public boolean isImmobile() {
        return true;
    }

    public String getDisplayName() {
        return this.name;
    }
    /**
     * The transformation encodes the y value in the top 5 decimal digits and
     * the x value in the bottom 5.  Could more efficiently encode this by
     * partitioning the binary representation, but this is more human readable
     * and still allows for a 99999x99999 hex map.
     */
     
    // encode 2 numbers into 1
    public static int coordsToId(Coords c) {
        return c.y * 100000 + c.x;
    }
    
    // decode 1 number into 2
    public static Coords idToCoords(int id) {
        int y = id / 100000;
        return new Coords(id - (y * 100000), y);
    }    

}
