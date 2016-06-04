/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import megamek.common.event.BoardListener;

/**
 */
public interface IBoard {

    /**
     * @return Map width in hexes
     */
    public abstract int getWidth();

    /**
     * @return Map height in hexes
     */
    public abstract int getHeight();

    Coords getCenter();

    /**
     * Creates a new data set for the board, with the specified dimensions and
     * data; notifies listeners that a new data set has been created.
     *
     * @param width
     *            the width dimension.
     * @param height
     *            the height dimension.
     * @param data
     *            new hex data appropriate for the board.
     */
    public abstract void newData(int width, int height, IHex[] data);

    /**
     * Creates a new data set for the board, with the specified dimensions;
     * notifies listeners that a new data set has been created.
     *
     * @param width
     *            the width dimension.
     * @param height
     *            the height dimension.
     */
    public abstract void newData(int width, int height);

    /**
     * Determines if this Board contains the (x, y) Coords, and if so, returns
     * the Hex at that position.
     *
     * @param x
     *            the x Coords.
     * @param y
     *            the y Coords.
     * @return the <code>Hex</code>, if this Board contains the (x, y) location;
     *         <code>null</code> otherwise.
     */
    public abstract IHex getHex(int x, int y);

    /**
     * Gets the hex in the specified direction from the specified starting
     * coordinates.
     *
     * @param c
     *            starting coordinates
     * @param dir
     *            direction
     * @return the hex in the specified direction from the specified starting
     *         coordinates.
     */
    public abstract IHex getHexInDir(Coords c, int dir);

    public abstract Enumeration<Coords> getHexesAtDistance(Coords coords,
            int distance);

    /**
     * Gets the hex in the specified direction from the specified starting
     * coordinates. Avoids calls to Coords.translated, and thus, object
     * construction.
     *
     * @param x
     *            starting x coordinate
     * @param y
     *            starting y coordinate
     * @param dir
     *            direction
     * @return the hex in the specified direction from the specified starting
     *         coordinates.
     */
    public abstract IHex getHexInDir(int x, int y, int dir);

    /**
     * Initialize a hex and the hexes around it
     */
    public abstract void initializeAround(int x, int y);

    /**
     * Determines whether this Board "contains" the specified Coords.
     *
     * @param x
     *            the x Coords.
     * @param y
     *            the y Coords.
     * @return <code>true</code> if the board contains the specified coords
     */
    public abstract boolean contains(int x, int y);

    /**
     * Determines whether this Board "contains" the specified Coords.
     *
     * @param c
     *            the Coords.
     * @return <code>true</code> if the board contains the specified coords
     */
    public abstract boolean contains(Coords c);

    /**
     * Returns the Hex at the specified Coords.
     *
     * @param c
     *            the Coords.
     */
    public abstract IHex getHex(Coords c);

    /**
     * Determines if this Board contains the (x, y) Coords, and if so, sets the
     * specified Hex into that position and initializes it.
     *
     * @param x
     *            the x Coords.
     * @param y
     *            the y Coords.
     * @param hex
     *            the hex to be set into position.
     */
    public abstract void setHex(int x, int y, IHex hex);

    /**
     * Similar to the setHex function for a collection of coordinates and hexes.
     * For each coord/hex pair in the supplied collections, this method
     * determines if the Board contains the coords and if so updates the
     * specified hex into that position and intializes it.
     * <p/>
     * The method ensures that each hex that needs to be updated is only updated
     * once.
     *
     * @param coords
     *            A list of coordinates to be updated
     * @param hexes
     *            The hex to be updated for each coordinate
     * @see setHex
     */
    public abstract void setHexes(List<Coords> coords, List<IHex> hexes);

    /**
     * Sets the hex into the location specified by the Coords.
     *
     * @param c
     *            the Coords.
     * @param hex
     *            the hex to be set into position.
     */
    public abstract void setHex(Coords c, IHex hex);

    /**
     * Adds the specified board listener to receive board events from this
     * board.
     *
     * @param listener
     *            the board listener.
     */
    public abstract void addBoardListener(BoardListener listener);

    /**
     * Removes the specified board listener.
     *
     * @param listener
     *            the board listener.
     */
    public abstract void removeBoardListener(BoardListener listener);

