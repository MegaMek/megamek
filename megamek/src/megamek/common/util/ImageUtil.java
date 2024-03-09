/*
* MegaMek -
* Copyright (C) 2000-2016 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common.util;

import java.awt.*;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Hashtable;
import java.util.List;

import megamek.MMConstants;
import megamek.client.ui.swing.util.ImageAtlasMap;
import megamek.client.ui.swing.util.ImprovedAveragingScaleFilter;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Coords;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import org.apache.logging.log4j.LogManager;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Generic utility methods for image data
 */
public final class ImageUtil {
    /**
     * The graphics configuration of the local graphic card/monitor combination,
     * if we aren't running in "headless" mode.
     */
    private static final GraphicsConfiguration GC;
    static {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = null;
        try {
            gd = ge.getDefaultScreenDevice();
        } catch (HeadlessException ignored) {

        }
        GC = (null != gd) ? gd.getDefaultConfiguration() : null;
    }

    public static final int IMAGE_SCALE_BICUBIC = 1;
    public static final int IMAGE_SCALE_AVG_FILTER = 2;
    
    /** Holds a drawn "fail" image that can be used when image loading fails. */
    public static BufferedImage failStandardImage; 

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
        return (GC == null) ? new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
                : GC.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
    }
    
    /** 
     * Returns a standard size (84x72) "fail" image having a red on white cross. 
     * The image is drawn, not loaded and should therefore work in almost all cases. 
     */
    public static BufferedImage failStandardImage() {
        if (failStandardImage == null) {
            failStandardImage = new BufferedImage(84, 72, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = failStandardImage.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 84, 72);
            graphics.setStroke(new BasicStroke(4f));
            graphics.setColor(Color.RED);
            graphics.drawLine(62, 56, 22, 16);
            graphics.drawLine(62, 16, 22, 56);
        }
        return failStandardImage;
    }
    
    /**
     * Get a scaled version of the input image.
     *
     * @param img
     * @return
     */
    public static BufferedImage getScaledImage(Image img, int newWidth, int newHeight) {
        return getScaledImage(img, newWidth, newHeight, IMAGE_SCALE_BICUBIC);
    }

    /**
     * Converts the given image to a BufferedImage. If it already is a BufferedImage, the image is only
     * returned with a type cast without doing any further conversion.
     *
     * @param image An Image of any type
     * @return The image as a BufferedImage
     */
    public static BufferedImage convertToBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        } else {
            return getScaledImage(image, image.getWidth(null), image.getHeight(null));
        }
    }

    /**
     * Returns a scaled version of the given image. Scaling is adjusted so that the image fits in the
     * given maximum width and maximum height, keeping the aspect ratio of the image. Either the height
     * or the width of the resulting image will be equal to the given max width or height, while the other value
     * will be smaller than the given maximum. Uses the supplied scaling method.
     *
     * @param image The image to scale
     * @param maxWidth The maximum width of the resulting image
     * @param maxHeight The maximum height of the resulting image
     * @param scaleType The scale type, {@link #IMAGE_SCALE_BICUBIC} or {@link #IMAGE_SCALE_AVG_FILTER}
     * @return A scaled image fitting in a rectangle of the size (maxWidth, maxHeight)
     */
    public static BufferedImage fitImage(Image image, int maxWidth, int maxHeight, int scaleType) {
        int width = maxWidth;
        int height = maxHeight;
        if ((float) image.getWidth(null) / maxWidth >
                (float) image.getHeight(null) / maxHeight) {
            height = image.getHeight(null) * maxWidth / image.getWidth(null);
        } else {
            width = image.getWidth(null) * maxHeight / image.getHeight(null);
        }
        return getScaledImage(image, width, height, scaleType);
    }

    /**
     * Get a scaled version of the input image, using the supplied type to
     * select which scaling method to use.
     *
     * @param img
     * @return
     */
    public static BufferedImage getScaledImage(Image img, int newWidth, int newHeight, int scaleType) {
        if (scaleType == IMAGE_SCALE_BICUBIC) {
            BufferedImage scaled = createAcceleratedImage(newWidth, newHeight);
            Graphics2D g2 = (Graphics2D) scaled.getGraphics();
            UIUtil.setHighQualityRendering(g2);
            g2.drawImage(img, 0, 0, newWidth, newHeight, null);
            return scaled;
        } else {
            ImageFilter filter = new ImprovedAveragingScaleFilter(img.getWidth(null),
                    img.getHeight(null), newWidth, newHeight);

            ImageProducer prod = new FilteredImageSource(img.getSource(), filter);
            Image result = Toolkit.getDefaultToolkit().createImage(prod);
            waitUntilLoaded(result);
            return ImageUtil.createAcceleratedImage(result);
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

    /**
     * Loads and returns the image of the given fileName. This method does not make sure the image
     * is fully loaded, this must be done by the caller when necessary (the simplest way is to
     * create a new ImageIcon with the image). If the image cannot be loaded for any reason,
     * the method returns a placeholder image but not null.
     *
     * @param fileName The image filename
     * @return The image if possible, a placeholder image otherwise
     */
    public static Image loadImageFromFile(String fileName) {
        if (null == fileName) {
            return failStandardImage();
        }
        for (ImageLoader loader : IMAGE_LOADERS) {
            Image img = loader.loadImage(fileName);
            if (null != img) {
                return img;
            }
        }
        return failStandardImage();
    }

    private ImageUtil() {}

    /**
     * Interface that defines methods for an ImageLoader.
     */
    public interface ImageLoader {

        /**
         * Given a string representation of a file,
         * @param fileName
         * @return
         */
        Image loadImage(String fileName);
    }

    /**
     * ImageLoader implementation that expects a path to an image file, and that file is loaded
     * directly and the loaded image is returned.
     */
    public static class AWTImageLoader implements ImageLoader {
        @Override
        public @Nullable Image loadImage(String fileName) {
            File fin = new File(fileName);
            if (!fin.exists()) {
                LogManager.getLogger().error("Trying to load image for a non-existent file " + fileName);
                return null;
            }
            Image result = Toolkit.getDefaultToolkit().getImage(fileName);
            if (result == null) {
                return null;
            }
            final boolean isAnimated = waitUntilLoaded(result);
            if ((result.getWidth(null) < 0) || (result.getHeight(null) < 0)) {
                return null;
            }
            return isAnimated ? result : ImageUtil.createAcceleratedImage(result);
        }
    }

    /**
     * ImageLoader that loads sub-regions from a larger atlas file. The filename is assumed to have
     * the format {imageFile}(X,Y-Width,Height), where X,Y is the start of the image tile, and
     * Width,Height are the size of the image tile.
     */
    public static class TileMapImageLoader implements ImageLoader {
        /**
         * Given a String with the format "X,Y" split this into the X,Y components, and use those to
         * create a Coords object.
         *
         * @param c
         * @return
         */
        protected @Nullable Coords parseCoords(@Nullable String c) {
            if (null == c || c.isEmpty()) {
                return null;
            }
            String[] elements = c.split(",", -1);
            if (elements.length != 2) {
                return null;
            }
            try {
                int x = Integer.parseInt(elements[0]);
                int y = Integer.parseInt(elements[1]);
                return new Coords(x, y);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }

        /**
         * Given a string with the format {imageFile}(X,Y-W,H), load the image file and then use
         * X,Y and W,H to find a sub-image within the original image and return that sub-image.
         */
        @Override
        public @Nullable Image loadImage(String fileName) {
            int tileStart = fileName.indexOf('(');
            int tileEnd = fileName.indexOf(')');
            if ((tileStart == -1) || (tileEnd == -1) || (tileEnd < tileStart)) {
                return null;
            }
            String coords = fileName.substring(tileStart + 1, tileEnd);
            int coordsSplitter = coords.indexOf('-');
            if (coordsSplitter == -1) {
                return null;
            }
            Coords start = parseCoords(coords.substring(0, coordsSplitter));
            Coords size = parseCoords(coords.substring(coordsSplitter + 1));
            if ((null == start) || (null == size) || (0 == size.getX()) || (0 == size.getY())) {
                return null;
            }
            String baseName = fileName.substring(0, tileStart);
            File baseFile = new File(baseName);
            if (!baseFile.exists()) {
                return null;
            }
            LogManager.getLogger().info("Loading atlas: " + baseFile);
            Image base = Toolkit.getDefaultToolkit().getImage(baseFile.getPath());
            if (null == base) {
                return null;
            }
            waitUntilLoaded(base);
            BufferedImage result = ImageUtil.createAcceleratedImage(Math.abs(size.getX()), Math.abs(size.getY()));
            Graphics2D g2d = result.createGraphics();
            g2d.drawImage(base, 0, 0, result.getWidth(), result.getHeight(),
                start.getX(), start.getY(), start.getX() + size.getX(), start.getY() + size.getY(), null);
            g2d.dispose();
            return result;
        }
    }

    /**
     * ImageLoader that loads sub-regions from a larger atlas file, but is given
     * file names that are mapped into an atlas. When constructed, this class
     * reads in a map that maps image files to an atlas image and offset
     * location. When an image file is requested to be opened, it first looks to
     * see if the map contains that file, and if it does returns an image from
     * the corresponding key which includes an atlas and offset.
     */
    public static class AtlasImageLoader extends TileMapImageLoader {
        ImageAtlasMap imgFileToAtlasMap;

        public AtlasImageLoader() {
            imgFileToAtlasMap = ImageAtlasMap.readFromFile();
        }

        @Override
        public Image loadImage(String fileName) {
            // The tileset could be using the tiling syntax to flip the image
            // We may still need to look up the base file name in an atlas and then modify the image
            int tileStart = fileName.indexOf('(');
            int tileEnd = fileName.indexOf(')');

            String baseName;
            Coords start, size;
            start = size = null;
            boolean tileAdjusting = (tileStart != -1) && (tileEnd != -1) && (tileEnd > tileStart);
            if (tileAdjusting) {
                String coords = fileName.substring(tileStart + 1, tileEnd);
                int coordsSplitter = coords.indexOf('-');
                // It's possible we have a unit with a paren in the name, we still want to try to load that
                if (coordsSplitter == -1) {
                    baseName = fileName;
                    tileAdjusting = false;
                } else {
                    start = parseCoords(coords.substring(0, coordsSplitter));
                    size = parseCoords(coords.substring(coordsSplitter + 1));
                    if ((null == start) || (null == size) || (0 == size.getX()) || (0 == size.getY())) {
                        return null;
                    }
                    // If we don't have any negative values, this entry isn't doing any image manipulation
                    // therefore, it must be a TileMapImageLoader entry, and we should ignore it
                    if (size.getX() > 0 && size.getY() > 0) {
                        return null;
                    }
                    baseName = fileName.substring(0, tileStart);
                }
            } else {
                baseName = fileName;
            }

            // Check to see if the base file is in an atlas
            File fn = new File(baseName);
            Path p = fn.toPath();
            if ((imgFileToAtlasMap == null) || !imgFileToAtlasMap.containsKey(p)) {
                return null;
            }

            // Check to see if we need to flip the image
            if (tileAdjusting) {
               Image img = super.loadImage(imgFileToAtlasMap.get(p));
               BufferedImage result = ImageUtil.createAcceleratedImage(Math.abs(size.getX()), Math.abs(size.getY()));
               Graphics2D g2d = result.createGraphics();
               g2d.drawImage(img, 0, 0, result.getWidth(), result.getHeight(),
                   start.getX(), start.getY(), start.getX() + size.getX(), start.getY() + size.getY(), null);
               g2d.dispose();
               return img;
            } else {
                // Otherwise just return the image loaded from the atlas
                return super.loadImage(imgFileToAtlasMap.get(p));
            }
        }
    }

    /**
     * Wait until the given toolkit image is fully loaded and return if the image is animated.
     *
     * @param result  Returns true if the given image is animated.
     * @return
     */
    private static boolean waitUntilLoaded(Image result) {
        FinishedLoadingObserver observer = new FinishedLoadingObserver(Thread.currentThread());
        // Check to see if the image is loaded
        if (!Toolkit.getDefaultToolkit().prepareImage(result, -1, -1, observer)) {
            long startTime = System.currentTimeMillis();
            long maxRuntime = 10000;
            long runTime = 0;
            while (!observer.isLoaded() && runTime < maxRuntime) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                    // Do nothing
                }
                runTime = System.currentTimeMillis() - startTime;
            }
        }
        return observer.isAnimated();
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
            if ((infoflags & DONE) > 0) {
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

    /**
     * takes an image and converts it to text in the Base64 encoding. When the given image is null, an
     * empty String is returned.
     */
    public static String base64TextEncodeImage(@Nullable Image image) {
        if (image == null) {
            return "";
        }
        BufferedImage bufferedImage = convertToBufferedImage(image);
        String base64Text;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            baos.flush();
            base64Text = Base64.getEncoder().encodeToString(baos.toByteArray());
            baos.close();
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }

        return base64Text;
    }

    /**
     * creates a ? image, used when units are hidden in double blind
     */
    public static void createDoubleBlindHiddenImage(Hashtable<Integer, String> imgCache) {
        BufferedImage image = new BufferedImage(56, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        UIUtil.setHighQualityRendering(graphics);
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, 56, 48);
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(UIManager.getColor("Label.foreground"));
        graphics.setFont(new Font(MMConstants.FONT_DIALOG, Font.BOLD, 26));
        graphics.drawString("?", 20, 40);

        String base64Text = base64TextEncodeImage(image);
        String img = "<img src='data:image/png;base64," + base64Text + "'>";
        imgCache.put(Report.HIDDEN_ENTITY_NUM, img);
    }
}
