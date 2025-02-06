/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.utilities;

import com.squareup.gifencoder.GifEncoder;
import com.squareup.gifencoder.ImageOptions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static megamek.common.Configuration.gameSummaryImagesMMDir;

/**
 * GifWriter
 * Utility class to create a gif from a series of images of a game summary.
 * @author Luana Coppio
 */
public class GifWriter {

    /**
     * Creates a gif from a series of images of a game summary.
     * @param gameSummary the game summary to create the gif from, its commonly a UUID inside the /logs/gameSummary/minimap folder
     * @throws IOException if an I/O error occurs
     */
    public static void createGifFromGameSummary(String gameSummary) throws IOException {
        new GifWriter().run(gameSummary);
    }

    private GifWriter() {}

    private void run(String gameSummary) throws IOException {
        File folder = new File(gameSummaryImagesMMDir(), gameSummary);

        List<File> files = new ArrayList<>(
            List.of(
                Objects.requireNonNull(
                    folder.listFiles(
                        // grab all .png files, but only the Movement and Firing images, otherwise it takes too long
                        (dir, name) -> name.endsWith(".png") && (name.contains("_12_M") || name.contains("_18_F"))
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

        File outputFile = new File(gameSummaryImagesMMDir(), gameSummary + ".gif");

        // grab the output image type from the first image in the sequence
        BufferedImage firstImage = ImageIO.read(files.get(0));

        OutputStream outputStream = new FileOutputStream(outputFile);

        var width = firstImage.getWidth();
        var height = firstImage.getHeight();

        var encoder = new GifEncoder(outputStream, width, height, 0);
        ImageOptions moveOption = new ImageOptions();
        moveOption.setDelay(200, TimeUnit.MILLISECONDS);
        ImageOptions attackOption = new ImageOptions();
        attackOption.setDelay(400, TimeUnit.MILLISECONDS);
        int[] rgbData;
        boolean odd = true;
        for (File imageFile : files) {
            BufferedImage nextImage = ImageIO.read(imageFile);
            rgbData = nextImage.getRGB(0, 0, width, height, null, 0, width);
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

    public static void main(String[] args) throws Exception {
        GifWriter.createGifFromGameSummary("2d27bf27-03d3-4fd4-b573-dc019d9204ca");
    }

}
