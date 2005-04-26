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

import java.io.PrintWriter;
import java.util.Locale;

/**
 * Interface for common client settings store 
 */
public interface IClientPreferences extends IPreferenceStore {

    boolean defaultAutoejectDisabled();

    String getLastConnectAddr();

    int getLastConnectPort();

    String getLastPlayerName();

    String getLastServerPass();

    int getLastServerPort();

    Locale getLocale();

    String getLocaleString();

    String getMapTileset();

    int getMaxPathfinderTime();

    String getDataDirectory();

    String getMechDirectory();

    PrintWriter getMekHitLocLog();

    String getMetaServerName(); 

    void setMetaServerName(String name); 

    int getGoalPlayers(); 

    void setGoalPlayers(int n); 
    
    String getServerlogFilename();

    int getServerlogMaxSize();

    boolean getShowUnitId();

    char getUnitStartChar();

    boolean keepServerlog();

    boolean memoryDumpOn();

    void setDefaultAutoejectDisabled(boolean state);

    void setKeepServerlog(boolean state);

    void setLastConnectAddr(String serverAddr);

    void setLastConnectPort(int port);

    void setLastPlayerCamoName(String camoFileName);

    void setLastPlayerCategory(String camoCategory);

    void setLastPlayerColor(int colorIndex);

    void setLastPlayerName(String name);

    void setLastServerPass(String serverPass);

    void setLastServerPort(int port);

    void setLocale(String text);

    void setMaxPathfinderTime(int i);

    void setServerlogFilename(String text);

    void setServerlogMaxSize(int i);

    void setShowUnitId(boolean state);

    void setUnitStartChar(char c);

}
