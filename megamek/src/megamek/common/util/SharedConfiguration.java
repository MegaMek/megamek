/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.util;


import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.MMLogger;
import megamek.common.logging.LogLevel;

/**
 * Singleton class used to get configuration values shared across different applications.
 * 
 * Use this class to get configuration values that might need to be different between 
 * Megamek and any application that is launching Megamek, e.g. MekHQ. There should be 
 * a properties file in the <code>mmconf</code> folder called <code>shared.properties</code>.
 * This file will have configuration values that might need to differ based on the application 
 * using it. 
 * 
 * @author James Allred (wildj79 at gmail dot com)
 * @since 10/31/2017 9:24 AM
 */
public class SharedConfiguration {
    private Properties properties = new Properties();
    private static SharedConfiguration instance;
    
    private SharedConfiguration() {
        final MMLogger logger = DefaultMmLogger.getInstance();
        
        try {
            InputStream is = new FileInputStream("mmconf/shared.properties");
            properties.load(is);
        } catch (Exception e) {
            logger.log(SharedConfiguration.class, 
                       "SharedConfiguration()", 
                       LogLevel.ERROR, 
                       "Error trying to load shared.properties", 
                       e);
        }
    }

    /**
     * Get's the single instance of the {@link SharedConfiguration} class.
     * 
     * @return A single instance of the {@link SharedConfiguration} class.
     */
    public static SharedConfiguration getInstance() {
        if (instance == null) {
            synchronized (SharedConfiguration.class) {
                if (instance == null) {
                    instance = new SharedConfiguration();
                }
            }
        }
        
        return instance;
    }

    /**
     * Get's the configuration value of the provided key.
     * 
     * @param key The name of the property.
     * @return The value of the property or an empty string if the key doesn't exist.
     */
    public String getProperty(String key) {
        return getProperty(key, "");
    }

    /**
     * Get's the configuration value of the provided key.
     * 
     * @param key The name of the property.
     * @param defaultValue The default value to return if the key doesn't exist.
     * @return The value of the property or the provided default value if the key doesn't exist.
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
