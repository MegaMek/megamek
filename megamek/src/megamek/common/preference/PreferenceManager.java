/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import megamek.common.Configuration;

public class PreferenceManager {

    public static final String DEFAULT_CFG_FILE_NAME = "clientsettings.xml";
    public static final String CFG_FILE_OPTION_NAME = "cfgfilename";
    public static final String ROOT_NODE_NAME = "MegaMekSettings";
    public static final String CLIENT_SETTINGS_STORE_NAME = "ClientSettings";

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
        String cfgName = System.getProperty(
                CFG_FILE_OPTION_NAME,
                new File(Configuration.configDir(), DEFAULT_CFG_FILE_NAME).toString()
        );
        load(cfgName);
        clientPreferences = new ClientPreferences(clientPreferenceStore);
    }

    protected void load(String fileName) {
        InputStream is = null;

        try {
            is = new FileInputStream(new File(fileName));
        } catch (FileNotFoundException e) {
            return;
        }

        try {
            JAXBContext jc = JAXBContext.newInstance(Settings.class);
            
            Unmarshaller um = jc.createUnmarshaller();
            Settings opts = (Settings) um.unmarshal(is);

            for (Store store : opts.stores) {
                if (CLIENT_SETTINGS_STORE_NAME.equals(store.name)) {
                    for (XmlProperty prop : store.preferences) {
                        clientPreferenceStore.putValue(prop.key, prop.value);
                    }
                } else {
                    IPreferenceStore ips = getPreferenceStore(store.name);
                    for (XmlProperty prop : store.preferences) {
                        ips.putValue(prop.key, prop.value);
                    }
                }
            }
        } catch (JAXBException ex) {
            System.err.println("Error loading XML for client settings: " + ex.getMessage()); //$NON-NLS-1$
            ex.printStackTrace();
        }
    }

    public void save() {
        save(new File(Configuration.configDir(), DEFAULT_CFG_FILE_NAME));
    }
    
    public void save(final File file) {
        try {
            JAXBContext jc = JAXBContext.newInstance(Settings.class);
            
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            
            // The default header has the encoding and standalone properties
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty("com.sun.xml.internal.bind.xmlHeaders", "<?xml version=\"1.0\"?>");
            
            JAXBElement<Settings> element = new JAXBElement<>(new QName(ROOT_NODE_NAME), Settings.class, new Settings(clientPreferenceStore, stores));
            
            marshaller.marshal(element, file);
        } catch (JAXBException ex) {
            System.err.println("Error writing XML for client settings: " + ex.getMessage()); //$NON-NLS-1$
            ex.printStackTrace();
        }
    }

    /**
     * A wrapper class for all of the client settings.
     */
    @XmlRootElement(name = ROOT_NODE_NAME)
    @XmlAccessorType(XmlAccessType.NONE)
    private static class Settings {

        @XmlElement(name = "store")
        List<Store> stores = new ArrayList<>();
        
        Settings(final PreferenceStore clientPreferenceStore, final Map<String, IPreferenceStore> stores) {
            if (clientPreferenceStore != null) {
                this.stores.add(new Store(CLIENT_SETTINGS_STORE_NAME, clientPreferenceStore));
            }
            
            if (stores != null) {
                for (Entry<String, IPreferenceStore> ps : stores.entrySet()) {
                    this.stores.add(new Store(ps.getKey(), (PreferenceStore) ps.getValue()));
                }
            }
        }

        /**
         * Required for JAXB.
         */
        private Settings() {
        }
        
    }
    
    /**
     * A wrapper class for each PreferenceStore.
     */
    @XmlType
    private static class Store {
        
        @XmlAttribute
        String name;
        
        @XmlElement(name = "preference")
        List<XmlProperty> preferences = new ArrayList<>();

        Store(final String name, final PreferenceStore preferenceStore) {
            this.name = name;
            
            for (Entry<Object, Object> prop : preferenceStore.properties.entrySet()) {
                preferences.add(new XmlProperty(prop.getKey().toString(), prop.getValue().toString()));
            }
        }

        /**
         * Required for JAXB.
         */
        private Store() {
        }
        
    }
    
    /**
     * A wrapper class for entries in a Properties object.
     */
    @XmlType
    private static class XmlProperty {
        
        @XmlAttribute(name = "name")
        String key;
        
        @XmlAttribute
        String value;

        XmlProperty(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Required for JAXB.
         */
        private XmlProperty() {
        }
    }
}
