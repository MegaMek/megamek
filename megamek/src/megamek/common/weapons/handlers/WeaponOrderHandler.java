/*

 * Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.parsers.DocumentBuilder;

import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.enums.WeaponSortOrder;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class loads the custom weapon orders lists from the mmconf/customWeaponOrder.xml files.
 *
 * @author Arlith
 */
public class WeaponOrderHandler {
    private static final MMLogger logger = MMLogger.create(WeaponOrderHandler.class);

    public static class WeaponOrder {
        public WeaponSortOrder orderType = WeaponSortOrder.DEFAULT;
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
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicBoolean updated = new AtomicBoolean(false);

    /**
     * Save customWeaponOrderMap to a file.
     *
     */
    public synchronized static void saveWeaponOrderFile() throws IOException {
        // If the map hasn't been updated, we don't need to save it.
        if (!updated.get()) {
            return;
        }

        String path = CUSTOM_WEAPON_ORDER_FILENAME;
        File file = new MegaMekFile(Configuration.configDir(), path).getFile();
        if (file.exists() && !file.canWrite()) {
            logger.error("Could not save custom weapon orders from {}", path);
            return;
        }

        Writer output = new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(file)));

        // Output the doctype and header stuff.
        output.write("<?xml version=\"1.0\"?>\n");
        output.write("<" + CUSTOM_WEAPON_ORDER + ">\n");

        // Create the UNIT_ID tag for each chassis/model id
        for (String unitId : weaponOrderMap.keySet()) {
            StringBuilder weaponList = new StringBuilder();
            StringBuilder orderList = new StringBuilder();
            WeaponOrder weaponOrder = weaponOrderMap.get(unitId);
            if (weaponOrder == null) {
                continue;
            }

            if (weaponOrder.orderType.isCustom()) {
                // Build weapon and order lists
                for (Integer weaponID : weaponOrder.customWeaponOrderMap.keySet()) {
                    Integer order = weaponOrder.customWeaponOrderMap.get(weaponID);
                    weaponList.append(weaponID).append(",");
                    orderList.append(order).append(",");
                }
                weaponList.deleteCharAt(weaponList.length() - 1);
                orderList.deleteCharAt(orderList.length() - 1);
            }

            // Write out XML
            output.write("\t");
            output.write("<" + UNIT + ">");
            output.write("\n\t\t");
            output.write("<" + ID + ">");
            output.write(unitId);
            output.write("</" + ID + ">");
            output.write("\n\t\t");
            output.write("<" + ORDER_TYPE + ">");
            output.write(weaponOrder.orderType.name());
            output.write("</" + ORDER_TYPE + ">");
            output.write("\n\t\t");
            output.write("<" + WEAPON_LIST + ">");
            output.write(weaponList.toString());
            output.write("</" + WEAPON_LIST + ">");
            output.write("\n\t\t");
            output.write("<" + ORDER_LIST + ">");
            output.write(orderList.toString());
            output.write("</" + ORDER_LIST + ">");
            output.write("\n\t");
            output.write("</" + UNIT + ">");
        }

        output.write("\n</" + CUSTOM_WEAPON_ORDER + ">");

        output.close();
    }

    /**
     * Load customWeaponOrderMap from a file.
     *
     */
    private synchronized static Map<String, WeaponOrder> loadWeaponOrderFile() throws IOException {
        Map<String, WeaponOrder> weapOrderMap = new HashMap<>();

        String path = CUSTOM_WEAPON_ORDER_FILENAME;
        File file = new MegaMekFile(Configuration.configDir(), path).getFile();
        if (!file.exists() || !file.isFile()) {
            logger.warn("Could not load custom weapon orders from {}", path);
            return weapOrderMap;
        }

        // Build the XML document.
        StringBuilder log = new StringBuilder();
        try {
            DocumentBuilder builder = MMXMLUtility.newSafeDocumentBuilder();
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

                WeaponOrder weaponOrder = new WeaponOrder();
                weaponOrder.orderType = WeaponSortOrder.valueOf(orderTypeElement.getTextContent());
                if (weaponOrder.orderType.isCustom()) {
                    String[] weaponList = weaponListElement.getTextContent().split(",");
                    String[] orderList = orderListElement.getTextContent().split(",");
                    for (int i = 0; i < weaponList.length; i++) {
                        weaponOrder.customWeaponOrderMap.put(
                              Integer.parseInt(weaponList[i]),
                              Integer.parseInt(orderList[i]));
                    }
                }
                weapOrderMap.put(unitId, weaponOrder);
            }
            return weapOrderMap;
        } catch (Exception ex) {
            throw new IOException(ex);
        } finally {
            logger.info(log);
        }
    }

    /**
     * Retrieves the custom weapon order Map for the given chassis/model.
     *
     * @param chassis The unit's chassis.
     * @param model   The unit's model
     *
     * @return A {@code Map} for the custom weapon order for the given unit. If the unit is not in the list, a NULL
     *       value is returned.
     */
    @Nullable
    public static synchronized WeaponOrder getWeaponOrder(String chassis, String model) {
        if (!initialized.get() || (null == weaponOrderMap)) {
            try {
                weaponOrderMap = loadWeaponOrderFile();
                initialized.set(true);
            } catch (Exception ex) {
                logger.error("Failed to load custom weapon order file", ex);
                return null;
            }
        }
        WeaponOrder newWeaponOrder = new WeaponOrder();

        // Build the unit ID from the chassis and model.
        String unitId = chassis;
        unitId += " " + model;

        try {
            if (weaponOrderMap.containsKey(unitId)) {
                final WeaponOrder storedOrder = weaponOrderMap.get(unitId);
                newWeaponOrder.orderType = storedOrder.orderType;
                if (storedOrder.customWeaponOrderMap != null) {
                    newWeaponOrder.customWeaponOrderMap.putAll(storedOrder.customWeaponOrderMap);
                }
                return newWeaponOrder;
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
     */
    public synchronized static void setWeaponOrder(String chassis, String model, WeaponSortOrder type,
          Map<Integer, Integer> customWeaponOrder) {
        if (!initialized.get() || (null == weaponOrderMap)) {
            try {
                weaponOrderMap = loadWeaponOrderFile();
                initialized.set(true);
            } catch (Exception e) {
                logger.error("Failed to load custom weapon order file", e);
            }
        }

        if (chassis == null || chassis.isEmpty() || model == null || model.isEmpty()) {
            return;
        }
        String unitId = chassis;
        unitId += " " + model;
        WeaponOrder weaponOrder = new WeaponOrder();
        weaponOrder.orderType = type;
        weaponOrder.customWeaponOrderMap = customWeaponOrder;
        weaponOrderMap.put(unitId, weaponOrder);
        updated.set(true);
    }
}
