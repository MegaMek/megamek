/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.net.events;

import java.util.EventObject;

import megamek.common.net.connections.AbstractConnection;
import megamek.common.net.enums.ConnectionEventType;
import megamek.common.net.listeners.ConnectionListener;

/**
 * Instances of descendant classes are sent as a result of changes of the Connection state or packet arrival.
 *
 * @see ConnectionListener
 */
public abstract class AbstractConnectionEvent extends EventObject {
    //region Variable Declarations
    private static final long serialVersionUID = 6124300183866317006L;
    private final ConnectionEventType type;
    //endregion Variable Declarations

    //region Constructors

    /**
     * Constructs a connection event
     *
     * @param type   the connection event type
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
