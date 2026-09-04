/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2016-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * @author nderwin
 */
class GameOptionsTest {

    private GameOptions testMe;

    @TempDir
    private Path tempDirectory;

    @BeforeEach
    void beforeEach() {
        testMe = new GameOptions();
    }

    @Test
    void testSaveAndLoadOptions() throws IOException {
        assertTrue(Files.isDirectory(tempDirectory));
        final Path createdFilePath = Files.createFile(tempDirectory.resolve("test-game-options.xml"));
        final File file = createdFilePath.toFile();

        Vector<IBasicOption> options = new Vector<>();
        Enumeration<IOption> opts = testMe.getOptions();
        int count = 0;
        while (opts.hasMoreElements()) {
            IOption io = opts.nextElement();

            switch (io.getType()) {
                case IOption.STRING:
                case IOption.CHOICE:
                    io.setValue("" + count);
                    break;

                case IOption.BOOLEAN:
                    if (count % 2 == 0) {
                        io.setValue(Boolean.TRUE);
                    } else {
                        io.setValue(Boolean.FALSE);
                    }
                    break;

                case IOption.INTEGER:
                    io.setValue(count);
                    break;

                case IOption.FLOAT:
                    io.setValue(Float.valueOf("" + count));
                    break;
            }

            options.add(io);
            count++;
        }

        GameOptions.saveOptions(options, file.getAbsolutePath());

        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        testMe.loadOptions(file, true);
        opts = testMe.getOptions();
        count = 0;
        while (opts.hasMoreElements()) {
            IOption io = opts.nextElement();

            switch (io.getType()) {
                case IOption.STRING:
                case IOption.CHOICE:
                case IOption.INTEGER:
                    assertEquals(io.getValue().toString(), "" + count);
                    break;
                case IOption.BOOLEAN:
                    if ((count % 2) == 0) {
                        assertTrue(io.booleanValue());
                    } else {
                        assertFalse(io.booleanValue());
                    }
                    break;
                case IOption.FLOAT:
                    assertEquals(Float.parseFloat("" + count), io.floatValue(), 0.0f);
                    break;
            }

            count++;
        }
    }

    @Test
    void rulesSystemReplacesLegacyPlaytestOptions() {
        IOption rulesSystem = testMe.getOption(OptionsConstants.RULES_SYSTEM);

        assertEquals(IOption.CHOICE, rulesSystem.getType());
        assertEquals(OptionsConstants.RULES_CORE, rulesSystem.stringValue());
        assertNull(testMe.getOption("twrules"));
        assertNull(testMe.getOption("playtest_1"));
        assertNull(testMe.getOption("playtest_2"));
        assertNull(testMe.getOption("playtest_3"));
    }

    /** The separate Manei Domini switch is gone; what it meant now lives in the pilot implants setting. */
    @Test
    void maneiDominiSwitchIsNoLongerRegistered() {
        assertNull(testMe.getOption(LEGACY_MANEI_DOMINI));
        assertNotNull(testMe.getOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE));
    }

    /**
     * An options file saved before the merge with Manei Domini on and the neural interface rules off still
     * allows implants after loading, whichever order the two were written in.
     */
    @Test
    void loadingASavedManeiDominiSwitchRaisesOffToPilotAbilitiesOnly() {
        File file = tempDirectory.resolve("legacy-manei-domini-on.xml").toFile();
        Vector<IBasicOption> saved = new Vector<>();
        saved.add(new BasicOption(LEGACY_MANEI_DOMINI, true));
        saved.add(new BasicOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE,
              OptionsConstants.NEURAL_INTERFACE_MODE_OFF));
        GameOptions.saveOptions(saved, file.getAbsolutePath());

        Vector<IOption> changed = testMe.loadOptions(file, false);

        assertEquals(OptionsConstants.NEURAL_INTERFACE_MODE_PILOT_ONLY,
              testMe.stringOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE));
        assertTrue(changed.contains(testMe.getOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE)),
              "the raised setting is reported as a changed option");
    }

    /** A file that already allowed implants through Full Tracking keeps that stricter setting. */
    @Test
    void loadingASavedManeiDominiSwitchLeavesFullTrackingAlone() {
        File file = tempDirectory.resolve("legacy-manei-domini-full.xml").toFile();
        Vector<IBasicOption> saved = new Vector<>();
        saved.add(new BasicOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE,
              OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING));
        saved.add(new BasicOption(LEGACY_MANEI_DOMINI, true));
        GameOptions.saveOptions(saved, file.getAbsolutePath());

        testMe.loadOptions(file, false);

        assertEquals(OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING,
              testMe.stringOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE));
    }

    /** A saved switch that was off changes nothing, so the rules stay exactly as the file says. */
    @Test
    void loadingASavedManeiDominiSwitchThatWasOffChangesNothing() {
        File file = tempDirectory.resolve("legacy-manei-domini-off.xml").toFile();
        Vector<IBasicOption> saved = new Vector<>();
        saved.add(new BasicOption(LEGACY_MANEI_DOMINI, false));
        saved.add(new BasicOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE,
              OptionsConstants.NEURAL_INTERFACE_MODE_OFF));
        GameOptions.saveOptions(saved, file.getAbsolutePath());

        testMe.loadOptions(file, false);

        assertEquals(OptionsConstants.NEURAL_INTERFACE_MODE_OFF,
              testMe.stringOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE));
    }

    /** The campaign-file loader does the same fold, even with the switch written before the setting. */
    @Test
    void fillingFromCampaignXmlRaisesOffWhenManeiDominiWasOn() throws Exception {
        NodeList nodes = campaignGameOptionNodes(
              gameOptionNode(LEGACY_MANEI_DOMINI, "true")
                    + gameOptionNode(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE,
                    OptionsConstants.NEURAL_INTERFACE_MODE_OFF));

        testMe.fillFromXML(nodes);

        assertEquals(OptionsConstants.NEURAL_INTERFACE_MODE_PILOT_ONLY,
              testMe.stringOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE));
    }

    @Test
    void fillingFromCampaignXmlKeepsFullTrackingWhenManeiDominiWasOn() throws Exception {
        NodeList nodes = campaignGameOptionNodes(
              gameOptionNode(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE,
                    OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING)
                    + gameOptionNode(LEGACY_MANEI_DOMINI, "true"));

        testMe.fillFromXML(nodes);

        assertEquals(OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING,
              testMe.stringOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE));
    }

    @Test
    void fillingFromCampaignXmlLeavesOffWhenManeiDominiWasOff() throws Exception {
        NodeList nodes = campaignGameOptionNodes(
              gameOptionNode(LEGACY_MANEI_DOMINI, "false")
                    + gameOptionNode(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE,
                    OptionsConstants.NEURAL_INTERFACE_MODE_OFF));

        testMe.fillFromXML(nodes);

        assertEquals(OptionsConstants.NEURAL_INTERFACE_MODE_OFF,
              testMe.stringOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE));
    }

    /** The name the retired switch was saved under, spelled out so the test does not depend on the constant. */
    private static final String LEGACY_MANEI_DOMINI = "manei_domini";

    private static String gameOptionNode(String name, String value) {
        return "<gameOption><name>" + name + "</name><value>" + value + "</value></gameOption>";
    }

    /** @return the child nodes of a {@code gameOptions} element, as {@link GameOptions#writeToXML} lays them out */
    private static NodeList campaignGameOptionNodes(String gameOptionNodes) throws Exception {
        String xml = "<gameOptions>" + gameOptionNodes + "</gameOptions>";
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
              .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return document.getDocumentElement().getChildNodes();
    }
}
