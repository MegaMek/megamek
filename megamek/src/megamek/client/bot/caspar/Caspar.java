/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.caspar;

import megamek.client.bot.princess.PathRanker;
import megamek.client.bot.princess.Princess;
import megamek.logging.MMLogger;

/**
 * The bot client for CASPAR (Combat Algorithmic System for Predictive Analysis and Response).
 * @author Luana Coppio
 */
public class Caspar extends Princess {
    private static final MMLogger logger = MMLogger.create(Caspar.class);

    /**
     * Constructor - initializes a new instance of the Princess bot.
     *
     * @param name The display name.
     * @param host The host address to which to connect.
     * @param port The port on the host where to connect.
     */
    public Caspar(String name, String host, int port) {
        super(name, host, port);
    }

    @Override
    public void initializePathRankers() {
        super.initializePathRankers();

        CasparStandardPathRanker casparStandardPathRanker = new CasparStandardPathRanker(this);
        casparStandardPathRanker.setPathEnumerator(precognition.getPathEnumerator());
        pathRankers.put(PathRanker.PathRankerType.Basic, casparStandardPathRanker);
    }
}
