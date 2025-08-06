/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.utils;

import java.io.IOException;
import java.net.ServerSocket;

import megamek.MMConstants;
import megamek.server.AbstractGameManager;
import megamek.server.Server;

public class ServerFactory {

    /**
     * @param gameManager A server requires a GameManager instance of some type to instantiate
     *
     * @return Server a valid server using an open port
     *
     * @throws IOException
     */
    public static Server createServer(AbstractGameManager gameManager) throws IOException {

        int port = getOpenPort(MMConstants.MIN_PORT_FOR_QUICK_GAME, MMConstants.MAX_PORT);
        if (port == -1) {
            throw new IOException("No ports available in range!");
        }

        return new Server(
              null, port, gameManager, false, "", null, true
        );
    }

    /**
     * @param minPort Port number at which to begin searching
     * @param maxPort Port number at which to stop the search
     *
     * @return int Port number that is currently unused
     */
    public static int getOpenPort(int minPort, int maxPort) {
        int maxIterations = maxPort - minPort;
        int port;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            port = minPort + iteration;
            if (isLocalPortFree(port)) {
                return port;
            }
        }
        return -1;
    }

    /**
     * @param port Port number to test on the local machine
     *
     * @return boolean true if port is currently unused
     */
    public static boolean isLocalPortFree(int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
