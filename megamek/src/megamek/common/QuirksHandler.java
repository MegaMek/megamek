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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Quirks;
import megamek.common.options.WeaponQuirks;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class loads the quirks lists from the data/canonUnitQuirks.xml and /mmconf/unitQuirksOverride.xml files.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %I% %G%
 * @since 2012-03-05
 */
public class QuirksHandler {
    
    private static final String CUSTOM_QUIRKS_FOOTER = "</unitQuirks>";
    private static final String CUSTOM_QUIRKS_HEADER;
    
    static {
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");
        sb.append("<!--\n");
    
        sb.append("NOTE: saving quirks for units within MM will cause this file to get re-written and all changes will be lost!\n\n");
        sb.append("This file allows users to customize the default cannon quirks list.  Any quirk assignments in this file will override\n");
        sb.append("  the cannon quirk entries entirely.  Changes to this file will not take effect until the next time megamek is launched.\n\n");

        sb.append("To assign a unit a quirk, the entry should be in the following format:\n");
        sb.append("    <unit>\n");
        sb.append("        <chassis>[chassis name]</chassis>\n");
        sb.append("        <model>{model}</model>\n");
        sb.append("        <quirk>[quirk1 name]</quirk>\n");
        sb.append("        <quirk>[quirk2 name]</quirk>\n");
        sb.append("        <weaponQuirk>\n");
        sb.append("            <weaponQuirkName>[weapon quirk 1 name]</weaponQuirkName>\n");
        sb.append("            <location>[location of weapon]</location>\n");
        sb.append("            <slot>[critical slot of weapon]</slot>\n");
        sb.append("            <weaponName>[name of weapon]</weaponName>\n");
        sb.append("        </weaponQuirk>\n");
        sb.append("        <weaponQuirk>\n");
        sb.append("            <weaponQuirkName>[weapon quirk 2 name]</weaponQuirkName>\n");
        sb.append("            <location>[location of weapon]</location>\n");
        sb.append("            <slot>[critical slot of weapon]</slot>\n");
        sb.append("            <weaponName>[name of weapon]</weaponName>\n");
        sb.append("        </weaponQuirk>\n");
        sb.append("    </unit>\n\n");

        sb.append("The \"model\" field can be left blank if there is no model number for the unit (common for some tank chassis), but the\n");
        sb.append("  tags should still be included.  A <model> of \"all\" will cause all units with the same <chassis> to have the defined\n");
        sb.append("  quirks.  This can later be overridden with entries for specific models.\n\n");

        sb.append("Multiple quirks should be contained within separate \"quirk\" tags.\n\n");

        sb.append("Multiple weapon quirks should be contained within separate \"weaponQuirk\" structures, even if multiple quirks apply to\n");
        sb.append("  the same weapon.\n\n");

        sb.append("The proper names for quirks can be found in the\n");
        sb.append("  l10n/megamek/common/options/messages.properties file.  Search for the \"QuirksInfo\" section.\n");
        sb.append("  The name you want will fall between \"option\" and \"displayableName\".  For example, if you wish to apply the\n");
        sb.append("  \"Anti-Aircraft Targeting\" quirk to a unit, you will find the following entry in the messages.properties file:\n");
        sb.append("    QuirksInfo.option.anti_air.displayableName\n");
        sb.append("  The name you want to include in this file for the <quirk> entry is \"anti_air\".\n");

        sb.append("If you wish to remove all quirks for a unit, you can create an entry in this file with a <quirk> of \"none\".\n\n");

        sb.append("Example:  If you wish to declare that all Atlas variants do not have the Command Mech quirk:\n");
        sb.append("    <unit>\n");
        sb.append("        <chassis>Atlas</chassis>\n");
        sb.append("        <model>all</model>\n");
        sb.append("        <quirk>none</quirk>\n");
        sb.append("    </unit>\n\n");

        sb.append("Example: If you decide only the AS7-D Atlas, but no other variant, should have the Command Mech quirk:\n");
        
        sb.append("        <unit>\n");
        sb.append("        <chassis>Atlas</chassis>\n");
        sb.append("        <model>all</model>\n");
        sb.append("        <quirk>none</quirk>\n");
        sb.append("    </unit>\n");
        sb.append("    <unit>\n");
        sb.append("        <chassis>Atlas</chassis>\n");
        sb.append("        <model>AS7-D</model>\n");
        sb.append("        <quirk>command_mech</quirk>\n");
        sb.append("    </unit>\n\n");

        sb.append("Example: You can also do this in the opposite direction, so that all Atlases have the Command Mech quirk except the AS7-D:\n");
        sb.append("    <unit>\n");
        sb.append("        <chassis>Atlas</chassis>\n");
        sb.append("        <model>all</model>\n");
        sb.append("        <quirk>command_mech</quirk>\n");
        sb.append("    </unit>\n");
        sb.append("    <unit>\n");
        sb.append("        <chassis>Atlas</chassis>\n");
        sb.append("        <model>AS7-D</model>\n");
        sb.append("        <quirk>none</quirk>\n");
        sb.append("    </unit>\n\n");

        sb.append("Example: You can define quirks that affect all units of a given chassis and then add specific quirks to specific models:\n");
        sb.append("    <unit>\n");
        sb.append("        <chassis>Atlas</chassis>\n");
        sb.append("        <model>all</model>\n");
        sb.append("        <quirk>command_mech</quirk>\n");
        sb.append("    </unit>\n");
        sb.append("    <unit>\n");
        sb.append("        <chassis>Atlas</chassis>\n");
        sb.append("        <model>AS7-D</model>\n");
        sb.append("        <quirk>anti_air</quirk>\n");
        sb.append("    </unit>\n");
        sb.append("-->\n\n");

        sb.append("<unitQuirks xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../data/unitQuirksSchema.xsl\">\n");
    
        CUSTOM_QUIRKS_HEADER = sb.toString();
    }
    
