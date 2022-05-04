/*
* MegaMek -
* Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common;

import megamek.common.Entity.WeaponSortOrder;
import megamek.common.annotations.Nullable;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.utils.MegaMekXmlUtil;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class loads the custom weapon orders lists from the
 * mmconf/customWeaponOrder.xml files.
 *
 * @author Arlith
 */
public class WeaponOrderHandler {

    public static class WeaponOrder {
        public Entity.WeaponSortOrder orderType = WeaponSortOrder.DEFAULT;
        public Map<Integer, Integer> customWeaponOrderMap = new HashMap<>();

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if ((null == obj) || (getClass() != obj.getClass())) {
                return false;
            } else {
                final WeaponOrder other = (WeaponOrder) obj;
                return Objects.equals(orderType, other.orderType)
                        && Objects.equals(customWeaponOrderMap, other.customWeaponOrderMap);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderType, customWeaponOrderMap);
        }
    }

    public static final String CUSTOM_WEAPON_ORDER_FILENAME = "customWeaponOrder.xml";

    private static final String CUSTOM_WEAPON_ORDER = "customWeaponOrders";
    private static final String UNIT = "unit";
    private static final String ID = "id";
    private static final String ORDER_TYPE = "orderType";
    private static final String WEAPON_LIST = "weaponList";
    private static final String ORDER_LIST = "orderList";

    private static Map<String, WeaponOrder> weaponOrderMap;
    private static AtomicBoolean initialized = new AtomicBoolean(false);
    private static AtomicBoolean updated = new AtomicBoolean(false);

    /**
     * Save customWeaponOrderMap to a file.
     *
     * @throws IOException
     */
    public synchronized static void saveWeaponOrderFile() throws IOException {
        // If the map hasn't been updated, we don't need to save it.
        if (!updated.get()) {
            return;
        }

        String path = CUSTOM_WEAPON_ORDER_FILENAME;
        File file = new MegaMekFile(Configuration.configDir(), path).getFile();
        if (file.exists() && !file.canWrite()) {
            LogManager.getLogger().error("Could not save custom weapon orders from " + path);
            return;
        }

        Writer output = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file)));

         // Output the doctype and header stuff.
        output.write("<?xml version=\"1.0\"?>\n");
        output.write("<" + CUSTOM_WEAPON_ORDER +">\n");

        // Create the UNIT_ID tag for each chassis/model id
        for (String unitId : weaponOrderMap.keySet()) {
            StringBuilder weaponList = new StringBuilder();
            StringBuilder orderList = new StringBuilder();
            WeaponOrder weapOrder = weaponOrderMap.get(unitId);
            if (weapOrder == null) {
                continue;
            }


            if (weapOrder.orderType == WeaponSortOrder.CUSTOM) {
                // Build weapon and order lists
                for (Integer weapId : weapOrder.customWeaponOrderMap.keySet()) {
                    Integer order = weapOrder.customWeaponOrderMap.get(weapId);
                    weaponList.append(weapId + ",");
                    orderList.append(order + ",");
                }
                weaponList.deleteCharAt(weaponList.length() - 1);
                orderList.deleteCharAt(orderList.length() - 1);
            }

            // Write out XML
            output.write("\t");
            output.write("<" + UNIT +">");
            output.write("\n\t\t");
            output.write("<" + ID +">");
            output.write(unitId);
            output.write("</" + ID +">");
            output.write("\n\t\t");
            output.write("<" + ORDER_TYPE +">");
            output.write(weapOrder.orderType.toString());
            output.write("</" + ORDER_TYPE +">");
            output.write("\n\t\t");
            output.write("<" + WEAPON_LIST +">");
            output.write(weaponList.toString());
            output.write("</" + WEAPON_LIST +">");
            output.write("\n\t\t");
            output.write("<" + ORDER_LIST +">");
            output.write(orderList.toString());
            output.write("</" + ORDER_LIST +">");
            output.write("\n\t");
            output.write("</" + UNIT +">");
        }

        output.write("\n</" + CUSTOM_WEAPON_ORDER +">");

        output.close();
    }

    /**
     * Load customWeaponOrderMap from a file.
     *
     * @return
     * @throws IOException
     */
    private synchronized static Map<String, WeaponOrder>
        loadWeaponOrderFile() throws IOException {
        Map<String, WeaponOrder> weapOrderMap = new HashMap<>();

        String path = CUSTOM_WEAPON_ORDER_FILENAME;
        File file = new MegaMekFile(Configuration.configDir(), path).getFile();
        if (!file.exists() || !file.isFile()) {
            System.err.println("WARN: Could not load custom weapon orders " +
                    "from " + path);
            return weapOrderMap;
        }

        // Build the XML document.
        StringBuilder log = new StringBuilder();
        try {
            DocumentBuilder builder = MegaMekXmlUtil.newSafeDocumentBuilder();
            log.append("Parsing ").append(path);
            Document doc = builder.parse(file);
            log.append("\n...Parsing finished.");

            // Get the list of units.
            NodeList listOfEntries = doc.getElementsByTagName(UNIT);
            int totalEntries = listOfEntries.getLength();
            log.append("\n\tTotal number of custom weapon order entries: ")
                    .append(totalEntries);
            for (int unitCount = 0; unitCount < totalEntries; unitCount++) {

                // Get the first element of this node.
                Element unitList = (Element) listOfEntries.item(unitCount);

                // Get the chassis
                Element unitIdElement = (Element) unitList
                        .getElementsByTagName(ID).item(0);
                if (unitIdElement == null) {
                    log.append("\n\tMissing <" + ID + "> element #").append(
                            unitCount);
                    continue;
                }
                String unitId = unitIdElement.getTextContent();

                // Get the weapon sort order type
                Element orderTypeElement = (Element) unitList
                        .getElementsByTagName(ORDER_TYPE).item(0);
                if (orderTypeElement == null) {
                    log.append("\n\tMissing <" + ORDER_TYPE + "> element #")
                            .append(unitCount);
                    continue;
                }

                // Get the weapon order
                Element weaponListElement = (Element) unitList
                        .getElementsByTagName(WEAPON_LIST).item(0);
                if (weaponListElement == null) {
                    log.append("\n\tMissing <" + WEAPON_LIST + "> element #")
                            .append(unitCount);
                    continue;
                }
                Element orderListElement = (Element) unitList
                        .getElementsByTagName(ORDER_LIST).item(0);
                if (orderListElement == null) {
                    log.append("\n\tMissing <" + ORDER_LIST + "> element #").append(unitCount);
                    continue;
                }

                WeaponOrder weapOrder = new WeaponOrder();
                weapOrder.orderType = WeaponSortOrder.valueOf(orderTypeElement.getTextContent());
                if (weapOrder.orderType == WeaponSortOrder.CUSTOM) {
                    String[] weaponList = weaponListElement.getTextContent().split(",");
                    String[] orderList = orderListElement.getTextContent().split(",");

                    for (int i = 0; i < weaponList.length; i++) {
                        weapOrder.customWeaponOrderMap.put(
                                Integer.parseInt(weaponList[i]),
                                Integer.parseInt(orderList[i]));
                    }
                }
                weapOrderMap.put(unitId, weapOrder);
            }
            return weapOrderMap;
        } catch (Exception ex) {
            throw new IOException(ex);
        } finally {
            LogManager.getLogger().info(log);
        }
    }

    /**
     * Retrieves the custom weapon order Map for the given chassis/model.
     *
     * @param chassis The unit's chassis.
     * @param model   The unit's model
     * @return A {@code Map} for the custom weapon order for the given
     *         unit. If the unit is not in the list, a NULL value is returned.
     */
    @Nullable
    public static synchronized WeaponOrder getWeaponOrder(String chassis, String model) {
        if (!initialized.get() || (null == weaponOrderMap)) {
            try {
                weaponOrderMap = loadWeaponOrderFile();
                initialized.set(true);
            } catch (Exception ex) {
                LogManager.getLogger().error("Failed to load custom weapon order file", ex);
                return null;
            }
        }
        WeaponOrder newWeapOrder = new WeaponOrder();


        // Build the unit ID from the chassis and model.
        String unitId = chassis;
        unitId += " " + model;

        try {
            if (weaponOrderMap.containsKey(unitId)) {
                final WeaponOrder storedOrder = weaponOrderMap.get(unitId);
                newWeapOrder.orderType = storedOrder.orderType;
                if (storedOrder.customWeaponOrderMap != null) {
                    newWeapOrder.customWeaponOrderMap.putAll(storedOrder.customWeaponOrderMap);
                }
                return newWeapOrder;
            } else {
                return null;
            }
        } catch (Exception e) {
            String msg = "'\nunitId: '" + unitId + "'\n";
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Sets the custom weapon order for the given chassis and model.
     *
     * @param chassis
     * @param model
     * @param type
     * @param customWeapOrder
     */
    public synchronized static void setWeaponOrder(String chassis, String model,
                                                   WeaponSortOrder type,
                                                   Map<Integer, Integer> customWeapOrder) {
        if (!initialized.get() || (null == weaponOrderMap)) {
            try {
                weaponOrderMap = loadWeaponOrderFile();
                initialized.set(true);
            } catch (Exception e) {
                LogManager.getLogger().error("Failed to load custom weapon order file", e);
            }
        }

        if (chassis == null || chassis.length() < 1 || model == null
                || model.length() < 1) {
            return;
        }
        String unitId = chassis;
        unitId += " " + model;
        WeaponOrder weapOrder = new WeaponOrder();
        weapOrder.orderType = type;
        weapOrder.customWeaponOrderMap = customWeapOrder;
        weaponOrderMap.put(unitId, weapOrder);
        updated.set(true);
    }
}
