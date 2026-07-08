/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.caspar;

import megamek.client.bot.princess.Princess;

/**
 * The CASPAR bot: the experimental successor to {@link Princess}. It shares Princess's entire
 * network/phase/state plumbing and, in this initial version, plays identically to Princess. Experimental
 * behavior is added by overriding Princess's wiring seams (for example {@code initializePathRankers()} and
 * {@code initializeFireControls()}) so that each divergence can be compared against Princess.
 */
public class Caspar extends Princess {

    /**
     * Creates a new CASPAR bot with the given display name, configured for the given host and port.
     *
     * @param name The display name
     * @param host The host address to which to connect
     * @param port The port on the host where to connect
     */
    public Caspar(final String name, final String host, final int port) {
        super(name, host, port);
    }
}
