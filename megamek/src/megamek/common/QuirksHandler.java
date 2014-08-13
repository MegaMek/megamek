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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class loads the default quirks list from the mmconf/canonUnitQuirks.xml
 * file.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %I% %G%
 * @since 2012-03-05
 */
public class QuirksHandler {
    private static final String CHASSIS = "chassis";
    private static final String MODEL = "model";
    private static final String QUIRK = "quirk";
    private static final String WEAPON_QUIRK = "weaponQuirk";
    private static final String LOCATION = "location";
    private static final String SLOT = "slot";
    private static final String WEAPON_NAME = "weaponName";
    private static final String WEAPON_QUIRK_NAME = "weaponQuirkName";

    private static Map<String, List<QuirkEntry>> canonQuirkMap;
    private static Map<String, List<QuirkEntry>> customQuirkMap;
    private static AtomicBoolean initialized = new AtomicBoolean(false);

    private static Map<String, List<QuirkEntry>> loadQuirksFile(String path) throws IOException {
        Map<String, List<QuirkEntry>> quirkMap = new HashMap<>();

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            System.err.println("WARN: Could not load quirks from " + path);
            return quirkMap;
        }

        // Build the XML document.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        StringBuilder log = new StringBuilder();
        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            log.append("Parsing ").append(path);
            Document doc = builder.parse(file);
            log.append("\n...Parsing finished.");

            // Get the list of units.
            NodeList listOfEntries = doc.getElementsByTagName("unit");
            int totalEntries = listOfEntries.getLength();
            log.append("\n\tTotal number of quirk entries: ").append(totalEntries);
            for (int unitCount = 0; unitCount < totalEntries; unitCount++) {

                // Get the first element of this node.
                Element unitList = (Element) listOfEntries.item(unitCount);

                // Get the chassis
                Element chassisElement = (Element) unitList.getElementsByTagName(CHASSIS).item(0);
                if (chassisElement == null) {
                    log.append("\n\tMissing <chassis> element #").append(unitCount);
                    continue;
                }
                String chassis = chassisElement.getTextContent();

                // Get the model.
                Element modelElement = (Element) unitList.getElementsByTagName(MODEL).item(0);
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
                NodeList weapQuirkNodes = unitList.getElementsByTagName(WEAPON_QUIRK);
                List<QuirkEntry> quirkList = new ArrayList<>(quirkNodes.getLength() + weapQuirkNodes.getLength());

                // Add the quirks.
                for (int quirkCount = 0; quirkCount < quirkNodes.getLength(); quirkCount++) {

                    // Create the quirk entry and add it to the list.
                    Element quirkElement = (Element) quirkNodes.item(quirkCount);
                    if ((quirkElement.getTextContent() == null) || quirkElement.getTextContent().isEmpty()) {
                        continue;
                    }
                    QuirkEntry quirkEntry = new QuirkEntry(quirkElement.getTextContent(), unitId);
                    quirkList.add(quirkEntry);
                }

                // Add the weapon quirks.
                for (int quirkCount = 0; quirkCount < weapQuirkNodes.getLength(); quirkCount++) {
                    Element quirkElement = (Element) weapQuirkNodes.item(quirkCount);

                    // Get the name of the quirk.
                    Element nameElement = (Element) quirkElement.getElementsByTagName(WEAPON_QUIRK_NAME).item(0);
                    if (nameElement == null) {
                        continue;
                    }
                    String weaponQuirkName = nameElement.getTextContent();

                    // Get the weapon's location.
                    Element locElement = (Element) quirkElement.getElementsByTagName(LOCATION).item(0);
                    if (locElement == null) {
                        continue;
                    }
                    String location = locElement.getTextContent();

                    // Get the weapon's critical slot.
                    Element slotElement = (Element) quirkElement.getElementsByTagName(SLOT).item(0);
                    if (slotElement == null) {
                        continue;
                    }
                    String slot = slotElement.getTextContent();

                    // Get the weapon's name.
                    Element weapElement = (Element) quirkElement.getElementsByTagName(WEAPON_NAME).item(0);
                    if (weapElement == null) {
                        continue;
                    }
                    String weaponName = weapElement.getTextContent();

                    // Add the weapon quirk to the list.
                    QuirkEntry weaponQuirk = new QuirkEntry(weaponQuirkName, location, Integer.parseInt(slot),
                                                            weaponName, unitId);
                    quirkList.add(weaponQuirk);
                }

                // Add the unit to the default quirks list.
                if (quirkList.isEmpty()) {
                    log.append("No quirks found for ").append(unitId);
                }
                quirkMap.put(unitId, quirkList);
            }