    private static final String UNIT = "unit";
    private static final String CHASSIS = "chassis";
    private static final String MODEL = "model";
    private static final String UNIT_TYPE = "unitType";
    private static final String QUIRK = "quirk";
    private static final String WEAPON_QUIRK = "weaponQuirk";
    private static final String LOCATION = "location";
    private static final String SLOT = "slot";
    private static final String WEAPON_NAME = "weaponName";
    private static final String WEAPON_QUIRK_NAME = "weaponQuirkName";

    private static final String MODEL_ALL = "all";
    
    private static Map<String, List<QuirkEntry>> canonQuirkMap;
    private static Map<String, List<QuirkEntry>> customQuirkMap;
    private static AtomicBoolean customQuirksDirty = new AtomicBoolean(false);
    private static AtomicBoolean initialized = new AtomicBoolean(false);
    
    /**
     * Generate a Quirk's Unit ID given an Entity.
     * 
     * @param Entity Entity to generate UnitId from
     * @param useModel determines if the model should be used, or be 'all'
     * @return
     */
    public static String getUnitId(Entity ent, boolean useModel) {
        String typeText = Entity.getEntityMajorTypeName(ent.getEntityType());
        if (useModel) {
            return ent.getChassis() + "~" + ent.getModel() + "~" + typeText;
        } else {
            return ent.getChassis() + "~~" + typeText;
        }
    }
    
    public static String getUnitId(String chassis, String model, String type) {
        return chassis + "~" + (model.equals(MODEL_ALL) ? "" : model) + "~" + type;
    }

    public static String getChassis(String unitId) {
        int splitIdx = unitId.indexOf("~");
        if (splitIdx == -1) {
            return unitId;
        } else {
            return unitId.substring(0, splitIdx);
        }
    }

    public static String getModel(String unitId) {
        int splitIdx = unitId.indexOf("~");
        int endIdx = unitId.lastIndexOf("~");
        if (splitIdx == -1) {
            return null;
        } else {
            return unitId.substring(splitIdx + 1, endIdx);
        }
    }

    public static String getUnitType(String unitId) {
        int splitIdx = unitId.lastIndexOf("~");
        if (splitIdx == -1) {
            return null;
        } else {
            return unitId.substring(splitIdx + 1, unitId.length());
        }
    }

