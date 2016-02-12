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

import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

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
    public static String DEFAULT_BINDINGS_FILE = "defaultKeyBinds.xml";
    
    //XML tag defines
    public static String KEY_BIND = "KeyBind";
    public static String KEY_CODE = "keyCode";
    public static String KEY_MODIFIER = "modifier";
    public static String COMMAND = "command";
    public static String IS_REPEATABLE = "isRepeatable";
    
    public static void parseKeyBindings(MegaMekController controller){
        // Get the path to the default bindings file.
        File file = new File(Configuration.configDir(), DEFAULT_BINDINGS_FILE);
        if (!file.exists() || !file.isFile()) {
            registerDefaultKeyBinds(controller);
            return;
        }

        // Build the XML document.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            System.out.println("Parsing " + file.getName());
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
                        Boolean.parseBoolean(elem.getTextContent());
                
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
            controller.removeAllKeyCommandBinds();
            registerDefaultKeyBinds(controller);
        }
    }
    
    /**
     * Each KeyCommand has a built-in default; if now key binding file can be
     * found, we should register those defaults.
     * 
     * @param controller
     */
    public static void registerDefaultKeyBinds(MegaMekController controller) {
        for (KeyCommandBind kcb : KeyCommandBind.values()) {
            controller.registerKeyCommandBind(kcb);
        }
    }
    
    /**
     * Write the current keybindings to the default XML file.
     */
    public static void writeKeyBindings(){
        try {
            Writer output = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(new File(Configuration.configDir(), 
                            DEFAULT_BINDINGS_FILE))));
            output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            output.write("<KeyBindings " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                    " xsi:noNamespaceSchemaLocation=\"keyBindingSchema.xsl\">\n");
            
            for (KeyCommandBind kcb : KeyCommandBind.values()){
                output.write("    <KeyBind>\n");
                output.write("         <command>"+kcb.cmd+"</command> ");
                String keyTxt = "";
                if (kcb.modifiers != 0) {
                    keyTxt = KeyEvent.getKeyModifiersText(kcb.modifiers);
                    keyTxt += "-";
                }
                keyTxt += KeyEvent.getKeyText(kcb.key);
                output.write("<!-- " + keyTxt + " -->\n");
                output.write("        <keyCode>"+kcb.key+"</keyCode>\n");
                output.write("        <modifier>"+kcb.modifiers+"</modifier>\n");
                output.write("        <isRepeatable>"+kcb.isRepeatable
                        +"</isRepeatable>\n");
                output.write("    </KeyBind>\n");
                output.write("\n");
            }
            
            output.write("</KeyBindings>");
            output.close();
        } catch (IOException e) {
            System.err.println("Error writing keybindings file!");
            e.printStackTrace(System.err);
        }

        
    }
    
}
