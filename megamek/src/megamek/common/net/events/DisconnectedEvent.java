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

import megamek.common.net.enums.ConnectionEventType;

/**
 * Instances of this class are sent when Connection closed
 */
public class DisconnectedEvent extends AbstractConnectionEvent {
    //region Variable Declarations
    private static final long serialVersionUID = -1427252999207396447L;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Constructs a disconnected connection event
     * @param source The object on which the Event initially occurred.
     */
    public DisconnectedEvent(final Object source) {
        super(ConnectionEventType.DISCONNECTED, source);
    }
    //endregion Constructors
}