    public static String replaceUnitType(String unitId, String newUnitType) {
        int splitIdx = unitId.lastIndexOf("~");
        return unitId.substring(0, splitIdx) + "~" + newUnitType;
    }

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
            NodeList listOfEntries = doc.getElementsByTagName(UNIT);
            int totalEntries = listOfEntries.getLength();
            log.append("\n\tTotal number of unit tags: ").append(totalEntries);
            for (int unitCount = 0; unitCount < totalEntries; unitCount++) {

                // Get the first element of this node.
                Element unitList = (Element) listOfEntries.item(unitCount);

                // Get the chassis
                Element chassisElement = (Element) unitList.getElementsByTagName(CHASSIS).item(0);
                if (chassisElement == null) {
                    log.append("\n\tMissing <chassis> element #").append(unitCount);
                    continue;
                }
                String chassis = chassisElement.getTextContent().trim();

                // Get the model.
                Element modelElement = (Element) unitList.getElementsByTagName(MODEL).item(0);
                // default to "all" model for entries that don't list a model.. backwards compatibility with older quirks files
                String model = MODEL_ALL;
                if (modelElement != null) {
                    model = modelElement.getTextContent().trim();
                }

                Element typeElement = (Element) unitList.getElementsByTagName(UNIT_TYPE).item(0);
                // default to "Mech" type for entries that don't list a type.. backwards compatibility with older quirks files
                String unitType = "Mech";
                if (typeElement != null) {
                    unitType = typeElement.getTextContent().trim();
                }

                // Generate the unit ID
                String unitId = getUnitId(chassis, model, unitType);

                // Get the quirks.
                NodeList quirkNodes = unitList.getElementsByTagName(QUIRK);
                NodeList weapQuirkNodes = unitList.getElementsByTagName(WEAPON_QUIRK);
                List<QuirkEntry> quirkList = new ArrayList<>(quirkNodes.getLength() + weapQuirkNodes.getLength());

                // Add the quirks.
                for (int quirkCount = 0; quirkCount < quirkNodes.getLength(); quirkCount++) {
                    // Create the quirk entry and add it to the list.
                    Element quirkElement = (Element) quirkNodes.item(quirkCount);
                    String qeText = quirkElement.getTextContent().trim();
                    if ((quirkElement.getTextContent() == null) || qeText.isEmpty()) {
                        log.append("\n\t\t" + unitId + ": no text content!");
                        continue;
                    }
                    QuirkEntry quirkEntry = new QuirkEntry(qeText, unitId);
                    quirkList.add(quirkEntry);
                }

                // Add the weapon quirks.
                for (int quirkCount = 0; quirkCount < weapQuirkNodes.getLength(); quirkCount++) {
                    Element quirkElement = (Element) weapQuirkNodes.item(quirkCount);

                    // Get the name of the quirk.
                    Element nameElement = (Element) quirkElement.getElementsByTagName(WEAPON_QUIRK_NAME).item(0);
                    if (nameElement == null) {
                        log.append("\n\t\t" + unitId + ": no weapon quirk name!");
                        continue;
                    }
                    String weaponQuirkName = nameElement.getTextContent().trim();

                    // Get the weapon's location.
                    Element locElement = (Element) quirkElement.getElementsByTagName(LOCATION).item(0);
                    if (locElement == null) {
                        log.append("\n\t\t" + unitId + ": no weapon quirk loc!");
                        continue;
                    }
                    String location = locElement.getTextContent().trim();

                    // Get the weapon's critical slot.
                    Element slotElement = (Element) quirkElement.getElementsByTagName(SLOT).item(0);
                    if (slotElement == null) {
                        log.append("\n\t\t" + unitId + ": no weapon quirk slot!");
                        continue;
                    }
                    String slot = slotElement.getTextContent().trim();
                    if (slot.length() < 1) {
                        throw new IllegalArgumentException(unitId
                                + " weapon quirk " + weaponQuirkName
                                + " has an illegal slot entry!");
                    } 

                    // Get the weapon's name.
                    Element weapElement = (Element) quirkElement.getElementsByTagName(WEAPON_NAME).item(0);
                    if (weapElement == null) {
                        log.append("\n\t\t" + unitId + ": no weapon quirk weapon name!");
                        continue;
                    }
                    String weaponName = weapElement.getTextContent().trim();

                    // Add the weapon quirk to the list.
                    QuirkEntry weaponQuirk = new QuirkEntry(weaponQuirkName,
                            location, Integer.parseInt(slot), weaponName,
                            unitId);
                    quirkList.add(weaponQuirk);
                }

                // Add the unit to the default quirks list.
                if (quirkList.isEmpty()) {
                    log.append("\n\t\tNo quirks found for ");
                    if (unitId.length() > 0) {
                        log.append(unitId);
                    } else {
                        log.append("<BlankUnitId>");
                    }
                }
                if (quirkMap.containsKey(unitId)) {
                    log.append("\n\t\t" + unitId + ": duplicate entry added!");
                }
                quirkMap.put(unitId, quirkList);
            }
            log.append("\n\tTotal number of quirk entries: ").append(quirkMap.size());
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
        String filePath = Configuration.dataDir().getAbsolutePath() + File.separator + "canonUnitQuirks.xml";
        canonQuirkMap = loadQuirksFile(filePath);

        // Load the custom quirks list.
        filePath = userDir + "mmconf" + File.separator + "unitQuirksOverride.xml";
        customQuirkMap = loadQuirksFile(filePath);

