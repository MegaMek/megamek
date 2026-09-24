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
    /**
     * Damage reduction for CASE, CASE II, etc.
     *
     * @param mek the MEK taking damage
     * @param hit the hit data
     * @param damage the damage amount
     * @param ammoExplosion true if this is an ammo explosion
     * @param reportVec vector of reports describing the damage reduction
     * @return the reduced damage amount
     */
    public abstract int explosionDamageReduction(Mek mek, HitData hit, int damage, boolean ammoExplosion,
          Vector<Report> reportVec);

    /**
     * CASE II damage reduction.
     *
     * @param entity the entity taking damage
     * @param hit the hit data
     * @param damage the damage amount
     * @param ammoExplosion true if this is an ammo explosion
     * @param reportVec vector of reports describing the damage reduction
     * @return the reduced damage amount
     */
    public abstract int applyCASEIIDamageReduction(Entity entity, HitData hit, int damage, boolean ammoExplosion,
          Vector<Report> reportVec);

    /**
     * CASE II check crit chance for explosions.
     *
     * @param hasCaseII true if the entity has CASE II
     * @param ammoExplosion true if this is an ammo explosion
     * @return the critical hit modifier
     */
    public abstract int explosionCASEIImod(boolean hasCaseII, boolean ammoExplosion);

    /**
     * How much damage to equipment explosions do.
     *
     * @param mounted the mounted equipment
     * @param weaponType the weapon type
     * @return the damage amount
     */
    public abstract int equipmentDamage(Mounted<?> mounted, WeaponType weaponType);

    /**
     * Are M and B pods explosive?
     * @param mounted the mounted weapon
     * @return true if they are
     */
    public abstract boolean arePodsExplosive(Mounted<?> mounted);

    /**
     * Should explosions be reduced? 
     * @return false by default
     */
    public boolean explosionsAreReduced() {
        return false;
    }
}
