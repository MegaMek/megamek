/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2015 Nicholas Walczak (walczak@cs.umn.edu)
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
package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.widget.SkinSpecification.UIComponents;
import megamek.common.Configuration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class reads in an XML file that specifies different aspects of the 
 * visual skin for Megamek.
 * 
 * @author arlith
 *
 */
public class SkinXMLHandler {
    
    public static String SKIN_FOOTER = "</skin>";
    public static String SKIN_HEADER;

    static {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!--\n");
        sb.append("  This is the default skin for Megamek\n");
        sb.append("\n");
        sb.append("  New skins can be created by specifying UI_Element tags\n");
        sb.append("\n");
        sb.append("  The defaultElement UI_Element specifies the default border to be used by UI\n");
        sb.append("    components\n");
        sb.append("\n");
        sb.append("  The defaultButton UI_Element specifies the default border and background\n");
        sb.append("   images to use for Megamek buttons.  The first image is the base default\n");
        sb.append("   image and the second image is the pressed image\n");
        sb.append("\n");
        sb.append("  NOTE: All locations should be in data/images/widgets\n");
        sb.append("-->\n");
        sb.append("<skin xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("    xsi:noNamespaceSchemaLocation=\"skinSchema.xsl\">\n");
        SKIN_HEADER = sb.toString();
    }
    
    /**
     * The file name for the default Skin XML file, found in the config dir.
     */
    public static String defaultSkinXML = "defaultSkin.xml";
    
    public static String UI_ELEMENT = "UI_Element";
    public static String NAME = "name";
    public static String FONT_COLOR = "font_color";
    public static String BORDER = "border";
    public static String PLAIN = "plain";
    public static String NO_BORDER = "no_border";
    public static String TILE_BACKGROUND = "tile_background";
    public static String TR_CORNER = "corner_top_right";
    public static String TL_CORNER = "corner_top_left";
    public static String BR_CORNER = "corner_bottom_right";
    public static String BL_CORNER = "corner_bottom_left";
    public static String EDGE = "edge";
    public static String EDGE_NAME = "edgeName";
    public static String EDGE_ICON = "edgeIcon";
    public static String ICON = "icon";
    public static String TILED = "tiled";
    public static String TOP_LINE = "line_top";
    public static String BOTTOM_LINE = "line_bottom";
    public static String RIGHT_LINE = "line_right";
    public static String LEFT_LINE = "line_left";
    public static String BACKGROUND_IMAGE = "background_image";
    public static String SHOW_SCROLL_BARS = "show_scroll_bars";
    
    private static Hashtable<String, SkinSpecification> skinSpecs;
    
