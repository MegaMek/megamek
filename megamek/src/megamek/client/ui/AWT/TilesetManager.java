/*
 * MegaMek - Copyright (C) 2002-2003 Ben Mazur (bmazur@sev.org)
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

/*
 * TilesetManager.java
 *
 * Created on April 15, 2002, 11:41 PM
 */

package megamek.client;

import com.sun.java.util.collections.*;
import java.awt.image.*;
import megamek.common.*;

import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import megamek.client.util.RotateFilter;
import megamek.client.util.TintFilter;

/**
 * Handles loading and manipulating images from both the mech tileset and the
 * terrain tileset.
 *
 * @author  Ben
 * @version 
 */
public class TilesetManager {
    // component to load images to
    private Component comp;
    
    // keep tracking of loading images
    private MediaTracker tracker;
    private boolean started = false;
    private boolean loaded = false;
    
    
    // mech images
    private MechTileset mechTileset = new MechTileset();
    private ArrayList mechImageList = new ArrayList();
    private HashMap mechImages = new HashMap();
    
    // hex images
    private HexTileset hexTileset = new HexTileset();
    

    /** Creates new TilesetManager */
    public TilesetManager(Component comp) {
        this.comp = comp;
        this.tracker = new MediaTracker(comp);
        
        
        
        mechTileset.loadFromFile("mechset.txt");
        hexTileset.loadFromFile(Settings.mapTileset);

    }
    
    /**
     * Return the image for the entity
     */
    public Image imageFor(Entity entity) {
        EntityImage entityImage = (EntityImage)mechImages.get(new Integer(entity.getId()));
        if (entityImage == null) {
            // probably double_blind.  Try to load on the fly
            System.out.println("Loading image for " + entity.getShortName() + " on the fly.");
            loadImage(entity);
            entityImage = (EntityImage)mechImages.get(new Integer(entity.getId()));
            if (entityImage == null) {
                // now it's a real problem
                System.out.println("Unable to load image for entity: " + entity.getShortName());
            }            
        }
  // Not every entity has a secondary facing.
  if ( entity.canChangeSecondaryFacing() ) {
        return entityImage.getFacing(entity.getSecondaryFacing());
    }
  return entityImage.getFacing(entity.getFacing());
    }
    
    /**
     * Return the base image for the hex
     */
    public Image baseFor(Hex hex) {
        if (hex.getBase() == null) {
            hexTileset.assignMatch(hex, comp);
        }
        return hex.getBase();
    }
    
    /**
     * Return a list of superimposed images for the hex
     */
    public List supersFor(Hex hex) {
        return hex.getSupers();
    }
    
    
    
    /**
     * @returns true if we're in the process of loading some images
     */
    public boolean isStarted() {
        return started;
    }
    
    /**
     * @returns true if we'return done loading images
     */
    public boolean isLoaded() {
        if (!loaded) {
            loaded = tracker.checkAll(true);
        }
        return started && loaded;
    }
    
    /**
     * Load all the images we'll need for the game and place them in the tracker
     */
    public void loadNeededImages(Game game) {
        loaded = false;
        
        // pre-match all hexes with images, load hex images
        for (int y = 0; y < game.board.height; y++) {
            for (int x = 0; x < game.board.width; x++) {
                Hex hex = game.board.getHex(x, y);
                hexTileset.assignMatch(hex, comp);
                hexTileset.trackHexImages(hex, tracker);
            }
        }
        
        // load all mech images
        for (java.util.Enumeration i = game.getEntities(); i.hasMoreElements();) {
            loadImage((Entity)i.nextElement());
        }
        
        started = true;
    }
    
    /**
     * Loads all the hex tileset images
     */
    public void loadAllHexes() {
        hexTileset.loadAllImages(comp, tracker);
    }
    
    /**
     * Load a single entity image
     */
    public void loadImage(Entity entity)
    {
            Image base = mechTileset.imageFor(entity, comp);
            int tint = entity.getOwner().getColorRGB();
            String camo = entity.getOwner().getCamoFileName();
            EntityImage entityImage = null;
            
            // check if we have a duplicate image already loaded
            for (Iterator j = mechImageList.iterator(); j.hasNext();) {
                EntityImage onList = (EntityImage)j.next();
                if (onList.getBase() == base && onList.tint == tint) {
                    entityImage = onList;
                    break;
                }
            }
            
            // if we don't have a cached image, make a new one
            if (entityImage == null) {
                entityImage = new EntityImage(base, tint, camo, comp);
                mechImageList.add(entityImage);
                entityImage.loadFacings();
                for (int j = 0; j < 6; j++) {
                    tracker.addImage(entityImage.getFacing(j), 1);
                }
            }
            
            // relate this id to this image set
            mechImages.put(new Integer(entity.getId()), entityImage);
    }
    
