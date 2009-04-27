/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.test.xml;

import gd.xml.ParseException;
import gd.xml.tiny.ParsedXML;
import gd.xml.tiny.TinyParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.zip.GZIPOutputStream;

import megamek.common.Coords;
import megamek.common.Game;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.ITerrainFactory;
import megamek.common.InfernoTracker;
import megamek.common.Terrains;
import megamek.common.xml.BoardEncoder;

/**
 * This class will confirm that the <code>BoardEncoder</code> is equivalent to
 * the serialization of a <code>Board</code> object. TODO: integrate JUnit
 * into this class.
 */
public class TestBoardEncoder {

    /**
     * Create a <code>Board</code>, encode it to XML, decode it from the XML,
     * and compare the serialized versions from before and after the encoding.
     */
    public static void main(String[] args) {

        // The Game containing the Board.
        IGame game = new Game();
        IBoard board = game.getBoard();
        Coords coords = null;
        boolean success = true;

        // Try to conduct the test.
        try {
            // The serialized board *before* it is encoded.
            ByteArrayOutputStream before = new ByteArrayOutputStream();
            GZIPOutputStream bzos = new GZIPOutputStream(before);
            ObjectOutputStream boos = new ObjectOutputStream(bzos);

            // The serialized board *after* it is encoded
            ByteArrayOutputStream after = new ByteArrayOutputStream();
            GZIPOutputStream azos = new GZIPOutputStream(after);
            ObjectOutputStream aoos = new ObjectOutputStream(azos);

            // The character writer for encoding.
            CharArrayWriter to = new CharArrayWriter();

            // Load the test board.
            board.load(TestBoardEncoder.getTestInputStream());

            // Add some infernos and fires.
            ITerrainFactory f = Terrains.getTerrainFactory();
            coords = new Coords(5, 3);
            board.addInfernoTo(coords, InfernoTracker.STANDARD_ROUND, 1);
            board.getHex(coords).addTerrain(f.createTerrain(Terrains.FIRE, 1));
            coords = new Coords(8, 6);
            board.addInfernoTo(coords, InfernoTracker.STANDARD_ROUND, 1);
            board.getHex(coords).addTerrain(f.createTerrain(Terrains.FIRE, 2));
            coords = new Coords(4, 10);
            board.getHex(coords).addTerrain(f.createTerrain(Terrains.FIRE, 2));
            coords = new Coords(7, 13);
            board.addInfernoTo(coords, InfernoTracker.STANDARD_ROUND, 2);
            board.getHex(coords).addTerrain(f.createTerrain(Terrains.FIRE, 2));
            coords = new Coords(11, 14);
            board.getHex(coords).addTerrain(f.createTerrain(Terrains.FIRE, 2));

            // Save a copy of the board before XML encoding.
            boos.writeObject(board);
            boos.close();

            // Encode the board in XML.
            BoardEncoder.encode(board, to);
            to.close();

            // Decode the board from XML.
            ParsedXML root = TinyParser.parseXML(new ByteArrayInputStream(to
                    .toString().getBytes()));
            Enumeration<?> rootChildren = root.elements();
            if (!rootChildren.hasMoreElements()) {
                throw new ParseException("No children of the root.");
            }
            ParsedXML rootNode = (ParsedXML) rootChildren.nextElement();
            board = BoardEncoder.decode(rootNode, game);

            // Save a copy of the board before XML encoding.
            aoos.writeObject(board);
            aoos.close();

            // Walk through the before and after, comparing each in turn.
            byte[] beforeBuf = before.toByteArray();
            byte[] afterBuf = after.toByteArray();
            if (beforeBuf.length != afterBuf.length) {
                System.out.print("Different lengths!!!  Before: ");
                System.out.print(beforeBuf.length);
                System.out.print(", After: ");
                System.out.print(afterBuf.length);
                System.out.println(".");
                success = false;
            } else {
                System.out.print("Comparing ");
                System.out.print(beforeBuf.length);
                System.out.println(" bytes.");
                for (int index = 0; success && index < beforeBuf.length; index++) {
                    if (beforeBuf[index] != afterBuf[index]) {
                        System.out.print("Different bytes at index ");
                        System.out.print(index);
                        System.out.print("!!!  Before: ");
                        System.out.print(beforeBuf[index]);
                        System.out.print(", After: ");
                        System.out.print(afterBuf[index]);
                        System.out.println(".");
                        success = false;
                    }
                }
            }

        } catch (Throwable err) {
            err.printStackTrace();
            success = false;
        }
        if (success) {
            System.out.println("Success!!!");
        }

    }

