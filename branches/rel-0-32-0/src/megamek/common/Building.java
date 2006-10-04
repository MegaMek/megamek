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

import java.util.ArrayList;
import java.util.Vector;
import java.util.Enumeration;
import java.io.Serializable;

/**
 * This class represents a single, possibly multi-hex building on the board.
 *
 * @author  Suvarov454@sourceforge.net (James A. Damour )
 * @version $Revision$
 */
public class Building implements Serializable {

    // Private attributes and helper functions.

    /**
     * The ID of this building.
     */
    private int         id              = Building.UNKNOWN;

    /**
     * The coordinates of every hex of this building.
     */
    private Vector      coordinates     = new Vector();

    /**
     * The construction type of the building.
     */
    private int         type            = Building.UNKNOWN;

   /**
     * The current construction factor of the building.
     * Any damage immediately updates this value.
     */
    private int         currentCF       = Building.UNKNOWN;

    /**
     * The construction factor of the building at the start of this
     * attack phase.  Damage that is received during the phase is
     * applied at the end of the phase.
     */
    private int         phaseCF         = Building.UNKNOWN;

    /**
     * The name of the building.
     */
    private String      name            = null;

    /**
     * Flag that indicates whether this building is burning
     */
    private boolean     burning         = false;

    public class DemolitionCharge implements Serializable {
        public int damage;
        public int playerId;

        public DemolitionCharge(int playerId, int damage) {
            this.damage = damage;
            this.playerId = playerId;
        }
    }

    private ArrayList demolitionCharges = new ArrayList();

    // Public and Protected constants, constructors, and methods.

    /**
     * Update this building to include the new hex (and all hexes off
     * the new hex, which aren't already included).
     *
     * @param   coords - the <code>Coords</code> of the new hex.
     * @param   board - the game's <code>IBoard</code> object.
     * @exception an <code>IllegalArgumentException</code> will be thrown if
     *          the given coordinates do not contain a building, or if the
     *          building covers multiple hexes with different CF.
     */
    protected void include( Coords coords, IBoard board, int structureType ) {

        // If the hex is already in the building, we've covered it before.
        if ( this.isIn( coords ) ) {
            return;
        }

        // Get the nextHex hex.
        IHex nextHex = board.getHex( coords );
        if(null == nextHex || !(nextHex.containsTerrain(structureType)))
            return;

        if(structureType == Terrains.BUILDING) {
            // Error off if the building type or CF is off.
            if ( this.type != nextHex.terrainLevel( Terrains.BUILDING ) ) {
                throw new IllegalArgumentException
                    ( "The coordinates, " +
                      coords.getBoardNum() +
                      ", should contain the same type of building as " +
                      ( (Coords) this.coordinates.elementAt(0)).getBoardNum() );
            }
            boolean hexHasCF = nextHex.containsTerrain( Terrains.BLDG_CF );
            if ( (hexHasCF && 
                  this.currentCF != nextHex.terrainLevel( Terrains.BLDG_CF )) ||
                 (!hexHasCF &&
                  this.currentCF != getDefaultCF( this.type )) ) {
                throw new IllegalArgumentException
                    ( "The coordinates, " +
                      coords.getBoardNum() +
                      ", should contain a building with the same CF as " +
                      ( (Coords) this.coordinates.elementAt(0)).getBoardNum() );
            }
        }
        // We passed our tests, add the next hex to this building.
        this.coordinates.addElement( coords );

        // Walk through the exit directions and
        // identify all hexes in this building.
        for ( int dir = 0; dir < 6; dir++ ) {

            // Does the building exit in this direction?
            if ( nextHex.containsTerrainExit( structureType, dir ) ) {
                this.include( coords.translated(dir), board, structureType );
            }

        }

    } // End void protected include( Coords, Board )

    /**
     * Generic flag for uninitialized values.
     */
    protected static final int  UNKNOWN = -1;

    /**
     * Various construction types.
     */
    public static final int     LIGHT   = 1;
    public static final int     MEDIUM  = 2;
    public static final int     HEAVY   = 3;
    public static final int     HARDENED= 4;

