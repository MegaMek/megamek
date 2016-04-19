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
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.List;

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
    
    /**
     * @return an image in a format best fitting for hardware acceleration, if possible,
     *         else just the image passed to it
     */
    public static BufferedImage createAcceleratedImage(BufferedImage base) {
        if(null == GC) {
            return base;
        }
        BufferedImage acceleratedImage
            = GC.createCompatibleImage(base.getWidth(), base.getHeight(), base.getTransparency());
        Graphics2D g2d = acceleratedImage.createGraphics();
        g2d.drawImage(base, 0, 0, base.getWidth(), base.getHeight(), null);
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

    /** Image loaders */
    private static final List<ImageLoader> IMAGE_LOADERS;
    static {
        IMAGE_LOADERS = new ArrayList<>();
        IMAGE_LOADERS.add(new TileMapImageLoader());
        IMAGE_LOADERS.add(new AWTImageLoader());
    }
    
    /** Add a new image loader to the first position of the list, if it isn't there already */
    public static void addImageLoader(ImageLoader loader) {
        if (null != loader && !IMAGE_LOADERS.contains(loader)) {
            IMAGE_LOADERS.add(0, loader);
        }
    }
    
    public static Image loadImageFromFile(String fileName, Toolkit toolkit) {
        if(null == fileName) {
            return null;
        }
        for (ImageLoader loader : IMAGE_LOADERS) {
            Image img = loader.loadImage(fileName, toolkit);
            if (null != img) {
                return img;
            }
        }
        return null;
    }
    
    private ImageUtil() {}
    
    public interface ImageLoader {
        Image loadImage(String fileName, Toolkit toolkit);
    }
    
    public static class AWTImageLoader implements ImageLoader {
        @Override
        public Image loadImage(String fileName, Toolkit toolkit) {
            ToolkitImage result = (ToolkitImage) toolkit.getImage(fileName);
            if(null == result) {
                return null;
            }
            FinishedLoadingObserver observer = new FinishedLoadingObserver(Thread.currentThread());
            result.preload(observer);
            while(!observer.isLoaded()) {
                try {
                    Thread.sleep(10);
                } catch(InterruptedException ex) {
                    break;
                }
            }
            return ImageUtil.createAcceleratedImage(result.getBufferedImage());
        }
    }
    
    public static class TileMapImageLoader implements ImageLoader {
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
        
        @Override
        public Image loadImage(String fileName, Toolkit toolkit) {
            int tileStart = fileName.indexOf('('); //$NON-NLS-1$
            int tileEnd = fileName.indexOf(')'); //$NON-NLS-1$
            if((tileStart == -1) || (tileEnd == -1) || (tileEnd < tileStart)) {
                return null;
            }
            String[] tileCoords = fileName.substring(tileStart + 1, tileEnd).split("-", -1); //$NON-NLS-1$
            if(tileCoords.length != 2) {
                return null;
            }
            Coords start = parseCoords(tileCoords[0]);
            Coords size = parseCoords(tileCoords[1]);
            if((null == start) || (null == size) || (0 == size.getX()) || (0 == size.getY())) {
                return null;
            }
            String baseName = fileName.substring(0, tileStart);
            ToolkitImage base = (ToolkitImage) toolkit.getImage(baseName);
            if(null == base) {
                return null;
            }
            FinishedLoadingObserver observer = new FinishedLoadingObserver(Thread.currentThread());
            base.preload(observer);
            while(!observer.isLoaded()) {
                try {
                    Thread.sleep(10);
                } catch(InterruptedException ex) {
                    break;
                }
            }
            BufferedImage result = ImageUtil.createAcceleratedImage(size.getX(), size.getY());
            Graphics2D g2d = result.createGraphics();
            g2d.drawImage(base, 0, 0, result.getWidth(), result.getHeight(),
                start.getX(), start.getY(), start.getX() + size.getX(), start.getY() + size.getY(), null);
            g2d.dispose();
            return result;
        }
    }
    
    private static class FinishedLoadingObserver implements ImageObserver {
        private static final int DONE
            = ImageObserver.ABORT | ImageObserver.ERROR | ImageObserver.ALLBITS;
        
        private final Thread mainThread;
        private volatile boolean loaded = false;

        public FinishedLoadingObserver(Thread mainThread) {
            this.mainThread = mainThread;
        }
        
        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
            if((infoflags & DONE) > 1) {
                loaded = true;
                mainThread.interrupt();
                return false;
            }
            return true;
        }
        
        public boolean isLoaded() {
            return loaded;
        }
    }
}