            return quirkMap;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            System.out.println(log);
        }
    }

    /**
     * Reads in the values from the canonUnitQuirks.xml file and stores them in memory.
     *
     * @throws IOException
     */
    public static void initQuirksList() throws IOException {

        // Get the path to the canonUnitQuirks.xml file.
        String userDir = System.getProperty("user.dir");
        if (!userDir.endsWith(File.separator)) {
            userDir += File.separator;
        }

        // Load the canon quirks list.
        String filePath = userDir + "data" + File.separator + "canonUnitQuirks.xml";
        canonQuirkMap = loadQuirksFile(filePath);

        // Load the custom quirks list.
        filePath = userDir + "mmconf" + File.separator + "unitQuirksOverride.xml";
        customQuirkMap = loadQuirksFile(filePath);

        initialized.set(true);
    }

    /**
     * Retrieves the list of quirks for the identified unit.
     *
     * @param chassis The unit's chassis.
     * @param model   The unit's model (may be left NULL or an empty string if there
     *                is no model number).
     * @return A {@code List} of the quirks ({@code QuirkEntry}) for the given
     *         unit. If the unit is not in the list, a NULL value is returned.
     */
    public static List<QuirkEntry> getQuirks(String chassis, String model) {
        final String NO_QUIRKS = "none";

        if (!initialized.get() || (null == canonQuirkMap)) {
            return null;
        }
        List<QuirkEntry> quirks = new ArrayList<>();

        // General entry for the chassis.
        String generalId = chassis + " all";

        // Build the unit ID from the chassis and model.
        String unitId = chassis;
        if (!StringUtil.isNullOrEmpty(model)) {
            unitId += " " + model;
        }

        try {
            // Check for a general entry for this chassis in the custom list.
            if (customQuirkMap.containsKey(generalId)) {
                quirks.addAll(customQuirkMap.get(generalId));
            }

            // Check for a model-specific entry.
            if (customQuirkMap.containsKey(unitId)) {

                // If this specific model has no quirks, return null.
                if (NO_QUIRKS.equalsIgnoreCase(customQuirkMap.get(unitId).get(0).getQuirk())) {
                    return null;
                }

                // Add the model-specific quirks.
                quirks.addAll(customQuirkMap.get(unitId));
            }

            // If there is only one quirk on the list and it indicates that the custom list has removed all quirks from
            // this unit, return null.
            if ((quirks.size() == 1) && NO_QUIRKS.equalsIgnoreCase(quirks.get(0).getQuirk())) {
                return null;
            }

            // If quirk entries were found on the customized list, return those quirks.
            if (!quirks.isEmpty()) {
                return quirks;
            }

            // Check the canonical list for a general entry for this chassis.
            if (canonQuirkMap.containsKey(generalId)) {
                quirks.addAll(canonQuirkMap.get(generalId));
            }

            // Check for a model-specific entry.
            if (canonQuirkMap.containsKey(unitId)) {

                // If this specific model, has no quirks, return null.
                if (NO_QUIRKS.equalsIgnoreCase(canonQuirkMap.get(unitId).get(0).getQuirk())) {
                    return null;
                }

                // Add the model-specific quirks.
                quirks.addAll(canonQuirkMap.get(unitId));
            }

            return quirks.isEmpty() ? null : quirks;
        } catch (Exception e) {
            String msg = "generalId: '" + generalId + "'\nunitId: '" + unitId + "'\n";
            throw new RuntimeException(msg, e);
        }
    }
}
