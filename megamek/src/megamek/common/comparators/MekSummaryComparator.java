/*
 * Copyright (C) 2002 Josh Yockey
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.comparators;

import java.util.Comparator;

import megamek.common.loaders.MekSummary;

public record MekSummaryComparator(int m_nType) implements Comparator<MekSummary> {
    public static final int T_CHASSIS = 0;
    public static final int T_MODEL = 1;
    public static final int T_WEIGHT = 2;
    public static final int T_BV = 3;
    public static final int T_YEAR = 4;
    public static final int T_COST = 5;
    public static final int T_LEVEL = 6;

    @Override
    public int compare(MekSummary ms1, MekSummary ms2) {
        return switch (m_nType) {
            case T_CHASSIS -> ms1.getChassis().compareTo(ms2.getChassis());
            case T_MODEL -> ms1.getModel().compareTo(ms2.getModel());
            case T_WEIGHT -> Double.compare(ms1.getTons(), ms2.getTons());
            case T_BV -> Integer.compare(ms1.getBV(), ms2.getBV());
            case T_YEAR -> Integer.compare(ms1.getYear(), ms2.getYear());
            case T_COST -> Long.compare(ms1.getCost(), ms2.getCost());
            case T_LEVEL -> ms1.getLevel().compareTo(ms2.getLevel());
            default -> 0;
        };
    }
}
