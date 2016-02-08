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
 * HelpCommand.java
 *
 * Created on March 30, 2002, 7:03 PM
 */

package megamek.server.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import megamek.server.Server;

/**
 * The help command lists the other commands when run without arguments. When
 * run with another command name as an argument, it queries that command for its
 * help string and send that to the client.
 * 
 * @author Ben
 * @version
 */
public class HelpCommand extends ServerCommand {

    /** Creates new HelpCommand */
    public HelpCommand(Server server) {
        super(
                server,
                "help",
                "Lists all of the commands available, or gives help on a specific command.  Usage: /help [command]");
    }

    @Override
    public void run(int connId, String[] args) {
        if (args.length == 1) {
            // no args
            server.sendServerChat(connId,
                    "Type /help [command] for help on a specific command.  Commands available: "
                            + commandList());
        } else {
            // argument
            ServerCommand command = server.getCommand(args[1]);
            if (command == null) {
                server.sendServerChat(connId, "Command \"" + args[1]
                        + "\" not recognized.  Commands available: "
                        + commandList());
            } else {
                server.sendServerChat(connId, "/" + command.getName() + " : "
                        + command.getHelp());
            }
        }
    }

    private String commandList() {
        StringBuffer commandList = new StringBuffer();

        ArrayList<String> cmdNames = new ArrayList<>();
        for (Enumeration<String> i = server.getAllCommandNames(); i
                .hasMoreElements();) {           
            ServerCommand command = server.getCommand(i.nextElement());
            cmdNames.add(command.getName());
        }
        
        Collections.sort(cmdNames);
        for (String cmdName : cmdNames) {
            if (commandList.length() > 0) {
                commandList.append(", ");
            }
            commandList.append(cmdName);
        }

        return commandList.toString();
    }
}
