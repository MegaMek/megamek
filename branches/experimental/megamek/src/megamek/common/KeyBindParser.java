/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class provides a static method to read in the defaultKeybinds.xml and
 * set all of the <code>KeyCommandbind</code>'s based on the specifications in
 * the XML file.
 * 
 * @author arlith
 *
 */
public class KeyBindParser {

	/**
	 * Default path to the key bindings XML file.
	 */
	public static String keyBindingsFilename = "defaultKeyBinds.xml";
	
	//XML tag defines
	public static String KEY_BIND = "KeyBind";
	public static String KEY_CODE = "keyCode";
	public static String KEY_MODIFIER = "modifier";
	public static String COMMAND = "command";
	public static String IS_REPEATABLE = "isRepeatable";
	
	public static void parseKeyBindings(MegaMekController controller){
		// Get the path to the defaultQuirks.xml file.
        String filePath = System.getProperty("user.dir");
        if (!filePath.endsWith(File.separator)) {
            filePath += File.separator;
        }
        filePath += "mmconf" + File.separator + keyBindingsFilename;
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
            NodeList listOfUnits = doc.getElementsByTagName(KEY_BIND);
            int totalBinds = listOfUnits.getLength();
            System.out.println("Total number of key binds parsed: "
                    + totalBinds);
           
            for (int bindCount = 0; bindCount < totalBinds; bindCount++) {

                // Get the first element of this node.
                Element bindingList = (Element) listOfUnits.item(bindCount);

                // Get the key code
				Element elem = (Element) bindingList
						.getElementsByTagName(KEY_CODE).item(0);
                if (elem == null) {
                    System.err.println("Missing " + KEY_CODE + " element #"
                            + bindCount);
                    continue;
                }
                int keyCode = Integer.parseInt(elem.getTextContent());

                // Get the modifier.
				elem = (Element) bindingList
						.getElementsByTagName(KEY_MODIFIER).item(0);
				if (elem == null) {
                    System.err.println("Missing " + KEY_MODIFIER + " element #"
                            + bindCount);
                    continue;
                }
				int modifiers = Integer.parseInt(elem.getTextContent());
               
                
				// Get the command
				elem = (Element) bindingList
						.getElementsByTagName(COMMAND).item(0);
				if (elem == null) {
                    System.err.println("Missing " + COMMAND + " element #"
                            + bindCount);
                    continue;
                }
				String command = elem.getTextContent();
				
				// Get the isRepeatable
				elem = (Element) bindingList
						.getElementsByTagName(IS_REPEATABLE).item(0);
				if (elem == null) {
                    System.err.println("Missing " + IS_REPEATABLE + " element #"
                            + bindCount);
                    continue;
                }
				boolean isRepeatable = 
						Byte.parseByte(elem.getTextContent()) == 1;
				
				KeyCommandBind keyBind = KeyCommandBind.getBindByCmd(command);
				
				if (keyBind == null){
					System.err.println("Unknown command: " + command + 
							", element #" + bindCount);
				} else {
					keyBind.key = keyCode;
					keyBind.modifiers = modifiers;
					keyBind.isRepeatable = isRepeatable;
					controller.registerKeyCommandBind(keyBind);
				}
            }
        } catch (Exception e) {
            System.err.println("Error parsing key bindings!");
            e.printStackTrace(System.err);
        }
	}
	
}
