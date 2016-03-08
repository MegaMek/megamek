/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2015 Nicholas Walczak (walczak@cs.umn.edu)
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
package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.util.ArrayList;

import megamek.common.Messages;
import megamek.common.annotations.Nullable;

/**
 * A class that contains state information that specifies a skin.
 * 
 * @author arlith
 *
 */
public class SkinSpecification {

    public static enum UIComponents {
        DefaultUIElement("defaultElement"),
        DefaultButton("defaultButton"),
        BoardView("BoardViewBorder"),
        PhaseDisplay("PhaseDisplayBorder"),
        PhaseDisplayDoneButton("PhaseDisplayDoneButton"),
        PhaseDisplayButton("PhaseDisplayButton"),
        ChatLounge("ChatLoungeBorder"),
        ChatLoungeDoneButton("ChatLoungeDoneButton"),
        UnitDisplay("UnitDisplay"),
        ;

        private final String comp;

        /**
         * Constructions a new UIComponent with the given internalcomponent name
         * @param comp The internal name of this component
         */
        private UIComponents(final String comp) {
            this.comp = comp;
        }

        /**
         * Returns a string representation of this component, which is just the
         * intenral name.
         *
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return getName();
        }
        
        public String getComp() {
            return comp;
        }

        /**
         * Returns an internationalized name for this component.
         * @return
         */
        public String getName() {
            return Messages.getString("SkinSpec." + getComp() + ".Text");
        }

        /**
         * Returns an internationalized description for this component.
         * @return
         */
        public String getDescription() {
            return Messages.getString("SkinSpec." + getComp() + ".Desc");
        }
        
        /**
         * Given a component name, return it's UIComponents instance or null
         * if no match is found.
         * 
         * @param val
         * @return
         */
        @Nullable
        public static UIComponents getUIComponent(@Nullable String val) {
            for (UIComponents c : UIComponents.values()) {
                if (c.getComp().equals(val)) {
                    return c;
                }
            }
            return null;
        }
    };
    
    /**
     * Indicates the maximum number of colors a skin specification can have.
     * No UI elements will use more than this many colors, so there is no reason
     * to allow more than this, and it prevents the editor UI from expanding out
     * of control.
     */
    public static final int MAX_NUM_COLORS = 3;

    /**
     * Path to an image to be used in the top left corner.
     */
    public String tl_corner;
    
    /**
     * Path to an image to be used in the top right corner.
     */
    public String tr_corner;
    
    /**
     * Path to an image to be used in the bottom left corner.
     */
    public String bl_corner;
    
    /**
     * Path to an image to be used in the bottom right corner.
     */
    public String br_corner;
    
    
    /**
     * Path to images to be used along the top edge.
     */
    public ArrayList<String> topEdge;
    /**
     * Has an entry for each image for the top edge that determines whether
     * that image should be tiled or just drawn once.
     */
    public ArrayList<Boolean> topShouldTile;
    
    /**
     * Path to images to be used along the right edge.
     */
    public ArrayList<String> rightEdge;
    /**
     * Has an entry for each image for the right edge that determines whether
     * that image should be tiled or just drawn once.
     */
    public ArrayList<Boolean> rightShouldTile;
    
    /**
     * Path to images to be used along the bottom edge.
     */
    public ArrayList<String> bottomEdge;
    /**
     * Has an entry for each image for the bottom edge that determines whether
     * that image should be tiled or just drawn once.
     */
    public ArrayList<Boolean> bottomShouldTile;
    
    /**
     * Path to images to be used along the left edge.
     */
    public ArrayList<String> leftEdge;
    /**
     * Has an entry for each image for the left edge that determines whether
     * that image should be tiled or just drawn once.
     */
    public ArrayList<Boolean> leftShouldTile;
    
    /**
     * Specifies the font color for the UI component
     */
    public ArrayList<Color> fontColors;
    
    /**
     * A collection of background images.  Most UI components only need one,
     * but some have more.  For instance, buttons have a normal background and
     * a pressed background.
     */
    public ArrayList<String> backgrounds;
    
    /**
     * It set, it indicates that no borders should be drawn.
     */
    public boolean noBorder = false;
    
    /**
     * Flag that determines whether the background image should be tiled or
     * scaled.
     */
    public boolean tileBackground = true;
    
    /**
     * Used to specify whether a component should display scrollbars,
     * particularly for the board view. 
     */
    public boolean showScrollBars = false;
    
    public SkinSpecification(){
        tl_corner = tr_corner = bl_corner = br_corner = "";
        topEdge = new ArrayList<String>();
        rightEdge = new ArrayList<String>();
        bottomEdge = new ArrayList<String>();
        leftEdge = new ArrayList<String>();
        backgrounds = new ArrayList<String>();
        topShouldTile = new ArrayList<Boolean>();
        rightShouldTile = new ArrayList<Boolean>();
        bottomShouldTile = new ArrayList<Boolean>();
        leftShouldTile = new ArrayList<Boolean>();
        fontColors = new ArrayList<>();
        fontColors.add(Color.black);
    }
    
    public boolean hasBorder() {
        // Return false if any corner doesn't exsit
        if (tl_corner.equals("") || tr_corner.equals("")
                || bl_corner.equals("") || br_corner.equals("")) {
            return false;
        }
        
        // Return false if any edge doesn't exsit
        if (topEdge.size() == 0 || rightEdge.size() == 0
                || bottomEdge.size() == 0 || leftEdge.size() == 0) {
            return false;
        }
        
        // Make sure edges don't contain empty strings
        for (String edge : topEdge) {
            if (edge.equals("")) {
                return false;
            }
        }
        
        for (String edge : rightEdge) {
            if (edge.equals("")) {
                return false;
            }
        }
        
        for (String edge : bottomEdge) {
            if (edge.equals("")) {
                return false;
            }
        }
        
        for (String edge : leftEdge) {
            if (edge.equals("")) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean hasBackgrounds() {
        if (backgrounds.size() == 0) {
            return false;
        }
        
        for (String bg : backgrounds) {
            if (bg.equals("")) {
                return false;
            }
        }
        return true;
    }


}
