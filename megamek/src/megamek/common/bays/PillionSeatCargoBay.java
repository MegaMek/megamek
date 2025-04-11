/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
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
 */
package megamek.common.bays;

import java.io.Serial;

/**
 * Represents an external or exposed support vehicle crew seat.
 */
public final class PillionSeatCargoBay extends StandardSeatCargoBay {
    @Serial
    private static final long serialVersionUID = 145634308684637504L;

    /**
     * Creates pillion crew seating for support vehicles.
     *
     * @param space The number of seats
     */
    public PillionSeatCargoBay(double space) {
        super(space);
        weight = space * 0.025;
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        return "Seating (Pillion) - " + currentSpace;
    }

    @Override
    public String getType() {
        return "Pillion Seats";
    }

    @Override
    public String toString() {
        String bayType = "pillionSeats";
        return this.bayString(bayType, currentSpace, doors);
    }

    @Override
    public long getCost() {
        return 10L * (long) totalSpace;
    }
}