    /**
     * Checks whether the given path points to a file that is a valid skin
     * specification.
     * 
     * @param path
     * @return
     */
    public static boolean validSkinSpecFile(String fileName) {
        File file = new File(Configuration.configDir(), fileName);
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(file);
            // TODO: Just validate against the XSD
            // Until that's done, just assume anything with UI_ELEMENT tags is
            //  valid
            NodeList listOfComponents = doc.getElementsByTagName(UI_ELEMENT);
            if (listOfComponents.getLength() > 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Initializes using the default skin file.
     * @throws IOException
     */
    public static boolean initSkinXMLHandler() {
        String skinFilePath = GUIPreferences.getInstance().getSkinFile();
        return initSkinXMLHandler(skinFilePath);
    }
    
    /**
     * Initializes using the supplied skin file.
     * @throws IOException
     */
    public synchronized static boolean initSkinXMLHandler(String fileName) {

        if (fileName == null) {
            System.out.println("ERROR: Bad skin specification file: " +
                    "null filename!");
            return false;
        }
        
        File file = new File(Configuration.configDir(), fileName);
        if (!file.exists() || !file.isFile()) {
            System.out.println("ERROR: Bad skin specification file: " +
                    "file doesn't exist!  File name: " + fileName);
            return false;
        }

        // Build the XML document.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            System.out.println("Parsing " + file.getName());
            Document doc = builder.parse(file);
            System.out.println("Parsing finished.");

            // Get the list of units.
            NodeList listOfComponents = doc.getElementsByTagName(UI_ELEMENT);
            int totalComponents = listOfComponents.getLength();
            skinSpecs = new Hashtable<String, SkinSpecification>(
                    (int)(totalComponents * 1.25));
            for (int comp = 0; comp < totalComponents; comp++) {
                SkinSpecification skinSpec;
                Element borderList = (Element) listOfComponents.item(comp);
                
                // Parse no border
                Element noBorderEle = (Element) borderList
                        .getElementsByTagName(NO_BORDER).item(0);
                boolean noBorder = false;
                if (noBorderEle != null) {
                    noBorder = Boolean
                            .parseBoolean(noBorderEle.getTextContent());
                }
                
                // Get the first element of this node.
                Element plainTag = (Element) 
                        borderList.getElementsByTagName(PLAIN).item(0);
                // If there is no plain tag, load the icons
                if (plainTag == null && !noBorder) {
                    // Get the border specs
                    Element border = (Element) 
                            borderList.getElementsByTagName(BORDER).item(0);
                    if (border == null) {
                        System.err.println("Missing <" + BORDER +  
                                "> tag in element #" + comp);
                        continue;
                    }
                    
                    skinSpec = parseBorderTag(border);
                } else { // Plain skin, no icons
                    skinSpec = new SkinSpecification();
                    skinSpec.noBorder = noBorder;
                }
                
                // Get the background specs
                if (plainTag == null) {
                    NodeList backgrounds = 
                            borderList.getElementsByTagName(BACKGROUND_IMAGE);
                    if (backgrounds != null){
                        for (int bg = 0; bg < backgrounds.getLength(); bg++){
                            skinSpec.backgrounds.add(
                                    backgrounds.item(bg).getTextContent());
                        }
                    }
                }
                
                // Pase show scroll bars
                Element showScrollEle = (Element) borderList
                        .getElementsByTagName(SHOW_SCROLL_BARS).item(0);
                if (showScrollEle != null) {
                    skinSpec.showScrollBars = Boolean
                            .parseBoolean(showScrollEle.getTextContent());
                }
                
                // Parse font colors
                NodeList fontColors = borderList
                        .getElementsByTagName(FONT_COLOR);
                if (fontColors.getLength() > 0) {
                    skinSpec.fontColors.clear();
                    for (int fc = 0; fc < fontColors.getLength(); fc++) {
                        String fontColorContent = fontColors.item(fc)
                                .getTextContent();
                        skinSpec.fontColors.add(Color.decode(fontColorContent));
                    }
                }

                // Parse tile background
                Element tileBGEle = (Element) borderList
                        .getElementsByTagName(TILE_BACKGROUND).item(0);
                if (tileBGEle != null) {
                    skinSpec.tileBackground = Boolean
                            .parseBoolean(tileBGEle.getTextContent());
                }
                
                String name = borderList.getElementsByTagName(NAME).
                        item(0).getTextContent();

                skinSpecs.put(name, skinSpec);
            }
            
            if (!skinSpecs.containsKey(UIComponents.DefaultUIElement.getComp()) 
                    || !skinSpecs.containsKey(UIComponents.DefaultButton.getComp())) {
                System.out.println("ERROR: Bad skin specification file: " +
                        "file doesn't specify " + UIComponents.DefaultUIElement + 
                        " or " + UIComponents.DefaultButton + "!");
                return false;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     *  Create a new SkinSpecification and populate it from the supplied border
     *  tag
     *
     * @param border
     * @return
     */
    private static SkinSpecification parseBorderTag(Element border){
        SkinSpecification skinSpec = new SkinSpecification();

        // Parse Corner Icons
        skinSpec.tr_corner = border.getElementsByTagName(TR_CORNER).
                item(0).getTextContent();
        skinSpec.tl_corner = border.getElementsByTagName(TL_CORNER).
                item(0).getTextContent();
        skinSpec.br_corner = border.getElementsByTagName(BR_CORNER).
                item(0).getTextContent();
        skinSpec.bl_corner = border.getElementsByTagName(BL_CORNER).
                item(0).getTextContent();

        // Parse Edge Icons from Edge tag
        NodeList edgeNodes = border.getElementsByTagName(EDGE);
        for (int i = 0; i < edgeNodes.getLength(); i++){
            // An edge tag will have some number of icons
            ArrayList<String> icons = new ArrayList<String>();
            // And icon can be tiled or static
            ArrayList<Boolean> shouldTile = new ArrayList<Boolean>(); 
            NodeList edgeIcons = ((Element) edgeNodes.item(i))
                    .getElementsByTagName(EDGE_ICON);
            // Iterate through each icon/tiled pair
            for (int j = 0; j < edgeIcons.getLength(); j++){
                String icon = ((Element) edgeIcons.item(j))
                        .getElementsByTagName(ICON).item(0).getTextContent();
                String tiled = ((Element) edgeIcons.item(j))
                        .getElementsByTagName(TILED).item(0).getTextContent();

                if (icon == null){
                    System.err.println("Missing <" + ICON + "> tag");
                    continue;
                }
                if (tiled == null){
                    System.err.println("Missing <" + TILED + "> tag");
                    continue;
                }
                icons.add(icon);
                shouldTile.add(tiled.equalsIgnoreCase("true"));
            }

            String edgeName = ((Element) edgeNodes.item(i))
                    .getElementsByTagName(EDGE_NAME).item(0).getTextContent();

            if (edgeName == null){
                System.err.println("Missing <" + EDGE_NAME + "> tag");
                continue;
            }

            if (edgeName.equals("top")){
                skinSpec.topEdge = icons;
                skinSpec.topShouldTile = shouldTile;
            } else if (edgeName.equals("bottom")){
                skinSpec.bottomEdge = icons;
                skinSpec.bottomShouldTile = shouldTile;
            } else if (edgeName.equals("left")){
                skinSpec.leftEdge = icons;
                skinSpec.leftShouldTile = shouldTile;
            } else if (edgeName.equals("right")){
                skinSpec.rightEdge = icons;
                skinSpec.rightShouldTile = shouldTile;
            }
        }        
        return skinSpec;        
    }

    public static void writeSkinToFile(String filename) {
        String userDir = System.getProperty("user.dir");
        if (!userDir.endsWith(File.separator)) {
            userDir += File.separator;
        }
        String filePath = userDir + "mmconf" + File.separator
                + filename;

        Writer output = null;
        try {
            output = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(filePath)));
            output.write(SKIN_HEADER);
            for (String component : skinSpecs.keySet()) {
                writeSkinComponent(component, output);
            }
            output.write(SKIN_FOOTER);
            output.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Convenience method for writing out the UI_ELEMENT tag.
     * @param component
     * @param out
     * @throws IOException
     */
    private static void writeSkinComponent(String component, Writer out)
            throws IOException {
        out.write("\t<" + UI_ELEMENT + ">\n");

        // Write Component name
        out.write("\t\t<" + NAME + ">");
        out.write(component);
        out.write("</" + NAME + ">\n");

        SkinSpecification skinSpec = getSkin(component);

        // Write Border
        out.write("\t\t<" + NO_BORDER + ">");
        out.write(((Boolean)skinSpec.noBorder).toString());
        out.write("</" + NO_BORDER + ">\n");
        writeBorder(skinSpec, out);

        // Write background
        for (String bgImg : skinSpec.backgrounds) {
            out.write("\t\t<" + BACKGROUND_IMAGE + ">");
            out.write(bgImg);
            out.write("</" + BACKGROUND_IMAGE + ">\n");
        }
        out.write("\t\t<" + TILE_BACKGROUND + ">");
        out.write(((Boolean)skinSpec.tileBackground).toString());
        out.write("</" + TILE_BACKGROUND + ">\n");

        // Write colors
        for (Color fontColor : skinSpec.fontColors) {
            out.write("\t\t<" + FONT_COLOR + ">");
            out.write("#"
                    + Integer.toHexString(fontColor.getRGB()).substring(2));
            out.write("</" + FONT_COLOR + ">\n");
        }

        // Write show scroll bars
        out.write("\t\t<" + SHOW_SCROLL_BARS + ">");
        out.write(((Boolean)skinSpec.showScrollBars).toString());
        out.write("</" + SHOW_SCROLL_BARS + ">\n");

        // Close UI_ELEMENT tag
        out.write("\t</" + UI_ELEMENT + ">\n\n");
    }

    /**
     * Convenience method for writing out the BORDER element.
     *
     * @param skinSpec
     * @param out
     * @throws IOException
     */
    private static void writeBorder(SkinSpecification skinSpec, Writer out)
            throws IOException {
        out.write("\t\t<!-- Specification of border images -->\n");
        out.write("\t\t<" + BORDER + ">\n");

        out.write("\t\t\t<!-- Corner images -->\n");
        // Top Left Corner
        out.write("\t\t\t<" + TL_CORNER + ">");
        out.write(skinSpec.tl_corner);
        out.write("</" + TL_CORNER + ">\n");
        // Top Right Corner
        out.write("\t\t\t<" + TR_CORNER + ">");
        out.write(skinSpec.tr_corner);
        out.write("</" + TR_CORNER + ">\n");
        // Bottom Left Corner
        out.write("\t\t\t<" + BL_CORNER + ">");
        out.write(skinSpec.bl_corner);
        out.write("</" + BL_CORNER + ">\n");
        // Bottom Right Corner
        out.write("\t\t\t<" + BR_CORNER + ">");
        out.write(skinSpec.br_corner);
        out.write("</" + BR_CORNER + ">\n");

        // Edges
        out.write("\t\t\t<!-- Border lines: these images will be tiled -->\n");
        out.write("\t\t\t<" + EDGE + ">\n");
        // Top Edge
        for (int i = 0; i < skinSpec.topEdge.size(); i++) {
            out.write("\t\t\t\t<" + EDGE_ICON + ">\n");
            // Edge Icon
            out.write("\t\t\t\t\t<" + ICON + ">");
            out.write(skinSpec.topEdge.get(i));
            out.write("</" + ICON + ">\n");
            // Tile state of icon
            out.write("\t\t\t\t\t<" + TILED + ">");
            out.write(((Boolean)skinSpec.topShouldTile.get(i)).toString());
            out.write("</" + TILED + ">\n");
            out.write("\t\t\t\t</" + EDGE_ICON + ">\n");
        }

        out.write("\t\t\t\t<" + EDGE_NAME + ">");
        out.write("top");
        out.write("</" + EDGE_NAME + ">\n");

        out.write("\t\t\t</" + EDGE + ">\n");

        // Bottom Edge
        out.write("\t\t\t<" + EDGE + ">\n");
        for (int i = 0; i < skinSpec.bottomEdge.size(); i++) {
            out.write("\t\t\t\t<" + EDGE_ICON + ">\n");
            // Edge Icon
            out.write("\t\t\t\t\t<" + ICON + ">");
            out.write(skinSpec.bottomEdge.get(i));
            out.write("</" + ICON + ">\n");
            // Tile state of icon
            out.write("\t\t\t\t\t<" + TILED + ">");
            out.write(((Boolean)skinSpec.bottomShouldTile.get(i)).toString());
            out.write("</" + TILED + ">\n");
            out.write("\t\t\t\t</" + EDGE_ICON + ">\n");
        }
        out.write("\t\t\t\t<" + EDGE_NAME + ">");
        out.write("bottom");
        out.write("</" + EDGE_NAME + ">\n");

        out.write("\t\t\t</" + EDGE + ">\n");

        // Left Edge
        out.write("\t\t\t<" + EDGE + ">\n");
        for (int i = 0; i < skinSpec.leftEdge.size(); i++) {
            out.write("\t\t\t\t<" + EDGE_ICON + ">\n");
            // Edge Icon
            out.write("\t\t\t\t\t<" + ICON + ">");
            out.write(skinSpec.leftEdge.get(i));
            out.write("</" + ICON + ">\n");
            // Tile state of icon
            out.write("\t\t\t\t\t<" + TILED + ">");
            out.write(((Boolean)skinSpec.leftShouldTile.get(i)).toString());
            out.write("</" + TILED + ">\n");
            out.write("\t\t\t\t</" + EDGE_ICON + ">\n");
        }
        out.write("\t\t\t\t<" + EDGE_NAME + ">");
        out.write("left");
        out.write("</" + EDGE_NAME + ">\n");

        out.write("\t\t\t</" + EDGE + ">\n");

        // Right Edge
        out.write("\t\t\t<" + EDGE + ">\n");
        for (int i = 0; i < skinSpec.rightEdge.size(); i++) {
            out.write("\t\t\t\t<" + EDGE_ICON + ">\n");
            // Edge Icon
            out.write("\t\t\t\t\t<" + ICON + ">");
            out.write(skinSpec.rightEdge.get(i));
            out.write("</" + ICON + ">\n");
            // Tile state of icon
            out.write("\t\t\t\t\t<" + TILED + ">");
            out.write(((Boolean)skinSpec.rightShouldTile.get(i)).toString());
            out.write("</" + TILED + ">\n");
            out.write("\t\t\t\t</" + EDGE_ICON + ">\n");
        }
        out.write("\t\t\t\t<" + EDGE_NAME + ">");
        out.write("right");
        out.write("</" + EDGE_NAME + ">\n");

        out.write("\t\t\t</" + EDGE + ">\n");

        out.write("\t\t</" + BORDER + ">\n");
    }


    public static SkinSpecification getSkin(String component){
        return getSkin(component,false);
    }

    /**
     * Returns the list of components that have SkinSpecifications.
     * @return
     */
    public synchronized static Set<String> getSkinnedComponents() {
        return skinSpecs.keySet();
    }

    /**
     * Get a <code>SkinSpecification</code> for a given component.
     * 
     * @param component  The name of the component to get skin info for.
     * @return           
     */
    public synchronized static SkinSpecification getSkin(String component, 
            boolean isBtn){
        if (skinSpecs == null ){
            boolean rv = initSkinXMLHandler();
            if (!rv) {
                // This will return a blank skin spec file, which will act like
                // a plain skin.
                return new SkinSpecification();
            }
        }
        
        SkinSpecification spec = skinSpecs.get(component);
        if (spec == null){
            if (isBtn){
                spec = skinSpecs.get(UIComponents.DefaultButton.getComp());
            } else {
                spec = skinSpecs.get(UIComponents.DefaultUIElement.getComp());
            }
        }
        return spec;
    }

    /**
     * Adds a new component to the SkinSpecs map.
     *
     * @param component
     */
    public synchronized static void addNewComp(String component) {
        if (skinSpecs == null ){
            boolean rv = initSkinXMLHandler();
            if (!rv) {
                return ;
            }
        }
        SkinSpecification newSpec = new SkinSpecification();
        skinSpecs.put(component, newSpec);
    }

}
