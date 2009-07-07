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

package megamek.client.ui.swing;

import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import megamek.client.ui.ITilesetManager;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.client.ui.swing.util.RotateFilter;
import megamek.common.Entity;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Mech;
import megamek.common.Minefield;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.DirectoryItems;

/**
 * Handles loading and manipulating images from both the mech tileset and the
 * terrain tileset.
 * 
 * @author Ben
 */
public class TilesetManager implements IPreferenceChangeListener, ITilesetManager {
    // component to load images to
    private Component comp;

    // keep tracking of loading images
    private MediaTracker tracker;
    private boolean started = false;
    private boolean loaded = false;

    // keep track of camo images
    private DirectoryItems camos;

    // mech images
    private MechTileset mechTileset = new MechTileset("data/images/units/"); //$NON-NLS-1$
    private MechTileset wreckTileset = new MechTileset(
            "data/images/units/wrecks/"); //$NON-NLS-1$
    private ArrayList<EntityImage> mechImageList = new ArrayList<EntityImage>();
    private HashMap<Integer, EntityImage> mechImages = new HashMap<Integer, EntityImage>();

    // hex images
    private HexTileset hexTileset = new HexTileset();

    private Image minefieldSign;
    private Image nightFog;
    private Image artilleryAutohit;
    private Image artilleryAdjusted;
    private Image artilleryIncoming;
    private HashMap<Integer, Image> ecmShades = new HashMap<Integer, Image>();
    private static final String NIGHT_IMAGE_FILE = "data/images/hexes/transparent/night.png";
    private static final String ARTILLERY_AUTOHIT_IMAGE_FILE = "data/images/hexes/artyauto.gif";
    private static final String ARTILLERY_ADJUSTED_IMAGE_FILE = "data/images/hexes/artyadj.gif";
    private static final String ARTILLERY_INCOMING_IMAGE_FILE = "data/images/hexes/artyinc.gif";

    public static final int ARTILLERY_AUTOHIT = 0;
    public static final int ARTILLERY_ADJUSTED = 1;
    public static final int ARTILLERY_INCOMING = 2;