    /**
     * Can the player deploy an entity here? There are no canon rules for the
     * deployment phase (?!). I'm using 3 hexes from map edge.
     */
    public abstract boolean isLegalDeployment(Coords c, int nDir);

    /**
     * Record that the given coordinates have recieved a hit from an inferno.
     *
     * @param coords
     *            - the <code>Coords</code> of the hit.
     * @param round
     *            - the kind of round that hit the hex.
     * @param hits
     *            - the <code>int</code> number of rounds that hit. If a
     *            negative number is passed, then an
     *            <code>IllegalArgumentException</code> will be thrown.
     */
    public abstract void addInfernoTo(Coords coords,
            InfernoTracker.Inferno round, int hits);

    /**
     * Extinguish inferno at the target hex.
     *
     * @param coords
     *            - the <code>Coords</code> of the hit.
     */
    public abstract void removeInfernoFrom(Coords coords);

    /**
     * Determine if the given coordinates has a burning inferno.
     *
     * @param coords
     *            - the <code>Coords</code> being checked.
     * @return <code>true</code> if those coordinates have a burning inferno
     *         round. <code>false</code> if no inferno has hit those coordinates
     *         or if it has burned out.
     */
    public abstract boolean isInfernoBurning(Coords coords);

    /**
     * Record that a new round of burning has passed for the given coordinates.
     * This routine also determines if the fire is still burning.
     *
     * @param coords
     *            - the <code>Coords</code> being checked.
     * @return <code>true</code> if those coordinates have a burning inferno
     *         round. <code>false</code> if no inferno has hit those coordinates
     *         or if it has burned out.
     */
    public abstract boolean burnInferno(Coords coords);

    /**
     * Get an enumeration of all coordinates with infernos still burning.
     *
     * @return an <code>Enumeration</code> of <code>Coords</code> that have
     *         infernos still burning.
     */
    public abstract Enumeration<Coords> getInfernoBurningCoords();

    /**
     * returns a hash of the inferno trackers
     *
     * @return an <code>Hashtable</code of <code>InfernoTrackers</code> on the
     *         board.
     */
    public abstract Hashtable<Coords, InfernoTracker> getInfernos();

    /**
     * Determine the remaining number of turns the given coordinates will have a
     * burning inferno.
     *
     * @param coords
     *            - the <code>Coords</code> being checked. This value must not
     *            be <code>null</code>. Unchecked.
     * @return the <code>int</code> number of burn turns left for all infernos
     *         This value will be non-negative.
     */
    public abstract int getInfernoBurnTurns(Coords coords);

    /**
     * Determine the remaining number of turns the given coordinates will have a
     * burning Inferno IV round.
     *
     * @param coords
     *            - the <code>Coords</code> being checked. This value must not
     *            be <code>null</code>. Unchecked.
     * @return the <code>int</code> number of burn turns left for Arrow IV
     *         infernos. This value will be non-negative.
     */
    public abstract int getInfernoIVBurnTurns(Coords coords);

    /**
     * This returns special events that should be makred on hexes, such as
     * artilery fire.
     */
    public abstract Collection<SpecialHexDisplay> getSpecialHexDisplay(
            Coords coords);

    public abstract void addSpecialHexDisplay(Coords coords,
            SpecialHexDisplay shd);

    public abstract void removeSpecialHexDisplay(Coords coords,
            SpecialHexDisplay shd);

    public abstract void setSpecialHexDisplayTable(
            Hashtable<Coords, Collection<SpecialHexDisplay>> shd);

    public abstract Hashtable<Coords, Collection<SpecialHexDisplay>> getSpecialHexDisplayTable();

    /**
     * Get an enumeration of all buildings on the board.
     *
     * @return an <code>Enumeration</code> of <code>Building</code>s.
     */
    public abstract Enumeration<Building> getBuildings();

    /**
     * Get the building at the given coordinates.
     *
     * @param coords
     *            - the <code>Coords</code> being examined.
     * @return a <code>Building</code> object, if there is one at the given
     *         coordinates, otherwise a <code>null</code> will be returned.
     */
    public abstract Building getBuildingAt(Coords coords);

    /**
     * Collapse an array of buildings.
     *
     * @param bldgs
     *            - the <code>Vector</code> of <code>Building</code> objects to
     *            be collapsed.
     */
    public abstract void collapseBuilding(Vector<Coords> bldgs);

