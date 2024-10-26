/*
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.commands;

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;

/**
 * @author dirk
 */
public abstract class ClientCommand {

    public static final String CLIENT_COMMAND = "#";

    protected final ClientGUI clientGUI;
    private final String name;
    private final String helpText;

    /** Creates new ServerCommand */
    public ClientCommand(ClientGUI clientGUI, String name, String helpText) {
        this.clientGUI = clientGUI;
        this.name = name;
        this.helpText = helpText;
    }

    public Client getClient() {
        return clientGUI.getClient();
    }

    public ClientGUI getClientGUI() {
        return clientGUI;
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
    public abstract String run(String[] args);

    // Utility functions
    public static int getDirection(String arg) {
        int face;

        if (arg.equalsIgnoreCase("N")) {
            face = 0;
        } else if (arg.equalsIgnoreCase("NE")) {
            face = 1;
        } else if (arg.equalsIgnoreCase("SE")) {
            face = 2;
        } else if (arg.equalsIgnoreCase("S")) {
            face = 3;
        } else if (arg.equalsIgnoreCase("SW")) {
            face = 4;
        } else if (arg.equalsIgnoreCase("NW")) {
            face = 5;
        } else {
            try {
                face = Integer.parseInt(arg);
            } catch (NumberFormatException ignored) {
                face = 0;
            }
        }

        return face;
    }

    public static String getDirection(int arg) {
        switch (arg) {
            case 0:
                return "N";
            case 1:
                return "NE";
            case 2:
                return "SE";
            case 3:
                return "S";
            case 4:
                return "SW";
            case 5:
                return "NW";
            default:
                return "Unk";
        }
    }
}
