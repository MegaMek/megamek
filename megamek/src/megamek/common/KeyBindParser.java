/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Nicholas Walczak (walczak@cs.umn.edu)
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

package megamek.common;

import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;

import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.MegaMekController;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class provides a static method to read in the defaultKeybinds.xml and set all of the {@link KeyCommandBind}'s
 * based on the specifications in the XML file.
 *
 * @author arlith
 */
public class KeyBindParser {
    private static final MMLogger logger = MMLogger.create(KeyBindParser.class);

    /**
     * Default path to the key bindings XML file.
     */
    public static String DEFAULT_BINDINGS_FILE = "defaultKeyBinds.xml";

    // XML tag defines
    public static String KEY_BIND = "KeyBind";
    public static String KEY_CODE = "keyCode";
    public static String KEY_MODIFIER = "modifier";
    public static String COMMAND = "command";
    public static String IS_REPEATABLE = "isRepeatable";

    // Keybinds change event
    private static final ArrayList<IPreferenceChangeListener> listeners = new ArrayList<>();
    public static final String KEYBINDS_CHANGED = "keyBindsChanged";

    public static void parseKeyBindings(MegaMekController controller) {
        // Always register the hard-coded defaults first so that new binds get their
        // keys
        registerDefaultKeyBinds(controller);

        // Get the path to the default bindings file.
        File file = new MegaMekFile(Configuration.configDir(), DEFAULT_BINDINGS_FILE).getFile();
        if (!file.exists() || !file.isFile()) {
            return;
        }

        // Build the XML document.
        try {
            DocumentBuilder builder = MMXMLUtility.newSafeDocumentBuilder();
            logger.debug("Parsing {}", file.getName());
            Document doc = builder.parse(file);
            logger.debug("Parsing finished.");

            // Get the list of units.
            NodeList listOfUnits = doc.getElementsByTagName(KEY_BIND);
            int totalBinds = listOfUnits.getLength();
            logger.debug("Total number of key binds parsed: {}", totalBinds);

            for (int bindCount = 0; bindCount < totalBinds; bindCount++) {

                // Get the first element of this node.
                Element bindingList = (Element) listOfUnits.item(bindCount);

                // Get the key code
                Element elem = (Element) bindingList.getElementsByTagName(KEY_CODE).item(0);
                if (elem == null) {
                    logger.error("KeyCode - Missing {} element #{}", KEY_CODE, bindCount);
                    continue;
                }
                int keyCode = Integer.parseInt(elem.getTextContent());

                // Get the modifier.
                elem = (Element) bindingList.getElementsByTagName(KEY_MODIFIER).item(0);
                if (elem == null) {
                    logger.error("Modifier - Missing {} element #{}", KEY_MODIFIER, bindCount);
                    continue;
                }
                int modifiers = Integer.parseInt(elem.getTextContent());

                // Get the command
                elem = (Element) bindingList.getElementsByTagName(COMMAND).item(0);
                if (elem == null) {
                    logger.error("Command - Missing {} element #{}", COMMAND, bindCount);
                    continue;
                }
                String command = elem.getTextContent();

                // Get the isRepeatable
                elem = (Element) bindingList.getElementsByTagName(IS_REPEATABLE).item(0);
                if (elem == null) {
                    logger.error("Repeatable - Missing {} element #{}", IS_REPEATABLE, bindCount);
                    continue;
                }
                boolean isRepeatable = Boolean.parseBoolean(elem.getTextContent());

                KeyCommandBind keyBind = KeyCommandBind.getBindByCmd(command);

                if (keyBind == null) {
                    logger.error("Unknown command: {}, element #{}", command, bindCount);
                } else {
                    keyBind.key = keyCode;
                    keyBind.modifiers = modifiers;
                    keyBind.isRepeatable = isRepeatable;
                    controller.registerKeyCommandBind(keyBind);
                }
            }
        } catch (Exception ex) {
            logger.error("Error parsing key bindings!", ex);
            controller.removeAllKeyCommandBinds();
            registerDefaultKeyBinds(controller);
        }
    }

    /**
     * Each KeyCommand has a built-in default; if no key binding file can be found, we should register those defaults.
     *
     */
    public static void registerDefaultKeyBinds(MegaMekController controller) {
        for (KeyCommandBind kcb : KeyCommandBind.values()) {
            controller.registerKeyCommandBind(kcb);
        }
    }

    /**
     * Write the current keybindings to the default XML file.
     */
    public static void writeKeyBindings() {
        try {
            Writer output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new MegaMekFile(Configuration.configDir(),
                  DEFAULT_BINDINGS_FILE).getFile())));
            output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            output.write("<KeyBindings "
                  + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                  + " xsi:noNamespaceSchemaLocation=\"keyBindingSchema.xsd\">\n");

            for (KeyCommandBind kcb : KeyCommandBind.values()) {
                String keyTxt = "Unbound";
                if (kcb.key != 0) {
                    keyTxt = KeyEvent.getKeyText(kcb.key);
                    if (kcb.modifiers != 0) {
                        keyTxt = KeyEvent.getModifiersExText(kcb.modifiers) + "+" + keyTxt;
                    }
                }
                output.write("    <KeyBind>\n");
                output.write("        <command>" + kcb.cmd + "</command> <!-- " + keyTxt + " -->\n");
                output.write("        <keyCode>" + kcb.key + "</keyCode>\n");
                output.write("        <modifier>" + kcb.modifiers + "</modifier>\n");
                output.write("        <isRepeatable>" + kcb.isRepeatable + "</isRepeatable>\n");
                output.write("    </KeyBind>\n");
                output.write("\n");
            }

            output.write("</KeyBindings>");
            output.close();
            fireKeyBindsChangeEvent();
        } catch (Exception ex) {
            logger.error("Error writing keybindings file!", ex);
        }
    }

    /**
     * Register an object that wishes to be alerted when the key binds (may) have changed. When the keybinds change, a
     * PreferenceChange with the name KeyBindParser.KEYBINDS_CHANGED is fired.
     *
     * @param listener the <code>PreferenceListener</code> that wants to register itself.
     */
    public static synchronized void addPreferenceChangeListener(IPreferenceChangeListener listener) {
        if (!listeners.contains((listener))) {
            listeners.add(listener);
        }
    }

    /**
     * De-register an object from being alerted when the key binds (may) have changed.
     *
     * @param listener the <code>PreferenceListener</code> that wants to remove itself.
     */
    public synchronized static void removePreferenceChangeListener(IPreferenceChangeListener listener) {
        listeners.remove(listener);
    }

    private synchronized static void fireKeyBindsChangeEvent() {
        final PreferenceChangeEvent pe = new PreferenceChangeEvent(KeyBindParser.class, KEYBINDS_CHANGED, null, null);
        listeners.forEach(l -> l.preferenceChange(pe));
    }

}
