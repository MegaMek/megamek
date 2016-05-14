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
import java.util.Set;
import java.util.HashMap;
import java.util.Map;


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
    
    // General XML Tags
    public static String UI_ELEMENT = "UI_Element";
    public static String NAME = "name";

    // Skin Specification XML Tags
    public static final String FONT_COLOR = "font_color"; //$NON-NLS-1$
    public static final String BORDER = "border"; //$NON-NLS-1$
    public static final String PLAIN = "plain"; //$NON-NLS-1$
    public static final String NO_BORDER = "no_border"; //$NON-NLS-1$
    public static final String TILE_BACKGROUND = "tile_background"; //$NON-NLS-1$
    public static final String TR_CORNER = "corner_top_right"; //$NON-NLS-1$
    public static final String TL_CORNER = "corner_top_left"; //$NON-NLS-1$
    public static final String BR_CORNER = "corner_bottom_right"; //$NON-NLS-1$
    public static final String BL_CORNER = "corner_bottom_left"; //$NON-NLS-1$
    public static final String EDGE = "edge"; //$NON-NLS-1$
    public static final String EDGE_NAME = "edgeName"; //$NON-NLS-1$
    public static final String EDGE_ICON = "edgeIcon"; //$NON-NLS-1$
    public static final String ICON = "icon"; //$NON-NLS-1$
    public static final String TILED = "tiled"; //$NON-NLS-1$
    public static final String TOP_LINE = "line_top"; //$NON-NLS-1$
    public static final String BOTTOM_LINE = "line_bottom"; //$NON-NLS-1$
    public static final String RIGHT_LINE = "line_right"; //$NON-NLS-1$
    public static final String LEFT_LINE = "line_left"; //$NON-NLS-1$
    public static final String BACKGROUND_IMAGE = "background_image"; //$NON-NLS-1$
    public static final String SHOW_SCROLL_BARS = "show_scroll_bars"; //$NON-NLS-1$

    // Unit Display Skin Specification XML tags
    public static final String GeneralTabIdle = "tab_general_idle"; //$NON-NLS-1$
    public static final String PilotTabIdle = "tab_pilot_idle"; //$NON-NLS-1$
    public static final String ArmorTabIdle = "tab_armor_idle"; //$NON-NLS-1$
    public static final String SystemsTabIdle = "tab_systems_idle"; //$NON-NLS-1$
    public static final String WeaponsTabIdle = "tab_weapon_idle"; //$NON-NLS-1$
    public static final String ExtrasTabIdle = "tab_extras_idle"; //$NON-NLS-1$
    public static final String GeneralTabActive = "tab_general_active"; //$NON-NLS-1$
    public static final String PilotTabActive = "tab_pilot_active"; //$NON-NLS-1$
    public static final String ArmorTabActive = "tab_armor_active"; //$NON-NLS-1$
    public static final String SystemsTabActive = "tab_systems_active"; //$NON-NLS-1$
    public static final String WeaponsTabActive = "tab_weapon_active"; //$NON-NLS-1$
    public static final String ExtraTabActive = "tab_extras_active"; //$NON-NLS-1$
    public static final String CornerIdle = "idle_corner"; //$NON-NLS-1$
    public static final String CornerActive = "active_corner"; //$NON-NLS-1$

    public static final String BackgroundTile = "background_tile"; //$NON-NLS-1$
    public static final String TopLine = "top_line"; //$NON-NLS-1$
    public static final String BottomLine = "bottom_line"; //$NON-NLS-1$
    public static final String LeftLine = "left_line"; //$NON-NLS-1$
    public static final String RightLine = "right_line"; //$NON-NLS-1$
    public static final String TopLeftCorner = "tl_corner"; //$NON-NLS-1$
    public static final String BottomLeftCorner = "bl_corner"; //$NON-NLS-1$
    public static final String TopRightCorner = "tr_corner"; //$NON-NLS-1$
    public static final String BottomRightCorner = "br_corner"; //$NON-NLS-1$

    public static final String MechOutline = "mech_outline"; //$NON-NLS-1$

    private static Map<String, SkinSpecification> skinSpecs;

    private static UnitDisplaySkinSpecification udSpec = null;
    
    /**
     * Checks whether the given path points to a file that is a valid skin
     * specification.
     * 
     * @param path
     * @return
     */
    public static boolean validSkinSpecFile(String fileName) {
        File file = new File(Configuration.skinsDir(), fileName);
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
        // Reset UnitDisplay spec
        udSpec = null;

        if (fileName == null) {
            System.out.println("ERROR: Bad skin specification file: " +
                    "null filename!");
            return false;
        }
        
        File file = new File(Configuration.skinsDir(), fileName);
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
            skinSpecs = new HashMap<String, SkinSpecification>(
                    (int)(totalComponents * 1.25));
            for (int comp = 0; comp < totalComponents; comp++) {
                Element borderList = (Element) listOfComponents.item(comp);

                String name = borderList.getElementsByTagName(NAME).
                        item(0).getTextContent();
                
                if (name.equals(SkinSpecification.UIComponents.UnitDisplay.getComp())) {
                    parseUnitDisplaySkinSpec(borderList);
                    continue;
                }

                SkinSpecification skinSpec;
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

                if (SkinSpecification.UIComponents.getUIComponent(name) == null) {
                    System.out.println("SKIN ERROR: "
                            + "Unable to add unrecognized UI component: "
                            + name + "!");
                } else {
                    skinSpecs.put(name, skinSpec);
                }
            }

            if (!skinSpecs.containsKey(UIComponents.DefaultUIElement.getComp()) 
                    || !skinSpecs.containsKey(UIComponents.DefaultButton.getComp())) {
                System.out.println("SKIN ERROR: Bad skin specification file: " +
                        "file doesn't specify " + UIComponents.DefaultUIElement + 
                        " or " + UIComponents.DefaultButton + "!");
                return false;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
        // Skin spec didn't specify UnitDisplay skin, use default
        if (udSpec == null) {
            udSpec = new UnitDisplaySkinSpecification();
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

    /**
     * Given a UI_Component component with a UnitDisplay name, parse it into
     * a new UnitDisplaySkinSpecification.  This tupe of UI_Element has a
     * different structure than other UI_Elements.
     *
     * @param border
     */
    private static void parseUnitDisplaySkinSpec(Element border) {
        udSpec = new UnitDisplaySkinSpecification();

        if (border.getElementsByTagName(GeneralTabIdle).getLength() > 0) {
            udSpec.setGeneralTabIdle(border
                    .getElementsByTagName(GeneralTabIdle).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(PilotTabIdle).getLength() > 0) {
            udSpec.setPilotTabIdle(border.getElementsByTagName(PilotTabIdle)
                    .item(0).getTextContent());
        }
        if (border.getElementsByTagName(ArmorTabIdle).getLength() > 0) {
            udSpec.setArmorTabIdle(border.getElementsByTagName(ArmorTabIdle)
                    .item(0).getTextContent());
        }
        if (border.getElementsByTagName(SystemsTabIdle).getLength() > 0) {
            udSpec.setSystemsTabIdle(border
                    .getElementsByTagName(SystemsTabIdle).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(WeaponsTabIdle).getLength() > 0) {
            udSpec.setWeaponsTabIdle(border
                    .getElementsByTagName(WeaponsTabIdle).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(ExtrasTabIdle).getLength() > 0) {
            udSpec.setExtrasTabIdle(border.getElementsByTagName(ExtrasTabIdle)
                    .item(0).getTextContent());
        }
        if (border.getElementsByTagName(GeneralTabActive).getLength() > 0) {
            udSpec.setGeneralTabActive(border
                    .getElementsByTagName(GeneralTabActive).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(PilotTabActive).getLength() > 0) {
            udSpec.setPilotTabActive(border
                    .getElementsByTagName(PilotTabActive).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(ArmorTabActive).getLength() > 0) {
            udSpec.setArmorTabActive(border
                    .getElementsByTagName(ArmorTabActive).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(SystemsTabActive).getLength() > 0) {
            udSpec.setSystemsTabActive(border
                    .getElementsByTagName(SystemsTabActive).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(WeaponsTabActive).getLength() > 0) {
            udSpec.setWeaponsTabActive(border
                    .getElementsByTagName(WeaponsTabActive).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(ExtraTabActive).getLength() > 0) {
            udSpec.setExtraTabActive(border
                    .getElementsByTagName(ExtraTabActive).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(CornerIdle).getLength() > 0) {
            udSpec.setCornerIdle(border.getElementsByTagName(CornerIdle)
                    .item(0).getTextContent());
        }
        if (border.getElementsByTagName(CornerActive).getLength() > 0) {
            udSpec.setCornerActive(border.getElementsByTagName(CornerActive)
                    .item(0).getTextContent());
        }

        if (border.getElementsByTagName(BackgroundTile).getLength() > 0) {
            udSpec.setBackgroundTile(border
                    .getElementsByTagName(BackgroundTile).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(TopLine).getLength() > 0) {
            udSpec.setTopLine(border
                    .getElementsByTagName(TopLine).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(BottomLine).getLength() > 0) {
            udSpec.setBottomLine(border
                    .getElementsByTagName(BottomLine).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(LeftLine).getLength() > 0) {
            udSpec.setLeftLine(border.getElementsByTagName(LeftLine)
                    .item(0).getTextContent());
        }
        if (border.getElementsByTagName(RightLine).getLength() > 0) {
            udSpec.setRightLine(border.getElementsByTagName(RightLine)
                    .item(0).getTextContent());
        }
        if (border.getElementsByTagName(TopLeftCorner).getLength() > 0) {
            udSpec.setTopLeftCorner(border.getElementsByTagName(TopLeftCorner)
                    .item(0).getTextContent());
        }
        if (border.getElementsByTagName(BottomLeftCorner).getLength() > 0) {
            udSpec.setBottomLeftCorner(border
                    .getElementsByTagName(BottomLeftCorner).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(TopRightCorner).getLength() > 0) {
            udSpec.setTopRightCorner(border
                    .getElementsByTagName(TopRightCorner).item(0)
                    .getTextContent());
        }
        if (border.getElementsByTagName(BottomRightCorner).getLength() > 0) {
            udSpec.setBottomRightCorner(border
                    .getElementsByTagName(BottomRightCorner).item(0)
                    .getTextContent());
        }

        if (border.getElementsByTagName(MechOutline).getLength() > 0) {
            udSpec.setMechOutline(border
                    .getElementsByTagName(MechOutline).item(0)
                    .getTextContent());
        }
    }

    /**
     * Writes the current skin to the specified XML file.
     *
     * @param filename
     */
    public static void writeSkinToFile(String filename) {
        File filePath = new File(Configuration.skinsDir(),
                filename);

        try (Writer output = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filePath)));){
            output.write(SKIN_HEADER);
            for (String component : skinSpecs.keySet()) {
                writeSkinComponent(component, output);
            }
            if (udSpec != null) {
                writeUnitDisplaySkinSpec(output);
            }
            output.write(SKIN_FOOTER);
            output.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method for writing the UI_Element tag related to a
     * UnitDisplaySkinSpecification.
     *
     * @param out
     * @throws IOException
     */
    private static void writeUnitDisplaySkinSpec(Writer out) throws IOException {
        // If the spec is null, nothing to do
        if (udSpec == null) {
            return;
        }

        out.write("\t<" + UI_ELEMENT + ">\n");

        // Write Component name
        out.write("\t\t<" + NAME + ">");
        out.write(SkinSpecification.UIComponents.UnitDisplay.getComp());
        out.write("</" + NAME + ">\n");

        out.write("\t\t\t<" + GeneralTabIdle + ">");
        out.write(udSpec.getGeneralTabIdle());
        out.write("</" + GeneralTabIdle + ">\n");

        out.write("\t\t\t<" + PilotTabIdle + ">");
        out.write(udSpec.getPilotTabIdle());
        out.write("</" + PilotTabIdle + ">\n");

        out.write("\t\t\t<" + ArmorTabIdle + ">");
        out.write(udSpec.getArmorTabIdle());
        out.write("</" + ArmorTabIdle + ">\n");

        out.write("\t\t\t<" + SystemsTabIdle + ">");
        out.write(udSpec.getSystemsTabIdle());
        out.write("</" + SystemsTabIdle + ">\n");

        out.write("\t\t\t<" + WeaponsTabIdle + ">");
        out.write(udSpec.getWeaponsTabIdle());
        out.write("</" + WeaponsTabIdle + ">\n");

        out.write("\t\t\t<" + ExtrasTabIdle + ">");
        out.write(udSpec.getExtrasTabIdle());
        out.write("</" + ExtrasTabIdle + ">\n");

        out.write("\t\t\t<" + GeneralTabActive + ">");
        out.write(udSpec.getGeneralTabActive());
        out.write("</" + GeneralTabActive + ">\n");

        out.write("\t\t\t<" + PilotTabActive + ">");
        out.write(udSpec.getPilotTabActive());
        out.write("</" + PilotTabActive + ">\n");

        out.write("\t\t\t<" + ArmorTabActive + ">");
        out.write(udSpec.getArmorTabActive());
        out.write("</" + ArmorTabActive + ">\n");

        out.write("\t\t\t<" + SystemsTabActive + ">");
        out.write(udSpec.getSystemsTabActive());
        out.write("</" + SystemsTabActive + ">\n");

        out.write("\t\t\t<" + WeaponsTabActive + ">");
        out.write(udSpec.getWeaponsTabActive());
        out.write("</" + WeaponsTabActive + ">\n");

        out.write("\t\t\t<" + ExtraTabActive + ">");
        out.write(udSpec.getExtraTabActive());
        out.write("</" + ExtraTabActive + ">\n");

        out.write("\t\t\t<" + CornerIdle + ">");
        out.write(udSpec.getCornerIdle());
        out.write("</" + CornerIdle + ">\n");

        out.write("\t\t\t<" + CornerActive + ">");
        out.write(udSpec.getCornerActive());
        out.write("</" + CornerActive + ">\n");

        out.write("\t\t\t<" + BackgroundTile + ">");
        out.write(udSpec.getBackgroundTile());
        out.write("</" + BackgroundTile + ">\n");

        out.write("\t\t\t<" + TopLine + ">");
        out.write(udSpec.getTopLine());
        out.write("</" + TopLine + ">\n");

        out.write("\t\t\t<" + BottomLine + ">");
        out.write(udSpec.getBottomLine());
        out.write("</" + BottomLine + ">\n");

        out.write("\t\t\t<" + LeftLine + ">");
        out.write(udSpec.getLeftLine());
        out.write("</" + LeftLine + ">\n");

        out.write("\t\t\t<" + RightLine + ">");
        out.write(udSpec.getRightLine());
        out.write("</" + RightLine + ">\n");

        out.write("\t\t\t<" + TopLeftCorner + ">");
        out.write(udSpec.getTopLeftCorner());
        out.write("</" + TopLeftCorner + ">\n");

        out.write("\t\t\t<" + BottomLeftCorner + ">");
        out.write(udSpec.getBottomLeftCorner());
        out.write("</" + BottomLeftCorner + ">\n");

        out.write("\t\t\t<" + TopRightCorner + ">");
        out.write(udSpec.getTopRightCorner());
        out.write("</" + TopRightCorner + ">\n");

        out.write("\t\t\t<" + BottomRightCorner + ">");
        out.write(udSpec.getBottomRightCorner());
        out.write("</" + BottomRightCorner + ">\n");

        out.write("\t\t\t<" + MechOutline + ">");
        out.write(udSpec.getMechOutline());
        out.write("</" + MechOutline + ">\n");

        // Close UI_ELEMENT tag
        out.write("\t</" + UI_ELEMENT + ">\n\n");
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

    public synchronized static UnitDisplaySkinSpecification getUnitDisplaySkin() {
        if (udSpec == null) {
            boolean rv = initSkinXMLHandler();
            if (!rv || (udSpec == null)) {
                // This will return a blank ud skin spec file, which will show
                // the default skin for the unit display
                return new UnitDisplaySkinSpecification();
            }
        }
        return udSpec;
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

    /**
     * Remove the specified componenet from the SkinSpecs map.
     *
     * @param component
     */
    public synchronized static void removeComp(String component) {
        skinSpecs.remove(component);
    }

}
