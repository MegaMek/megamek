/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class loads the default quirks list from the mmconf/defaultQuirks.xml
 * file.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %I% %G%
 * @since 2012-03-05
 */
public class DefaultQuirksHandler {
    private static final String CHASSIS = "chassis";
    private static final String MODEL = "model";
    private static final String QUIRK = "quirk";
    private static final String WEAPON_QUIRK = "weaponQuirk";
    private static final String LOCATION = "location";
    private static final String SLOT = "slot";
    private static final String WEAPON_NAME = "weaponName";
    private static final String WEAPON_QUIRK_NAME = "weaponQuirkName";

    private static Map<String, List<QuirkEntry>> defaultQuirkMap;
    private static boolean initialized = false;

    /**
     * Reads in the values from the defaultQuirks.xml file and stores them in
     * memory.
     *
     * @throws IOException
     */
    public static void initQuirksList() throws IOException {

        // Get the path to the defaultQuirks.xml file.
        String filePath = System.getProperty("user.dir");
        if (!filePath.endsWith(File.separator)) {
            filePath += File.separator;
        }
        filePath += "mmconf" + File.separator + "defaultQuirks.xml";
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return;
        }

        // Build the XML document.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            System.out.println("Parsing " + filePath);
            Document doc = builder.parse(file);
            System.out.println("Parsing finished.");

            // Get the list of units.
            NodeList listOfUnits = doc.getElementsByTagName("unit");
            int totalUnits = listOfUnits.getLength();
            System.out.println("Total number of units with default quirks: "
                               + totalUnits);
            defaultQuirkMap = new HashMap<String, List<QuirkEntry>>(totalUnits);
            for (int unitCount = 0; unitCount < totalUnits; unitCount++) {

                // Get the first element of this node.
                Element unitList = (Element) listOfUnits.item(unitCount);

                // Get the chassis
                Element chassisElement = (Element) unitList
                        .getElementsByTagName(CHASSIS).item(0);
                if (chassisElement == null) {
                    System.err.println("Missing <chassis> element #"
                                       + unitCount);
                    continue;
                }
                String chassis = chassisElement.getTextContent();

                // Get the model.
                Element modelElement = (Element) unitList.getElementsByTagName(
                        MODEL).item(0);
                String model = null;
                if (modelElement != null) {
                    model = modelElement.getTextContent();
                }

                // Generate the unit ID
                String unitId = chassis;
                if ((model != null) && !model.isEmpty()) {
                    unitId += " " + model;
                }

                // Get the quirks.
                NodeList quirkNodes = unitList.getElementsByTagName(QUIRK);
                NodeList weapQuirkNodes = unitList
                        .getElementsByTagName(WEAPON_QUIRK);
                List<QuirkEntry> quirkList = new ArrayList<QuirkEntry>(
                        quirkNodes.getLength() + weapQuirkNodes.getLength());

                // Add the quirks.
                for (int quirkCount = 0; quirkCount < quirkNodes.getLength(); quirkCount++) {

                    // Create the quirk entry and add it to the list.
                    Element quirkElement = (Element) quirkNodes
                            .item(quirkCount);
                    if ((quirkElement.getTextContent() == null)
                        || quirkElement.getTextContent().isEmpty()) {
                        continue;
                    }
                    QuirkEntry quirkEntry = new QuirkEntry(
                            quirkElement.getTextContent(), unitId);
                    quirkList.add(quirkEntry);
                }

                // Add the weapon quirks.
                for (int quirkCount = 0; quirkCount < weapQuirkNodes
                        .getLength(); quirkCount++) {
                    Element quirkElement = (Element) weapQuirkNodes
                            .item(quirkCount);

                    // Get the name of the quirk.
                    Element nameElement = (Element) quirkElement
                            .getElementsByTagName(WEAPON_QUIRK_NAME).item(0);
                    if (nameElement == null) {
                        continue;
                    }
                    String weaponQuirkName = nameElement.getTextContent();

                    // Get the weapon's location.
                    Element locElement = (Element) quirkElement
                            .getElementsByTagName(LOCATION).item(0);
                    if (locElement == null) {
                        continue;
                    }
                    String location = locElement.getTextContent();

                    // Get the weapon's critical slot.
                    Element slotElement = (Element) quirkElement
                            .getElementsByTagName(SLOT).item(0);
                    if (slotElement == null) {
                        continue;
                    }
                    String slot = slotElement.getTextContent();

                    // Get the weapon's name.
                    Element weapElement = (Element) quirkElement
                            .getElementsByTagName(WEAPON_NAME).item(0);
                    if (weapElement == null) {
                        continue;
                    }
                    String weaponName = weapElement.getTextContent();

                    // Add the weapon quirk to the list.
                    QuirkEntry weaponQuirk = new QuirkEntry(weaponQuirkName,
                                                            location, Integer.parseInt(slot), weaponName,
                                                            unitId);
                    quirkList.add(weaponQuirk);
                }

                // Add the unit to the default quirks list.
                if (quirkList.isEmpty()) {
                    System.err.println("No quirks found for " + unitId);
                }
                defaultQuirkMap.put(unitId, quirkList);
            }

            initialized = true;

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Retrieves the list of quirks for the identified unit.
     *
     * @param chassis The unit's chassis.
     * @param model   The unit's model (may be left NULL or an empty string if there
     *                is no model number).
     * @return A {@code List} of the quirks ({@code QuirkEntry}) for the given
     *         unit. If the unit is not in the list, a NULL value is returned.
     * @throws java.io.IOException
     */
    public static List<QuirkEntry> getQuirks(String chassis, String model) {
        if (!initialized || (null == defaultQuirkMap)) {
            return null;
        }
        List<QuirkEntry> quirks = null;

        // Build the unit ID from the chassis and model.
        String unitId = chassis;
        if ((model != null) && !model.isEmpty()) {
            unitId += " " + model;
        }
        // System.out.println("Getting quirks for " + unitId);

        if (defaultQuirkMap.containsKey(chassis)) {
            quirks = defaultQuirkMap.get(chassis);
        }

        if (defaultQuirkMap.containsKey(unitId)) {
            if (quirks == null) {
                quirks = defaultQuirkMap.get(unitId);
            } else if (defaultQuirkMap.get(unitId).size() > 0) {
                quirks = new ArrayList<QuirkEntry>(quirks);
                quirks.addAll(defaultQuirkMap.get(unitId));
            }
        }

        return quirks;
    }
}