    /**
     * Construct a building for the given coordinates from the
     * board's information.  If the building covers multiple hexes,
     * every hex will be included in the building.
     *
     * @param   coords - the <code>Coords</code> of a hex of the building.
     *          If the building covers multiple hexes, this constructor will
     *          include them all in this building automatically.
     * @param   board - the game's <code>Board</code> object.
     * @exception an <code>IllegalArgumentException</code> will be thrown if
     *          the given coordinates do not contain a building, or if the
     *          building covers multiple hexes with different CFs.
     */
    public Building( Coords coords, IBoard board, int structureType ) {

        // The ID of the building will be the hashcode of the coords.
        // ASSUMPTION: this will be unique ID across ALL the building's
        //             hexes for ALL the clients of this board.
        this.id = coords.hashCode();

        // The building occupies the given coords, at least.
        this.coordinates.addElement( coords );

        // Get the Hex for those coords.
        IHex startHex = board.getHex( coords );

        // Read our construction type from the hex.
        if ( !startHex.containsTerrain( structureType ) ) {
            throw new IllegalArgumentException( "The coordinates, " +
                                                coords.getBoardNum() +
                                                ", do not contain a building."
                                                );
        }
        this.type = startHex.terrainLevel( structureType );

        // Insure that we've got a good type (and initialize our CF).
        this.currentCF = getDefaultCF( this.type );
        if ( this.currentCF == Building.UNKNOWN ) {
            throw new IllegalArgumentException( "Unknown construction type: " +
                                                this.type +
                                                ".  The board is invalid." );
        }

        // Now read the *real* CF, if the board specifies one.
        if ( structureType == Terrains.BUILDING && startHex.containsTerrain( Terrains.BLDG_CF ) ) {
            this.currentCF = startHex.terrainLevel( Terrains.BLDG_CF );
        }
        if ( structureType == Terrains.BRIDGE && startHex.containsTerrain( Terrains.BRIDGE_CF ) ) {
            this.currentCF = startHex.terrainLevel( Terrains.BRIDGE_CF );
        }
        this.phaseCF = this.currentCF;

        // Walk through the exit directions and
        // identify all hexes in this building.
        for ( int dir = 0; dir < 6; dir++ ) {

            // Does the building exit in this direction?
            if ( startHex.containsTerrainExit( structureType, dir ) ) {
                this.include( coords.translated(dir), board, structureType );
            }

        }

        // Set the building's name.
        StringBuffer buffer = new StringBuffer();
        if (structureType == Terrains.FUEL_TANK) {
            buffer.append("Fuel Tank #");
        } else if (structureType == Terrains.BUILDING) {
            buffer.append("Building #");
        } else if (structureType == Terrains.BRIDGE) {
            buffer.append("Bridge #");
        } else {
            buffer.append("Structure #");
        }
        buffer.append( this.id );
        this.name = buffer.toString();

    } // End public Building( Coords, Board )

    /**
     * Creates a new building of the specified type, name, ID, and
     * coordinates.  Do *not* use this method unless you have carefully
     * examined this class.  The construction factors for the building
     * will be based on the type.
     *
     * @param type      The <code>int</code> type of the building.
     * @param id        The <code>int</code> ID of this building.
     * @param name      The <code>String</code> name of this building.
     * @param coords    The <code>Vector</code> of <code>Coords<code>
     *                  for this building.  This object is used directly
     *                  without being copied.
     * @exception an <code>IllegalArgumentException</code> will be thrown if
     *          the given coordinates do not contain a building, or if the
     *          building covers multiple hexes with different CFs.
     */
    public Building( int type, int id, String name, Vector coords ) {
        this.type = type;
        this.id = id;
        this.name = name;
        this.coordinates = coords;

        // Insure that we've got a good type (and initialize our CF).
        this.currentCF = getDefaultCF( this.type );
        this.phaseCF = this.currentCF;
        if ( this.currentCF == Building.UNKNOWN ) {
            throw new IllegalArgumentException( "Invalid construction type: " +
                                                this.type + "." );
        }
    }

    /**
     * Get the ID of this building.  The same ID applies to all hexes.
     *
     * @return  the <code>int</code> ID of the building.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Determine if the building occupies given coordinates.  Multi-hex
     * buildings will occupy multiple coordinates.  Only one building per
     * hex.
     *
     * @param   coords - the <code>Coords</code> being examined.
     * @return  <code>true</code> if the building occupies the coordinates.
     *          <code>false</code> otherwise.
     */
    public boolean isIn( Coords coords ) {
        return this.coordinates.contains( coords );
    }

    /**
     * Get the coordinates that the building occupies.
     *
     * @return  an <code>Enumeration</code> of the <code>Coord</code> objects.
     */
    public Enumeration getCoords() {
        return this.coordinates.elements();
    }

    /**
     * Get the construction type of the building.  This value will be one
     * of the constants, LIGHT, MEDIUM, HEAVY, or HARDENED.
     *
     * @return  the <code>int</code> code of the building's construction type.
     */
    public int getType() {
        return this.type;
    }

