/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.net.factories;

import megamek.common.net.connections.DataStreamConnection;
import megamek.common.net.connections.AbstractConnection;

import java.net.Socket;

/**
 * Connections factory. Creates the Client/Server connections
 */
public class ConnectionFactory {
    //region Variable Declarations
    private static ConnectionFactory instance = new ConnectionFactory();
    //endregion Variable Declarations

    //region Constructors
    private ConnectionFactory() {

    }
    //endregion Constructors

    //region Getters
    /**
     * @return the factory instance
     */
    public static ConnectionFactory getInstance() {
        return instance;
    }
    //endregion Getters

    /**
     * Creates new Client (Client-Server) connection
     * 
     * @param host server host
     * @param port server port
     * @param id connection ID
     * @return new client (client-server) connection
     */
    public AbstractConnection createClientConnection(final String host, final int port, final int id) {
        return new DataStreamConnection(host, port, id);
    }

    /**
     * Creates new Server connection
     * 
     * @param socket socket to read/write
     * @param id connection ID
     * @return new Server connection
     */
    public AbstractConnection createServerConnection(final Socket socket, final int id) {
        return new DataStreamConnection(socket, id);
    }
}