        initialized.set(true);
    }
    
    public static void saveCustomQuirksList() throws IOException {
        // If customQuirkMap wasn't initialized, no reason to save it
        if (customQuirkMap == null) {
            return;
        }
        
        // If the custom quirks map wasn't ever changed, no reason to save
        if (!customQuirksDirty.get()) {
            return;
        }
        
        // Get the path to the unitQuirksOverride.xml file.
        String userDir = System.getProperty("user.dir");
        if (!userDir.endsWith(File.separator)) {
            userDir += File.separator;
        }
        String filePath = userDir + "mmconf" + File.separator
                + "unitQuirksOverride.xml";
        
        Writer output = null;
        try {
            output = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(filePath)));
            output.write(CUSTOM_QUIRKS_HEADER);
            for (String unitId : customQuirkMap.keySet()) {
                String chassis = getChassis(unitId);
                String model = getModel(unitId);
                String unitType = getUnitType(unitId);
                
                output.write("\t" + getOpenTag(UNIT) + "\n");
                
                // Write Chassis
                output.write("\t\t" + getOpenTag(CHASSIS));
                output.write(chassis);
                output.write(getCloseTag(CHASSIS) + "\n");
                
                // Write Model
                if (model.length() > 0) {
                    output.write("\t\t" + getOpenTag(MODEL));
                    output.write(model);
                    output.write(getCloseTag(MODEL) + "\n");
                }

                // Write unit type
                output.write("\t\t" + getOpenTag(UNIT_TYPE));
                output.write(unitType);
                output.write(getCloseTag(UNIT_TYPE) + "\n");

                // Write out quirks
                List<QuirkEntry> quirks = customQuirkMap.get(unitId);
                for (QuirkEntry quirk : quirks) {
                    // Write Weapon Quirk
                    if (quirk.isWeaponQuirk()) {
                        output.write("\t\t" + getOpenTag(WEAPON_QUIRK) + "\n");
                        // Quirk Name
                        output.write("\t\t\t" + getOpenTag(WEAPON_QUIRK_NAME));
                        output.write(quirk.getQuirk());
                        output.write(getCloseTag(WEAPON_QUIRK_NAME) + "\n");
                        // Location
                        output.write("\t\t\t" + getOpenTag(LOCATION));
                        output.write(quirk.getLocation());
                        output.write(getCloseTag(LOCATION) + "\n");
                        // Slot
                        output.write("\t\t\t" + getOpenTag(SLOT));
                        output.write(quirk.getSlot() + "");
                        output.write(getCloseTag(SLOT) + "\n");
                        // Weapon Name
                        output.write("\t\t\t" + getOpenTag(WEAPON_NAME));
                        output.write(quirk.getWeaponName());
                        output.write(getCloseTag(WEAPON_NAME) + "\n");
                        // Close Tag
                        output.write("\t\t" + getCloseTag(WEAPON_QUIRK) + "\n");
                    } else { // Write normal quirk
                        output.write("\t\t" + getOpenTag(QUIRK));
                        output.write(quirk.getQuirk());
                        output.write(getCloseTag(QUIRK) + "\n");
                    }
                }               
                output.write("\t" + getCloseTag(UNIT) + "\n\n");
            }
            
            output.write(CUSTOM_QUIRKS_FOOTER);
        } catch (IOException e) {
            System.err.println("Error writing keybindings file!");
            e.printStackTrace(System.err);
        } finally {
            if (output != null) {
                output.close();
            }
        }        
    }
    
    private static String getOpenTag(String s) {
        return "<" + s + ">";
    }
    
    private static String getCloseTag(String s) {
        return "</" + s + ">";
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
    @Nullable
    public static List<QuirkEntry> getQuirks(Entity entity) {
        final String NO_QUIRKS = "none";

        if (!initialized.get() || (null == canonQuirkMap)) {
            return null;
        }
        List<QuirkEntry> quirks = new ArrayList<>();

        // General entry for the chassis.
        String generalId = getUnitId(entity, false);

        // Build the unit ID from the chassis and model.
        String unitId = getUnitId(entity, true);

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
    
    public static void addCustomQuirk(Entity entity, boolean useModel) {
        // Shouldn't happen, but lets be careful
        if (customQuirkMap == null) {
            try {
                QuirksHandler.initQuirksList();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
        
        customQuirksDirty.set(true);
        
        // Generate Unit ID
        String unitId;
        unitId = getUnitId(entity, useModel);

        // Get a quirks list
        List<QuirkEntry> quirkEntries = customQuirkMap.get(unitId);
        if (quirkEntries == null) {
            quirkEntries = new ArrayList<>();
            customQuirkMap.put(unitId, quirkEntries);
        }
        quirkEntries.clear();
        
        // Add Entity Quirks
        if (entity.countQuirks() > 0) {
            Quirks entQuirks = entity.getQuirks();
            Enumeration<IOptionGroup> quirksGroup = entQuirks.getGroups();
            Enumeration<IOption> quirkOptions;
           while (quirksGroup.hasMoreElements()) {
                IOptionGroup group = quirksGroup.nextElement();
                quirkOptions = group.getSortedOptions();
                while (quirkOptions.hasMoreElements()) {
                    IOption option = quirkOptions.nextElement();
                    // Ignore illegal quirks, and ones that aren't set
                    if (!Quirks.isQuirkLegalFor(option, entity)
                            || !option.booleanValue()) {
                        continue;
                    }
                    // Add new QuirkEntry
                    QuirkEntry qe = new QuirkEntry(option.getName(), unitId);
                    quirkEntries.add(qe);
                }
            }
        }
        
        // Handle Weapon/Equipment Quirks
        // Need to keep track of processed mounts, for multi-crit equipment
        List<Mounted> addedEquipment = new ArrayList<Mounted>();
        // Need to know loc and slot, so can't iterate over Entity.getEquipment
        for  (int loc = 0; loc < entity.locations(); loc++) {
            int numCrits = entity.getNumberOfCriticals(loc);
            for (int slot = 0; slot < numCrits; slot++) {
                CriticalSlot crit = entity.getCritical(loc, slot);
                // Ignore systems and null entries
                if ((crit == null)
                        || (crit.getType() == CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }
                // Add quirk for mount if we haven't already
                if ((crit.getMount() != null)
                        && !addedEquipment.contains(crit.getMount())) {
                    addWeaponQuirk(quirkEntries, crit.getMount(), loc, slot,
                            unitId, entity);
                    addedEquipment.add(crit.getMount());
                }
                // Add quirk for mount2 if we haven't already
                if ((crit.getMount2() != null)
                        && !addedEquipment.contains(crit.getMount2())) {
                    addWeaponQuirk(quirkEntries, crit.getMount2(), loc, slot,
                            unitId, entity);
                    addedEquipment.add(crit.getMount2());
                }
            }
        }
    }
    
    /**
     * Convenience method for adding a weapon quirk to the quirk entries list.
     * 
     * @param quirkEntries
     * @param m
     * @param loc
     * @param slot
     * @param unitId
     * @param entity
     */
    private static void addWeaponQuirk(List<QuirkEntry> quirkEntries,
            @Nullable Mounted m, int loc, int slot, String unitId, Entity entity) {
        // If mount is null, nothing to do
        if (m == null) {
            return;
        }
        // Ignore if the mount has no quirks
        if (m.countQuirks() > 0) {
            WeaponQuirks weapQuirks = m.getQuirks();
            Enumeration<IOptionGroup> quirksGroup = weapQuirks.getGroups();
            Enumeration<IOption> quirkOptions;
           while (quirksGroup.hasMoreElements()) {
                IOptionGroup group = quirksGroup.nextElement();
                quirkOptions = group.getSortedOptions();
                while (quirkOptions.hasMoreElements()) {
                    IOption option = quirkOptions.nextElement();
                    // Don't add quirk if it's not on 
                    if (!option.booleanValue()) {
                        continue;
                    }
                    // Create new entry and add it
                    QuirkEntry qe = new QuirkEntry(option.getName(),
                            entity.getLocationAbbr(loc), slot, m.getType()
                                    .getInternalName(), unitId);
                    quirkEntries.add(qe);
                }
            }
        }
    }

    public static Set<String> getCanonQuirkIds() {
        return canonQuirkMap.keySet();
    }

    /**
     * Used by QuirkRewriteTool to take a quirk entry from canon quirks, and
     * munge its eType and write it to customQuirks.
     */
    public static void mungeQuirks(String quirkId, String newId) {
        // Shouldn't happen, but lets be careful
        if (customQuirkMap == null) {
            try {
                QuirksHandler.initQuirksList();
            } catch (IOException e) {
                System.out.println(e);
            }
        }

        customQuirksDirty.set(true);

        customQuirkMap.put(newId, canonQuirkMap.get(quirkId));
    }

    public static boolean customQuirksContain(String unitId) {
        return customQuirkMap.containsKey(unitId);
    }
}
