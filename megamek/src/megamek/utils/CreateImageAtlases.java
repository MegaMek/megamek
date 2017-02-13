package megamek.utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.thoughtworks.xstream.XStream;

import megamek.client.ui.swing.HexTileset;
import megamek.common.Configuration;

/**
 * Program that will scan the data/images directory for images and take all of
 * the images in a subdirectory and store them in a single image atlas. All of
 * the files added to an atlas in this fashion will then be stored and then each
 * tileset file will be scanned and updated to reflect the new image location.
 *
 * @author arlith
 *
 */
public class CreateImageAtlases {

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
    Map<String, String> imgFileToAtlasMap = new HashMap<>();

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

    /**
     * 
     */
    CreateImageAtlases() {
        this(10);
    }

    /**
     * 
     * @param imagesPerRow
     */
    CreateImageAtlases(int imagesPerRow) {
        this.imagesPerRow = imagesPerRow;
    }

    /**
     * 
     * @param file
     */
    void scanDirectory(File file) {
        if (file.isDirectory()) {
            // Ignore certian directories
            if (file.toString().contains("hexes/largeTextures")) {
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
        System.out.println("Processing: " + dir.toString());
        File[] imageFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // Ignore other atlas files, just in case
                return (name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".gif")
                        || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg"))
                        && !name.endsWith("_atlas.png");
            }
        });

        int numRows = (int) Math.ceil(imageFiles.length / (imagesPerRow + 0.0));
        // No images, nothing to do
        if (numRows <= 0) {
            return;
        }
        BufferedImage atlas = new BufferedImage(imagesPerRow * hexWidth, numRows * hexHeight,
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = atlas.getGraphics();
        String imgPath;
        File atlasFile = new File(dir, dir.getName() + "_atlas.png");
        String atlasLoc;

        int row, col;
        row = col = 0;
        int x, y;
        int writtenImages = 0;
        for (File imgFile : imageFiles) {
            BufferedImage currImg;
            try {
                currImg = ImageIO.read(imgFile);
            } catch (IOException e) {
                // If we can't read it, ignore it
                e.printStackTrace();
                continue;
            }

            // Error checking
            if (currImg.getHeight() != hexHeight || currImg.getWidth() != hexWidth) {
                System.out.println("Skipping image " + imgFile + " because dimensions don't match.  Image is "
                        + currImg.getWidth() + " x " + currImg.getHeight());
                improperImgDimsCount++;
                continue;
            }
            x = col * hexWidth;
            y = row * hexHeight;

            // Update imageFileToAtlas map
            imgPath = imgFile.toString();
            atlasLoc = atlasFile.toString() + "(" + x + "," + y + "-" + hexWidth + "," + hexHeight + ")";
            imgFileToAtlasMap.put(imgPath, atlasLoc);
            imagesStored.add(imgFile.toString());

            // Draw image in atlas
            g.drawImage(currImg, x, y, null);

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
                e.printStackTrace();
            }
        }
    }

    /**
     * Write the imgFile to Atlas map to a file.
     */
    public void writeImgFileToAtlasMap() {
        XStream xstream = new XStream();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(Configuration.imageFileAtlasMapFile()),
                Charset.forName("UTF-8"));) {
            xstream.toXML(imgFileToAtlasMap, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     */
    public static void printUsage() {

    }

    /**
     * 
     * @param args
     * @return
     */
    public static void main(String[] args) {
        CreateImageAtlases atlasCreator = new CreateImageAtlases();

        atlasCreator.imageDirPath = Configuration.unitImagesDir().toPath();
        atlasCreator.scanDirectory(Configuration.unitImagesDir());

        atlasCreator.imageDirPath = Configuration.hexesDir().toPath();
        atlasCreator.scanDirectory(Configuration.hexesDir());

        atlasCreator.writeImgFileToAtlasMap();

        try (BufferedWriter fout = new BufferedWriter(new FileWriter(new File("atlasedImages.txt")))) {
            for (String imgFile : atlasCreator.imagesStored) {
                fout.write(imgFile);
                fout.write("\n");
            }
        } catch (IOException e) {
            System.out.println("Failed to write out list of atlased images!");
            e.printStackTrace();
        }

        System.out.println("Skipped " + atlasCreator.improperImgDimsCount + " images due to improper dimensions.");
    }
}
