/*
 * MegaMek - Copyright (C) 2002,2003,2004 Ben Mazur (bmazur@sev.org)
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
import java.io.File;
import megamek.client.util.ImageFileFactory;
import megamek.client.util.RotateFilter;
import megamek.client.util.TintFilter;
import megamek.client.util.widget.BufferedPanel;
import megamek.client.util.widget.BackGroundDrawer;
import megamek.common.util.DirectoryItems;

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

    // keep track of camo images
    private DirectoryItems camos;
    
    // mech images
    private MechTileset mechTileset = new MechTileset("data/mex/");
    private MechTileset wreckTileset = new MechTileset("data/mex/wrecks/");
    private ArrayList mechImageList = new ArrayList();
    private HashMap mechImages = new HashMap();
    
    // hex images
    private HexTileset hexTileset = new HexTileset();
    
	private Image minefieldSign;    

    /** Creates new TilesetManager */
    public TilesetManager(Component comp) {
        this.comp = comp;
        this.tracker = new MediaTracker(comp);
        camos = new DirectoryItems( new File("data/camo"), "",
                                    ImageFileFactory.getInstance() );

        mechTileset.loadFromFile("mechset.txt");
        wreckTileset.loadFromFile("wreckset.txt");
        hexTileset.loadFromFile(Settings.mapTileset);

    }
    
    public Image iconFor(Entity entity) {
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
        return entityImage.getIcon();
    }

    public Image wreckMarkerFor(Entity entity) {
        EntityImage entityImage = (EntityImage)mechImages.get(new Integer(entity.getId()));
        if (entityImage == null) {
            // probably double_blind.  Try to load on the fly
            System.out.println("Loading image for " + entity.getShortName() + " on the fly.");
            loadImage(entity);
            entityImage = (EntityImage)mechImages.get(new Integer(entity.getId()));
            if (entityImage == null) {
                // now it's a real problem
                System.out.println("Unable to load image for entity: " + entity.getShortName());
                return null;
            }            
        }
        return entityImage.getWreckFacing(entity.getFacing());
    }
    /**
     * Return the image for the entity
     */
    public Image imageFor(Entity entity) {
        // mechs look like they're facing their secondary facing
        if (entity instanceof Mech) {
	    	return imageFor(entity, entity.getSecondaryFacing());
        } else {
	    	return imageFor(entity, entity.getFacing());
	    }
    }

    public Image imageFor(Entity entity, int facing) {
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
        // get image rotated for facing
        return entityImage.getFacing(facing);
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
    
    public Image getMinefieldSign() {
        return minefieldSign;
    }    
    
    /**
     * @returns true if we're in the process of loading some images
     */
    public boolean isStarted() {
        return started;
    }
    
    /**
     * @returns true if we're done loading images
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
                loadHexImage(hex);
            }
        }
        
        // load all mech images
        for (java.util.Enumeration i = game.getEntities(); i.hasMoreElements();) {
            loadImage((Entity)i.nextElement());
        }
        
        // load minefield sign
		minefieldSign = comp.getToolkit().getImage(Minefield.IMAGE_FILE);

        started = true;
    }
    
    /**
     * Loads the image(s) for this hex into the tracker.
     * @param hex the hex to load
     */
    private void loadHexImage(Hex hex) {
		hexTileset.assignMatch(hex, comp);
		hexTileset.trackHexImages(hex, tracker);
    }
    
    /**
     * Waits until a certain hex's images are done loading.
     * @param hex the hex to wait for
     */
    public void waitForHex(Hex hex) {
		loadHexImage(hex);
        try {
            tracker.waitForID(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Loads all the hex tileset images
     */
    public void loadAllHexes() {
        hexTileset.loadAllImages(comp, tracker);
    }
    
    // Loads a preview image of the unit into the BufferedPanel.
    public void loadPreviewImage(Entity entity, Image camo, int tint, BufferedPanel bp) {
		Image base = mechTileset.imageFor(entity, comp);
		EntityImage entityImage = new EntityImage(base, tint, camo, bp);
		Image preview = entityImage.loadPreviewImage();
		
		BackGroundDrawer bgdPreview = new BackGroundDrawer(preview);
		bp.removeBgDrawers();
		bp.addBgDrawer(bgdPreview);

		MediaTracker tracker = new MediaTracker(comp);
		tracker.addImage(preview, 0);
		try {
			tracker.waitForID(0);
		} catch (InterruptedException e) {
			;
		}

    }

    /**
     * Get the camo pattern for the given player.
     *
     * @param   player - the <code>Player</code> whose camo pattern is needed.
     * @return  The <code>Image</code> of the player's camo pattern.
     *          This value will be <code>null</code> if the player has selected
     *          no camo pattern or if there was an error loading it.
     */
    public Image getPlayerCamo( Player player ) {

        // Return a null if the player has selected no camo file.
        if ( null == player.getCamoCategory() ||
             Player.NO_CAMO.equals( player.getCamoCategory() ) ) {
            return null;
        }

        // Try to get the player's camo file.
        Image camo = null;
        try {

            // Translate the root camo directory name.
            String category = player.getCamoCategory();
            if ( Player.ROOT_CAMO.equals( category ) ) category = "";
            camo = (Image) camos.getItem( category, player.getCamoFileName() );

        } catch ( Exception err ) {
            err.printStackTrace();
        }
        return camo;
    }

    /**
     * Load a single entity image
     */
    public void loadImage(Entity entity)
    {
        Image base = mechTileset.imageFor(entity, comp);
        Image wreck = null;
        if ( !(entity instanceof Infantry) &&
             !(entity instanceof Protomech) ) {
            wreck = wreckTileset.imageFor(entity, comp);
        }
        Player player = entity.getOwner();
        int tint = player.getColorRGB();

        Image camo = getPlayerCamo( player );
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
            entityImage = new EntityImage(base, wreck, tint, camo, comp);
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
        private Image wreck;
        private Image icon;
        private int tint;
        private Image camo;
        private Image[] facings = new Image[6];
        private Image[] wreckFacings = new Image[6];
        private Component comp;
        
        private final int IMG_WIDTH = 84;        
        private final int IMG_HEIGHT = 72;
        private final int IMG_SIZE = IMG_WIDTH * IMG_HEIGHT;
        
        public EntityImage(Image base, int tint, Image camo, Component comp) {
        	this(base, null, tint, camo, comp);
        }

        public EntityImage(Image base, Image wreck, int tint, Image camo, Component comp) {
            this.base = base;
            this.tint = tint;
            this.camo = camo;
            this.comp = comp;
            this.wreck = wreck;
        }

        public void loadFacings() {
            base = applyColor(base);
            
            icon = base.getScaledInstance(56, 48, Image.SCALE_SMOOTH);
            for (int i = 0; i < 6; i++) {
                ImageProducer rotSource = new FilteredImageSource(base.getSource(), new RotateFilter((Math.PI / 3) * (6 - i)));
                facings[i] = comp.createImage(rotSource);
            }

            if (wreck != null) {
            	wreck = applyColor(wreck);
                for (int i = 0; i < 6; i++) {
                    ImageProducer rotSource = new FilteredImageSource(wreck.getSource(), new RotateFilter((Math.PI / 3) * (6 - i)));
                    wreckFacings[i] = comp.createImage(rotSource);
                }
            }
        }
        
        public Image loadPreviewImage() {
			base = applyColor(base);
            return base;
        }

        public Image getFacing(int facing) {
            return facings[facing];
        }
        
        public Image getWreckFacing(int facing) {
            return wreckFacings[facing];
        }
        
        public Image getBase() {
            return base;
        }
        
        public Image getIcon() {
            return icon;
        }

        private Image applyColor(Image image) {
          Image iMech;
          boolean useCamo = (camo != null);
          
          iMech = image;
    
          int[] pMech = new int[IMG_SIZE];
          int[] pCamo = new int[IMG_SIZE];
          PixelGrabber pgMech = new PixelGrabber(iMech, 0, 0, IMG_WIDTH, IMG_HEIGHT, pMech, 0, IMG_WIDTH);
    
          try {
              pgMech.grabPixels();
          } catch (InterruptedException e) {
              System.err.println("EntityImage.applyColor(): Failed to grab pixels for mech image." + e.getMessage());
              return image;
          }
          if ((pgMech.getStatus() & ImageObserver.ABORT) != 0) {
              System.err.println("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted.");
              return image;
          }
          
          if (useCamo) {
	          PixelGrabber pgCamo = new PixelGrabber(camo, 0, 0, IMG_WIDTH, IMG_HEIGHT, pCamo, 0, IMG_WIDTH);
	          try {
	              pgCamo.grabPixels();
	          } catch (InterruptedException e) {
	              System.err.println("EntityImage.applyColor(): Failed to grab pixels for camo image." + e.getMessage());
	              return image;
	          }
	          if ((pgCamo.getStatus() & ImageObserver.ABORT) != 0) {
	              System.err.println("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted.");
	              return image;
	          }
	      }
	      
          for (int i = 0; i < IMG_SIZE; i++) {
            int pixel = pMech[i];
            int alpha = (pixel >> 24) & 0xff;
          
            if (alpha != 0) {
              int pixel1 = useCamo ? pCamo[i] : tint;
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
        
          image = comp.createImage(new MemoryImageSource(IMG_WIDTH, IMG_HEIGHT, pMech, 0, IMG_WIDTH));
          return image;
        }
    }

}
