/*
 * Copyright (c) 2000-2016 - Ben Mazur (bmazur@sev.org).
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.imageio.ImageIO;

import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.ImageAtlasMap;
import megamek.common.Configuration;
import megamek.logging.MMLogger;

/**
 * Program that will scan the data/images directory for images and take all the images in a subdirectory and store them
 * in a single image atlas. All the files added to an atlas in this fashion will then be stored and then each tileset
 * file will be scanned and updated to reflect the new image location.
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
     * Keep a map of image paths stored in an atlas (relative to the image directory), mapped to their location within
     * the atlas. This can be written to a file which can later be used when loading images to see if a particular image
     * can be loaded from an atlas instead.
     */
    ImageAtlasMap imgFileToAtlasMap = new ImageAtlasMap();

    /**
     * Keep track of what images have been written to an atlas. At the end, this map can be saved to a file and then
     * used with an
     * <code>ImageLoader</code>, so that images packaged into an atlas can still
     * be loaded by using their original filename.
     */
    Path imageDirPath = Configuration.imagesDir().toPath();

    /**
     * Keeps track of the paths to images (relative to the current directory) that have been stored in an atlas. This
     * can be written to a file for later deletion.
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

            File[] files = file.listFiles();

            if (files != null) {
                for (File subFile : files) {
                    if (subFile.isDirectory()) {
                        scanDirectory(subFile);
                    }
                }
            }
        }
    }

    /**
     * Find all the image files in the given directory and generate an atlas large enough to hold them, then iterate
     * through each image and draw it into the atlas. The atlas is then saved as "atlas-dirname.png".
     *
     */
    void processDirectory(File dir) {
        logger.info("Processing: {}", dir);

        File[] imageFiles = dir.listFiles((dir1, name) -> ((name.toLowerCase().endsWith(".png") ||
              name.toLowerCase().endsWith(".gif") ||
              name.toLowerCase().endsWith(".jpg") ||
              name.toLowerCase().endsWith(".jpeg"))
              && !name.endsWith("_atlas.png")));

        int numRows = 0;

        if (imageFiles != null) {
            numRows = (int) Math.ceil(imageFiles.length / ((double) imagesPerRow));
        }

        // No images, nothing to do
        if (numRows <= 0) {
            return;
        }

        BufferedImage atlas = new BufferedImage(imagesPerRow * hexWidth,
              numRows * hexHeight,
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
     * Main entrypoint for the Image Atlas creation system. Can be run from gradle with
     * <p>
     * ./gradlew createImageAtlases
     * <p>
     * or from the jar file with
     * <p>
     * java -cp MegaMek.jar megamek.utilities.CreateImageAtlases %lt;optional filename%gt;
     *
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
