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
package megamek.common.net.events;

import megamek.common.net.connections.AbstractConnection;
import megamek.common.net.listeners.ConnectionListener;
import megamek.common.net.enums.ConnectionEventType;

import java.util.EventObject;

/**
 * Instances of descendant classes are sent as a result of changes of the Connection state or packet
 * arrival.
 * @see ConnectionListener
 */
public abstract class AbstractConnectionEvent extends EventObject {
    //region Variable Declarations
    private static final long serialVersionUID = 6124300183866317006L;
    private final ConnectionEventType type;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Constructs connection event
     *
     * @param type the connection event type
     * @param source The object on which the Event initially occurred.
     */
    protected AbstractConnectionEvent(final ConnectionEventType type, final Object source) {
        super(source);
        this.type = type;
    }
    //endregion Constructors

    //region Getters
    /**
     * @return the type of the event
     */
    public ConnectionEventType getType() {
        return type;
    }

    /**
     * @return the connection on which the Event occurred
     */
    public AbstractConnection getConnection() {
        return (AbstractConnection) getSource();
    }
    //endregion Getters

    @Override
    public String toString() {
        return getType().name();
    }
}
