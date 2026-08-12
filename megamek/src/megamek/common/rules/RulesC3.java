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


import megamek.common.ToHitData;
import megamek.common.units.Entity;

public abstract class RulesC3 {

    /**
     * What range should C3 use.
     *
     * @param range the base range
     * @param c3range the C3 range
     * @param c3ecmRange the C3 ECM range
     * @return the range to use for C3
     */
    public abstract int getC3RangeToUse(int range, int c3range, int c3ecmRange);

    /**
     * What C3 range modifier to use.
     *
     * @param mods the hit data modifications
     * @param range the base range
     * @param usingRange the range being used
     * @param c3ecmRange the C3 ECM range
     * @param c3range the C3 range
     * @param ecmAffected whether ECM is affecting the shot
     * @param attacker the attacking entity
     */
    public abstract void getC3RangeModifier(ToHitData mods, int range, int usingRange,
          int c3ecmRange, int c3range, boolean ecmAffected, Entity attacker);

    /**
     * Do C3 spotters require LOS?
     *
     * @return true if C3 spotters require line of sight
     */
    public abstract boolean c3SpotterLOSRequired();
    
    /**
     * Can C3 work with ECM.
     *
     * @return true if C3 is allowed with ECM
     */
    public abstract boolean c3AllowedWithECM();
}
