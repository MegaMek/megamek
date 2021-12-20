/*
 * MegaMek - Copyright (C) 2017 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class that loads the canon unit roles from a text file and provides lookup access.
 * The data was gathered from the master unit list in August 2016. Any unit that did not have a
 * role listed returns a value of UnitRole.UNDETERMINED.
 * 
 * @author Neoancient
 */
public class UnitRoleHandler {
    
    private static final String FILE_LOC = "unit_roles.txt";
    
    private static final UnitRoleHandler instance = new UnitRoleHandler();
    
    private final Map<String,UnitRole> roleMap = new HashMap<>();
    private volatile boolean initialized = false;
    
    /**
     * Preloads unit roles from file
     */
    public static void initialize() {
        synchronized (instance) {
            if (!instance.initialized) {
                instance.loadRoles();
            }
        }
    }
    
    /**
     * Clears all loaded data and marks as not initialized.
     */
    public static void dispose() {
        synchronized (instance) {
            if (instance.initialized) {
                instance.roleMap.clear();
                instance.initialized = false;
            }
        }
    }
    
    /**
     * Find the role used for Campaign Operations/AlphaStrike formation building rules.
     * 
     * @param unitName Canonical name of a unit
     * @return         The role defined for the unit in data/unit_roles.txt, or UNDETERMINED if
     *                 the unit has no entry
     */
    public static UnitRole getRoleFor(String unitName) {
        synchronized(instance) {
            if (!instance.initialized) {
                instance.loadRoles();
            }
        }
        UnitRole role = instance.roleMap.get(unitName);
        if (null != role) {
            return role;
        }
        return UnitRole.UNDETERMINED;
    }
    
    /**
     * Find the role used for Campaign Operations/AlphaStrike formation building rules.
     * 
     * @param ms       A unit summary
     * @return         The role defined for the unit in data/unit_roles.txt, or UNDETERMINED if
     *                 the unit has no entry
     */
    public static UnitRole getRoleFor(MechSummary ms) {
        return getRoleFor(ms.getName());
    }
    
    /**
     * Find the role used for Campaign Operations/AlphaStrike formation building rules.
     * 
     * @param en       The Entity
     * @return         The role defined for the unit in data/unit_roles.txt, or UNDETERMINED if
     *                 the unit has no entry
     */
    public static UnitRole getRoleFor(Entity en) {
        return getRoleFor(en.getShortNameRaw());
    }
    
    /**
     * Reads the values from the file. This should only be called while a lock is held on instance
     * to maintain thread safety.
     */
    private void loadRoles() {
        File f = new MegaMekFile(Configuration.dataDir(), FILE_LOC).getFile();
        FileInputStream is = null;
        BufferedReader reader = null;
        try {
            is = new FileInputStream(f);
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line = null;
            while (null != (line = reader.readLine())) {
                if (line.startsWith("#")) {
                    continue;
                }
                int delimiter = line.lastIndexOf(":");
                if (delimiter > 0) {
                    String unitName = line.substring(0, delimiter);
                    UnitRole role = UnitRole.parseRole(line.substring(delimiter + 1));
                    if (UnitRole.UNDETERMINED != role) {
                        roleMap.put(unitName, role);
                    }
                }
            }
            reader.close();
            is.close();
        } catch (FileNotFoundException e) {
            LogManager.getLogger().error("Could not locate unit role file " + f.getName());
        } catch (IOException e) {
            LogManager.getLogger().error("Error reading unit role file " + f.getName());
        }
        // We're going to mark it as initialized even if it fails because there is no benefit to
        // repeating an attempt if the file is not there or cannot be read.
        initialized = true;
    }
}
