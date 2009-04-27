/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
package megamek.client.commands;

import megamek.client.Client;

/**
 * @author dirk
 */
public abstract class ClientCommand {
    protected Client client;

    private String name;
    private String helpText;

    /** Creates new ServerCommand */
    public ClientCommand(Client client, String name, String helpText) {
        this.client = client;
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
    public abstract String run(String[] args);

    // Utility functions
    public static int getDirection(String arg) {
        int face = 0;

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
            } catch (NumberFormatException nfe) {
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