    /**
     * Create a test <code>Board</code> in an <code>InputStream</code>.
     * 
     * @return an <code>InputStream</code> containing a <code>Board</code>.
     */
    public static InputStream getTestInputStream() {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);

        // Write a test board to the writer.
        writer.println("size 16 17");
        writer.println("hex 0101 0 \"\" \"\"");
        writer.println("hex 0201 0 \"\" \"\"");
        writer.println("hex 0301 0 \"\" \"\"");
        writer.println("hex 0401 0 \"\" \"\"");
        writer.println("hex 0501 0 \"\" \"\"");
        writer.println("hex 0601 0 \"\" \"\"");
        writer.println("hex 0701 0 \"\" \"\"");
        writer.println("hex 0801 0 \"\" \"\"");
        writer.println("hex 0901 0 \"\" \"\"");
        writer.println("hex 1001 0 \"\" \"\"");
        writer.println("hex 1101 0 \"\" \"\"");
        writer.println("hex 1201 0 \"\" \"\"");
        writer.println("hex 1301 0 \"\" \"\"");
        writer.println("hex 1401 0 \"\" \"\"");
        writer.println("hex 1501 0 \"\" \"\"");
        writer.println("hex 1601 0 \"\" \"\"");
        writer.println("hex 0102 0 \"\" \"\"");
        writer.println("hex 0202 0 \"woods:1\" \"\"");
        writer.println("hex 0302 0 \"woods:1\" \"\"");
        writer.println("hex 0402 0 \"woods:1\" \"\"");
        writer.println("hex 0502 0 \"\" \"\"");
        writer.println("hex 0602 0 \"\" \"\"");
        writer.println("hex 0702 0 \"\" \"\"");
        writer.println("hex 0802 0 \"\" \"\"");
        writer.println("hex 0902 0 \"woods:1\" \"\"");
        writer.println("hex 1002 0 \"rough:1\" \"\"");
        writer.println("hex 1102 1 \"\" \"\"");
        writer.println("hex 1202 2 \"\" \"\"");
        writer.println("hex 1302 1 \"\" \"\"");
        writer.println("hex 1402 2 \"\" \"\"");
        writer.println("hex 1502 0 \"\" \"\"");
        writer.println("hex 1602 0 \"\" \"\"");
        writer.println("hex 0103 0 \"\" \"\"");
        writer.println("hex 0203 0 \"\" \"\"");
        writer.println("hex 0303 0 \"woods:2\" \"\"");
        writer.println("hex 0403 0 \"woods:1\" \"\"");
        writer.println("hex 0503 0 \"\" \"\"");
        writer.println("hex 0603 0 \"\" \"\"");
        writer.println("hex 0703 0 \"\" \"\"");
        writer.println("hex 0803 0 \"\" \"\"");
        writer.println("hex 0903 0 \"\" \"\"");
        writer.println("hex 1003 0 \"\" \"\"");
        writer.println("hex 1103 0 \"rough:1\" \"\"");
        writer.println("hex 1203 0 \"\" \"\"");
        writer.println("hex 1303 2 \"woods:1\" \"\"");
        writer.println("hex 1403 3 \"\" \"\"");
        writer.println("hex 1503 0 \"\" \"\"");
        writer.println("hex 1603 0 \"\" \"\"");
        writer.println("hex 0104 0 \"\" \"\"");
        writer.println("hex 0204 0 \"\" \"\"");
        writer.println("hex 0304 0 \"\" \"\"");
        writer.println("hex 0404 0 \"\" \"\"");
        writer.println("hex 0504 0 \"\" \"\"");
        writer.println("hex 0604 0 \"\" \"\""); // Inferno hex
        writer.println("hex 0704 0 \"\" \"\"");
        writer.println("hex 0804 0 \"\" \"\"");
        writer.println("hex 0904 0 \"\" \"\"");
        writer.println("hex 1004 0 \"\" \"\"");
        writer.println("hex 1104 0 \"\" \"\"");
        writer.println("hex 1204 2 \"\" \"\"");
        writer.println("hex 1304 3 \"\" \"\"");
        writer.println("hex 1404 0 \"\" \"\"");
        writer.println("hex 1504 0 \"\" \"\"");
        writer.println("hex 1604 0 \"\" \"\"");
        writer.println("hex 0105 0 \"\" \"\"");
        writer.println("hex 0205 0 \"\" \"\"");
        writer.println("hex 0305 0 \"\" \"\"");
        writer.println("hex 0405 0 \"\" \"\"");
        writer.println("hex 0505 0 \"\" \"\"");
        writer.println("hex 0605 0 \"\" \"\"");
        writer.println("hex 0705 0 \"\" \"\"");
        writer.println("hex 0805 0 \"\" \"\"");
        writer.println("hex 0905 0 \"\" \"\"");
        writer.println("hex 1005 0 \"\" \"\"");
        writer.println("hex 1105 0 \"woods:1\" \"\"");
        writer.println("hex 1205 0 \"woods:2\" \"\"");
        writer.println("hex 1305 1 \"\" \"\"");
        writer.println("hex 1405 1 \"\" \"\"");
        writer.println("hex 1505 0 \"\" \"\"");
        writer.println("hex 1605 0 \"\" \"\"");
        writer.println("hex 0106 0 \"\" \"\"");
        writer.println("hex 0206 0 \"\" \"\"");
        writer.println("hex 0306 0 \"\" \"\"");
        writer.println("hex 0406 0 \"\" \"\"");
        writer.println("hex 0506 0 \"\" \"\"");
        writer.println("hex 0606 0 \"\" \"\"");
        writer.println("hex 0706 0 \"\" \"\"");
        writer.println("hex 0806 0 \"\" \"\"");
        writer.println("hex 0906 0 \"\" \"\"");
        writer.println("hex 1006 0 \"woods:1\" \"\"");
        writer.println("hex 1106 0 \"\" \"\"");
        writer.println("hex 1206 0 \"\" \"\"");
        writer.println("hex 1306 0 \"woods:1\" \"\"");
        writer.println("hex 1406 0 \"\" \"\"");
        writer.println("hex 1506 0 \"\" \"\"");
        writer.println("hex 1606 0 \"\" \"\"");
        writer.println("hex 0107 0 \"\" \"\"");
        writer.println("hex 0207 0 \"\" \"\"");
        writer.println("hex 0307 0 \"\" \"\"");
        writer.println("hex 0407 0 \"\" \"\"");
        writer.println("hex 0507 0 \"\" \"\"");
        writer.println("hex 0607 0 \"water:1\" \"\"");
        writer.println("hex 0707 0 \"water:1\" \"\"");
        writer.println("hex 0807 0 \"\" \"\"");
        writer.println("hex 0907 0 \"woods:1\" \"\""); // Inferno Hex
        writer.println("hex 1007 0 \"woods:1\" \"\"");
        writer.println("hex 1107 0 \"\" \"\"");
        writer.println("hex 1207 0 \"\" \"\"");
        writer.println("hex 1307 0 \"\" \"\"");
        writer.println("hex 1407 0 \"\" \"\"");
        writer.println("hex 1507 0 \"\" \"\"");
        writer.println("hex 1607 0 \"\" \"\"");
        writer.println("hex 0108 0 \"\" \"\"");
        writer.println("hex 0208 0 \"\" \"\"");
        writer.println("hex 0308 0 \"\" \"\"");
        writer.println("hex 0408 0 \"\" \"\"");
        writer.println("hex 0508 0 \"\" \"\"");
        writer.println("hex 0608 0 \"\" \"\"");
        writer.println("hex 0708 0 \"water:2\" \"\"");
        writer.println("hex 0808 0 \"water:1\" \"\"");
        writer.println("hex 0908 0 \"water:1\" \"\"");
        writer.println("hex 1008 0 \"\" \"\"");
        writer.println("hex 1108 0 \"\" \"\"");
        writer.println("hex 1208 0 \"\" \"\"");
        writer.println("hex 1308 0 \"\" \"\"");
        writer.println("hex 1408 0 \"\" \"\"");
        writer.println("hex 1508 0 \"\" \"\"");
        writer.println("hex 1608 0 \"\" \"\"");
        writer.println("hex 0109 0 \"\" \"\"");
        writer.println("hex 0209 0 \"\" \"\"");
        writer.println("hex 0309 0 \"\" \"\"");
        writer.println("hex 0409 0 \"\" \"\"");
        writer.println("hex 0509 0 \"\" \"\"");
        writer.println("hex 0609 0 \"woods:1\" \"\"");
        writer.println("hex 0709 0 \"\" \"\"");
        writer.println("hex 0809 0 \"water:1\" \"\"");
        writer.println("hex 0909 0 \"\" \"\"");
        writer.println("hex 1009 0 \"building:2;bldg_cf:40;bldg_elev:3\" \"\"");
        writer.println("hex 1109 0 \"\" \"\"");
        writer.println("hex 1209 0 \"\" \"\"");
        writer.println("hex 1309 0 \"\" \"\"");
        writer.println("hex 1409 0 \"\" \"\"");
        writer.println("hex 1509 0 \"\" \"\"");
        writer.println("hex 1609 0 \"\" \"\"");
        writer.println("hex 0110 0 \"\" \"\"");
        writer.println("hex 0210 0 \"\" \"\"");
        writer.println("hex 0310 0 \"\" \"\"");
        writer.println("hex 0410 0 \"\" \"\"");
        writer.println("hex 0510 0 \"\" \"\"");
        writer.println("hex 0610 0 \"woods:2\" \"\"");
        writer.println("hex 0710 0 \"water:1\" \"\"");
        writer.println("hex 0810 0 \"water:2\" \"\"");
        writer.println("hex 0910 0 \"water:1\" \"\"");
        writer.println("hex 1010 0 \"\" \"\"");
        writer.println("hex 1110 0 \"\" \"\"");
        writer.println("hex 1210 0 \"\" \"\"");
        writer.println("hex 1310 0 \"woods:1\" \"\"");
        writer.println("hex 1410 0 \"woods:1\" \"\"");
        writer.println("hex 1510 0 \"\" \"\"");
        writer.println("hex 1610 0 \"\" \"\"");
        writer.println("hex 0111 0 \"\" \"\"");
        writer.println("hex 0211 0 \"\" \"\"");
        writer.println("hex 0311 0 \"\" \"\"");
        writer.println("hex 0411 0 \"\" \"\"");
        writer.println("hex 0511 0 \"woods:1\" \"\""); // Fire hex
        writer.println("hex 0611 0 \"\" \"\"");
        writer.println("hex 0711 0 \"water:1\" \"\"");
        writer.println("hex 0811 0 \"\" \"\"");
        writer.println("hex 0911 0 \"\" \"\"");
        writer.println("hex 1011 0 \"\" \"\"");
        writer.println("hex 1111 0 \"\" \"\"");
        writer.println("hex 1211 0 \"\" \"\"");
        writer.println("hex 1311 0 \"rough:1\" \"\"");
        writer.println("hex 1411 0 \"\" \"\"");
        writer.println("hex 1511 0 \"\" \"\"");
        writer.println("hex 1611 0 \"\" \"\"");
        writer.println("hex 0112 0 \"\" \"\"");
        writer.println("hex 0212 0 \"\" \"\"");
        writer.println("hex 0312 0 \"\" \"\"");
        writer.println("hex 0412 0 \"\" \"\"");
        writer.println("hex 0512 0 \"\" \"\"");
        writer.println("hex 0612 0 \"\" \"\"");
        writer.println("hex 0712 0 \"\" \"\"");
        writer.println("hex 0812 0 \"\" \"\"");
        writer.println("hex 0912 0 \"\" \"\"");
        writer.println("hex 1012 0 \"\" \"\"");
        writer.println("hex 1112 0 \"woods:1\" \"\"");
        writer.println("hex 1212 0 \"woods:2\" \"\"");
        writer.println("hex 1312 1 \"\" \"\"");
        writer.println("hex 1412 0 \"\" \"\"");
        writer.println("hex 1512 0 \"\" \"\"");
        writer.println("hex 1612 0 \"\" \"\"");
        writer.println("hex 0113 0 \"\" \"\"");
        writer.println("hex 0213 0 \"\" \"\"");
        writer.println("hex 0313 0 \"\" \"\"");
        writer.println("hex 0413 0 \"\" \"\"");
        writer.println("hex 0513 0 \"\" \"\"");
        writer.println("hex 0613 0 \"\" \"\"");
        writer.println("hex 0713 0 \"\" \"\"");
        writer.println("hex 0813 0 \"\" \"\"");
        writer.println("hex 0913 0 \"\" \"\"");
        writer.println("hex 1013 0 \"\" \"\"");
        writer.println("hex 1113 0 \"woods:1\" \"\"");
        writer.println("hex 1213 2 \"\" \"\"");
        writer.println("hex 1313 2 \"\" \"\"");
        writer.println("hex 1413 2 \"\" \"\"");
        writer.println("hex 1513 0 \"\" \"\"");
        writer.println("hex 1613 0 \"\" \"\"");
        writer.println("hex 0114 0 \"\" \"\"");
        writer.println("hex 0214 0 \"\" \"\"");
        writer.println("hex 0314 0 \"\" \"\"");
        writer.println("hex 0414 0 \"woods:2\" \"\"");
        writer.println("hex 0514 0 \"woods:1\" \"\"");
        writer.println("hex 0614 0 \"\" \"\"");
        writer.println("hex 0714 0 \"\" \"\"");
        writer.println("hex 0814 0 \"building:2;bldg_cf:40;bldg_elev:2\" \"\""); // Inferno
                                                                                    // hex
        writer.println("hex 0914 0 \"\" \"\"");
        writer.println("hex 1014 0 \"\" \"\"");
        writer.println("hex 1114 0 \"\" \"\"");
        writer.println("hex 1214 3 \"woods:1\" \"\"");
        writer.println("hex 1314 3 \"woods:1\" \"\"");
        writer.println("hex 1414 0 \"\" \"\"");
        writer.println("hex 1514 0 \"\" \"\"");
        writer.println("hex 1614 0 \"\" \"\"");
        writer.println("hex 0115 0 \"\" \"\"");
        writer.println("hex 0215 0 \"\" \"\"");
        writer.println("hex 0315 0 \"woods:1\" \"\"");
        writer.println("hex 0415 0 \"\" \"\"");
        writer.println("hex 0515 0 \"woods:1\" \"\"");
        writer.println("hex 0615 0 \"\" \"\"");
        writer.println("hex 0715 0 \"\" \"\"");
        writer.println("hex 0815 0 \"\" \"\"");
        writer.println("hex 0915 0 \"\" \"\"");
        writer.println("hex 1015 0 \"rough:1\" \"\"");
        writer.println("hex 1115 1 \"\" \"\"");
        writer
                .println("hex 1215 2 \"building:4;bldg_cf:100;bldg_elev:1\" \"\""); // Fire
                                                                                    // hex
        writer.println("hex 1315 3 \"woods:1\" \"\"");
        writer.println("hex 1415 0 \"\" \"\"");
        writer.println("hex 1515 0 \"\" \"\"");
        writer.println("hex 1615 0 \"\" \"\"");
        writer.println("hex 0116 0 \"\" \"\"");
        writer.println("hex 0216 0 \"\" \"\"");
        writer.println("hex 0316 0 \"\" \"\"");
        writer.println("hex 0416 0 \"\" \"\"");
        writer.println("hex 0516 0 \"\" \"\"");
        writer.println("hex 0616 0 \"\" \"\"");
        writer.println("hex 0716 0 \"\" \"\"");
        writer.println("hex 0816 0 \"\" \"\"");
        writer.println("hex 0916 0 \"\" \"\"");
        writer.println("hex 1016 0 \"\" \"\"");
        writer.println("hex 1116 1 \"\" \"\"");
        writer.println("hex 1216 0 \"\" \"\"");
        writer.println("hex 1316 1 \"\" \"\"");
        writer.println("hex 1416 0 \"\" \"\"");
        writer.println("hex 1516 0 \"\" \"\"");
        writer.println("hex 1616 0 \"\" \"\"");
        writer.println("hex 0117 0 \"\" \"\"");
        writer.println("hex 0217 0 \"\" \"\"");
        writer.println("hex 0317 0 \"\" \"\"");
        writer.println("hex 0417 0 \"\" \"\"");
        writer.println("hex 0517 0 \"\" \"\"");
        writer.println("hex 0617 0 \"\" \"\"");
        writer.println("hex 0717 0 \"\" \"\"");
        writer.println("hex 0817 0 \"\" \"\"");
        writer.println("hex 0917 0 \"\" \"\"");
        writer.println("hex 1017 0 \"\" \"\"");
        writer.println("hex 1117 0 \"\" \"\"");
        writer.println("hex 1217 0 \"\" \"\"");
        writer.println("hex 1317 0 \"\" \"\"");
        writer.println("hex 1417 0 \"\" \"\"");
        writer.println("hex 1517 0 \"\" \"\"");
        writer.println("hex 1617 0 \"\" \"\"");
        writer.println("end");
        writer.close();

        // Return an InputStream containing the test board.
        return new ByteArrayInputStream(buffer.toString().getBytes());
    }

}