    /**
     * Get the current construction factor of the building.
     * Any damage immediately updates this value.
     *
     * @return  the <code>int</code> value of the building's current
     *          construction factor.  This value will be greater than
     *          or equal to zero.
     */
    public int getCurrentCF() {
        return this.currentCF;
    }

    /**
     * Get the construction factor of the building at the start of the
     * current phase.  Damage that is received during the phase is
     * applied at the end of the phase.
     *
     * @return  the <code>int</code> value of the building's construction
     *          factor at the start of this phase.  This value will be
     *          greater than or equal to zero.
     */
    public int getPhaseCF() {
        return this.phaseCF;
    }

    /**
     * Set the current construction factor of the building.
     * Call this method immediately when the building sustains any damage.
     *
     * @param   cf - the <code>int</code> value of the building's current
     *          construction factor.  This value must be greater than
     *          or equal to zero.
     * @exception If the passed value is less than zero, an
     *          <code>IllegalArgumentException</code> is thrown.
     */
    public void setCurrentCF( int cf ) {
        if (cf < 0) {
            throw new IllegalArgumentException
                ("Invalid value for Construction Factor: " + cf);
        }

        this.currentCF = cf;
    }

    /**
     * Set the construction factor of the building for the start of the
     * next phase.  Call this method at the end of the phase to apply
     * damage sustained by the building during the phase.
     *
     * @param   cf - the <code>int</code> value of the building's current
     *          construction factor.  This value must be greater than
     *          or equal to zero.
     * @exception If the passed value is less than zero, an
     *          <code>IllegalArgumentException</code> is thrown.
     */
    public void setPhaseCF( int cf ) {
        if ( cf < 0 ) {
            throw new IllegalArgumentException
                ( "Invalid value for Construction Factor: " + cf );
        }

        this.phaseCF = cf;
    }

    /**
     * Get the name of this building.
     *
     * @return  the <code>String</code> name of this building.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the default construction factor for the given type of building.
     *
     * @param   type - the <code>int</code> construction type of the building.
     * @return  the <code>int</code> default construction factor for that type
     *          of building.  If a bad type value is passed, the constant
     *          <code>Building.UNKNOWN</code> will be returned instead.
     */
    public static int getDefaultCF( int type ) {
        int retval = Building.UNKNOWN;
        switch ( type ) {
        case Building.LIGHT    :
            retval = 15;
            break;
        case Building.MEDIUM   :
            retval = 40;
            break;
        case Building.HEAVY    :
            retval = 90;
            break;
        case Building.HARDENED :
            retval = 120;
            break;
        }
        return retval;
    }

    /**
     * Override <code>Object#equals(Object)</code>.
     *
     * @param   other - the other <code>Object</code> to compare to this
     *          <code>Building</code>.
     * @return  <code>true</code> if the other object is the same as this
     *          <code>Building</code>.  The value <code>false</code> will be
     *          returned if the other object is <code>null</code>, is not a
     *          <code>Buildig</code>, or if it is not the same as this
     *          <code>Building</code>.
     */
    public boolean equals( Object other ) {
        if ( other == null ||
             !(other instanceof Building) ) {
            return false;
        }

        // True until we're talking about more than one Board per Game.
        Building bldg = (Building) other;
        return ( this.id == bldg.id );
    }

    /**
     * Get a String for this building.
     */
    public String toString() {

        // Assemble the string in pieces.
        StringBuffer buf = new StringBuffer();

        // Add the building type to the buffer.
        switch ( this.getType() ) {
        case Building.LIGHT   : buf.append( "Light " ); break;
        case Building.MEDIUM  : buf.append( "Medium " ); break;
        case Building.HEAVY   : buf.append( "Heavy " ); break;
        case Building.HARDENED: buf.append( "Hardened " ); break;
        }

        // Add the building's name.
        buf.append( this.name );

        // Return the string.
        return buf.toString();
    }

    /**
     * Determine if this building is on fire.
     *
     * @return  <code>true</code> if the building is on fire.
     */
    public boolean isBurning() {
        return burning;
    }

    /**
     * Set the flag that indicates that this building is on fire.
     *
     * @param   onFire - a <code>boolean</code> value that indicates whether
     *          this building is on fire.
     */
    public void setBurning( boolean onFire ) {
        this.burning = onFire;
    }

    public void addDemolitionCharge( int playerId, int damage ) {
        DemolitionCharge charge = new DemolitionCharge(playerId, damage);
        demolitionCharges.add(charge);
    }
} // End public class Building implements Serializable
