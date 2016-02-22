/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006,2007,2008 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.border.EtchedBorder;

import megamek.common.Configuration;

/**
 * A Border that has an image for each corner as well as images for the line
 * inbetween each corner (an edge).  Edges can consist of multiple possible 
 * icon and each possible icon can be tiled or static.  The total amount of 
 * space taken up by tiled icons is determined by subtracting the space of the
 * static icons and then evenly distributing it amongst each tiled icon.
 * 
 * @author arlith
 *
 */
public class MegamekBorder extends EtchedBorder {

    // Abbreviations: tl = top left, tr = top right, 
    //  bl = bottom left, br = bottom right
    protected ImageIcon tlCorner, trCorner, blCorner, brCorner;
    protected ArrayList<ImageIcon> leftLine, topLine, rightLine, bottomLine;
    // We need to know whether each tile in each edge should be tiled or static
    public ArrayList<Boolean> leftShouldTile,topShouldTile;
    public ArrayList<Boolean> rightShouldTile,bottomShouldTile;
    // Keep track of the total number of space taken up by static (non-tiled)
    //  icons for each edge
    protected int leftStaticSpace, topStaticSpace;
    protected int rightStaticSpace, bottomStaticSpace;
    // Keep track of the number of tiled icons we have in each edge
    protected int leftNumTiledIcons, topNumTiledIcons;
    protected int rightNumTiledIcons, bottomNumTiledIcons;
    
    boolean iconsLoaded =  false;    
    
    /**
     * Flag that determines whether a border should be drawn or not.
     */
    boolean noBorder = false;
    
    protected Insets insets;
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public MegamekBorder(){
        super();
        initialize(SkinXMLHandler
                .getSkin(SkinSpecification.UIComponents.DefaultUIElement
                        .getComp()));
    }
    
    public MegamekBorder(SkinSpecification spec){
        super();
        initialize(spec);
    }
    
    public MegamekBorder(String component){
        super();
        initialize(SkinXMLHandler.getSkin(component));
    }
    
    private void initialize(SkinSpecification skinSpec) {
        noBorder = skinSpec.noBorder;
        // Only load icons if we are displaying a border
        if (!noBorder) {
            loadIcons(skinSpec);
        }        
    }
    
