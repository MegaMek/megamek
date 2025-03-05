/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.utilities;

import megamek.common.Coords;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ImageHexSlicer {
//I have an image that I need to cut and/or split it into multiple hexagons, those hexagons must follows an odd-q hexgrid, every hex is 84px wide, 72 px tall.
//
//The number of columns and rows that the image is going to be split into is a parameters.
//The image that is going to be sourced has also to be resized so the hex cut grid fits neatly inside of it. The image must always be resized in a way to keep its original ratio, and must after its being resized be bigger or of equal size to the final area of the cut.
//The clipping hex grid must be slightliy, to make sure that the cuts have the same amount of border in the top and in the bottom, and the same amount in the left and in the right. Always staying centralized in the image.
//The cutting logic must then return as response a Set of an record, this record must have two properties.
//- Image image
//- Coords coords
//
//Coords is an object with the contructor Coords(int x, int y), it serves as X,Y coordinates. Image is the java.awt.Image.
//This project is a java project. The original image is a png and it has transparency in it, the finalized records of the split images must also keep the transparency.
//
//I also need the function to have a debug flag inside of it to persist the images as files with the name following the template x_#_y_#.png

    /**
     * Represents a hexagon tile with its image and coordinates.
     */
    public static class HexTile {
        private final Image image;
        private final Coords coords;

        public HexTile(Image image, Coords coords) {
            this.image = image;
            this.coords = coords;
        }

        public Image getImage() {
            return image;
        }

        public Coords getCoords() {
            return coords;
        }
    }

    /**
     * Cuts an image into flat-topped hexagonal tiles based on an odd-q grid.
     *
     * @param sourceImage The source image to cut
     * @param columns Number of columns in the hex grid
     * @param rows Number of rows in the hex grid
     * @param debug If true, saves the individual hexagon images to disk
     * @return A set of HexTile objects containing the cut images and their coordinates
     * @throws IOException If there's an error processing the image
     */
    public static Set<HexTile> cutImageIntoHexGrid(
        BufferedImage sourceImage,
        int columns,
        int rows,
        boolean debug) throws IOException {
        int hexWidth = 84;
        int hexHeight = 72;
        // Calculate the required dimensions for the grid
        int gridWidth = columns * hexWidth;
        int gridHeight = (int) Math.ceil((rows + 0.5) * hexHeight);

        // Resize the source image while maintaining aspect ratio
        BufferedImage resizedImage = resizeImageToFitGrid(sourceImage, gridWidth, gridHeight);

        // Calculate centering offsets
        int offsetX = (resizedImage.getWidth() - gridWidth) / 2;
        int offsetY = (resizedImage.getHeight() - gridHeight) / 2;

        // Create the result set
        Set<HexTile> result = new HashSet<>();

        // For each position in the grid
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                // Calculate the center position of the current hexagon
                // For odd-q grid with flat-topped hexagons, even columns are displaced downward
                int centerX = col * hexWidth + offsetX + hexWidth / 2;
                int centerY = row * hexHeight + offsetY + hexHeight / 2;

                // Apply the offset for even columns (displacement downward)
                if (col % 2 != 0) {
                    centerY += hexHeight / 2;
                }

                // Create flat-topped hexagon polygon
                Polygon hexagon = createFlatTopHexagon(centerX, centerY, hexWidth, hexHeight);

                // Cut the hexagon from the image
                BufferedImage hexImage = cutHexagonFromImage(resizedImage, hexagon, hexWidth, hexHeight);

                // Create the coordinates for this hexagon
                Coords coords = new Coords(col, row);

                // Add to result set
                result.add(new HexTile(hexImage, coords));

                // Save the image if debug mode is enabled
                if (debug) {
                    File outputFile = new File("x_" + col + "_y_" + row + ".png");
                    ImageIO.write(hexImage, "png", outputFile);
                }
            }
        }

        return result;
    }

    /**
     * Resizes the source image to fit the required grid dimensions while maintaining aspect ratio.
     * The image will be equal to or larger than the grid dimensions.
     */
    private static BufferedImage resizeImageToFitGrid(BufferedImage sourceImage, int gridWidth, int gridHeight) {
        // Calculate the scale factors for width and height
        double scaleX = (double) gridWidth / sourceImage.getWidth();
        double scaleY = (double) gridHeight / sourceImage.getHeight();

        // Use the larger scale factor to ensure the image is at least as large as required
        double scale = Math.max(scaleX, scaleY);

        // Calculate new dimensions
        int newWidth = (int) Math.ceil(sourceImage.getWidth() * scale);
        int newHeight = (int) Math.ceil(sourceImage.getHeight() * scale);

        // Create the resized image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the resized image
        g2d.drawImage(sourceImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    /**
     * Creates a flat-topped hexagon polygon at the specified center coordinates.
     */
    private static Polygon createFlatTopHexagon(int centerX, int centerY, int width, int height) {
        Polygon hexagon = new Polygon();

        // For a flat-topped hexagon, we start at the right point and go counterclockwise
        // We need to adjust the width and height to create the correct hexagon shape
        int halfWidth = width / 2;
        int halfHeight = height / 2;

        // Starting from the rightmost vertex and moving counterclockwise
        hexagon.addPoint(centerX + halfWidth, centerY);
        hexagon.addPoint(centerX + halfWidth/2, centerY + halfHeight);
        hexagon.addPoint(centerX - halfWidth/2, centerY + halfHeight);
        hexagon.addPoint(centerX - halfWidth, centerY);
        hexagon.addPoint(centerX - halfWidth/2, centerY - halfHeight);
        hexagon.addPoint(centerX + halfWidth/2, centerY - halfHeight);

        return hexagon;
    }

    /**
     * Cuts a hexagon from the source image and returns it as a separate image.
     */
    private static BufferedImage cutHexagonFromImage(BufferedImage sourceImage, Polygon hexagon, int width, int height) {
        // Create a new image for the hexagon with transparency
        BufferedImage hexImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = hexImage.createGraphics();

        // Set up rendering quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Calculate the bounds of the hexagon
        Rectangle bounds = hexagon.getBounds();

        // Center the hexagon in the new image
        int txOffset = (width - bounds.width) / 2;
        int tyOffset = (height - bounds.height) / 2;

        // Create a transform to center the hexagon
        AffineTransform transform = new AffineTransform();
        transform.translate(txOffset - bounds.x, tyOffset - bounds.y);

        // Apply the transform to the hexagon
        Polygon translatedHex = new Polygon();
        for (int i = 0; i < hexagon.npoints; i++) {
            translatedHex.addPoint(
                hexagon.xpoints[i] + txOffset - bounds.x,
                hexagon.ypoints[i] + tyOffset - bounds.y
            );
        }

        // Set the clip to the hexagon shape
        g2d.setClip(translatedHex);

        // Draw the source image's corresponding part
        g2d.drawImage(
            sourceImage,
            txOffset - bounds.x,
            tyOffset - bounds.y,
            null
        );

        g2d.dispose();

        return hexImage;
    }

    /**
     * Stitches a set of hexagonal tiles back into a complete image and saves it to a file.
     *
     * @param hexTiles The set of HexTile objects to stitch together
     * @param outputPath The file path where the stitched image should be saved
     * @param imageFormat The format of the output image (e.g., "png", "jpg")
     * @throws IOException If there's an error saving the image
     */
    public static void stitchAndSaveImage(
        Set<HexTile> hexTiles,
        String outputPath,
        String imageFormat) throws IOException {
        int hexWidth = 84;
        int hexHeight = 72;
        // Find the maximum x and y coordinates to determine the size of the final image
        int maxX = -1;
        int maxY = -1;

        for (HexTile tile : hexTiles) {
            Coords coords = tile.getCoords();
            maxX = Math.max(maxX, coords.getX());
            maxY = Math.max(maxY, coords.getY());
        }

        // Calculate the dimensions of the final image
        // For odd-q flat-topped hexagons
        int columns = maxX + 1; // 0-indexed
        int rows = maxY + 1;    // 0-indexed

        int imageWidth = columns * hexWidth;
        int imageHeight = (int) Math.ceil((rows + 0.5) * hexHeight);

        // Create a new image with transparency
        BufferedImage stitchedImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = stitchedImage.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw each hexagon tile to the appropriate position in the stitched image
        for (HexTile tile : hexTiles) {
            Coords coords = tile.getCoords();
            BufferedImage tileImage = toBufferedImage(tile.getImage());

            int col = coords.getX();
            int row = coords.getY();

            // Calculate the position for this tile
            // For odd-q grid with flat-topped hexagons, even columns are displaced downward
            int x = col * hexWidth * 4 / 5;
            int y = row * hexHeight;

            // Apply the offset for even columns (displacement downward)
            if (col % 2 != 0) {
                y += hexHeight / 2;
            }

            // Draw the tile
            g2d.drawImage(tileImage, x, y, null);
        }

        g2d.dispose();

        // Save the stitched image
        File outputFile = new File(outputPath);
        ImageIO.write(stitchedImage, imageFormat, outputFile);
    }

    /**
     * Converts an Image object to a BufferedImage.
     * This is needed because the HexTile class stores images as the more generic Image type.
     */
    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a new buffered image with transparency
        BufferedImage bufferedImage = new BufferedImage(
            img.getWidth(null),
            img.getHeight(null),
            BufferedImage.TYPE_INT_ARGB
        );

        // Draw the image onto the buffered image
        Graphics2D g = bufferedImage.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();

        return bufferedImage;
    }

    /**
     * Example usage of the hex grid image cutter.
     */
    public static void main(String[] args) {
        try {
            // Load the source image
            BufferedImage sourceImage = ImageIO.read(new File(args[0]));

            // Parameters
            int columns = 9;
            int rows = 9;

            boolean debug = true;

            // Cut the image
            Set<HexTile> hexTiles = cutImageIntoHexGrid(sourceImage, columns, rows, debug);

            System.out.println("Successfully cut the image into " + hexTiles.size() + " hexagonal tiles.");

            // Example of accessing the tiles
            for (HexTile tile : hexTiles) {
                System.out.println("Tile at coordinates: " + tile.getCoords());
            }

            stitchAndSaveImage(hexTiles, "stitched_image.png", "png");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
