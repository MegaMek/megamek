/*
 * Copyright (c) 2000-2016 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.utilities;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import megamek.client.ui.swing.tileset.HexTileset;
import megamek.client.ui.swing.util.ImageAtlasMap;
import megamek.common.Configuration;
import megamek.logging.MMLogger;

/**
 * Program that will scan the data/images directory for images and take all of
 * the images in a subdirectory and store them in a single image atlas. All of
 * the files added to an atlas in this fashion will then be stored and then each
 * tileset file will be scanned and updated to reflect the new image location.
 *
 * @author arlith
 */
public class CreateImageAtlases {
    private static final MMLogger logger = MMLogger.create(CreateImageAtlases.class);

    /**
     * Keeps track of how many images per row we should have in the atlas
     */
    int imagesPerRow;

    int hexWidth = HexTileset.HEX_W;
    int hexHeight = HexTileset.HEX_H;

    /**
     * Keep a map of image paths stored in an atlas (relative to the image
     * directory), mapped to their location within the atlas. This can be
     * written to a file which can later be used when loading images to see if a
     * particular image can be loaded from an atlas instead.
     */
    ImageAtlasMap imgFileToAtlasMap = new ImageAtlasMap();

    /**
     * Keep track of what images have been written to an atlas. At the end, this
     * map can be saved to a file and then used with an
     * <code>ImageLoader</code>, so that images packaged into an atlas can still
     * be loaded by using their original filename.
     */
    Path imageDirPath = Configuration.imagesDir().toPath();

    /**
     * Keeps track of the paths to images (relative to the current directory)
     * that have been stored in an atlas. This can be written to a file for
     * later deletion.
     */
    ArrayList<String> imagesStored = new ArrayList<>();

    int improperImgDimsCount = 0;

    CreateImageAtlases() {
        this(10);
    }

    CreateImageAtlases(int imagesPerRow) {
        this.imagesPerRow = imagesPerRow;
    }

    void scanDirectory(File file) {
        if (file.isDirectory()) {
            // Ignore certain directories
            if (file.toString().contains("largeTextures")) {
                return;
            }
            processDirectory(file);
            for (File subFile : file.listFiles()) {
                if (subFile.isDirectory()) {
                    scanDirectory(subFile);
                }
            }
        }
    }

    /**
     * Find all of the image files in the given directory and generate an atlas
     * large enough to hold them, then iterate through each image and draw it
     * into the atlas. The atlas is then saved as "atlas-dirname.png".
     *
     * @param dir
     */
    void processDirectory(File dir) {
        logger.info("Processing: {}", dir);

        File[] imageFiles = dir.listFiles((dir1, name) -> ((name.toLowerCase().endsWith(".png") ||
                name.toLowerCase().endsWith(".gif") ||
                name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".jpeg"))
                && !name.endsWith("_atlas.png")));

        int numRows = (int) Math.ceil(imageFiles.length / (imagesPerRow + 0.0));

        // No images, nothing to do
        if (numRows <= 0) {
            return;
        }

        BufferedImage atlas = new BufferedImage(imagesPerRow * hexWidth, numRows * hexHeight,
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = atlas.getGraphics();
        File atlasFile = new File(dir, dir.getName() + "_atlas.png");
        String atlasLoc;

        int row;
        int col;

        row = col = 0;
        int x;
        int y;

        int writtenImages = 0;
        for (File imgFile : imageFiles) {
            BufferedImage currentImg;

            try {
                currentImg = ImageIO.read(imgFile);
            } catch (IOException e) {
                logger.error(e, "Error reading image.");
                e.printStackTrace();
                continue;
            }

            // Error checking
            if (currentImg.getHeight() != hexHeight || currentImg.getWidth() != hexWidth) {
                logger.info(
                        "Skipping image {} because dimensions don't match expected size. Image is {} x {} ( expected {} x {} )",
                        imgFile,
                        currentImg.getWidth(),
                        currentImg.getHeight(),
                        hexWidth,
                        hexHeight);
                improperImgDimsCount++;
                continue;
            }

            x = col * hexWidth;
            y = row * hexHeight;

            // Update imageFileToAtlas map
            atlasLoc = atlasFile + "(" + x + "," + y + "-" + hexWidth + "," + hexHeight + ")";
            File atlasLocFile = new File(atlasLoc);
            imgFileToAtlasMap.put(imgFile.toPath(), atlasLocFile.toPath());
            imagesStored.add(imgFile.toString());

            // Draw image in atlas
            g.drawImage(currentImg, x, y, null);

            // Update indices
            col++;
            if (col >= imagesPerRow) {
                col = 0;
                row++;
            }
            writtenImages++;
        }
        g.dispose();

        // Write out atlas
        if (writtenImages > 0) {
            try {
                ImageIO.write(atlas, "png", atlasFile);
            } catch (IOException e) {
                logger.error(e, "Unable to write atlas.");
            }
        }
    }

    /**
     * Write the imgFile to Atlas map to a file.
     */
    public void writeImgFileToAtlasMap() {
        imgFileToAtlasMap.writeToFile();
    }

    /**
     * Main entrypoint for the Image Atlas creation system. Can be ran from gradle
     * with
     *
     * ./gradlew createImageAtlases
     *
     * or from the jar file with
     *
     * java -cp MegaMek.jar megamek.utilities.CreateImageAtlases %lt;optional
     * filename%gt;
     *
     * @param args
     */
    public static void main(String[] args) {
        String fileName = "atlasedImages.txt";

        if (args.length > 0) {
            fileName = args[0];
        }

        CreateImageAtlases atlasCreator = new CreateImageAtlases();

        atlasCreator.imageDirPath = Configuration.unitImagesDir().toPath();
        atlasCreator.scanDirectory(Configuration.unitImagesDir());

        atlasCreator.imageDirPath = Configuration.hexesDir().toPath();
        atlasCreator.scanDirectory(Configuration.hexesDir());

        atlasCreator.writeImgFileToAtlasMap();

        try (FileWriter fw = new FileWriter(fileName);
                BufferedWriter bw = new BufferedWriter(fw)) {
            for (String imgFile : atlasCreator.imagesStored) {
                bw.write(imgFile);
                bw.write("\n");
            }
        } catch (IOException e) {
            logger.error(e, "Failed to write out list of atlased images!");
        }

        logger.info("Skipped {} images due to improper dimensions.",
                atlasCreator.improperImgDimsCount);
    }
}
