/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.preference;

import gd.xml.ParseException;
import gd.xml.tiny.ParsedXML;
import gd.xml.tiny.TinyParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;

import megamek.common.CommonConstants;

public class PreferenceManager {

    public static final String DEFAULT_CFG_FILE_NAME = "mmconf/clientsettings.xml";
    public static final String CFG_FILE_OPTION_NAME = "cfgfilename";
    public static final String ROOT_NODE_NAME = "MegaMekSettings";
    public static final String CLIENT_SETTINGS_STORE_NAME = "ClientSettings";
    public static final String STORE_NODE_NAME = "store";
    public static final String PREFERENCE_NODE_NAME = "preference";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String VALUE_ATTRIBUTE = "value";

    protected Hashtable<String, IPreferenceStore> stores;
    protected ClientPreferences clientPreferences;
    protected PreferenceStore clientPreferenceStore;

    protected static PreferenceManager instance = new PreferenceManager();

    protected PreferenceManager() {
        stores = new Hashtable<String, IPreferenceStore>();
        clientPreferenceStore = new PreferenceStore();
        load();
        clientPreferences = new ClientPreferences(clientPreferenceStore);
    }

    public static PreferenceManager getInstance() {
        return instance;
    }

    public static IClientPreferences getClientPreferences() {
        return getInstance().clientPreferences;
    }

    public IPreferenceStore getPreferenceStore(String name) {
        IPreferenceStore result = stores.get(name);
        if (result == null) {
            result = new PreferenceStore();
            stores.put(name, result);
        }
        return result;
    }

    protected void load() {
        stores = new Hashtable<String, IPreferenceStore>();
        clientPreferenceStore = new PreferenceStore();
        String cfgName = System.getProperty(CFG_FILE_OPTION_NAME,
                DEFAULT_CFG_FILE_NAME);
        load(cfgName);
        clientPreferences = new ClientPreferences(clientPreferenceStore);
    }

    protected void load(String fileName) {
        ParsedXML root = null;
        InputStream is = null;

        try {
            is = new FileInputStream(new File(fileName));
        } catch (FileNotFoundException e) {
            return;
        }

        try {
            root = TinyParser.parseXML(is);
        } catch (ParseException e) {
            System.out
                    .println("Error parsing settings file'" + fileName + ",.");
            e.printStackTrace(System.out);
            return;
        }

        Enumeration<?> rootChildren = root.elements();
        ParsedXML optionsNode = (ParsedXML) rootChildren.nextElement();

        if (optionsNode.getName().equals(ROOT_NODE_NAME)) {
            Enumeration<?> children = optionsNode.elements();
            while (children.hasMoreElements()) {
                ParsedXML child = (ParsedXML) children.nextElement();
                if (child != null && child.getName().equals(STORE_NODE_NAME)) {
                    String name = child.getAttribute(NAME_ATTRIBUTE);
                    if (name.equals(CLIENT_SETTINGS_STORE_NAME)) {
                        loadGroup(child, clientPreferenceStore);
                    } else {
                        loadGroup(child, getPreferenceStore(name));
                    }
                }
            }

        } else {
            System.out
                    .println("Root node of settings file is incorrectly named. Name should be '"
                            + "ROOT_NODE_NAME"
                            + "' but name is '"
                            + optionsNode.getName() + "'");
        }
    }

    protected void loadGroup(ParsedXML node, IPreferenceStore cp) {
        Enumeration<?> children = node.elements();
        while (children.hasMoreElements()) {
            ParsedXML child = (ParsedXML) children.nextElement();
            if (child != null && child.getName().equals(PREFERENCE_NODE_NAME)) {
                String name = child.getAttribute(NAME_ATTRIBUTE);
                String value = child.getAttribute(VALUE_ATTRIBUTE);
                if (name != null && value != null) {
                    cp.putValue(name, value);
                }
            }
        }
    }

    public void save() {
        try {

            Writer output = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(new File(DEFAULT_CFG_FILE_NAME))));

            output.write("<?xml version=\"1.0\"?>");
            output.write(CommonConstants.NL);
            output.write("<" + ROOT_NODE_NAME + ">");
            output.write(CommonConstants.NL);

            // save client preference store
            saveStore(output, CLIENT_SETTINGS_STORE_NAME, clientPreferenceStore);

            // save all other stores
            for (Enumeration<String> e = stores.keys(); e.hasMoreElements();) {
                String name = e.nextElement();
                PreferenceStore store = (PreferenceStore) stores.get(name);
                saveStore(output, name, store);
            }
            output.write("</" + ROOT_NODE_NAME + ">");
            output.write(CommonConstants.NL);
            output.flush();
            output.close();
        } catch (IOException e) {
        }
    }

    protected void saveStore(Writer output, String name, PreferenceStore ps)
            throws IOException {
        output.write("\t<" + STORE_NODE_NAME + " " + NAME_ATTRIBUTE + "=\""
                + quoteXMLChars(name) + "\">");
        output.write(CommonConstants.NL);
        for (Enumeration<?> e = ps.properties.keys(); e.hasMoreElements();) {
            String pname = (String) e.nextElement();
            String pvalue = (String) ps.properties.get(pname);
            output.write("\t\t<" + PREFERENCE_NODE_NAME + " " + NAME_ATTRIBUTE
                    + "=\"" + quoteXMLChars(pname) + "\" " + VALUE_ATTRIBUTE
                    + "=\"" + quoteXMLChars(pvalue) + "\"/>");
            output.write(CommonConstants.NL);

        }
        output.write("\t</" + STORE_NODE_NAME + ">");
        output.write(CommonConstants.NL);

    }

    protected static String quoteXMLChars(String s) {
        StringBuffer result = null;
        for (int i = 0, max = s.length(), delta = 0; i < max; i++) {
            char c = s.charAt(i);
            String replacement = null;

            if (c == '&') {
                replacement = "&amp;";
            } else if (c == '<') {
                replacement = "&lt;";
            } else if (c == '\r') {
                replacement = "&#13;";
            } else if (c == '>') {
                replacement = "&gt;";
            } else if (c == '"') {
                replacement = "&quot;";
            } else if (c == '\'') {
                replacement = "&apos;";
            }

            if (replacement != null) {
                if (result == null) {
                    result = new StringBuffer(s);
                }
                String temp = result.toString();
                String firstHalf = temp.substring(0, i + delta);
                String secondHalf = temp
                        .substring(i + delta + 1, temp.length());
                result = new StringBuffer(firstHalf + replacement + secondHalf);
                delta += (replacement.length() - 1);
            }
        }
        if (result == null) {
            return s;
        }
        return result.toString();
    }

}
