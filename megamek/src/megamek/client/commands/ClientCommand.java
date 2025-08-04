/*
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.commands;

import megamek.client.Client;
import megamek.client.ui.clientGUI.ClientGUI;

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
