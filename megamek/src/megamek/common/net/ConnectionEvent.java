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
package megamek.common.net;

import java.util.EventObject;

/**
 * Instances of descendant classes are sent as a result of changes of the Connection state or packet
 * arrival.
 * 
 * @see ConnectionListener
 */
public abstract class ConnectionEvent extends EventObject {
    private static final long serialVersionUID = 6124300183866317006L;
    public static final int CONNECTED = 0;
    public static final int DISCONNECTED = 1;
    public static final int PACKET_RECEIVED = 2;

    private int type;

    /**
     * Constructs connection event
     * 
     * @param source The object on which the Event initially occurred.
     * @param type event type
     */
    protected ConnectionEvent(Object source, int type) {
        super(source);
        this.type = type;
    }

    /**
     * @return the type of the event
     */
    public int getType() {
        return type;
    }

    /**
     * Returns
     * 
     * @return the connection on which the Event occurred; Equivalent to the {@link #getSource())
     */
    public AbstractConnection getConnection() {
        return (AbstractConnection) getSource();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        switch (this.type) {
            case CONNECTED:
                sb.append("Connected");
                break;
            case DISCONNECTED:
                sb.append("Disconnected");
                break;
            case PACKET_RECEIVED:
                sb.append("Packet Received");
                break;
            default:
                sb.append("Unknown");
                break;
        }
        sb.append(" connection event ");
        return sb.toString();
    }
}
