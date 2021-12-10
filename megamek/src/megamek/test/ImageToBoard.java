/*
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
 *  Copyright © 2014 Nicholas Walczak (walczak@cs.umn.edu)
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
package megamek.test;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * This program was designed to test the idea of turning an image into a board
 * file for Megamek.  It takes an image, and then cuts the image the image into
 * hex-sized bits and saves each of those hex images as a file.  It also
 * generates a board file with each hex image mapped to a fluff number for the
 * hex it belongs to.  The fluff numbers are also written to a file that can be
 * added to a tileset file.
 * 
 * This program really isn't complete, so many of the parameters are just hard
 * coded.  The basic premise works, although it needs more refinement.  I also
 * think that using terrain fluff to map an image to each hex is kind of an
 * abuse of the framework.
 * 
 * @author arlith
 * @date October 2014
 */
public class ImageToBoard {
    
    boolean loaded = false;
    
    int hexCols = 41;
    
    int hexRows = 51;
    
    int colOffset = 6;
    
    int rowOffset = 12;
    
    /**
     * Width of a hex in MegaMek.
     */
    int hexWidth = 84;
    
    /**
     * Height of a hex in Megamek.
     */
    int hexHeight = 72;
    
    BufferedImage src, hexTemplate;     
    
    BufferedWriter tilesetOut, boardOut;
    
    String outputDir;
    
    public static void main(String[] args) {
        String fileName = "/home/walczak/Downloads/ChYWZZx.jpg";
        String outDir = "/home/walczak/Downloads/tmp";
        
        ImageToBoard mm = new ImageToBoard(fileName, outDir);        
        mm.process();
    }
    
    ImageToBoard(String inPath, String outDir) {
        outputDir = outDir;
        try {
            src = ImageIO.read(new File(inPath));
            hexTemplate = ImageIO.read(new File("data/images/misc/hex_filled.png"));
            tilesetOut = new BufferedWriter(new FileWriter(new File(outputDir,
                    "new.tileset")));
            boardOut = new BufferedWriter(new FileWriter(new File(outputDir,
                    "new.board")));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } 
        loaded = true;
    }
    
    void process() {
        if (!loaded) {
            return;
        }
        BufferedImage hexImg = new BufferedImage(hexWidth, hexHeight,
                BufferedImage.TYPE_INT_ARGB);
        
        try {
            boardOut.write("size " + hexCols + " " + hexRows + "\n");
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
        
        int black = (255 << 8) & (255 << 16) & (255); 
        int transparent = 0;
        Graphics hexGraphics = hexImg.getGraphics();
        int width = src.getWidth() - colOffset;
        int height = src.getHeight() - rowOffset;
        int mapHexHeight = height / hexRows;
        double tmp = Math.sin(Math.PI/6) * (mapHexHeight);
        int mapHexWidth = (int) (width / (mapHexHeight + tmp));
        int mapHexSpacing = width / hexCols;
        for (int col = 0; col < hexCols; col++) {
            for (int row = 0; row < hexRows; row++) {
                int x = colOffset + col * mapHexSpacing;
                int y = row * mapHexHeight;
                if (x % 2 == 1) {
                    y -= mapHexHeight/2;
                }
                if (x + mapHexWidth > width || y + mapHexHeight > height ||
                        y < 0) {
                    continue;
                }
                
                BufferedImage hexROI = src.getSubimage(x, y, mapHexWidth,
                        mapHexHeight);
                hexGraphics.drawImage(hexROI, 0, 0, hexWidth, hexHeight, null);
                for (int i = 0; i < hexWidth; i++) {
                    for (int j = 0; j < hexHeight; j++) {
                        if (hexTemplate.getRGB(i, j) == black) {
                            hexImg.setRGB(i,j, transparent);
                        }
                    }
                }
                String colName = String.format("%1$02d", col);
                String rowName = String.format("%1$02d", row);
                String fileName = "hexImage" + colName + rowName + ".png";
                try {
                    String terrName = colName + rowName;
                    File outFile = new File(outputDir, fileName);
                    ImageIO.write(hexImg, "PNG", outFile);
                    tilesetOut.write("super * \"fluff:99" + terrName
                            + "\" \"\" \"tmp/" + fileName + "\"\n");
                    colName = String.format("%1$02d", col+1);
                    rowName = String.format("%1$02d", row+1);
                    terrName = colName + rowName;
                    boardOut.write("hex " + terrName + " 0 \"fluff:99"
                            + terrName + "\" \"\"\n");
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
        try {
            boardOut.close();
            tilesetOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }        
    }
}