    /**
     * Creates new TilesetManager
     */
    public TilesetManager(Component comp) throws IOException {
        this.comp = comp;
        tracker = new MediaTracker(comp);
        try {
            camos = new DirectoryItems(new File("data/images/camo"), "", //$NON-NLS-1$ //$NON-NLS-2$
                    ImageFileFactory.getInstance());
        } catch (Exception e) {
            camos = null;
        }
        mechTileset.loadFromFile("mechset.txt"); //$NON-NLS-1$
        wreckTileset.loadFromFile("wreckset.txt"); //$NON-NLS-1$
        hexTileset.loadFromFile(PreferenceManager.getClientPreferences()
                .getMapTileset());
        PreferenceManager.getClientPreferences().addPreferenceChangeListener(
                this);
    }

    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(IClientPreferences.MAP_TILESET)) {
            HexTileset hts = new HexTileset();
            try {
                hts.loadFromFile((String) e.getNewValue());
                hexTileset = hts;
            } catch (IOException ex) {
                return;
            }
        }
    }

    public Image iconFor(Entity entity) {
        EntityImage entityImage = mechImages.get(new Integer(entity.getId()));
        if (entityImage == null) {
            // probably double_blind. Try to load on the fly
            System.out
                    .println("Loading image for " + entity.getShortNameRaw() + " on the fly."); //$NON-NLS-1$ //$NON-NLS-2$
            loadImage(entity);
            entityImage = mechImages.get(new Integer(entity.getId()));
            if (entityImage == null) {
                // now it's a real problem
                System.out
                        .println("Unable to load image for entity: " + entity.getShortNameRaw()); //$NON-NLS-1$
                return null;
            }
        }
        return entityImage.getIcon();
    }

    public Image wreckMarkerFor(Entity entity) {
        EntityImage entityImage = mechImages.get(new Integer(entity.getId()));
        if (entityImage == null) {
            // probably double_blind. Try to load on the fly
            System.out
                    .println("Loading image for " + entity.getShortNameRaw() + " on the fly."); //$NON-NLS-1$ //$NON-NLS-2$
            loadImage(entity);
            entityImage = mechImages.get(new Integer(entity.getId()));
            if (entityImage == null) {
                // now it's a real problem
                System.out
                        .println("Unable to load image for entity: " + entity.getShortNameRaw()); //$NON-NLS-1$
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
        if (entity instanceof Mech || entity instanceof Protomech) {
            return imageFor(entity, entity.getSecondaryFacing());
        }
        return imageFor(entity, entity.getFacing());
    }

    public Image imageFor(Entity entity, int facing) {
        EntityImage entityImage = mechImages.get(new Integer(entity.getId()));
        if (entityImage == null) {
            // probably double_blind. Try to load on the fly
            System.out
                    .println("Loading image for " + entity.getShortNameRaw() + " on the fly."); //$NON-NLS-1$ //$NON-NLS-2$
            loadImage(entity);
            entityImage = mechImages.get(new Integer(entity.getId()));
            if (entityImage == null) {
                // now it's a real problem
                System.out
                        .println("Unable to load image for entity: " + entity.getShortNameRaw()); //$NON-NLS-1$
                return null;
            }
        }
        // get image rotated for facing
        return entityImage.getFacing(facing);
    }

    /**
     * Return the base image for the hex
     */
    public Image baseFor(IHex hex) {
        return hexTileset.getBase(hex, comp);
    }

    /**
     * Return a list of superimposed images for the hex
     */
    public List<Image> supersFor(IHex hex) {
        return hexTileset.getSupers(hex, comp);
    }

    public Image getMinefieldSign() {
        return minefieldSign;
    }

    public Image getNightFog() {
        return nightFog;
    }

    public Image getEcmShade(int tint) {
        Image image = ecmShades.get(new Integer(tint));
        if (image == null) {
            Image iMech;

            iMech = nightFog;

            int[] pMech = new int[EntityImage.IMG_SIZE];
            PixelGrabber pgMech = new PixelGrabber(iMech, 0, 0,
                    EntityImage.IMG_WIDTH, EntityImage.IMG_HEIGHT, pMech, 0,
                    EntityImage.IMG_WIDTH);

            try {
                pgMech.grabPixels();
            } catch (InterruptedException e) {
                System.err
                        .println("EntityImage.applyColor(): Failed to grab pixels for mech image." + e.getMessage()); //$NON-NLS-1$
                return image;
            }
            if ((pgMech.getStatus() & ImageObserver.ABORT) != 0) {
                System.err
                        .println("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted."); //$NON-NLS-1$
                return image;
            }

            for (int i = 0; i < EntityImage.IMG_SIZE; i++) {
                int pixel = pMech[i];
                int alpha = (pixel >> 24) & 0xff;

                if (alpha != 0) {
                    int pixel1 = tint & 0xffffff;
                    pMech[i] = (alpha << 24) | pixel1;
                }
            }

            image = comp.createImage(new MemoryImageSource(
                    EntityImage.IMG_WIDTH, EntityImage.IMG_HEIGHT, pMech, 0,
                    EntityImage.IMG_WIDTH));
            ecmShades.put(new Integer(tint), image);
        }
        return image;
    }

    public Image getArtilleryTarget(int which) {
        switch (which) {
            case ARTILLERY_AUTOHIT:
                return artilleryAutohit;
            case ARTILLERY_ADJUSTED:
                return artilleryAdjusted;
            case ARTILLERY_INCOMING:
            default:
                return artilleryIncoming;
        }
    }

    /**
     * @return true if we're in the process of loading some images
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * @return true if we're done loading images
     */
    public synchronized boolean isLoaded() {
        if (!loaded) {
            loaded = tracker.checkAll(true);
        }
        return started && loaded;
    }

    /**
     * Load all the images we'll need for the game and place them in the tracker
     */
    public void loadNeededImages(IGame game) {
        loaded = false;
        IBoard board = game.getBoard();
        // pre-match all hexes with images, load hex images
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                IHex hex = board.getHex(x, y);
                loadHexImage(hex);
            }
        }

        // load all mech images
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            loadImage(i.nextElement());
        }

        // load minefield sign
        minefieldSign = comp.getToolkit().getImage(Minefield.IMAGE_FILE);

        // load night overlay
        nightFog = comp.getToolkit().getImage(NIGHT_IMAGE_FILE);

        // load artillery targets
        artilleryAutohit = comp.getToolkit().getImage(
                ARTILLERY_AUTOHIT_IMAGE_FILE);
        artilleryAdjusted = comp.getToolkit().getImage(
                ARTILLERY_ADJUSTED_IMAGE_FILE);
        artilleryIncoming = comp.getToolkit().getImage(
                ARTILLERY_INCOMING_IMAGE_FILE);

        started = true;
    }

    /**
     * Loads the image(s) for this hex into the tracker.
     * 
     * @param hex the hex to load
     */
    private synchronized void loadHexImage(IHex hex) {
        hexTileset.assignMatch(hex, comp);
        hexTileset.trackHexImages(hex, tracker);
    }

    /**
     * Removes the hex images from the cache.
     * 
     * @param hex
     */
    public void clearHex(IHex hex) {
        hexTileset.clearHex(hex);
    }

    /**
     * Waits until a certain hex's images are done loading.
     * 
     * @param hex the hex to wait for
     */
    public synchronized void waitForHex(IHex hex) {
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
    public synchronized void loadAllHexes() {
        hexTileset.loadAllImages(comp, tracker);
    }

    /**
     *  Loads a preview image of the unit into the BufferedPanel.
     */
    public Image loadPreviewImage(Entity entity, Image camo, int tint, Component bp) {
        Image base = mechTileset.imageFor(entity, comp);
        EntityImage entityImage = new EntityImage(base, tint, camo, bp);
        Image preview = entityImage.loadPreviewImage();

        MediaTracker loadTracker = new MediaTracker(comp);
        loadTracker.addImage(preview, 0);
        try {
            loadTracker.waitForID(0);
        } catch (InterruptedException e) {
            // should never come here

        }
        
        return preview;
    }

    /**
     * Get the camo pattern for the given player.
     * 
     * @param player - the <code>Player</code> whose camo pattern is needed.
     * @return The <code>Image</code> of the player's camo pattern. This value
     *         will be <code>null</code> if the player has selected no camo
     *         pattern or if there was an error loading it.
     */
    public Image getPlayerCamo(Player player) {

        // Return a null if the player has selected no camo file.
        if (null == player.getCamoCategory()
                || Player.NO_CAMO.equals(player.getCamoCategory())) {
            return null;
        }

        // Try to get the player's camo file.
        Image camo = null;
        try {

            // Translate the root camo directory name.
            String category = player.getCamoCategory();
            if (Player.ROOT_CAMO.equals(category))
                category = ""; //$NON-NLS-1$
            camo = (Image) camos.getItem(category, player.getCamoFileName());

        } catch (Exception err) {
            err.printStackTrace();
        }
        return camo;
    }

    /**
     * Load a single entity image
     */
    public synchronized void loadImage(Entity entity) {
        Image base = mechTileset.imageFor(entity, comp);
        Image wreck = wreckTileset.imageFor(entity, comp);

        Player player = entity.getOwner();
        int tint = PlayerColors.getColorRGB(player.getColorIndex());

        Image camo = getPlayerCamo(player);
        EntityImage entityImage = null;

        // check if we have a duplicate image already loaded
        for (Iterator<EntityImage> j = mechImageList.iterator(); j.hasNext();) {
            EntityImage onList = j.next();
            if (onList.getBase().equals(base) && onList.tint == tint) {
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
    public synchronized void reset() {
        loaded = false;
        started = false;

        tracker = new MediaTracker(comp);
        hexTileset.reset();
    }

    /**
     * A class to handle the image permutations for an entity
     */
    private class EntityImage {
        private Image base;
        private Image wreck;
        private Image icon;
        int tint;
        private Image camo;
        private Image[] facings = new Image[6];
        private Image[] wreckFacings = new Image[6];
        private Component parent;

        private static final int IMG_WIDTH = 84;
        private static final int IMG_HEIGHT = 72;
        private static final int IMG_SIZE = IMG_WIDTH * IMG_HEIGHT;

        public EntityImage(Image base, int tint, Image camo, Component comp) {
            this(base, null, tint, camo, comp);
        }

        public EntityImage(Image base, Image wreck, int tint, Image camo,
                Component comp) {
            this.base = base;
            this.tint = tint;
            this.camo = camo;
            this.parent = comp;
            this.wreck = wreck;
        }

        public void loadFacings() {
            base = applyColor(base);

            icon = base.getScaledInstance(56, 48, Image.SCALE_SMOOTH);
            for (int i = 0; i < 6; i++) {
                ImageProducer rotSource = new FilteredImageSource(base
                        .getSource(), new RotateFilter((Math.PI / 3) * (6 - i)));
                facings[i] = parent.createImage(rotSource);
            }

            if (wreck != null) {
                wreck = applyColor(wreck);
                for (int i = 0; i < 6; i++) {
                    ImageProducer rotSource = new FilteredImageSource(wreck
                            .getSource(), new RotateFilter((Math.PI / 3)
                            * (6 - i)));
                    wreckFacings[i] = parent.createImage(rotSource);
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
            PixelGrabber pgMech = new PixelGrabber(iMech, 0, 0, IMG_WIDTH,
                    IMG_HEIGHT, pMech, 0, IMG_WIDTH);

            try {
                pgMech.grabPixels();
            } catch (InterruptedException e) {
                System.err
                        .println("EntityImage.applyColor(): Failed to grab pixels for mech image." + e.getMessage()); //$NON-NLS-1$
                return image;
            }
            if ((pgMech.getStatus() & ImageObserver.ABORT) != 0) {
                System.err
                        .println("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted."); //$NON-NLS-1$
                return image;
            }

            if (useCamo) {
                PixelGrabber pgCamo = new PixelGrabber(camo, 0, 0, IMG_WIDTH,
                        IMG_HEIGHT, pCamo, 0, IMG_WIDTH);
                try {
                    pgCamo.grabPixels();
                } catch (InterruptedException e) {
                    System.err
                            .println("EntityImage.applyColor(): Failed to grab pixels for camo image." + e.getMessage()); //$NON-NLS-1$
                    return image;
                }
                if ((pgCamo.getStatus() & ImageObserver.ABORT) != 0) {
                    System.err
                            .println("EntityImage.applyColor(): Failed to grab pixels for mech image. ImageObserver aborted."); //$NON-NLS-1$
                    return image;
                }
            }

            for (int i = 0; i < IMG_SIZE; i++) {
                int pixel = pMech[i];
                int alpha = (pixel >> 24) & 0xff;

                if (alpha != 0) {
                    int pixel1 = useCamo ? pCamo[i] : tint;
                    float red1 = ((float) ((pixel1 >> 16) & 0xff)) / 255;
                    float green1 = ((float) ((pixel1 >> 8) & 0xff)) / 255;
                    float blue1 = ((float) ((pixel1) & 0xff)) / 255;

                    float black = ((pMech[i]) & 0xff);

                    int red2 = Math.round(red1 * black);
                    int green2 = Math.round(green1 * black);
                    int blue2 = Math.round(blue1 * black);

                    pMech[i] = (alpha << 24) | (red2 << 16) | (green2 << 8)
                            | blue2;
                }
            }

            image = parent.createImage(new MemoryImageSource(IMG_WIDTH,
                    IMG_HEIGHT, pMech, 0, IMG_WIDTH));
            return image;
        }
    }

}
