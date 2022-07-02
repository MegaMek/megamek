/*
 * MechSelectorDialog.java - Copyright (C) 2009 Jay Lawson
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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
package megamek.client.ui.swing.util;

import megamek.common.*;
import megamek.common.util.fileUtils.MegaMekFile;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Looks for a fluff image for an entity based on model and chassis.
 *
 * Heavily based on code from MegaMekLab's ImageHelper.
 * @author Jay Lawson
 */
public class FluffImageHelper {
    public static final String DIR_NAME_BA = "BattleArmor";
    public static final String DIR_NAME_CONVFIGHTER = "ConvFighter";
    public static final String DIR_NAME_DROPSHIP = "DropShip";
    public static final String DIR_NAME_FIGHTER = "Fighter";
    public static final String DIR_NAME_INFANTRY = "Infantry";
    public static final String DIR_NAME_JUMPSHIP = "JumpShip";
    public static final String DIR_NAME_MECH = "Mech";
    public static final String DIR_NAME_PROTOMEK= "ProtoMek";
    public static final String DIR_NAME_SMALLCRAFT = "Small Craft";
    public static final String DIR_NAME_SPACESTATION = "Space Station";
    public static final String DIR_NAME_VEHICLE = "Vehicle";
    public static final String DIR_NAME_WARSHIP = "WarShip";
    public static final String[] EXTENSIONS_FLUFF_IMAGE_FORMATS = { ".png", ".jpg", ".gif", ".PNG", ".JPG", ".GIF" };

    /**
     * Get the fluff image for the specified unit, if available.
     * 
     * @param unit The unit.
     * @return An image file, if one is available, else {@code null}.
     */
    public static Image getFluffImage(final Entity unit) {
        Image fluff = loadFluffImage(unit);

        if (fluff == null) {
            fluff = loadFluffImageHeuristic(unit);
        }

        return fluff;
    }

    /**
     * Attempt to load the fluff image specified in the Entity data.
     * 
     * @param unit
     *            The unit.
     * @return An image or {@code null}.
     */
    protected static Image loadFluffImage(final Entity unit) {
        Image fluff = null;
        String path = unit.getFluff().getMMLImagePath();
        if (new File(path).isFile()) {
            fluff = new ImageIcon(path).getImage();
        }
        return fluff;
    }

    /**
     * Attempt to load a fluff image by combining elements of type and name.
     * 
     * @param unit
     *            The unit.
     * @return An image or {@code null}.
     */
    protected static Image loadFluffImageHeuristic(final Entity unit) {
        Image fluff = null;

        String dir = DIR_NAME_MECH;
        
        if (unit instanceof Warship) {
        	dir = DIR_NAME_WARSHIP;
    	} else if (unit instanceof SpaceStation) {
    		dir = DIR_NAME_SPACESTATION;
    	} else if (unit instanceof Jumpship) {
    		dir = DIR_NAME_JUMPSHIP;
    	} else if (unit instanceof ConvFighter) {
    		dir = DIR_NAME_CONVFIGHTER;          
    	} else if (unit instanceof Dropship) {
            dir = DIR_NAME_DROPSHIP;
    	} else if (unit instanceof SmallCraft) {
            dir = DIR_NAME_SMALLCRAFT;            
    	} else if (unit instanceof Aero) {
            dir = DIR_NAME_FIGHTER;
    	} else if (unit instanceof BattleArmor) {
    		dir = DIR_NAME_BA;           
    	} else if (unit instanceof Infantry) {
    		dir = DIR_NAME_INFANTRY;     
    	} else if (unit instanceof Protomech) {
    		dir = DIR_NAME_PROTOMEK;    
          } else if (unit instanceof Tank) {
            dir = DIR_NAME_VEHICLE;
        }

        File fluff_image_file = findFluffImage(
                new MegaMekFile(Configuration.fluffImagesDir(), dir).getFile(), unit);
        if (fluff_image_file != null) {
            fluff = new ImageIcon(fluff_image_file.toString()).getImage();
        }

        return fluff;
    }

    /**
     * Find a fluff image file for the unit.
     * 
     * @param directory
     *            Directory to search.
     * @param unit
     *            The unit.
     * @return Path to an appropriate file or {@code null} if none is found.
     */
    protected static File findFluffImage(final File directory, final Entity unit) {
        // Search for a file in the specified directory.
        // Searches for each supported extension on each of the following
        // combinations:
        // Chassis + model
        // Model only
        // Chassis only
        // Model needs .replace("\"", "") because Windows disallows double quote
        // in the filename.
        File fluff_file = null;
        // Remove characters that will cause path problems
        String sanitizedChassis = unit.getChassis().replace("\"", "")
                .replace("/", "");
        String sanitizedModel = unit.getModel().replace("\"", "")
                .replace("/", "");
        String[] basenames = {
                new MegaMekFile(directory, sanitizedChassis + " " + sanitizedModel)
                        .toString(),
                new MegaMekFile(directory, sanitizedModel).toString(),
                new MegaMekFile(directory, sanitizedChassis).toString(), };

        for (String basename : basenames) {
            for (String extension : EXTENSIONS_FLUFF_IMAGE_FORMATS) {
                File filepath = new File(basename + extension);
                if (filepath.isFile()) {
                    fluff_file = filepath;
                    break;
                }
            }
            if (fluff_file != null) {
                break;
            }
        }
        final String model = unit.getModel().replace("\"", "");
        final String chassisModel = unit.getChassis() + " " + model;
         
        // If the previous checks failed, we're going to try to discount the
        //  CSO author name, which will make the file look like:
        //   Chassis + model + [ <author> ] + extension
        if (fluff_file == null) {
            File[] files = directory.listFiles((direc, name) -> {
                boolean extMatch = false;
                for (String ext : EXTENSIONS_FLUFF_IMAGE_FORMATS) {
                    extMatch |= name.endsWith(ext);
                }
                return name.contains(chassisModel) && extMatch;
            });

            if ((files != null) && (files.length > 0)) {
                fluff_file = files[0];
            }
        }
        
        // If we still haven't found a file, see if ignoring the model helps
        if (fluff_file == null) {
            File[] files = directory.listFiles((direc, name) -> {
                boolean extMatch = false;
                for (String ext : EXTENSIONS_FLUFF_IMAGE_FORMATS) {
                    extMatch |= name.endsWith(ext);
                }
                String chassis = name.split("\\[")[0].trim();
                return chassis.equalsIgnoreCase(unit.getChassis()) && extMatch;
            });

            if ((files != null) && (files.length > 0)) {
                fluff_file = files[0];
            }
        }
        
        return fluff_file;
    }
}
