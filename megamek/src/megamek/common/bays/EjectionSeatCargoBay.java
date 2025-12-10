/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.bays;

import java.io.Serial;

import megamek.common.TechAdvancement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;

/**
 * Support vehicle ejection seats.
 */
public final class EjectionSeatCargoBay extends StandardSeatCargoBay {
    @Serial
    private static final long serialVersionUID = 8916801835963112628L;

    /**
     * The default constructor is only for serialization.
     */
    private EjectionSeatCargoBay() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Creates pillion crew seating for support vehicles.
     *
     * @param space The number of seats
     */
    public EjectionSeatCargoBay(double space) {
        super(space);
        weight = space * 0.1;
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        return "Seating (Ejection) - " + currentSpace;
    }

    @Override
    public String getTransporterType() {
        return "Ejection Seats";
    }

    @Override
    public String toString() {
        String bayType = "ejectionseats";
        return this.bayString(
              bayType,
              currentSpace,
              doors
        );
    }

    @Override
    public long getCost() {
        return 25000 * (long) totalSpace;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return EjectionSeatCargoBay.techAdvancement();
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TechBase.ALL)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
              .setAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE);
    }
}