    /**
     * The given building has collapsed. Remove it from the board and replace it
     * with rubble.
     *
     * @param bldg
     *            - the <code>Building</code> that has collapsed.
     */
    public abstract void collapseBuilding(Building bldg);

    /**
     * The given building hex has collapsed. Remove it from the board and
     * replace it with rubble.
     *
     * @param coords
     *            - the <code>Building</code> that has collapsed.
     */
    public abstract void collapseBuilding(Coords coords);

    /**
     * Update the construction factors on an array of buildings.
     *
     * @param bldgs
     *            - the <code>Vector</code> of <code>Building</code> objects to
     *            be updated.
     */
    public abstract void updateBuildings(Vector<Building> bldgs);

    /**
     * Get the current value of the "road auto-exit" option.
     *
     * @return <code>true</code> if roads should automatically exit onto all
     *         adjacent pavement hexes. <code>false</code> otherwise.
     */
    public abstract boolean getRoadsAutoExit();

    /**
     * Set the value of the "road auto-exit" option.
     *
     * @param value
     *            - The value to set for the option; <code>true</code> if roads
     *            should automatically exit onto all adjacent pavement hexes.
     *            <code>false</code> otherwise.
     */
    public abstract void setRoadsAutoExit(boolean value);

    /**
     * Set the CF of bridges
     *
     * @param value
     *            - The value to set the bridge CF to.
     */
    public abstract void setBridgeCF(int value);

    public abstract void setRandomBasementsOff();

    public abstract void setType(int t);

    public abstract int getType();

    public abstract boolean onGround();

    public abstract boolean inAtmosphere();

    public abstract boolean inSpace();

    /**
     * Load board data from a file in the boards data directory.
     *
     * @param filename
     *            The path to the file, relative to the board data directory.
     * @deprecated Use {@link #load(File)} instead.
     */
    @Deprecated
    public abstract void load(final String filename);

    /**
     * Load board data from a file.
     *
     * @param filepath
     *            The path to the file.
     */
    public abstract void load(final File filepath);

    /**
     * Loads this board from an InputStream
     *
     * @param is
     *            input stream
     */
    public abstract void load(InputStream is);

    /**
     * Writes data for the board, as text to the OutputStream
     *
     * @param os
     *            output stream
     */
    public abstract void save(OutputStream os);

    /**
     * Returns the lowest elevation hex on the board.
     *
     * @return
     */
    public abstract int getMinElevation();

    /**
     * Returns the highest elevation hex on the board.
     *
     * @return
     */
    public abstract int getMaxElevation();

    /**
     * Resets the Min and Max elevations to their default values.
     */
    public abstract void resetStoredElevation();

    boolean containsBridges();

    /**
     * Returns the list of background images associated with this board. If
     * created from a single board file, then the list should only have one
     * element. Multiple elements exist when the board is created by combinging
     * multiple board files.
     *
     * @return
     */
    public abstract List<String> getBackgroundPaths();

    /**
     * Returns the first element of the background path list, or null if it is
     * empty.
     *
     * @return
     */
    public abstract String getBackgroundPath();

    /**
     * Returns the number of boards in width that were used to created this
     * board. Only used when background paths are set.
     *
     * @return
     */
    public abstract int getNumBoardsWidth();

    /**
     * Returns the number of boards in height that were used to created this
     * board. Only used when background paths are set.
     *
     * @return
     */
    public abstract int getNumBoardsHeight();

    /**
     * Flag that determines if the board background image should be flipped
     * horizontally. Only used when background paths are set.
     *
     * @return
     */
    public abstract List<Boolean> getFlipBGHoriz();

    /**
     * Flag that determines if the board background image should be flipped
     * vertically. Only used when background paths are set.
     *
     * @return
     */
    public abstract List<Boolean> getFlipBGVert();

    public int getSubBoardWidth();

    public int getSubBoardHeight();

    public void setSubBoardWidth(int width);

    public void setSubBoardHeight(int height);

    public abstract void setNumBoardsWidth(int width);

    public abstract void setNumBoardsHeight(int height);

    public abstract void addBackgroundPath(String path, boolean flipVert,
            boolean flipHorz);

    public abstract boolean hasBoardBackground();
}