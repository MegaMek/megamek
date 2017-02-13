/*
 * MegaMek - Copyright (C) 2000-2016 Ben Mazur (bmazur@sev.org)
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
package megamek.common.util;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.XStream;

import megamek.client.ui.swing.util.ImprovedAveragingScaleFilter;
import megamek.common.Configuration;
import megamek.common.Coords;
import sun.awt.image.ToolkitImage;

/**
 * Generic utility methods for image data
 */
public final class ImageUtil {
    /**
     * The graphics configuration of the local graphic card/monitor combination,
     * if we aren't running in "headless" mode.
     */
    private final static GraphicsConfiguration GC;
    static {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = null;
        try {
            gd = ge.getDefaultScreenDevice();
        } catch(HeadlessException he) {
        }
        GC = (null != gd) ? gd.getDefaultConfiguration() : null;
    }

    public static final int IMAGE_SCALE_BICUBIC = 1;
    public static final int IMAGE_SCALE_AVG_FILTER = 2;

    /**
     * @return an image in a format best fitting for hardware acceleration, if
     *         possible, else just the image passed to it
     */
    public static BufferedImage createAcceleratedImage(Image base) {
        if ((null == GC) || (null == base)) {
            return null;
        }
        BufferedImage acceleratedImage = GC.createCompatibleImage(
                base.getWidth(null), base.getHeight(null),
                Transparency.TRANSLUCENT);
        Graphics2D g2d = acceleratedImage.createGraphics();
        g2d.drawImage(base, 0, 0, base.getWidth(null), base.getHeight(null),
                null);
        g2d.dispose();
        return acceleratedImage;
    }

