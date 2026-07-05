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


import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

import java.util.Vector;

public abstract class RulesExplosions {
    // damage reduction for CASE, CASE II, etc
    public abstract int explosionDamageReduction(Mek mek, HitData hit, int damage, boolean ammoExplosion,
          Vector<Report> reportVec);

    // CASE II damage reduction
    public abstract int applyCASEIIDamageReduction(Entity entity, HitData hit, int damage, boolean ammoExplosion,
          Vector<Report> reportVec);

    // CASE II check crit chance for explosions
    public abstract int explosionCASEIImod(boolean hasCaseII, boolean ammoExplosion);

    // How much damage to equipment explosions do
    public abstract int equipmentDamage(Mounted<?> mounted, WeaponType weaponType);
}