    /**
     * Resets the started and loaded flags
     */
    public void reset() {
        loaded = false;
        started = false;
    }
    
    
    /**
     * A class to handle the image permutations for an entity
     */
    private class EntityImage {
        private Image base;
        private int tint;
        private String camo;
        private Image[] facings = new Image[6];
        private Component comp;
        
        public static final int IMG_WIDTH = 84;        
        public static final int IMG_HEIGHT = 72;
        public static final int IMG_SIZE = IMG_WIDTH * IMG_HEIGHT;
        
        public EntityImage(Image base, int tint, String camo, Component comp) {
            this.base = base;
            this.tint = tint;
            this.camo = camo;
            this.comp = comp;
        }

        public void loadFacings() {
          if ( (null == camo) || Player.NO_CAMO.equals(camo) ) {
            for (int i = 0; i < 6; i++) {
                ImageProducer rotSource = new FilteredImageSource(base.getSource(), new RotateFilter((Math.PI / 3) * (6 - i)));
                facings[i] = comp.createImage(new FilteredImageSource(rotSource, new TintFilter(tint)));
            }
          } else {
            applyColor();
            
            for (int i = 0; i < 6; i++) {
                ImageProducer rotSource = new FilteredImageSource(base.getSource(), new RotateFilter((Math.PI / 3) * (6 - i)));
                facings[i] = comp.createImage(rotSource);
            }
          }
        }
        
        public Image getFacing(int facing) {
            return facings[facing];
        }
        
        public Image getBase() {
            return base;
        }
        
        private void applyColor() {
          Image iMech, iCamo;
          
          iMech = base;
    
          int[] pMech = new int[IMG_SIZE];
          int[] pCamo = new int[IMG_SIZE];
          PixelGrabber pgMech = new PixelGrabber(iMech, 0, 0, IMG_WIDTH, IMG_HEIGHT, pMech, 0, IMG_WIDTH);
    
          try {
              pgMech.grabPixels();
          } catch (InterruptedException e) {
              System.err.println("EntityImage.applyColor(): Failed to grab pixels for mech image." + e.getMessage());
              return;
          }
          if ((pgMech.getStatus() & ImageObserver.ABORT) != 0) {
              System.err.println("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted.");
              return;
          }
          
          iCamo = comp.getToolkit().getImage("data/camo/" + camo + ".jpg");
          PixelGrabber pgCamo = new PixelGrabber(iCamo, 0, 0, IMG_WIDTH, IMG_HEIGHT, pCamo, 0, IMG_WIDTH);
          try {
              pgCamo.grabPixels();
          } catch (InterruptedException e) {
              System.err.println("EntityImage.applyColor(): Failed to grab pixels for camo image." + e.getMessage());
              return;
          }
          if ((pgCamo.getStatus() & ImageObserver.ABORT) != 0) {
              System.err.println("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted.");
              return;
          }
          
          for (int i = 0; i < IMG_SIZE; i++) {
            int pixel = pMech[i];
            int alpha = (pixel >> 24) & 0xff;
          
            if (alpha != 0) {
              int pixel1 = pCamo[i];
              float red1   = ((float) ((pixel1 >> 16) & 0xff)) / 255;
              float green1 = ((float) ((pixel1 >>  8) & 0xff)) / 255;
              float blue1  = ((float) ((pixel1      ) & 0xff)) / 255;
              
              float black = (float) ((pMech[i]) & 0xff);

              int red2   = (int)Math.round(red1   * black);
              int green2 = (int)Math.round(green1 * black);
              int blue2  = (int)Math.round(blue1  * black);

              pMech[i] = (alpha << 24) | (red2 << 16) | (green2 << 8) | blue2;
            }
          }
        
          base = comp.createImage(new MemoryImageSource(IMG_WIDTH, IMG_HEIGHT, pMech, 0, IMG_WIDTH));        
        }
    }

}
