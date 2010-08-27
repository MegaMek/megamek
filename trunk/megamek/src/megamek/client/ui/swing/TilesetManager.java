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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
    private HashMap<ArrayList<Integer>, EntityImage> mechImages = new HashMap<ArrayList<Integer>, EntityImage>();

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
        try {
            hexTileset.loadFromFile(PreferenceManager.getClientPreferences().getMapTileset());
        } catch (Exception FileNotFoundException) {
            if ( !new File("data/images/hexes/defaulthexset.txt").exists() ){
                createDefaultHexSet();
            }
            hexTileset.loadFromFile("defaulthexset.txt");
        }
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
        ArrayList<Integer> temp = new ArrayList<Integer>();
        temp.add(entity.getId());
        temp.add(-1);
        EntityImage entityImage = mechImages.get(temp);
        if (entityImage == null) {
            // probably double_blind. Try to load on the fly
            System.out
                    .println("Loading image for " + entity.getShortNameRaw() + " on the fly."); //$NON-NLS-1$ //$NON-NLS-2$
            loadImage(entity, -1);
            entityImage = mechImages.get(temp);
            if (entityImage == null) {
                // now it's a real problem
                System.out
                        .println("Unable to load image for entity: " + entity.getShortNameRaw()); //$NON-NLS-1$
                return null;
            }
        }
        return entityImage.getIcon();
    }

    public Image wreckMarkerFor(Entity entity, int secondaryPos) {
        ArrayList<Integer> temp = new ArrayList<Integer>();
        temp.add(entity.getId());
        temp.add(secondaryPos);
        EntityImage entityImage = mechImages.get(temp);
        if (entityImage == null) {
            // probably double_blind. Try to load on the fly
            System.out
                    .println("Loading image for " + entity.getShortNameRaw() + " on the fly."); //$NON-NLS-1$ //$NON-NLS-2$
            loadImage(entity, secondaryPos);
            entityImage = mechImages.get(temp);
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
        return imageFor(entity, -1);
    }

    public Image imageFor(Entity entity, int secondaryPos) {
        // mechs look like they're facing their secondary facing
        if ((entity instanceof Mech) || (entity instanceof Protomech)) {
            return imageFor(entity, entity.getSecondaryFacing(), secondaryPos);
        }
        return imageFor(entity, entity.getFacing(), secondaryPos);
    }

    public Image imageFor(Entity entity, int facing, int secondaryPos) {
        ArrayList<Integer> temp = new ArrayList<Integer>();
        temp.add(entity.getId());
        temp.add(secondaryPos);
        EntityImage entityImage = mechImages.get(temp);
        if (entityImage == null) {
            // probably double_blind. Try to load on the fly
            System.out
                    .println("Loading image for " + entity.getShortNameRaw() + " on the fly."); //$NON-NLS-1$ //$NON-NLS-2$
            loadImage(entity, secondaryPos);
            entityImage = mechImages.get(temp);
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
            Entity e = i.nextElement();
            loadImage(e, -1);
            for (Integer secPos : e.getSecondaryPositions().keySet()) {
                loadImage(e, secPos);
            }
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
        Image base = mechTileset.imageFor(entity, comp, -1);
        EntityImage entityImage = new EntityImage(base, tint, camo, bp);
        entityImage.loadFacings();
        Image preview = entityImage.getFacing(entity.getFacing());

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
        if ((null == player.getCamoCategory())
                || Player.NO_CAMO.equals(player.getCamoCategory())) {
            return null;
        }

        // Try to get the player's camo file.
        Image camo = null;
        try {

            // Translate the root camo directory name.
            String category = player.getCamoCategory();
            if (Player.ROOT_CAMO.equals(category)) {
                category = ""; //$NON-NLS-1$
            }
            camo = (Image) camos.getItem(category, player.getCamoFileName());

        } catch (Exception err) {
            err.printStackTrace();
        }
        return camo;
    }

    /**
     * Load a single entity image
     */
    public synchronized void loadImage(Entity entity, int secondaryPos) {
        Image base = mechTileset.imageFor(entity, comp, secondaryPos);
        Image wreck = wreckTileset.imageFor(entity, comp, secondaryPos);

        Player player = entity.getOwner();
        int tint = PlayerColors.getColorRGB(player.getColorIndex());

        Image camo = getPlayerCamo(player);
        EntityImage entityImage = null;

        // check if we have a duplicate image already loaded
        for (Iterator<EntityImage> j = mechImageList.iterator(); j.hasNext();) {
            EntityImage onList = j.next();
            if (onList.getBase().equals(base) && (onList.tint == tint)) {
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
        ArrayList<Integer> temp = new ArrayList<Integer>();
        temp.add(entity.getId());
        temp.add(secondaryPos);
        mechImages.put(temp, entityImage);
    }

    /**
     * Resets the started and loaded flags
     */
    public synchronized void reset() {
        loaded = false;
        started = false;

        tracker = new MediaTracker(comp);
        mechImageList.clear();
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
            parent = comp;
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

    private void createDefaultHexSet(){
        try {
            FileOutputStream fos = new FileOutputStream(new File("data/images/hexes/defaulthexset.txt"));
            PrintStream p = new PrintStream(fos);

            p.println("# suggested hex tileset");
            p.println("#");
            p.println("# format is:");
            p.println("# base/super <elevation> <terrains> <theme> <image>");
            p.println("#");
            p.println("");
            p.println("super * \"elevator:10\" \"\" \"boring/elevator1.gif\"");
            p.println("super * \"elevator:2\" \"\" \"boring/elevator2.gif\"");
            p.println("");
            p.println("super * \"geyser:1\" \"\" \"boring/geyservent.gif\"");
            p.println("super * \"geyser:2\" \"\" \"boring/geysererupt.gif\"");
            p.println("super * \"geyser:3\" \"\" \"boring/geyservent.gif\"");
            p.println("");
            p.println("super * \"road:1:00\" \"\" \"boring/road00.gif\"");
            p.println("super * \"road:1:01\" \"\" \"boring/road01.gif\"");
            p.println("super * \"road:1:02\" \"\" \"boring/road02.gif\"");
            p.println("super * \"road:1:03\" \"\" \"boring/road03.gif\"");
            p.println("super * \"road:1:04\" \"\" \"boring/road04.gif\"");
            p.println("super * \"road:1:05\" \"\" \"boring/road05.gif\"");
            p.println("super * \"road:1:06\" \"\" \"boring/road06.gif\"");
            p.println("super * \"road:1:07\" \"\" \"boring/road07.gif\"");
            p.println("super * \"road:1:08\" \"\" \"boring/road08.gif\"");
            p.println("super * \"road:1:09\" \"\" \"boring/road09.gif\"");
            p.println("super * \"road:1:10\" \"\" \"boring/road10.gif\"");
            p.println("super * \"road:1:11\" \"\" \"boring/road11.gif\"");
            p.println("super * \"road:1:12\" \"\" \"boring/road12.gif\"");
            p.println("super * \"road:1:13\" \"\" \"boring/road13.gif\"");
            p.println("super * \"road:1:14\" \"\" \"boring/road14.gif\"");
            p.println("super * \"road:1:15\" \"\" \"boring/road15.gif\"");
            p.println("super * \"road:1:16\" \"\" \"boring/road16.gif\"");
            p.println("super * \"road:1:17\" \"\" \"boring/road17.gif\"");
            p.println("super * \"road:1:18\" \"\" \"boring/road18.gif\"");
            p.println("super * \"road:1:19\" \"\" \"boring/road19.gif\"");
            p.println("super * \"road:1:20\" \"\" \"boring/road20.gif\"");
            p.println("super * \"road:1:21\" \"\" \"boring/road21.gif\"");
            p.println("super * \"road:1:22\" \"\" \"boring/road22.gif\"");
            p.println("super * \"road:1:23\" \"\" \"boring/road23.gif\"");
            p.println("super * \"road:1:24\" \"\" \"boring/road24.gif\"");
            p.println("super * \"road:1:25\" \"\" \"boring/road25.gif\"");
            p.println("super * \"road:1:26\" \"\" \"boring/road26.gif\"");
            p.println("super * \"road:1:27\" \"\" \"boring/road27.gif\"");
            p.println("super * \"road:1:28\" \"\" \"boring/road28.gif\"");
            p.println("super * \"road:1:29\" \"\" \"boring/road29.gif\"");
            p.println("super * \"road:1:30\" \"\" \"boring/road30.gif\"");
            p.println("super * \"road:1:31\" \"\" \"boring/road31.gif\"");
            p.println("super * \"road:1:32\" \"\" \"boring/road32.gif\"");
            p.println("super * \"road:1:33\" \"\" \"boring/road33.gif\"");
            p.println("super * \"road:1:34\" \"\" \"boring/road34.gif\"");
            p.println("super * \"road:1:35\" \"\" \"boring/road35.gif\"");
            p.println("super * \"road:1:36\" \"\" \"boring/road36.gif\"");
            p.println("super * \"road:1:37\" \"\" \"boring/road37.gif\"");
            p.println("super * \"road:1:38\" \"\" \"boring/road38.gif\"");
            p.println("super * \"road:1:39\" \"\" \"boring/road39.gif\"");
            p.println("super * \"road:1:40\" \"\" \"boring/road40.gif\"");
            p.println("super * \"road:1:41\" \"\" \"boring/road41.gif\"");
            p.println("super * \"road:1:42\" \"\" \"boring/road42.gif\"");
            p.println("super * \"road:1:43\" \"\" \"boring/road43.gif\"");
            p.println("super * \"road:1:44\" \"\" \"boring/road44.gif\"");
            p.println("super * \"road:1:45\" \"\" \"boring/road45.gif\"");
            p.println("super * \"road:1:46\" \"\" \"boring/road46.gif\"");
            p.println("super * \"road:1:47\" \"\" \"boring/road47.gif\"");
            p.println("super * \"road:1:48\" \"\" \"boring/road48.gif\"");
            p.println("super * \"road:1:49\" \"\" \"boring/road49.gif\"");
            p.println("super * \"road:1:50\" \"\" \"boring/road50.gif\"");
            p.println("super * \"road:1:51\" \"\" \"boring/road51.gif\"");
            p.println("super * \"road:1:52\" \"\" \"boring/road52.gif\"");
            p.println("super * \"road:1:53\" \"\" \"boring/road53.gif\"");
            p.println("super * \"road:1:54\" \"\" \"boring/road54.gif\"");
            p.println("super * \"road:1:55\" \"\" \"boring/road55.gif\"");
            p.println("super * \"road:1:56\" \"\" \"boring/road56.gif\"");
            p.println("super * \"road:1:57\" \"\" \"boring/road57.gif\"");
            p.println("super * \"road:1:58\" \"\" \"boring/road58.gif\"");
            p.println("super * \"road:1:59\" \"\" \"boring/road59.gif\"");
            p.println("super * \"road:1:60\" \"\" \"boring/road60.gif\"");
            p.println("super * \"road:1:61\" \"\" \"boring/road61.gif\"");
            p.println("super * \"road:1:62\" \"\" \"boring/road62.gif\"");
            p.println("super * \"road:1:63\" \"\" \"boring/road63.gif\"");
            p.println("");
            p.println("super * \"fluff:5:00\" \"\" \"fluff/cars_1.gif\"");
            p.println("super * \"fluff:5:01\" \"\" \"fluff/cars_2.gif\"");
            p.println("super * \"fluff:5:02\" \"\" \"fluff/cars_3.gif\"");
            p.println("super * \"fluff:5:03\" \"\" \"fluff/cars_4.gif\"");
            p.println("super * \"fluff:5:04\" \"\" \"fluff/cars_5.gif\"");
            p.println("super * \"fluff:5:05\" \"\" \"fluff/cars_6.gif\"");
            p.println("super * \"fluff:5:06\" \"\" \"fluff/cars_7.gif\"");
            p.println("super * \"fluff:5:07\" \"\" \"fluff/cars_8.gif\"");
            p.println("");
            p.println("super * \"fluff:4:00\" \"\" \"fluff/square1.gif\"");
            p.println("super * \"fluff:4:01\" \"\" \"fluff/square2.gif\"");
            p.println("super * \"fluff:4:02\" \"\" \"fluff/square3.gif\"");
            p.println("super * \"fluff:4:03\" \"\" \"fluff/square4.gif\"");
            p.println("super * \"fluff:4:04\" \"\" \"fluff/square5.gif\"");
            p.println("super * \"fluff:4:05\" \"\" \"fluff/square6.gif\"");
            p.println("super * \"fluff:4:06\" \"\" \"fluff/pillars1.gif\"");
            p.println("super * \"fluff:4:07\" \"\" \"fluff/pillars2.gif\"");
            p.println("super * \"fluff:4:08\" \"\" \"fluff/pillars3.gif\"");
            p.println("super * \"fluff:4:09\" \"\" \"fluff/pillars4.gif\"");
            p.println("super * \"fluff:4:10\" \"\" \"fluff/pillars5.gif\"");
            p.println("super * \"fluff:4:11\" \"\" \"fluff/pillars6.gif\"");
            p.println("");
            p.println("super * \"fluff:7:00\" \"\" \"fluff/construction1.gif\"");
            p.println("super * \"fluff:7:01\" \"\" \"fluff/construction2.gif\"");
            p.println("super * \"fluff:7:02\" \"\" \"fluff/construction3.gif\"");
            p.println("super * \"fluff:7:03\" \"\" \"fluff/suburb1.gif\"");
            p.println("super * \"fluff:7:04\" \"\" \"fluff/suburb2.gif\"");
            p.println("super * \"fluff:7:05\" \"\" \"fluff/suburb3.gif\"");
            p.println("");
            p.println("super * \"fluff:8:06\" \"\" \"fluff/garden1.gif\"");
            p.println("super * \"fluff:8:07\" \"\" \"fluff/garden2.gif\"");
            p.println("super * \"fluff:8:08\" \"\" \"fluff/garden3.gif\"");
            p.println("super * \"fluff:8:09\" \"\" \"fluff/garden4.gif\"");
            p.println("super * \"fluff:8:10\" \"\" \"fluff/garden5.gif\"");
            p.println("super * \"fluff:8:11\" \"\" \"fluff/garden6.gif\"");
            p.println("");
            p.println("super * \"fluff:9:00\" \"\" \"fluff/maglevtrack1.gif\"");
            p.println("super * \"fluff:9:01\" \"\" \"fluff/maglevtrack2.gif\"");
            p.println("super * \"fluff:9:02\" \"\" \"fluff/maglevtrack3.gif\"");
            p.println("super * \"fluff:9:03\" \"\" \"fluff/maglevstation1.gif\"");
            p.println("super * \"fluff:9:04\" \"\" \"fluff/maglevstation2.gif\"");
            p.println("super * \"fluff:9:05\" \"\" \"fluff/maglevstation3.gif\"");
            p.println("super * \"fluff:9:06\" \"\" \"fluff/maglevtrain1.gif\"");
            p.println("super * \"fluff:9:07\" \"\" \"fluff/maglevtrain2.gif\"");
            p.println("super * \"fluff:9:08\" \"\" \"fluff/maglevtrain3.gif\"");
            p.println("super * \"fluff:9:09\" \"\" \"fluff/maglevtrain4.gif\"");
            p.println("super * \"fluff:9:10\" \"\" \"fluff/maglevtrain5.gif\"");
            p.println("super * \"fluff:9:11\" \"\" \"fluff/maglevtrain6.gif\"");
            p.println("");
            p.println("super * \"road:2:00\" \"\" \"fluff/road_trees_00.gif\"");
            p.println("super * \"road:2:01\" \"\" \"fluff/road_trees_01.gif\"");
            p.println("super * \"road:2:02\" \"\" \"fluff/road_trees_02.gif\"");
            p.println("super * \"road:2:03\" \"\" \"fluff/road_trees_03.gif\"");
            p.println("super * \"road:2:04\" \"\" \"fluff/road_trees_04.gif\"");
            p.println("super * \"road:2:05\" \"\" \"fluff/road_trees_05.gif\"");
            p.println("super * \"road:2:06\" \"\" \"fluff/road_trees_06.gif\"");
            p.println("super * \"road:2:07\" \"\" \"fluff/road_trees_07.gif\"");
            p.println("super * \"road:2:08\" \"\" \"fluff/road_trees_08.gif\"");
            p.println("super * \"road:2:09\" \"\" \"fluff/road_trees_09.gif\"");
            p.println("super * \"road:2:10\" \"\" \"fluff/road_trees_10.gif\"");
            p.println("super * \"road:2:11\" \"\" \"fluff/road_trees_11.gif\"");
            p.println("super * \"road:2:12\" \"\" \"fluff/road_trees_12.gif\"");
            p.println("super * \"road:2:13\" \"\" \"fluff/road_trees_13.gif\"");
            p.println("super * \"road:2:14\" \"\" \"fluff/road_trees_14.gif\"");
            p.println("super * \"road:2:15\" \"\" \"fluff/road_trees_15.gif\"");
            p.println("super * \"road:2:16\" \"\" \"fluff/road_trees_16.gif\"");
            p.println("super * \"road:2:17\" \"\" \"fluff/road_trees_17.gif\"");
            p.println("super * \"road:2:18\" \"\" \"fluff/road_trees_18.gif\"");
            p.println("super * \"road:2:19\" \"\" \"fluff/road_trees_19.gif\"");
            p.println("super * \"road:2:20\" \"\" \"fluff/road_trees_20.gif\"");
            p.println("super * \"road:2:21\" \"\" \"fluff/road_trees_21.gif\"");
            p.println("super * \"road:2:22\" \"\" \"fluff/road_trees_22.gif\"");
            p.println("super * \"road:2:23\" \"\" \"fluff/road_trees_23.gif\"");
            p.println("super * \"road:2:24\" \"\" \"fluff/road_trees_24.gif\"");
            p.println("super * \"road:2:25\" \"\" \"fluff/road_trees_25.gif\"");
            p.println("super * \"road:2:26\" \"\" \"fluff/road_trees_26.gif\"");
            p.println("super * \"road:2:27\" \"\" \"fluff/road_trees_27.gif\"");
            p.println("super * \"road:2:28\" \"\" \"fluff/road_trees_28.gif\"");
            p.println("super * \"road:2:29\" \"\" \"fluff/road_trees_29.gif\"");
            p.println("super * \"road:2:30\" \"\" \"fluff/road_trees_30.gif\"");
            p.println("super * \"road:2:31\" \"\" \"fluff/road_trees_31.gif\"");
            p.println("super * \"road:2:32\" \"\" \"fluff/road_trees_32.gif\"");
            p.println("super * \"road:2:33\" \"\" \"fluff/road_trees_33.gif\"");
            p.println("super * \"road:2:34\" \"\" \"fluff/road_trees_34.gif\"");
            p.println("super * \"road:2:35\" \"\" \"fluff/road_trees_35.gif\"");
            p.println("super * \"road:2:36\" \"\" \"fluff/road_trees_36.gif\"");
            p.println("super * \"road:2:37\" \"\" \"fluff/road_trees_37.gif\"");
            p.println("super * \"road:2:38\" \"\" \"fluff/road_trees_38.gif\"");
            p.println("super * \"road:2:39\" \"\" \"fluff/road_trees_39.gif\"");
            p.println("super * \"road:2:40\" \"\" \"fluff/road_trees_40.gif\"");
            p.println("super * \"road:2:41\" \"\" \"fluff/road_trees_41.gif\"");
            p.println("super * \"road:2:42\" \"\" \"fluff/road_trees_42.gif\"");
            p.println("super * \"road:2:43\" \"\" \"fluff/road_trees_43.gif\"");
            p.println("super * \"road:2:44\" \"\" \"fluff/road_trees_44.gif\"");
            p.println("super * \"road:2:45\" \"\" \"fluff/road_trees_45.gif\"");
            p.println("super * \"road:2:46\" \"\" \"fluff/road_trees_46.gif\"");
            p.println("super * \"road:2:47\" \"\" \"fluff/road_trees_47.gif\"");
            p.println("super * \"road:2:48\" \"\" \"fluff/road_trees_48.gif\"");
            p.println("super * \"road:2:49\" \"\" \"fluff/road_trees_49.gif\"");
            p.println("super * \"road:2:50\" \"\" \"fluff/road_trees_50.gif\"");
            p.println("super * \"road:2:51\" \"\" \"fluff/road_trees_51.gif\"");
            p.println("super * \"road:2:52\" \"\" \"fluff/road_trees_52.gif\"");
            p.println("super * \"road:2:53\" \"\" \"fluff/road_trees_53.gif\"");
            p.println("super * \"road:2:54\" \"\" \"fluff/road_trees_54.gif\"");
            p.println("super * \"road:2:55\" \"\" \"fluff/road_trees_55.gif\"");
            p.println("super * \"road:2:56\" \"\" \"fluff/road_trees_56.gif\"");
            p.println("super * \"road:2:57\" \"\" \"fluff/road_trees_57.gif\"");
            p.println("super * \"road:2:58\" \"\" \"fluff/road_trees_58.gif\"");
            p.println("super * \"road:2:59\" \"\" \"fluff/road_trees_59.gif\"");
            p.println("super * \"road:2:60\" \"\" \"fluff/road_trees_60.gif\"");
            p.println("super * \"road:2:61\" \"\" \"fluff/road_trees_61.gif\"");
            p.println("super * \"road:2:62\" \"\" \"fluff/road_trees_62.gif\"");
            p.println("super * \"road:2:63\" \"\" \"fluff/road_trees_63.gif\"");
            p.println("");
            p.println("super * \"building:4;bldg_elev:1;bldg_cf:*\" \"\" \"singlehex/hardened_1.gif\"");
            p.println("super * \"building:4;bldg_elev:2;bldg_cf:*\" \"\" \"singlehex/hardened_2.gif\"");
            p.println("super * \"building:4;bldg_elev:3;bldg_cf:*\" \"\" \"singlehex/hardened_3.gif\"");
            p.println("super * \"building:4;bldg_elev:4;bldg_cf:*\" \"\" \"singlehex/hardened_4.gif\"");
            p.println("");
            p.println("super * \"building:3;bldg_elev:1;bldg_cf:*\" \"\" \"singlehex/heavy_1.gif\"");
            p.println("super * \"building:3;bldg_elev:2;bldg_cf:*\" \"\" \"singlehex/heavy_2.gif\"");
            p.println("super * \"building:3;bldg_elev:3;bldg_cf:*\" \"\" \"singlehex/heavy_3.gif\"");
            p.println("super * \"building:3;bldg_elev:4;bldg_cf:*\" \"\" \"singlehex/heavy_4.gif\"");
            p.println("");
            p.println("super * \"building:2;bldg_elev:1;bldg_cf:*\" \"\" \"singlehex/medium_1.gif\"");
            p.println("super * \"building:2;bldg_elev:2;bldg_cf:*\" \"\" \"singlehex/medium_2.gif\"");
            p.println("super * \"building:2;bldg_elev:3;bldg_cf:*\" \"\" \"singlehex/medium_3.gif\"");
            p.println("super * \"building:2;bldg_elev:4;bldg_cf:*\" \"\" \"singlehex/medium_4.gif\"");
            p.println("");
            p.println("super * \"building:1;bldg_elev:*;bldg_cf:*;fluff:3\"     \"\" \"singlehex/light_3.gif\"");
            p.println("super * \"building:1;bldg_elev:*;bldg_cf:*;fluff:1\"     \"\" \"singlehex/light_5.gif\"");
            p.println("super * \"building:1;bldg_elev:1;bldg_cf:*\" \"\" \"singlehex/light_6.gif\"");
            p.println("super * \"building:1;bldg_elev:2;bldg_cf:*\" \"\" \"singlehex/light_1.gif\"");
            p.println("super * \"building:1;bldg_elev:3;bldg_cf:*\" \"\" \"singlehex/light_4.gif\"");
            p.println("super * \"building:1;bldg_elev:4;bldg_cf:*\" \"\" \"singlehex/light_2.gif\"");
            p.println("");
            p.println("super * \"building:1;bldg_elev:1;bldg_cf:*\" \"\" \"boring/cropped_farm.png\"");
            p.println("super * \"building:1;bldg_elev:2;bldg_cf:*\" \"\" \"boring/cropped_church.png\"");
            p.println("super * \"building:1;bldg_elev:3;bldg_cf:*\" \"\" \"boring/light_bldg.png\"");
            p.println("super * \"building:2;bldg_elev:3;bldg_cf:*\" \"\" \"boring/cropped_cannon_tower.png\"");
            p.println("super * \"building:3;bldg_elev:3;bldg_cf:*\" \"\" \"boring/cropped_refinery.png\"");
            p.println("super * \"building:3;bldg_elev:4;bldg_cf:*\" \"\" \"boring/cropped_mage_tower.png\"");
            p.println("");
            p.println("super * \"building:1:00;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof00.gif\"");
            p.println("super * \"building:1:01;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof01.gif\"");
            p.println("super * \"building:1:02;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof02.gif\"");
            p.println("super * \"building:1:03;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof03.gif\"");
            p.println("super * \"building:1:04;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof04.gif\"");
            p.println("super * \"building:1:05;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof05.gif\"");
            p.println("super * \"building:1:06;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof06.gif\"");
            p.println("super * \"building:1:07;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof07.gif\"");
            p.println("super * \"building:1:08;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof08.gif\"");
            p.println("super * \"building:1:09;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof09.gif\"");
            p.println("super * \"building:1:10;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof10.gif\"");
            p.println("super * \"building:1:11;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof11.gif\"");
            p.println("super * \"building:1:12;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof12.gif\"");
            p.println("super * \"building:1:13;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof13.gif\"");
            p.println("super * \"building:1:14;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof14.gif\"");
            p.println("super * \"building:1:15;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof15.gif\"");
            p.println("super * \"building:1:16;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof16.gif\"");
            p.println("super * \"building:1:17;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof17.gif\"");
            p.println("super * \"building:1:18;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof18.gif\"");
            p.println("super * \"building:1:19;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof19.gif\"");
            p.println("super * \"building:1:20;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof20.gif\"");
            p.println("super * \"building:1:21;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof21.gif\"");
            p.println("super * \"building:1:22;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof22.gif\"");
            p.println("super * \"building:1:23;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof23.gif\"");
            p.println("super * \"building:1:24;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof24.gif\"");
            p.println("super * \"building:1:25;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof25.gif\"");
            p.println("super * \"building:1:26;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof26.gif\"");
            p.println("super * \"building:1:27;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof27.gif\"");
            p.println("super * \"building:1:28;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof28.gif\"");
            p.println("super * \"building:1:29;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof29.gif\"");
            p.println("super * \"building:1:30;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof30.gif\"");
            p.println("super * \"building:1:31;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof31.gif\"");
            p.println("super * \"building:1:32;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof32.gif\"");
            p.println("super * \"building:1:33;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof33.gif\"");
            p.println("super * \"building:1:34;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof34.gif\"");
            p.println("super * \"building:1:35;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof35.gif\"");
            p.println("super * \"building:1:36;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof36.gif\"");
            p.println("super * \"building:1:37;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof37.gif\"");
            p.println("super * \"building:1:38;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof38.gif\"");
            p.println("super * \"building:1:39;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof39.gif\"");
            p.println("super * \"building:1:40;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof40.gif\"");
            p.println("super * \"building:1:41;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof41.gif\"");
            p.println("super * \"building:1:42;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof42.gif\"");
            p.println("super * \"building:1:43;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof43.gif\"");
            p.println("super * \"building:1:44;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof44.gif\"");
            p.println("super * \"building:1:45;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof45.gif\"");
            p.println("super * \"building:1:46;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof46.gif\"");
            p.println("super * \"building:1:47;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof47.gif\"");
            p.println("super * \"building:1:48;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof48.gif\"");
            p.println("super * \"building:1:49;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof49.gif\"");
            p.println("super * \"building:1:50;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof50.gif\"");
            p.println("super * \"building:1:51;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof51.gif\"");
            p.println("super * \"building:1:52;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof52.gif\"");
            p.println("super * \"building:1:53;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof53.gif\"");
            p.println("super * \"building:1:54;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof54.gif\"");
            p.println("super * \"building:1:55;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof55.gif\"");
            p.println("super * \"building:1:56;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof56.gif\"");
            p.println("super * \"building:1:57;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof57.gif\"");
            p.println("super * \"building:1:58;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof58.gif\"");
            p.println("super * \"building:1:59;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof59.gif\"");
            p.println("super * \"building:1:60;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof60.gif\"");
            p.println("super * \"building:1:61;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof61.gif\"");
            p.println("super * \"building:1:62;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof62.gif\"");
            p.println("super * \"building:1:63;bldg_elev:*;bldg_cf:*\" \"\" \"light/light_roof63.gif\"");
            p.println("");
            p.println("super * \"building:2:00;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof00.gif\"");
            p.println("super * \"building:2:01;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof01.gif\"");
            p.println("super * \"building:2:02;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof02.gif\"");
            p.println("super * \"building:2:03;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof03.gif\"");
            p.println("super * \"building:2:04;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof04.gif\"");
            p.println("super * \"building:2:05;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof05.gif\"");
            p.println("super * \"building:2:06;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof06.gif\"");
            p.println("super * \"building:2:07;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof07.gif\"");
            p.println("super * \"building:2:08;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof08.gif\"");
            p.println("super * \"building:2:09;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof09.gif\"");
            p.println("super * \"building:2:10;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof10.gif\"");
            p.println("super * \"building:2:11;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof11.gif\"");
            p.println("super * \"building:2:12;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof12.gif\"");
            p.println("super * \"building:2:13;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof13.gif\"");
            p.println("super * \"building:2:14;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof14.gif\"");
            p.println("super * \"building:2:15;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof15.gif\"");
            p.println("super * \"building:2:16;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof16.gif\"");
            p.println("super * \"building:2:17;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof17.gif\"");
            p.println("super * \"building:2:18;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof18.gif\"");
            p.println("super * \"building:2:19;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof19.gif\"");
            p.println("super * \"building:2:20;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof20.gif\"");
            p.println("super * \"building:2:21;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof21.gif\"");
            p.println("super * \"building:2:22;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof22.gif\"");
            p.println("super * \"building:2:23;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof23.gif\"");
            p.println("super * \"building:2:24;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof24.gif\"");
            p.println("super * \"building:2:25;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof25.gif\"");
            p.println("super * \"building:2:26;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof26.gif\"");
            p.println("super * \"building:2:27;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof27.gif\"");
            p.println("super * \"building:2:28;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof28.gif\"");
            p.println("super * \"building:2:29;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof29.gif\"");
            p.println("super * \"building:2:30;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof30.gif\"");
            p.println("super * \"building:2:31;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof31.gif\"");
            p.println("super * \"building:2:32;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof32.gif\"");
            p.println("super * \"building:2:33;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof33.gif\"");
            p.println("super * \"building:2:34;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof34.gif\"");
            p.println("super * \"building:2:35;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof35.gif\"");
            p.println("super * \"building:2:36;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof36.gif\"");
            p.println("super * \"building:2:37;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof37.gif\"");
            p.println("super * \"building:2:38;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof38.gif\"");
            p.println("super * \"building:2:39;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof39.gif\"");
            p.println("super * \"building:2:40;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof40.gif\"");
            p.println("super * \"building:2:41;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof41.gif\"");
            p.println("super * \"building:2:42;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof42.gif\"");
            p.println("super * \"building:2:43;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof43.gif\"");
            p.println("super * \"building:2:44;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof44.gif\"");
            p.println("super * \"building:2:45;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof45.gif\"");
            p.println("super * \"building:2:46;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof46.gif\"");
            p.println("super * \"building:2:47;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof47.gif\"");
            p.println("super * \"building:2:48;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof48.gif\"");
            p.println("super * \"building:2:49;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof49.gif\"");
            p.println("super * \"building:2:50;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof50.gif\"");
            p.println("super * \"building:2:51;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof51.gif\"");
            p.println("super * \"building:2:52;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof52.gif\"");
            p.println("super * \"building:2:53;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof53.gif\"");
            p.println("super * \"building:2:54;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof54.gif\"");
            p.println("super * \"building:2:55;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof55.gif\"");
            p.println("super * \"building:2:56;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof56.gif\"");
            p.println("super * \"building:2:57;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof57.gif\"");
            p.println("super * \"building:2:58;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof58.gif\"");
            p.println("super * \"building:2:59;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof59.gif\"");
            p.println("super * \"building:2:60;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof60.gif\"");
            p.println("super * \"building:2:61;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof61.gif\"");
            p.println("super * \"building:2:62;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof62.gif\"");
            p.println("super * \"building:2:63;bldg_elev:*;bldg_cf:*\" \"\" \"megaart/roof63.gif\"");
            p.println("");
            p.println("super * \"building:3:00;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof00.gif\"");
            p.println("super * \"building:3:01;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof01.gif\"");
            p.println("super * \"building:3:02;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof02.gif\"");
            p.println("super * \"building:3:03;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof03.gif\"");
            p.println("super * \"building:3:04;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof04.gif\"");
            p.println("super * \"building:3:05;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof05.gif\"");
            p.println("super * \"building:3:06;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof06.gif\"");
            p.println("super * \"building:3:07;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof07.gif\"");
            p.println("super * \"building:3:08;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof08.gif\"");
            p.println("super * \"building:3:09;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof09.gif\"");
            p.println("super * \"building:3:10;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof10.gif\"");
            p.println("super * \"building:3:11;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof11.gif\"");
            p.println("super * \"building:3:12;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof12.gif\"");
            p.println("super * \"building:3:13;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof13.gif\"");
            p.println("super * \"building:3:14;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof14.gif\"");
            p.println("super * \"building:3:15;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof15.gif\"");
            p.println("super * \"building:3:16;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof16.gif\"");
            p.println("super * \"building:3:17;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof17.gif\"");
            p.println("super * \"building:3:18;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof18.gif\"");
            p.println("super * \"building:3:19;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof19.gif\"");
            p.println("super * \"building:3:20;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof20.gif\"");
            p.println("super * \"building:3:21;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof21.gif\"");
            p.println("super * \"building:3:22;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof22.gif\"");
            p.println("super * \"building:3:23;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof23.gif\"");
            p.println("super * \"building:3:24;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof24.gif\"");
            p.println("super * \"building:3:25;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof25.gif\"");
            p.println("super * \"building:3:26;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof26.gif\"");
            p.println("super * \"building:3:27;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof27.gif\"");
            p.println("super * \"building:3:28;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof28.gif\"");
            p.println("super * \"building:3:29;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof29.gif\"");
            p.println("super * \"building:3:30;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof30.gif\"");
            p.println("super * \"building:3:31;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof31.gif\"");
            p.println("super * \"building:3:32;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof32.gif\"");
            p.println("super * \"building:3:33;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof33.gif\"");
            p.println("super * \"building:3:34;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof34.gif\"");
            p.println("super * \"building:3:35;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof35.gif\"");
            p.println("super * \"building:3:36;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof36.gif\"");
            p.println("super * \"building:3:37;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof37.gif\"");
            p.println("super * \"building:3:38;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof38.gif\"");
            p.println("super * \"building:3:39;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof39.gif\"");
            p.println("super * \"building:3:40;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof40.gif\"");
            p.println("super * \"building:3:41;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof41.gif\"");
            p.println("super * \"building:3:42;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof42.gif\"");
            p.println("super * \"building:3:43;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof43.gif\"");
            p.println("super * \"building:3:44;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof44.gif\"");
            p.println("super * \"building:3:45;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof45.gif\"");
            p.println("super * \"building:3:46;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof46.gif\"");
            p.println("super * \"building:3:47;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof47.gif\"");
            p.println("super * \"building:3:48;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof48.gif\"");
            p.println("super * \"building:3:49;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof49.gif\"");
            p.println("super * \"building:3:50;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof50.gif\"");
            p.println("super * \"building:3:51;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof51.gif\"");
            p.println("super * \"building:3:52;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof52.gif\"");
            p.println("super * \"building:3:53;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof53.gif\"");
            p.println("super * \"building:3:54;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof54.gif\"");
            p.println("super * \"building:3:55;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof55.gif\"");
            p.println("super * \"building:3:56;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof56.gif\"");
            p.println("super * \"building:3:57;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof57.gif\"");
            p.println("super * \"building:3:58;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof58.gif\"");
            p.println("super * \"building:3:59;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof59.gif\"");
            p.println("super * \"building:3:60;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof60.gif\"");
            p.println("super * \"building:3:61;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof61.gif\"");
            p.println("super * \"building:3:62;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof62.gif\"");
            p.println("super * \"building:3:63;bldg_elev:*;bldg_cf:*\" \"\" \"heavy/heavy_roof63.gif\"");
            p.println("");
            p.println("super * \"building:4:00;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof00.gif\"");
            p.println("super * \"building:4:01;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof01.gif\"");
            p.println("super * \"building:4:02;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof02.gif\"");
            p.println("super * \"building:4:03;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof03.gif\"");
            p.println("super * \"building:4:04;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof04.gif\"");
            p.println("super * \"building:4:05;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof05.gif\"");
            p.println("super * \"building:4:06;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof06.gif\"");
            p.println("super * \"building:4:07;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof07.gif\"");
            p.println("super * \"building:4:08;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof08.gif\"");
            p.println("super * \"building:4:09;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof09.gif\"");
            p.println("super * \"building:4:10;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof10.gif\"");
            p.println("super * \"building:4:11;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof11.gif\"");
            p.println("super * \"building:4:12;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof12.gif\"");
            p.println("super * \"building:4:13;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof13.gif\"");
            p.println("super * \"building:4:14;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof14.gif\"");
            p.println("super * \"building:4:15;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof15.gif\"");
            p.println("super * \"building:4:16;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof16.gif\"");
            p.println("super * \"building:4:17;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof17.gif\"");
            p.println("super * \"building:4:18;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof18.gif\"");
            p.println("super * \"building:4:19;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof19.gif\"");
            p.println("super * \"building:4:20;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof20.gif\"");
            p.println("super * \"building:4:21;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof21.gif\"");
            p.println("super * \"building:4:22;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof22.gif\"");
            p.println("super * \"building:4:23;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof23.gif\"");
            p.println("super * \"building:4:24;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof24.gif\"");
            p.println("super * \"building:4:25;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof25.gif\"");
            p.println("super * \"building:4:26;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof26.gif\"");
            p.println("super * \"building:4:27;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof27.gif\"");
            p.println("super * \"building:4:28;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof28.gif\"");
            p.println("super * \"building:4:29;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof29.gif\"");
            p.println("super * \"building:4:30;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof30.gif\"");
            p.println("super * \"building:4:31;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof31.gif\"");
            p.println("super * \"building:4:32;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof32.gif\"");
            p.println("super * \"building:4:33;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof33.gif\"");
            p.println("super * \"building:4:34;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof34.gif\"");
            p.println("super * \"building:4:35;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof35.gif\"");
            p.println("super * \"building:4:36;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof36.gif\"");
            p.println("super * \"building:4:37;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof37.gif\"");
            p.println("super * \"building:4:38;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof38.gif\"");
            p.println("super * \"building:4:39;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof39.gif\"");
            p.println("super * \"building:4:40;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof40.gif\"");
            p.println("super * \"building:4:41;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof41.gif\"");
            p.println("super * \"building:4:42;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof42.gif\"");
            p.println("super * \"building:4:43;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof43.gif\"");
            p.println("super * \"building:4:44;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof44.gif\"");
            p.println("super * \"building:4:45;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof45.gif\"");
            p.println("super * \"building:4:46;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof46.gif\"");
            p.println("super * \"building:4:47;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof47.gif\"");
            p.println("super * \"building:4:48;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof48.gif\"");
            p.println("super * \"building:4:49;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof49.gif\"");
            p.println("super * \"building:4:50;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof50.gif\"");
            p.println("super * \"building:4:51;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof51.gif\"");
            p.println("super * \"building:4:52;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof52.gif\"");
            p.println("super * \"building:4:53;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof53.gif\"");
            p.println("super * \"building:4:54;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof54.gif\"");
            p.println("super * \"building:4:55;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof55.gif\"");
            p.println("super * \"building:4:56;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof56.gif\"");
            p.println("super * \"building:4:57;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof57.gif\"");
            p.println("super * \"building:4:58;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof58.gif\"");
            p.println("super * \"building:4:59;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof59.gif\"");
            p.println("super * \"building:4:60;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof60.gif\"");
            p.println("super * \"building:4:61;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof61.gif\"");
            p.println("super * \"building:4:62;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof62.gif\"");
            p.println("super * \"building:4:63;bldg_elev:*;bldg_cf:*\" \"\" \"hardened/hardened_roof63.gif\"");
            p.println("");
            p.println("super * \"rubble:1\" \"\" \"boring/rubble_light.gif\"");
            p.println("super * \"rubble:2\" \"\" \"boring/rubble_medium.gif\"");
            p.println("super * \"rubble:3\" \"\" \"boring/rubble_heavy.gif\"");
            p.println("super * \"rubble:4\" \"\" \"boring/rubble_hardened.gif\"");
            p.println("");
            p.println("super * \"fluff:6:00\" \"\" \"fluff/skylight1.gif\"");
            p.println("super * \"fluff:6:01\" \"\" \"fluff/skylight2.gif\"");
            p.println("super * \"fluff:6:02\" \"\" \"fluff/skylight3.gif\"");
            p.println("super * \"fluff:6:03\" \"\" \"fluff/skylight4.gif\"");
            p.println("super * \"fluff:6:04\" \"\" \"fluff/skylight5.gif\"");
            p.println("super * \"fluff:6:05\" \"\" \"fluff/skylight6.gif\"");
            p.println("super * \"fluff:6:06\" \"\" \"fluff/stack1.gif\"");
            p.println("super * \"fluff:6:07\" \"\" \"fluff/stack2.gif\"");
            p.println("super * \"fluff:6:08\" \"\" \"fluff/stack3.gif\"");
            p.println("super * \"fluff:6:09\" \"\" \"fluff/stack4.gif\"");
            p.println("super * \"fluff:6:10\" \"\" \"fluff/stack5.gif\"");
            p.println("super * \"fluff:6:11\" \"\" \"fluff/stack6.gif\"");
            p.println("super * \"fluff:6:12\" \"\" \"fluff/bevel1.gif\"");
            p.println("super * \"fluff:6:13\" \"\" \"fluff/bevel2.gif\"");
            p.println("super * \"fluff:6:14\" \"\" \"fluff/bevel3.gif\"");
            p.println("super * \"fluff:6:15\" \"\" \"fluff/bevel4.gif\"");
            p.println("super * \"fluff:6:16\" \"\" \"fluff/bevel5.gif\"");
            p.println("super * \"fluff:6:17\" \"\" \"fluff/bevel6.gif\"");
            p.println("super * \"fluff:6:18\" \"\" \"fluff/pool1.gif\"");
            p.println("");
            p.println("super * \"fluff:8:00\" \"\" \"fluff/ledge1.gif\"");
            p.println("super * \"fluff:8:01\" \"\" \"fluff/ledge2.gif\"");
            p.println("super * \"fluff:8:02\" \"\" \"fluff/ledge3.gif\"");
            p.println("super * \"fluff:8:03\" \"\" \"fluff/ledge4.gif\"");
            p.println("super * \"fluff:8:04\" \"\" \"fluff/ledge5.gif\"");
            p.println("super * \"fluff:8:05\" \"\" \"fluff/ledge6.gif\"");
            p.println("");
            p.println("super * \"fluff:2:00\" \"\" \"fluff/heli1.gif\"");
            p.println("super * \"fluff:2:01\" \"\" \"fluff/heli2.gif\"");
            p.println("super * \"fluff:2:02\" \"\" \"fluff/heli3.gif\"");
            p.println("super * \"fluff:2:03\" \"\" \"fluff/beacon1.gif\"");
            p.println("super * \"fluff:2:04\" \"\" \"fluff/beacon2.gif\"");
            p.println("");
            p.println("super * \"smoke:1\" \"\" \"boring/smoke.gif\"");
            p.println("super * \"smoke:2\" \"\" \"boring/heavysmoke.gif\"");
            p.println("super * \"fire:1\" \"\" \"boring/fire.gif\"");
            p.println("super * \"fire:2\" \"\" \"boring/fire.gif\"");
            p.println("");
            p.println("super * \"fortified:1\" \"\" \"boring/sandbags.gif\"");
            p.println("");
            p.println("super * \"arms:1\" \"\" \"limbs/arm1.gif\"");
            p.println("super * \"arms:2\" \"\" \"limbs/arm2.gif\"");
            p.println("super * \"arms:3\" \"\" \"limbs/arm2.gif\"");
            p.println("super * \"legs:1\" \"\" \"limbs/leg1.gif\"");
            p.println("super * \"legs:2\" \"\" \"limbs/leg2.gif\"");
            p.println("super * \"legs:3\" \"\" \"limbs/leg2.gif\"");
            p.println("");
            p.println("super * \"woods:1\" \"\" \"boring/lf0.gif;boring/lf1.gif;boring/lf2.gif;boring/lf3.gif;boring/lf4.gif\"");
            p.println("super * \"woods:2\" \"\" \"boring/hf0.gif;boring/hf1.gif;boring/hf2.gif;boring/hf3.gif\"");
            p.println("");
            p.println("base 0 \"\" \"\" \"boring/beige_plains_0.gif\"");
            p.println("base 1 \"\" \"\" \"boring/beige_plains_1.gif\"");
            p.println("base 2 \"\" \"\" \"boring/beige_plains_2.gif\"");
            p.println("base 3 \"\" \"\" \"boring/beige_plains_3.gif\"");
            p.println("base 4 \"\" \"\" \"boring/beige_plains_4.gif\"");
            p.println("base 5 \"\" \"\" \"boring/beige_plains_5.gif\"");
            p.println("base 6 \"\" \"\" \"boring/beige_plains_6.gif\"");
            p.println("base 7 \"\" \"\" \"boring/beige_plains_7.gif\"");
            p.println("base 8 \"\" \"\" \"boring/beige_plains_8.gif\"");
            p.println("base 9 \"\" \"\" \"boring/beige_plains_9.gif\"");
            p.println("base 10 \"\" \"\" \"boring/beige_plains_10.gif\"");
            p.println("base -1 \"\" \"\" \"boring/beige_sinkhole_1.gif\"");
            p.println("base -2 \"\" \"\" \"boring/beige_sinkhole_2.gif\"");
            p.println("base -3 \"\" \"\" \"boring/beige_sinkhole_3.gif\"");
            p.println("");
            p.println("base * \"impassable:1\" \"\" \"boring/rock.gif\"");
            p.println("");
            p.println("super * \"rough:1\" \"\" \"boring/rough_0.gif;boring/rough_1.gif;boring/rough_2.gif\"");
            p.println("");
            p.println("base 0 \"ice:1\" \"\" \"themes/ice_0.gif\"");
            p.println("base 1 \"ice:1\" \"\" \"themes/ice_1.gif\"");
            p.println("base 2 \"ice:1\" \"\" \"themes/ice_2.gif\"");
            p.println("base 3 \"ice:1\" \"\" \"themes/ice_3.gif\"");
            p.println("base 4 \"ice:1\" \"\" \"themes/ice_4.gif\"");
            p.println("base 5 \"ice:1\" \"\" \"themes/ice_5.gif\"");
            p.println("base 6 \"ice:1\" \"\" \"themes/ice_6.gif\"");
            p.println("base 7 \"ice:1\" \"\" \"themes/ice_7.gif\"");
            p.println("base 8 \"ice:1\" \"\" \"themes/ice_8.gif\"");
            p.println("base 9 \"ice:1\" \"\" \"themes/ice_9.gif\"");
            p.println("base 10 \"ice:1\" \"\" \"themes/ice_10.gif\"");
            p.println("base -1 \"ice:1\" \"\" \"themes/ice_-1.gif\"");
            p.println("base -2 \"ice:1\" \"\" \"themes/ice_-2.gif\"");
            p.println("base -3 \"ice:1\" \"\" \"themes/ice_-3.gif\"");
            p.println("base -4 \"ice:1\" \"\" \"themes/ice_-4.gif\"");
            p.println("base -5 \"ice:1\" \"\" \"themes/ice_-5.gif\"");
            p.println("base -6 \"ice:1\" \"\" \"themes/ice_-6.gif\"");
            p.println("");
            p.println("base * \"water:1\" \"\" \"boring/blue_water_1.gif\"");
            p.println("base * \"water:2\" \"\" \"boring/blue_water_2.gif\"");
            p.println("base * \"water:3\" \"\" \"boring/blue_water_3.gif\"");
            p.println("base * \"water:4\" \"\" \"boring/blue_water_4.gif\"");
            p.println("");
            p.println("base 0 \"pavement:1\" \"\" \"boring/grey_pavement_0.gif\"");
            p.println("base 1 \"pavement:1\" \"\" \"boring/grey_pavement_1.gif\"");
            p.println("base 2 \"pavement:1\" \"\" \"boring/grey_pavement_2.gif\"");
            p.println("base 3 \"pavement:1\" \"\" \"boring/grey_pavement_2.gif\"");
            p.println("base 4 \"pavement:1\" \"\" \"boring/grey_pavement_3.gif\"");
            p.println("base 5 \"pavement:1\" \"\" \"boring/grey_pavement_3.gif\"");
            p.println("base 6 \"pavement:1\" \"\" \"boring/grey_pavement_4.gif\"");
            p.println("base 7 \"pavement:1\" \"\" \"boring/grey_pavement_4.gif\"");
            p.println("base 8 \"pavement:1\" \"\" \"boring/grey_pavement_4.gif\"");
            p.println("base 9 \"pavement:1\" \"\" \"boring/grey_pavement_4.gif\"");
            p.println("base 10 \"pavement:1\" \"\" \"boring/grey_pavement_5.gif\"");
            p.println("base 20 \"pavement:1\" \"\" \"boring/grey_pavement_6.gif\"");
            p.println("base 30 \"pavement:1\" \"\" \"boring/grey_pavement_7.gif\"");
            p.println("base 40 \"pavement:1\" \"\" \"boring/grey_pavement_8.gif\"");
            p.println("");
            p.println("base 0 \"swamp:1\" \"\" \"swamp/swamp_clear_0.gif\"");
            p.println("base 0 \"swamp:2\" \"\" \"swamp/swamp_clear_0a.gif\"");
            p.println("base 0 \"swamp:3\" \"\" \"swamp/swamp_clear_0b.gif\"");
            p.println("base 0 \"swamp:4\" \"\" \"swamp/swamp_clear_0c.gif\"");
            p.println("base 1 \"swamp:1\" \"\" \"swamp/swamp_clear_1.gif\"");
            p.println("base 2 \"swamp:1\" \"\" \"swamp/swamp_clear_2.gif\"");
            p.println("base 3 \"swamp:1\" \"\" \"swamp/swamp_clear_3.gif\"");
            p.println("base 4 \"swamp:1\" \"\" \"swamp/swamp_clear_4.gif\"");
            p.println("base 5 \"swamp:1\" \"\" \"swamp/swamp_clear_5.gif\"");
            p.println("base 6 \"swamp:1\" \"\" \"swamp/swamp_clear_6.gif\"");
            p.println("base 7 \"swamp:1\" \"\" \"swamp/swamp_clear_7.gif\"");
            p.println("base 8 \"swamp:1\" \"\" \"swamp/swamp_clear_8.gif\"");
            p.println("base 9 \"swamp:1\" \"\" \"swamp/swamp_clear_9.gif\"");
            p.println("base 10 \"swamp:1\" \"\" \"swamp/swamp_clear_10.gif\"");
            p.println("base -1 \"swamp:1\" \"\" \"swamp/swamp_clear_-1.gif\"");
            p.println("base -2 \"swamp:1\" \"\" \"swamp/swamp_clear_-2.gif\"");
            p.println("base -3 \"swamp:1\" \"\" \"swamp/swamp_clear_-3.gif\"");
            p.println("base 0 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_0.gif\"");
            p.println("base 0 \"rough:1;swamp:2\" \"\" \"swamp/swamp_rough_0a.gif\"");
            p.println("base 0 \"rough:1;swamp:3\" \"\" \"swamp/swamp_rough_0b.gif\"");
            p.println("base 0 \"rough:1;swamp:4\" \"\" \"swamp/swamp_rough_0c.gif\"");
            p.println("base 1 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_1.gif\"");
            p.println("base 2 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_2.gif\"");
            p.println("base 3 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_3.gif\"");
            p.println("base 4 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_4.gif\"");
            p.println("base 5 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_5.gif\"");
            p.println("base 6 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_6.gif\"");
            p.println("base 7 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_7.gif\"");
            p.println("base 8 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_8.gif\"");
            p.println("base 9 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_9.gif\"");
            p.println("base -1 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_-1.gif\"");
            p.println("base -2 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_-2.gif\"");
            p.println("base -3 \"rough:1;swamp:1\" \"\" \"swamp/swamp_rough_-3.gif\"");
            p.println("base 0 \"woods:1;swamp:1\" \"\" \"swamp/swamp_light_forest_0.gif\"");
            p.println("base 1 \"woods:1;swamp:1\" \"\" \"swamp/swamp_light_forest_1.gif\"");
            p.println("base 2 \"woods:1;swamp:1\" \"\" \"swamp/swamp_light_forest_2.gif\"");
            p.println("base 0 \"woods:2;swamp:1\" \"\" \"swamp/swamp_heavy_forest_0.gif\"");
            p.println("base 1 \"woods:2;swamp:1\" \"\" \"swamp/swamp_heavy_forest_1.gif\"");
            p.println("");
            p.println("base -2 \"magma:1\" \"\" \"magma/crust_-2.gif\"");
            p.println("base -1 \"magma:1\" \"\" \"magma/crust_-1.gif\"");
            p.println("base 0 \"magma:1\" \"\" \"magma/crust_0.gif\"");
            p.println("base 1 \"magma:1\" \"\" \"magma/crust_1.gif\"");
            p.println("base 2 \"magma:1\" \"\" \"magma/crust_2.gif\"");
            p.println("base 3 \"magma:1\" \"\" \"magma/crust_3.gif\"");
            p.println("");
            p.println("base -2 \"magma:2\" \"\" \"magma/magma_-2.gif\"");
            p.println("base -1 \"magma:2\" \"\" \"magma/magma_-1.gif\"");
            p.println("base 0 \"magma:2\" \"\" \"magma/magma_0.gif\"");
            p.println("base 1 \"magma:2\" \"\" \"magma/magma_1.gif\"");
            p.println("base 2 \"magma:2\" \"\" \"magma/magma_2.gif\"");
            p.println("base 3 \"magma:2\" \"\" \"magma/magma_3.gif\"");
            p.println("");
            p.println("base -2 \"mud:1\" \"\" \"mud/mud_-2.gif\"");
            p.println("base -1 \"mud:1\" \"\" \"mud/mud_-1.gif\"");
            p.println("base 0 \"mud:1\" \"\" \"mud/mud_0.gif\"");
            p.println("base 1 \"mud:1\" \"\" \"mud/mud_1.gif\"");
            p.println("base 2 \"mud:1\" \"\" \"mud/mud_2.gif\"");
            p.println("base 3 \"mud:1\" \"\" \"mud/mud_3.gif\"");
            p.println("");
            p.println("base -2 \"mud:2\" \"\" \"mud/deepmud_-2.gif\"");
            p.println("base -1 \"mud:2\" \"\" \"mud/deepmud_-1.gif\"");
            p.println("base 0 \"mud:2\" \"\" \"mud/deepmud_0.gif\"");
            p.println("base 1 \"mud:2\" \"\" \"mud/deepmud_1.gif\"");
            p.println("base 2 \"mud:2\" \"\" \"mud/deepmud_2.gif\"");
            p.println("base 3 \"mud:2\" \"\" \"mud/deepmud_3.gif\"");
            p.println("");
            p.println("base -2 \"sand:1\" \"\" \"sand/sand_-2.gif\"");
            p.println("base -1 \"sand:1\" \"\" \"sand/sand_-1.gif\"");
            p.println("base 0 \"sand:1\" \"\" \"sand/sand_0.gif\"");
            p.println("base 1 \"sand:1\" \"\" \"sand/sand_1.gif\"");
            p.println("base 2 \"sand:1\" \"\" \"sand/sand_2.gif\"");
            p.println("base 3 \"sand:1\" \"\" \"sand/sand_3.gif\"");
            p.println("");
            p.println("base -2 \"tundra:1\" \"\" \"tundra/tundra_-2.gif\"");
            p.println("base -1 \"tundra:1\" \"\" \"tundra/tundra_-1.gif\"");
            p.println("base 0 \"tundra:1\" \"\" \"tundra/tundra_0.gif\"");
            p.println("base 1 \"tundra:1\" \"\" \"tundra/tundra_1.gif\"");
            p.println("base 2 \"tundra:1\" \"\" \"tundra/tundra_2.gif\"");
            p.println("base 3 \"tundra:1\" \"\" \"tundra/tundra_3.gif\"");
            p.println("");
            p.println("#------------------- BEGIN snow theme");
            p.println("");
            p.println("base 0 \"\" \"snow\" \"themes/snow_0.gif\"");
            p.println("base 1 \"\" \"snow\" \"themes/snow_1.gif\"");
            p.println("base 2 \"\" \"snow\" \"themes/snow_2.gif\"");
            p.println("base 3 \"\" \"snow\" \"themes/snow_3.gif\"");
            p.println("base 4 \"\" \"snow\" \"themes/snow_4.gif\"");
            p.println("base 5 \"\" \"snow\" \"themes/snow_5.gif\"");
            p.println("base 6 \"\" \"snow\" \"themes/snow_6.gif\"");
            p.println("base 7 \"\" \"snow\" \"themes/snow_7.gif\"");
            p.println("base 8 \"\" \"snow\" \"themes/snow_8.gif\"");
            p.println("base 9 \"\" \"snow\" \"themes/snow_9.gif\"");
            p.println("base 10 \"\" \"snow\" \"themes/snow_10.gif\"");
            p.println("base -1 \"\" \"snow\" \"themes/snow_-1.gif\"");
            p.println("base -2 \"\" \"snow\" \"themes/snow_-2.gif\"");
            p.println("base -3 \"\" \"snow\" \"themes/snow_-3.gif\"");
            p.println("base -4 \"\" \"snow\" \"themes/snow_-4.gif\"");
            p.println("base -5 \"\" \"snow\" \"themes/snow_-5.gif\"");
            p.println("base -6 \"\" \"snow\" \"themes/snow_-6.gif\"");
            p.println("");
            p.println("base 0 \"pavement:1\" \"snow\" \"themes/ice_0.gif\"");
            p.println("base 1 \"pavement:1\" \"snow\" \"themes/ice_1.gif\"");
            p.println("base 2 \"pavement:1\" \"snow\" \"themes/ice_2.gif\"");
            p.println("base 3 \"pavement:1\" \"snow\" \"themes/ice_3.gif\"");
            p.println("base 4 \"pavement:1\" \"snow\" \"themes/ice_4.gif\"");
            p.println("base 5 \"pavement:1\" \"snow\" \"themes/ice_5.gif\"");
            p.println("base 6 \"pavement:1\" \"snow\" \"themes/ice_6.gif\"");
            p.println("base 7 \"pavement:1\" \"snow\" \"themes/ice_7.gif\"");
            p.println("base 8 \"pavement:1\" \"snow\" \"themes/ice_8.gif\"");
            p.println("base 9 \"pavement:1\" \"snow\" \"themes/ice_9.gif\"");
            p.println("base 10 \"pavement:1\" \"snow\" \"themes/ice_10.gif\"");
            p.println("base -1 \"pavement:1\" \"snow\" \"themes/ice_-1.gif\"");
            p.println("base -2 \"pavement:1\" \"snow\" \"themes/ice_-2.gif\"");
            p.println("base -3 \"pavement:1\" \"snow\" \"themes/ice_-3.gif\"");
            p.println("base -4 \"pavement:1\" \"snow\" \"themes/ice_-4.gif\"");
            p.println("base -5 \"pavement:1\" \"snow\" \"themes/ice_-5.gif\"");
            p.println("base -6 \"pavement:1\" \"snow\" \"themes/ice_-6.gif\"");
            p.println("");
            p.println("base 0 \"rough:1\" \"snow\" \"themes/snow_rough_0.gif\"");
            p.println("base 1 \"rough:1\" \"snow\" \"themes/snow_rough_1.gif\"");
            p.println("base 3 \"rough:1\" \"snow\" \"themes/snow_rough_3.gif\"");
            p.println("base 5 \"rough:1\" \"snow\" \"themes/snow_rough_5.gif\"");
            p.println("base -1 \"rough:1\" \"snow\" \"themes/snow_rough_-1.gif\"");
            p.println("base -3 \"rough:1\" \"snow\" \"themes/snow_rough_-3.gif\"");
            p.println("base -5 \"rough:1\" \"snow\" \"themes/snow_rough_-5.gif\"");
            p.println("");
            p.println("base 0 \"woods:1\" \"snow\" \"themes/snow_light_forest_0.gif\"");
            p.println("base 1 \"woods:1\" \"snow\" \"themes/snow_light_forest_1.gif\"");
            p.println("base 2 \"woods:1\" \"snow\" \"themes/snow_light_forest_2.gif\"");
            p.println("base 0 \"woods:2\" \"snow\" \"themes/snow_heavy_forest_0.gif\"");
            p.println("base 1 \"woods:2\" \"snow\" \"themes/snow_heavy_forest_1.gif\"");
            p.println("");
            p.println("#------------------- END snow theme");
            p.println("");
            p.println("#------------------- BEGIN grass theme");
            p.println("");
            p.println("base 0 \"\" \"grass\" \"grass/grass_plains_0.gif\"");
            p.println("base 1 \"\" \"grass\" \"grass/grass_plains_1.gif\"");
            p.println("base 2 \"\" \"grass\" \"grass/grass_plains_2.gif\"");
            p.println("base 3 \"\" \"grass\" \"grass/grass_plains_3.gif\"");
            p.println("base 4 \"\" \"grass\" \"grass/grass_plains_4.gif\"");
            p.println("base 5 \"\" \"grass\" \"grass/grass_plains_5.gif\"");
            p.println("base 6 \"\" \"grass\" \"grass/grass_plains_6.gif\"");
            p.println("base 7 \"\" \"grass\" \"grass/grass_plains_7.gif\"");
            p.println("base 8 \"\" \"grass\" \"grass/grass_plains_8.gif\"");
            p.println("base 9 \"\" \"grass\" \"grass/grass_plains_9.gif\"");
            p.println("base 10 \"\" \"grass\" \"grass/grass_plains_10.gif\"");
            p.println("base -1 \"\" \"grass\" \"grass/grass_sinkhole_1.gif\"");
            p.println("base -2 \"\" \"grass\" \"grass/grass_sinkhole_2.gif\"");
            p.println("base -3 \"\" \"grass\" \"grass/grass_sinkhole_3.gif\"");
            p.println("");
            p.println("base 0 \"swamp:1\" \"grass\" \"grass/grass_swamp_0.gif\"");
            p.println("base 0 \"swamp:2\" \"grass\" \"grass/grass_swamp_0.gif\"");
            p.println("base 0 \"swamp:3\" \"grass\" \"grass/grass_swamp_0.gif\"");
            p.println("base 0 \"swamp:4\" \"grass\" \"grass/grass_swamp_0.gif\"");
            p.println("base 1 \"swamp:1\" \"grass\" \"grass/grass_swamp_1.gif\"");
            p.println("base 2 \"swamp:1\" \"grass\" \"grass/grass_swamp_2.gif\"");
            p.println("base 3 \"swamp:1\" \"grass\" \"grass/grass_swamp_3.gif\"");
            p.println("base 4 \"swamp:1\" \"grass\" \"grass/grass_swamp_4.gif\"");
            p.println("base 5 \"swamp:1\" \"grass\" \"grass/grass_swamp_5.gif\"");
            p.println("base 6 \"swamp:1\" \"grass\" \"grass/grass_swamp_6.gif\"");
            p.println("base 7 \"swamp:1\" \"grass\" \"grass/grass_swamp_7.gif\"");
            p.println("base 8 \"swamp:1\" \"grass\" \"grass/grass_swamp_8.gif\"");
            p.println("base 9 \"swamp:1\" \"grass\" \"grass/grass_swamp_9.gif\"");
            p.println("base 10 \"swamp:1\" \"grass\" \"grass/grass_swamp_10.gif\"");
            p.println("base -1 \"swamp:1\" \"grass\" \"grass/grass_swamp_-1.gif\"");
            p.println("base -2 \"swamp:1\" \"grass\" \"grass/grass_swamp_-2.gif\"");
            p.println("base -3 \"swamp:1\" \"grass\" \"grass/grass_swamp_-3.gif\"");
            p.println("base 0 \"woods:1;swamp:1\" \"grass\" \"grass/grass_l_swamp_0.gif\"");
            p.println("base 1 \"woods:1;swamp:1\" \"grass\" \"grass/grass_l_swamp_1.gif\"");
            p.println("base 2 \"woods:1;swamp:1\" \"grass\" \"grass/grass_l_swamp_2.gif\"");
            p.println("base 0 \"woods:2;swamp:1\" \"grass\" \"grass/grass_h_swamp_0.gif\"");
            p.println("base 1 \"woods:2;swamp:1\" \"grass\" \"grass/grass_h_swamp_1.gif\"");
            p.println("");
            p.println("#------------------- END grass theme");
            p.println("");
            p.println("#------------------- BEGIN mars theme");
            p.println("");
            p.println("base 0 \"\" \"mars\" \"mars/mars_plains_0.gif\"");
            p.println("base 1 \"\" \"mars\" \"mars/mars_plains_1.gif\"");
            p.println("base 2 \"\" \"mars\" \"mars/mars_plains_2.gif\"");
            p.println("base 3 \"\" \"mars\" \"mars/mars_plains_3.gif\"");
            p.println("base 4 \"\" \"mars\" \"mars/mars_plains_4.gif\"");
            p.println("base 5 \"\" \"mars\" \"mars/mars_plains_5.gif\"");
            p.println("base 6 \"\" \"mars\" \"mars/mars_plains_6.gif\"");
            p.println("base 7 \"\" \"mars\" \"mars/mars_plains_7.gif\"");
            p.println("base 8 \"\" \"mars\" \"mars/mars_plains_8.gif\"");
            p.println("base 9 \"\" \"mars\" \"mars/mars_plains_9.gif\"");
            p.println("base 10 \"\" \"mars\" \"mars/mars_plains_10.gif\"");
            p.println("base -1 \"\" \"mars\" \"mars/mars_sinkhole_1.gif\"");
            p.println("base -2 \"\" \"mars\" \"mars/mars_sinkhole_2.gif\"");
            p.println("base -3 \"\" \"mars\" \"mars/mars_sinkhole_3.gif\"");
            p.println("");
            p.println("#------------------- END mars theme");
            p.println("");
            p.println("#------------------- BEGIN lunar theme");
            p.println("");
            p.println("base 0 \"\" \"lunar\" \"lunar/lunar_plains_0.gif\"");
            p.println("base 1 \"\" \"lunar\" \"lunar/lunar_plains_1.gif\"");
            p.println("base 2 \"\" \"lunar\" \"lunar/lunar_plains_2.gif\"");
            p.println("base 3 \"\" \"lunar\" \"lunar/lunar_plains_3.gif\"");
            p.println("base 4 \"\" \"lunar\" \"lunar/lunar_plains_4.gif\"");
            p.println("base 5 \"\" \"lunar\" \"lunar/lunar_plains_5.gif\"");
            p.println("base 6 \"\" \"lunar\" \"lunar/lunar_plains_6.gif\"");
            p.println("base 7 \"\" \"lunar\" \"lunar/lunar_plains_7.gif\"");
            p.println("base 8 \"\" \"lunar\" \"lunar/lunar_plains_8.gif\"");
            p.println("base 9 \"\" \"lunar\" \"lunar/lunar_plains_9.gif\"");
            p.println("base 10 \"\" \"lunar\" \"lunar/lunar_plains_10.gif\"");
            p.println("base -1 \"\" \"lunar\" \"lunar/lunar_sinkhole_1.gif\"");
            p.println("base -2 \"\" \"lunar\" \"lunar/lunar_sinkhole_2.gif\"");
            p.println("base -3 \"\" \"lunar\" \"lunar/lunar_sinkhole_3.gif\"");
            p.println("");
            p.println("#------------------- END lunar theme");
            p.close();
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
