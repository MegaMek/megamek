/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.utilities;

import static megamek.common.Configuration.gameSummaryImagesMMDir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import com.squareup.gifencoder.GifEncoder;
import com.squareup.gifencoder.ImageOptions;
import megamek.logging.MMLogger;

/**
 * GifWriter Utility class to create a gif from a series of images of a game summary.
 *
 * @author Luana Coppio
 */
public class GifWriter {
    private static final MMLogger LOGGER = MMLogger.create(GifWriter.class);
    private static final int START_OFFSET = 0;
    private static final int LOOP_COUNT = 0;
    private final File folder;
    private final File outputFile;
    private int height = -1;
    private int width = -1;
    private GifEncoder encoder = null;
    private boolean isEncoding = false;
    private boolean isLive = true;

    /**
     * Creates a gif from a series of images of a game summary.
     *
     * @param gameSummary the game summary to create the gif from, it's commonly a UUID inside the
     *                    /logs/gameSummary/minimap folder
     *
     * @throws IOException if an I/O error occurs
     */
    public static void createGifFromGameSummary(String gameSummary) throws IOException {
        new GifWriter(gameSummary).run();
    }

    /**
     * Creates a new GifWriter.
     *
     * @param gameSummary the game summary to create the gif from
     */
    public GifWriter(String gameSummary) {
        folder = new File(gameSummaryImagesMMDir(), gameSummary);
        outputFile = new File(folder, gameSummary + ".gif");
    }

    private OutputStream outputStream = null;

    /**
     * Appends a frame to the gif.
     *
     * @param image    the frame image
     * @param duration the frame duration in milliseconds
     *
     * @throws IOException if an I/O error occurs
     */
    public void appendFrame(BufferedImage image, long duration) throws IOException {
        ensureImageSize(image);
        int[] rgbData = image.getRGB(
              START_OFFSET,
              START_OFFSET,
              width,
              height,
              null,
              START_OFFSET,
              width);
        getEncoder().addImage(rgbData, width, getImageOptions(duration));
        LOGGER.info("Appended frame with duration {} ms, image size: {}x{}",
              duration, width, height);
    }

    private ImageOptions getImageOptions(long duration) {
        ImageOptions options = new ImageOptions();
        options.setDelay(duration, TimeUnit.MILLISECONDS);
        return options;
    }

    private GifEncoder getEncoder() throws IOException {
        if (encoder == null) {
            encoder = new GifEncoder(getOutputStream(), width, height, LOOP_COUNT);
            isEncoding = true;
            LOGGER.info("Starting GIF encoding with dimensions: {}x{}", width, height);
        }
        return encoder;
    }

    private void ensureImageSize(BufferedImage image) {
        if (width == -1 || height == -1) {
            width = image.getWidth();
            height = image.getHeight();
            LOGGER.info("Setting GIF dimensions to: {}x{}", width, height);
        } else if (width != image.getWidth() || height != image.getHeight()) {
            throw new IllegalArgumentException("Image dimensions do not match previous images");
        }
    }

    private OutputStream getOutputStream() throws FileNotFoundException {
        if (outputStream == null) {
            outputStream = new FileOutputStream(outputFile);
            LOGGER.info("Output stream created for GIF file: {}", outputFile.getAbsolutePath());
        }
        return outputStream;
    }

    /**
     * Closes the gif writer.
     */
    public void close() {
        if (encoder != null && isEncoding) {
            try {
                encoder.finishEncoding();
                LOGGER.info("Added the end block on the GIF encoding");
            } catch (IOException e) {
                LOGGER.error(e, "Error finishing encoding");
            }
            encoder = null;
        }
        if (outputStream != null) {
            try {
                outputStream.close();
                LOGGER.info("Output stream closed for GIF file: {}", outputFile.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error(e, "Error closing output stream for GIF file {}", outputFile.getAbsolutePath());
            }
        }
        outputStream = null;
        isLive = false;
    }

    private void run() throws IOException {
        List<File> files = new ArrayList<>(
              List.of(
                    Objects.requireNonNull(
                          folder.listFiles(
                                // grab all .png files, but only the Movement and Firing images, otherwise it takes too long
                                (dir, name) -> name.endsWith(".png") && (name.contains("_12_M")
                                      || name.contains("_18_F"))
                          )
                    )
              )
        );

        files.sort((o1, o2) -> {
            var splitO1 = o1.getName().split("_");
            var splitO2 = o2.getName().split("_");
            var splitO11 = Integer.parseInt(splitO1[1]) * 1000;
            splitO11 += Integer.parseInt(splitO1[2]);
            var splitO21 = Integer.parseInt(splitO2[1]) * 1000;
            splitO21 += Integer.parseInt(splitO2[2]);
            return Integer.compare(splitO11, splitO21);
        });

        // grab the output image type from the first image in the sequence
        BufferedImage firstImage = ImageIO.read(files.get(0));

        OutputStream outputStream = new FileOutputStream(outputFile);

        var width = firstImage.getWidth();
        var height = firstImage.getHeight();

        var encoder = new GifEncoder(outputStream, width, height, LOOP_COUNT);
        ImageOptions moveOption = getImageOptions(200);
        ImageOptions attackOption = getImageOptions(400);
        int[] rgbData;
        boolean odd = true;
        for (File imageFile : files) {
            BufferedImage nextImage = ImageIO.read(imageFile);
            rgbData = nextImage.getRGB(START_OFFSET, START_OFFSET, width, height, null, START_OFFSET, width);
            if (odd) {
                encoder.addImage(rgbData, width, moveOption);
            } else {
                encoder.addImage(rgbData, width, attackOption);
            }
            odd = !odd;
        }

        encoder.finishEncoding();
        outputStream.close();
    }

    /**
     * Returns whether the gif writer is live.
     *
     * @return true if the gif writer is live
     */
    public boolean isLive() {
        return isLive;
    }

    public boolean delete() {
        return outputFile.delete();
    }

    public File getOutputFile() {
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        GifWriter.createGifFromGameSummary(args[0]);
    }

}
