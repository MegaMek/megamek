/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/*
 * ServerCommand.java
 *
 * Created on March 30, 2002, 6:55 PM
 */

package megamek.server.commands;

import megamek.server.*;

/**
 *
 * @author  Ben
 * @version 
 */
public abstract class ServerCommand {
    
    protected Server server;
    
    private String name;
    private String helpText;

    /** Creates new ServerCommand */
    public ServerCommand(Server server, String name, String helpText) {
        this.server = server;
        this.name = name;
        this.helpText = helpText;
    }
    
    
    /**
     * Return the string trigger for this command
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns some help text for this command
     */
    public String getHelp() {
        return helpText;
    }

    /**
     * Run this command with the arguments supplied
     */
    public abstract void run(int connId, String[] args);
    
}
