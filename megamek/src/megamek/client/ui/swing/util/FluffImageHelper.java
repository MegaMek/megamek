/*
 * MechSelectorDialog.java - Copyright (C) 2009 Jay Lawson
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

package megamek.client.ui.swing.util;

import java.awt.Image;
import java.io.File;

import javax.swing.ImageIcon;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Tank;

/**
 * 
 * @author Jay Lawson
 * Looks for a fluff image for an entity based on model and chassis
 * Heavily based on code from MegaMekLab#ImageHelper
 */
public class FluffImageHelper {
    
    public static String fluffPath = "data/images/fluff/";

    public static String imageMech = "mech";
    public static String imageAero = "aero";
    public static String imageBA = "BattleArmor";
    public static String imageVehicle = "vehicle";

    public static Image getFluffImage(Entity unit) {
        String dir = imageMech;
        if(unit instanceof Aero) {
            dir = imageAero;
        } 
        else if(unit instanceof BattleArmor) {
            dir = imageBA;
        }
        else if(unit instanceof Tank) {
            dir = imageVehicle;
        }
        
        
        Image fluff = null;

        String path = new File(fluffPath).getAbsolutePath() + File.separatorChar + dir + File.separatorChar;
        fluff = getFluffPNG(unit, path);

        if (fluff == null) {
            fluff = getFluffJPG(unit, path);
        }

        if (fluff == null) {
            fluff = getFluffGIF(unit, path);
        }
        return fluff;
    }

    public static Image getFluffPNG(Entity unit, String path) {
        Image fluff = null;

        String fluffFile = path + unit.getChassis() + " " + unit.getModel() + ".png";
        if (new File(fluffFile).isFile()) {
            fluff = new ImageIcon(fluffFile).getImage();
        }

        if (fluff == null) {
            fluffFile = path + unit.getModel() + ".png";
            if (new File(fluffFile).isFile()) {
                fluff = new ImageIcon(fluffFile).getImage();
            }
        }

        if (fluff == null) {
            fluffFile = path + unit.getChassis() + ".png";
            if (new File(fluffFile).isFile()) {
                fluff = new ImageIcon(fluffFile).getImage();
            }
        }

        return fluff;
    }

    public static Image getFluffJPG(Entity unit, String path) {
        Image fluff = null;

        String fluffFile = path + unit.getChassis() + " " + unit.getModel() + ".jpg";
        if (new File(fluffFile).isFile()) {
            fluff = new ImageIcon(fluffFile).getImage();
        }

        if (fluff == null) {
            fluffFile = path + unit.getModel() + ".jpg";
            if (new File(fluffFile).isFile()) {
                fluff = new ImageIcon(fluffFile).getImage();
            }
        }

        if (fluff == null) {
            fluffFile = path + unit.getChassis() + ".jpg";
            if (new File(fluffFile).isFile()) {
                fluff = new ImageIcon(fluffFile).getImage();
            }
        }

        return fluff;
    }

    public static Image getFluffGIF(Entity unit, String path) {
        Image fluff = null;

        String fluffFile = path + unit.getChassis() + " " + unit.getModel() + ".gif";
        if (new File(fluffFile).isFile()) {
            fluff = new ImageIcon(fluffFile).getImage();
        }

        if (fluff == null) {
            fluffFile = path + unit.getModel() + ".gif";
            if (new File(fluffFile).isFile()) {
                fluff = new ImageIcon(fluffFile).getImage();
            }
        }

        if (fluff == null) {
            fluffFile = path + unit.getChassis() + ".gif";
            if (new File(fluffFile).isFile()) {
                fluff = new ImageIcon(fluffFile).getImage();
            }
        }

        return fluff;
    }
}