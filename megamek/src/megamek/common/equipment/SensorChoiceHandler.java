/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.equipment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.parsers.DocumentBuilder;

import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Remembers which sensor a player wants a given chassis and model to start a battle with, in
 * {@code mmconf/customSensorChoice.xml}.
 *
 * <p>This is the sensor counterpart of {@link megamek.common.weapons.handlers.WeaponOrderHandler}, and works the same
 * way: the choice is stored against the unit's chassis and model rather than against one unit, and it is applied by
 * {@code MekFileParser} to every unit of that design as it loads. Because that parser is shared code, a choice saved
 * in MegaMek is also picked up by MekHQ and MegaMekLab.</p>
 *
 * <p>Example: a player sets a Marauder MAD-3R to Infrared in the lobby and ticks the box to remember it. Every
 * MAD-3R loaded from then on, in a MegaMek game or a MekHQ campaign, starts on infrared instead of the radar it
 * would otherwise default to.</p>
 *
 * <p>A {@link SensorFamily} is stored rather than a raw {@link Sensor} type, so the choice still resolves when a
 * variant carries a different flavour of the same sensor, and a design that no longer carries anything from the
 * saved family simply falls back to its normal default.</p>
 */
public final class SensorChoiceHandler {

    private static final MMLogger LOGGER = MMLogger.create(SensorChoiceHandler.class);

    public static final String CUSTOM_SENSOR_CHOICE_FILENAME = "customSensorChoice.xml";

    private static final String CUSTOM_SENSOR_CHOICES = "customSensorChoices";
    private static final String UNIT = "unit";
    private static final String ID = "id";
    private static final String SENSOR_FAMILY = "sensorFamily";

    private static Map<String, SensorFamily> sensorChoiceMap;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicBoolean updated = new AtomicBoolean(false);

    private SensorChoiceHandler() {
    }

    /**
     * Returns the sensor family saved for a chassis and model.
     *
     * @param chassis the unit's chassis
     * @param model   the unit's model
     *
     * @return the saved family, or {@code null} when nothing is saved for that design
     */
    public static synchronized @Nullable SensorFamily getSensorChoice(String chassis, String model) {
        if (!initialized.get() || (sensorChoiceMap == null)) {
            try {
                sensorChoiceMap = loadSensorChoiceFile();
                initialized.set(true);
            } catch (Exception exception) {
                LOGGER.error("Failed to load custom sensor choice file", exception);
                return null;
            }
        }
        return sensorChoiceMap.get(unitId(chassis, model));
    }

    /**
     * Saves a sensor family against a chassis and model. Pass {@code null} to forget a design.
     *
     * <p>The change is held in memory until {@link #saveSensorChoiceFile()} writes it out, which matches how the
     * custom weapon order is handled.</p>
     *
     * @param chassis the unit's chassis
     * @param model   the unit's model
     * @param family  the family to remember, or {@code null} to forget this design
     */
    public static synchronized void setSensorChoice(String chassis, String model, @Nullable SensorFamily family) {
        if ((chassis == null) || chassis.isEmpty() || (model == null) || model.isEmpty()) {
            return;
        }
        if (!initialized.get() || (sensorChoiceMap == null)) {
            try {
                sensorChoiceMap = loadSensorChoiceFile();
                initialized.set(true);
            } catch (Exception exception) {
                LOGGER.error("Failed to load custom sensor choice file", exception);
                sensorChoiceMap = new HashMap<>();
                initialized.set(true);
            }
        }

        String unitId = unitId(chassis, model);
        SensorFamily previous = (family == null)
              ? sensorChoiceMap.remove(unitId)
              : sensorChoiceMap.put(unitId, family);
        if (previous != family) {
            updated.set(true);
        }
    }

    /**
     * Writes the saved choices to {@code mmconf/customSensorChoice.xml}. Does nothing when nothing has changed.
     *
     * @throws IOException if the file cannot be written
     */
    public static synchronized void saveSensorChoiceFile() throws IOException {
        if (!updated.get() || (sensorChoiceMap == null)) {
            return;
        }

        File file = new MegaMekFile(Configuration.configDir(), CUSTOM_SENSOR_CHOICE_FILENAME).getFile();
        if (file.exists() && !file.canWrite()) {
            LOGGER.error("Could not save custom sensor choices to {}", CUSTOM_SENSOR_CHOICE_FILENAME);
            return;
        }

        try (Writer output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
            output.write("<?xml version=\"1.0\"?>\n");
            output.write("<" + CUSTOM_SENSOR_CHOICES + ">\n");
            for (Map.Entry<String, SensorFamily> entry : sensorChoiceMap.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                output.write("\t<" + UNIT + ">\n");
                output.write("\t\t<" + ID + ">" + MMXMLUtility.escape(entry.getKey()) + "</" + ID + ">\n");
                output.write("\t\t<" + SENSOR_FAMILY + ">" + entry.getValue().name() + "</" + SENSOR_FAMILY + ">\n");
                output.write("\t</" + UNIT + ">\n");
            }
            output.write("</" + CUSTOM_SENSOR_CHOICES + ">");
        }
        updated.set(false);
    }

    private static Map<String, SensorFamily> loadSensorChoiceFile() throws IOException {
        Map<String, SensorFamily> choices = new HashMap<>();

        File file = new MegaMekFile(Configuration.configDir(), CUSTOM_SENSOR_CHOICE_FILENAME).getFile();
        if (!file.exists() || !file.isFile()) {
            // Nothing saved yet is the normal state, not a problem worth a warning
            return choices;
        }

        try {
            DocumentBuilder builder = MMXMLUtility.newSafeDocumentBuilder();
            Document document = builder.parse(file);
            NodeList unitEntries = document.getElementsByTagName(UNIT);
            for (int entryIndex = 0; entryIndex < unitEntries.getLength(); entryIndex++) {
                Element unitElement = (Element) unitEntries.item(entryIndex);

                Element unitIdElement = (Element) unitElement.getElementsByTagName(ID).item(0);
                Element familyElement = (Element) unitElement.getElementsByTagName(SENSOR_FAMILY).item(0);
                if ((unitIdElement == null) || (familyElement == null)) {
                    LOGGER.warn("Skipping incomplete sensor choice entry #{}", entryIndex);
                    continue;
                }

                try {
                    choices.put(unitIdElement.getTextContent(),
                          SensorFamily.valueOf(familyElement.getTextContent().trim()));
                } catch (IllegalArgumentException ignored) {
                    // A family from a newer or older build; the design keeps its normal default
                    LOGGER.warn("Skipping unknown sensor family '{}' for {}",
                          familyElement.getTextContent(), unitIdElement.getTextContent());
                }
            }
            return choices;
        } catch (Exception exception) {
            throw new IOException(exception);
        }
    }

    private static String unitId(String chassis, String model) {
        return chassis + " " + model;
    }
}