    /**
     * @return an image in a format best fitting for hardware acceleration, if possible
     */
    public static BufferedImage createAcceleratedImage(int w, int h) {
        if(null == GC) {
            return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
        BufferedImage acceleratedImage = GC.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        return acceleratedImage;
    }

    /**
     * Get a scaled version of the input image.
     *
     * @param img
     * @return
     */
    public static BufferedImage getScaledImage(Image img, int newWidth,
            int newHeight) {
        return getScaledImage(img, newWidth, newHeight, IMAGE_SCALE_BICUBIC);
    }

    /**
     * Get a scaled version of the input image, using the supplied type to
     * select which scaling method to use.
     *
     * @param img
     * @return
     */
    public static BufferedImage getScaledImage(Image img, int newWidth,
            int newHeight, int scaleType) {
        if (scaleType == IMAGE_SCALE_BICUBIC) {
            BufferedImage scaled = createAcceleratedImage(newWidth, newHeight);
            Graphics2D g2 = (Graphics2D) scaled.getGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(img, 0, 0, newWidth, newHeight, null);
            return scaled;
        } else {
            ImageFilter filter;
            filter = new ImprovedAveragingScaleFilter(img.getWidth(null),
                    img.getHeight(null), newWidth, newHeight);

            ImageProducer prod;
            prod = new FilteredImageSource(img.getSource(), filter);
            return ImageUtil.createAcceleratedImage(
                    Toolkit.getDefaultToolkit().createImage(prod));
        }
    }

    /** Image loaders */
    private static final List<ImageLoader> IMAGE_LOADERS;
    static {
        IMAGE_LOADERS = new ArrayList<>();
        IMAGE_LOADERS.add(new AtlasImageLoader());
        IMAGE_LOADERS.add(new TileMapImageLoader());
        IMAGE_LOADERS.add(new AWTImageLoader());
    }
    
    /** Add a new image loader to the first position of the list, if it isn't there already */
    public static void addImageLoader(ImageLoader loader) {
        if (null != loader && !IMAGE_LOADERS.contains(loader)) {
            IMAGE_LOADERS.add(0, loader);
        }
    }
    
    public static Image loadImageFromFile(String fileName) {
        if(null == fileName) {
            return null;
        }
        for (ImageLoader loader : IMAGE_LOADERS) {
            Image img = loader.loadImage(fileName);
            if (null != img) {
                return img;
            }
        }
        return null;
    }
    
    private ImageUtil() {}
    
    /**
     * Interface that defines methods for an ImageLoader.
     *
     */
    public interface ImageLoader {
        
        /**
         * Given a string representation of a file, 
         * @param fileName
         * @param toolkit
         * @return
         */
        Image loadImage(String fileName);
    }

    /**
     * ImageLoader implementation that expects a path to an image file, and that file is loaded directly and the loaded
     * image is returned.
     *
     */
    public static class AWTImageLoader implements ImageLoader {
        @Override
        public Image loadImage(String fileName) {
            File fin = new File(fileName);
            if (!fin.exists()) {
                System.out.println("Trying to load image for a non-existant "
                        + "file! Path: " + fileName);
            }
            ToolkitImage result = (ToolkitImage) Toolkit.getDefaultToolkit().getImage(fileName);
            if(null == result) {
                return null;
            }
            FinishedLoadingObserver observer = new FinishedLoadingObserver(Thread.currentThread());
            // Check to see if the image is loaded
            int infoFlags = result.check(observer);
            if ((infoFlags & ImageObserver.ALLBITS) == 0) {
                // Image not loaded, wait for it to load
                long startTime = System.currentTimeMillis();
                long maxRuntime = 10000;
                long runTime = 0;
                result.preload(observer);
                while (!observer.isLoaded() && runTime < maxRuntime) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        // Do nothing
                    }
                    runTime = System.currentTimeMillis() - startTime;
                }
            }
            return observer.isAnimated() ? result : ImageUtil.createAcceleratedImage(result.getBufferedImage());
        }
    }

    /**
     * ImageLoader that loads subregions from a larger atlas file.  The filename is assumed to have the format:
     * <imageFile>(X,Y-Width,Height), where X,Y is the start of the image tile, and Width,Height are the size of the
     * image tile.
     */
    public static class TileMapImageLoader implements ImageLoader {
        /**
         * Given a String with the format "X,Y" split this into the X,Y components, and use those to greate a Coords
         * object.
         *
         * @param c
         * @return
         */
        private Coords parseCoords(String c) {
            if(null == c || c.isEmpty()) {
                return null;
            }
            String[] elements = c.split(",", -1); //$NON-NLS-1$
            if(elements.length != 2) {
                return null;
            }
            try {
                int x = Integer.parseInt(elements[0]);
                int y = Integer.parseInt(elements[1]);
                return new Coords(x, y);
            } catch(NumberFormatException nfe) {
                return null;
            }
        }
        
        /**
         * Given a string with the format <imageFile>(X,Y-W,H), load the image file and then use X,Y and W,H to find a
         * subimage within the original image and return that subimage.
         *
         */
        @Override
        public Image loadImage(String fileName) {
            int tileStart = fileName.indexOf('(');
            int tileEnd = fileName.indexOf(')');
            if((tileStart == -1) || (tileEnd == -1) || (tileEnd < tileStart)) {
                return null;
            }
            String coords = fileName.substring(tileStart + 1, tileEnd);
            int coordsSplitter = coords.indexOf('-');
            if(coordsSplitter == -1) {
                return null;
            }
            Coords start = parseCoords(coords.substring(0, coordsSplitter));
            Coords size = parseCoords(coords.substring(coordsSplitter + 1));
            if((null == start) || (null == size) || (0 == size.getX()) || (0 == size.getY())) {
                return null;
            }
            String baseName = fileName.substring(0, tileStart);
            ToolkitImage base = (ToolkitImage) Toolkit.getDefaultToolkit().getImage(baseName);
            if(null == base) {
                return null;
            }
            FinishedLoadingObserver observer = new FinishedLoadingObserver(Thread.currentThread());
            // Check to see if the image is loaded
            int infoFlags = base.check(observer);
            if ((infoFlags & ImageObserver.ALLBITS) == 0) {
                // Image not loaded, wait for it to load
                long startTime = System.currentTimeMillis();
                long maxRuntime = 10000;
                long runTime = 0;
                base.preload(observer);
                while (!observer.isLoaded() && runTime < maxRuntime) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        // Do nothing
                    }
                    runTime = System.currentTimeMillis() - startTime;
                }
            }
            BufferedImage result = ImageUtil.createAcceleratedImage(Math.abs(size.getX()), Math.abs(size.getY()));
            Graphics2D g2d = result.createGraphics();
            g2d.drawImage(base, 0, 0, result.getWidth(), result.getHeight(),
                start.getX(), start.getY(), start.getX() + size.getX(), start.getY() + size.getY(), null);
            g2d.dispose();
            return result;
        }
    }

    /**
     * ImageLoader that loads subregions from a larger atlas file, but is given
     * filenames that are mapped into an atlas. When constructed, this class
     * reads in a map that maps image files to an atlas image and offset
     * location. When an image file is requested to be opened, it first looks to
     * see if the map contains that file, and if it does returns an image from
     * the corresponding key which includes an atlas and offset.
     */
    public static class AtlasImageLoader extends TileMapImageLoader {

        Map<String, String> imgFileToAtlasMap;

        @SuppressWarnings("unchecked")
        public AtlasImageLoader() {
            if (!Configuration.imageFileAtlasMapFile().exists()) {
                imgFileToAtlasMap = null;
                return;
            }

            try (InputStream is = new FileInputStream(Configuration.imageFileAtlasMapFile())) {
                XStream xstream = new XStream();
                imgFileToAtlasMap = (Map<String, String>) xstream.fromXML(is);
            } catch (FileNotFoundException e) {
                imgFileToAtlasMap = null;
                e.printStackTrace();
            } catch (IOException e) {
                imgFileToAtlasMap = null;
                e.printStackTrace();
            }
        }

        public Image loadImage(String fileName) {
            if ((imgFileToAtlasMap == null) || !imgFileToAtlasMap.containsKey(fileName)) {
                return null;
            }
            return super.loadImage(imgFileToAtlasMap.get(fileName));
        }
    }

    private static class FinishedLoadingObserver implements ImageObserver {
        private static final int DONE
            = ImageObserver.ABORT | ImageObserver.ERROR | ImageObserver.FRAMEBITS | ImageObserver.ALLBITS;
        
        private final Thread mainThread;
        private volatile boolean loaded = false;
        private volatile boolean animated = false;

        public FinishedLoadingObserver(Thread mainThread) {
            this.mainThread = mainThread;
        }
        
        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
            if((infoflags & DONE) > 0) {
                loaded = true;
                animated = ((infoflags & ImageObserver.FRAMEBITS) > 0);
                mainThread.interrupt();
                return false;
            }
            return true;
        }
        
        public boolean isLoaded() {
            return loaded;
        }
        
        public boolean isAnimated() {
            return animated;
        }
    }
}
