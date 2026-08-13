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
import megamek.common.units.Entity;
import megamek.server.totalWarfare.TWDamageManager;

import java.util.Vector;

public abstract class RulesArmor {
    /**
     * Allow heat weapons for heat armor.
     *
     * @param heat_weapon true if the weapon is a heat weapon
     * @return true if heat weapons are allowed
     */
    public abstract boolean allowHeatWeapon(boolean heat_weapon);

    /**
     * Does armor allow armor piercing.
     *
     * @param mods the modifications info
     * @return true if armor piercing is allowed
     */
    public abstract boolean allowArmorPiercing(TWDamageManager.ModsInfo mods);

    /**
     * Impact resistant armor.
     *
     * @return the impact armor modifier
     */
    public abstract int impactArmorMod();

    /**
     * Impact Resistant Armor breach.
     *
     * @return the breach value modifier
     */
    public abstract int impactArmorBreach();

    /**
     * Does a lance penetrate the armor.
     *
     * @param armorType the type of armor
     * @return true if a lance penetrates the armor
     */
    public abstract boolean checkLancePenetration(int armorType);

    /**
     * Does the armor reduce heat?
     *
     * @param armorType the type of armor
     * @param heatDamage the amount of heat damage
     * @return the reduced heat damage amount
     */
    public abstract int reduceHeatDamageByArmor(int armorType, int heatDamage);

    /**
     * Does reflective armor cause modifiers for AP?
     *
     * @param reflectiveArmor true if the armor is reflective
     * @return true if reflective armor affects AP modifiers
     */
    public abstract boolean reflectiveAP(boolean reflectiveArmor);

    /**
     * Block TAC (Targeting Auto-Correlator).
     *
     * @param armorType the type of armor
     * @return true if TAC is blocked
     */
    public abstract boolean blockTAC(int armorType);

    /**
     * How does impact armor reduce damage.
     *
     * @param entityId the entity ID
     * @param hit the hit data
     * @param damage the damage amount
     * @param reportVec vector of reports describing the damage reduction
     * @param damageType the type of damage
     * @return the reduced damage amount
     */
    public abstract int reduceImpactDamage(int entityId, HitData hit, int damage, Vector<Report> reportVec,
          int damageType);
}