    private ImageIcon loadIcon(String path) throws MalformedURLException {
        ImageIcon icon;
        java.net.URI imgURL;
        File file;

        file = new File(Configuration.widgetsDir(), path);
        imgURL = file.toURI();
        icon = new ImageIcon(imgURL.toURL());
        if (!file.exists()){
            System.err.println("MegaMekBorder Error: icon doesn't exist: "
                    + file.getAbsolutePath());
            iconsLoaded = false;
        }
        return icon;
    }
    
    
    /**
     * Use the given skin specificaton to create ImageIcons for each of the 
     * files specified in the skin specification.
     * 
     * @param skin  The skin specification that specifies which icons should be 
     *                 used where
     */
    public void loadIcons(SkinSpecification skin){
        // Assume they're loaded until something fails
        iconsLoaded = true;
        // If none of the icons are loaded, treat this is a regular JButton
        if (!skin.hasBorder()) {
            iconsLoaded = false;
            insets = new Insets(5, 5, 5, 5);
            return;
        }
        try {
            leftStaticSpace = rightStaticSpace = 0;
            topStaticSpace = bottomStaticSpace = 0;
            leftNumTiledIcons = rightNumTiledIcons = 0;
            topNumTiledIcons = bottomNumTiledIcons = 0;
            
            java.net.URI imgURL;
            File file;
            
            // Create Corner Icons
            tlCorner = loadIcon(skin.tl_corner);
            trCorner = loadIcon(skin.tr_corner);
            blCorner = loadIcon(skin.bl_corner);
            brCorner = loadIcon(skin.br_corner);

            // Create icons for the left edge
            leftLine = new ArrayList<ImageIcon>();
            leftShouldTile = new ArrayList<Boolean>();
            for (int i = 0; i < skin.leftEdge.size(); i++){
                file = new File(Configuration.widgetsDir(),
                        skin.leftEdge.get(i));
                imgURL = file.toURI();
                if (!file.exists()){
                    System.err.println(
                            "MegaMekBorder Error: icon doesn't exist: "
                            + file.getAbsolutePath());
                    iconsLoaded = false;
                }
                leftLine.add(new ImageIcon(imgURL.toURL()));
                leftShouldTile.add(skin.leftShouldTile.get(i));
                if (!leftShouldTile.get(i)){
                    leftStaticSpace += leftLine.get(i).getIconHeight();
                } else {
                    leftNumTiledIcons++;
                }
            }
            
            // Create icons for the right edge
            rightLine = new ArrayList<ImageIcon>();
            rightShouldTile = new ArrayList<Boolean>();
            for (int i = 0; i < skin.rightEdge.size(); i++){
                file = new File(Configuration.widgetsDir(),
                        skin.rightEdge.get(i));
                imgURL = file.toURI();
                if (!file.exists()){
                    System.err.println(
                            "MegaMekBorder Error: icon doesn't exist: "
                            + file.getAbsolutePath());
                    iconsLoaded = false;
                }
                rightLine.add(new ImageIcon(imgURL.toURL()));
                rightShouldTile.add(skin.rightShouldTile.get(i));
                if (!rightShouldTile.get(i)){
                    rightStaticSpace += rightLine.get(i).getIconHeight();
                } else {
                    rightNumTiledIcons++;
                }
            }
            
            // Create icons for the top edge
            topLine = new ArrayList<ImageIcon>();
            topShouldTile = new ArrayList<Boolean>();
            for (int i = 0; i < skin.topEdge.size(); i++){
                file = new File(Configuration.widgetsDir(),
                        skin.topEdge.get(i));
                imgURL = file.toURI();
                if (!file.exists()){
                    System.err.println(
                            "MegaMekBorder Error: icon doesn't exist: "
                            + file.getAbsolutePath());
                    iconsLoaded = false;
                }
                topLine.add(new ImageIcon(imgURL.toURL()));
                topShouldTile.add(skin.topShouldTile.get(i));
                if (!topShouldTile.get(i)){
                    topStaticSpace += topLine.get(i).getIconWidth();
                } else {
                    topNumTiledIcons++;
                }
            }
            
            // Create icons for the bottom edge
            bottomLine = new ArrayList<ImageIcon>();
            bottomShouldTile = new ArrayList<Boolean>();
            for (int i = 0; i < skin.bottomEdge.size(); i++){
                file = new File(Configuration.widgetsDir(),
                        skin.bottomEdge.get(i));
                imgURL = file.toURI();
                if (!file.exists()){
                    System.err.println(
                            "MegaMekBorder Error: icon doesn't exist: "
                            + file.getAbsolutePath());
                    iconsLoaded = false;
                }
                bottomLine.add(new ImageIcon(imgURL.toURL()));
                bottomShouldTile.add(skin.bottomShouldTile.get(i));
                if (!bottomShouldTile.get(i)){
                    bottomStaticSpace += bottomLine.get(i).getIconWidth();
                } else {
                    bottomNumTiledIcons++;
                }
            }
            if (iconsLoaded) {
                insets = new Insets(0, 0, 0, 0);
                insets.top = Math.min(tlCorner.getIconHeight(),
                        trCorner.getIconHeight());
                for (ImageIcon icon : topLine) {
                    insets.top = Math.min(insets.top,
                            icon.getIconHeight());
                }
                insets.bottom = Math.min(blCorner.getIconHeight(),
                        brCorner.getIconHeight());
                for (ImageIcon icon : bottomLine) {
                    insets.bottom = Math.min(insets.bottom,
                            icon.getIconHeight());
                }
    
                insets.left = Math.min(tlCorner.getIconWidth(),
                        blCorner.getIconWidth());
                for (ImageIcon icon : leftLine) {
                    insets.left = Math.min(insets.left,
                            icon.getIconWidth());
                }
                insets.right = Math.min(trCorner.getIconWidth(),
                        brCorner.getIconWidth());
                for (ImageIcon icon : rightLine) {
                    insets.right = Math.min(insets.right,
                            icon.getIconWidth());
                }
            } else {
                insets = new Insets(5, 5, 5, 5);
            }
        } catch (Exception e){
            System.out.println("Error: loading icons for " +
                    "a MegamekBorder!");
            e.printStackTrace();
            iconsLoaded = false;
        }      
    }
    
