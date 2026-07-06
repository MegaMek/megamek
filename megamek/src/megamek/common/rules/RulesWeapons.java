package megamek.common.rules;
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

import megamek.common.CriticalSlot;
import megamek.common.Report;
import megamek.common.equipment.Mounted;

import java.util.Vector;

public abstract class RulesWeapons {
    // Does a rac unjamming cause issues?
    public abstract boolean getRACUnjamRestriction();

    // What size doe ATMs cluster in
    public abstract int getATMClusterSize();

    // Can ultra autocannons jam?
    public abstract boolean canUACsJam();

    // What happens when an AC is hit
    public abstract void setACHit(CriticalSlot cs, Mounted<?> mounted, Vector<Report> reports, int entityId);

    // Do ELRMs reduce rack size under minimums?
    public abstract int getELRMMinimumRackSize(int rackSize);
}