    /**
     * Paints the border using the loaded corner icons and edge icons.
     */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, 
            int height) {
        // Do nothing if we don't want to draw a border
        if (noBorder) {
            return;
        }
        
        // If the icons didn't loaded, treat this as a regualar border
        if (!iconsLoaded) {
            super.paintBorder(c, g, x, y, width, height);
            return;
        }
        
        g.translate(x, y);
        
        // Draw Top Left Corner Icon
        if (tlCorner.getImageLoadStatus() == MediaTracker.COMPLETE){
            paintCorner(c, g, 0, 0, tlCorner);
        }
        
        // Draw Bottom Left Corner Icon
        if (blCorner.getImageLoadStatus() == MediaTracker.COMPLETE){
            paintCorner(c, g, 0, height - blCorner.getIconHeight(), blCorner);
        }
        
        // Draw Top Right Corner Icon
        if (trCorner.getImageLoadStatus() == MediaTracker.COMPLETE){
            paintCorner(c, g, width-trCorner.getIconWidth(), 0, trCorner);
        }
        
        // Draw Bottom Right Corner Icon
        if (brCorner.getImageLoadStatus() == MediaTracker.COMPLETE){
        paintCorner(c, g, width-brCorner.getIconWidth(), 
                height-brCorner.getIconHeight(), brCorner);
        }
        
        // Compute the width and height for the border edges       
        int edgeWidth = width - (insets.left + insets.right);
        int edgeHeight = height - (insets.top + insets.bottom);
        
        // Paint top edge icons
        paintEdge(c, g, topLine, insets.left, 0, edgeWidth, insets.top, false,
                topShouldTile, topNumTiledIcons, topStaticSpace);
        
        // Paint bottom edge icons
        paintEdge(c, g, bottomLine, insets.left, height - insets.bottom,
                edgeWidth, insets.bottom, false, bottomShouldTile,
                bottomNumTiledIcons, bottomStaticSpace);

        // Paint left edge icons
        paintEdge(c, g, leftLine, 0, insets.top, insets.left, edgeHeight, true,
                leftShouldTile, leftNumTiledIcons, leftStaticSpace);
        
        // Paint right edge icons
        paintEdge(c, g, rightLine, width - insets.right, insets.top,
                insets.right, edgeHeight, true, rightShouldTile,
                rightNumTiledIcons, rightStaticSpace);
    
        g.translate(-x, -y);
    }
    
    private void paintCorner(Component c, Graphics g, int x, int y, 
            ImageIcon icon) {
        
        int tileW = icon.getIconWidth();
        int tileH = icon.getIconHeight();
        g = g.create(x, y, x+tileW, y+tileH);
        icon.paintIcon(c,g,0,0);
        g.dispose();        
    }
    
    /**
     * Paints an edge for the border given a list of icons to paint.  We need
     * to know whether each icon should be tiled, how many tiled icons there 
     * are and how much space (width/height) needs to be filled by tiled icons.
     * 
     * @param c  The Component to pain on
     * @param g  The Graphics object to paint with 
     * @param isLeftRight Are we drawing a left or right edge?
     * @param icons The ImageIcons to draw
     * @param shouldTile  Denotes whether each icon should be tiled or not
     * @param numTiledIcons The number of tiled icons we have to draw with
     * @param staticSpace How much space needs to be filled with tiledi cons
     */
    private void paintEdge(Component c, Graphics g, ArrayList<ImageIcon> icons, 
            int x, int y, int width, int height, boolean isLeftRight,
            ArrayList<Boolean> shouldTile, int numTiledIcons, int staticSpace){       
        g = g.create(x, y, width, height);
        
        // Determine how much width/height a tiled icons will get to consume
        int tiledWidth = isLeftRight ? width :
                (int)((width - staticSpace + 0.0) / numTiledIcons + 0.5);
        int tiledHeight = isLeftRight ? (int) ((height - staticSpace + 0.0)
                / numTiledIcons + 0.5) : height;
        
        x = 0; 
        y = 0;
        
        // Draw each icon
        for (int i = 0; i < icons.size(); i++){
            ImageIcon icon = icons.get(i);
            if (icon.getImageLoadStatus() != MediaTracker.COMPLETE){
                return;
            }
            if (shouldTile.get(i)){
                // Tile icons that should be tiled
                paintTiledIcon(c,g,icon,x,y,tiledWidth,tiledHeight);
                if (isLeftRight){
                    y += tiledHeight;
                } else {
                    x += tiledWidth;
                }
            } else {
                // Draw static icons once
                icons.get(i).paintIcon(c, g, x, y);
                if (isLeftRight){
                    y+= icon.getIconHeight();
                } else {
                    x+= icon.getIconWidth();
                }
            }
        }
        g.dispose();
    }
    
    /**
     * Paints a tiled icon.
     * 
     * @param c            The Component to paint onto
     * @param g            The Graphics to paint with
     * @param icon        The icon to paint
     * @param sX        The starting x location to paint the icon at
     * @param sY        The starting y location to paint the icon at
     * @param width     The width of the space that needs to be filled with 
     *                     the tiled icon
     * @param height    The height of the space that needs to be filled with 
     *                     the tiled icon
     */
    private void paintTiledIcon(Component c, Graphics g, ImageIcon icon, 
            int sX, int sY, int width, int height){
        int tileW = icon.getIconWidth();
        int tileH = icon.getIconHeight();
        width += sX;
        height += sY;
        for (int x = sX; x <= width; x += tileW) {
            for (int y = sY; y <= height; y += tileH) {
                icon.paintIcon(c, g, x, y);
            }
        }
    }
    
    public Insets getBorderInsets(Component c, Insets insets) {
        if (noBorder) {
            return new Insets(0,0,0,0);
        } else {
            return computeInsets(insets);
        }
    }
    
    private Insets computeInsets(Insets i) {
        return (Insets)(insets.clone());
    }
    
    public boolean isBorderOpaque() {
        return true;
    }

}
