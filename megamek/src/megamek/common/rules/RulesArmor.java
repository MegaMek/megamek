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


import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.enums.HitDamageType;
import megamek.common.units.Entity;
import megamek.server.totalWarfare.TWDamageManager;

public abstract class RulesArmor {
    /**
     * Allow heat weapons for heat armor.
     *
     * @param heat_weapon true if the weapon is a heat weapon
     * @return true if heat weapons are allowed
     */
    public abstract boolean allowHeatWeapon(boolean heat_weapon);

    /**
     * Whether the armor allows an armor-piercing critical check for this damage type.
     *
     * @param mods the modifications info
     * @param damageType the general damage type of the hit, to tell armor-piercing ammunition from missiles
     * @return true if armor piercing is allowed
     */
    public abstract boolean allowArmorPiercing(TWDamageManager.ModsInfo mods, HitDamageType damageType);

    /** Armor types that negate the armor-piercing check under TO:AUE pp.92-94. */
    protected static boolean allowsTacticalOperationsArmorPiercing(TWDamageManager.ModsInfo mods) {
        return !(mods.hardenedArmor || mods.ferroLamellorArmor || mods.reactiveArmor);
    }

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
     * Does the armor a hit strikes reduce the heat of that weapon? Laser-reflective armor does for the weapons it
     * reflects (BMM p.114). Heat-dissipating armor acts on all outside heat; see {@link #heatAfterDissipatingArmor}.
     *
     * @param armorType the type of armor the hit strikes
     * @param heatDamage the amount of heat damage
     * @return the reduced heat damage amount
     */
    public abstract int reduceHeatDamageByArmor(int armorType, int heatDamage);

    /**
     * The heat an outside heat effect adds after the target's heat-dissipating armor. Armor with special effects acts
     * while the location struck still has armor at the time the hit is applied (Core p.201), so this is asked as the
     * heat strikes, before the hit's own damage; the Heat Phase only adds the results (TW p.159).
     *
     * @param target   the unit the heat reaches
     * @param heat     the heat of the effect
     * @param location the location the heat struck, or {@link Entity#LOC_NONE} for heat that strikes no location,
     *                 such as fire and magma
     * @param rear     true when the heat struck the rear armor of the location
     *
     * @return the heat the effect adds
     */
    public abstract int heatAfterDissipatingArmor(Entity target, int heat, int location, boolean rear);

    /** Outside heat applied to the whole unit, such as fire, magma, or an area-effect inferno hit. */
    public int reduceNonLocationHeat(Entity target, int heat) {
        return heatAfterDissipatingArmor(target, heat, Entity.LOC_NONE, false);
    }

    /** Outside heat from extreme (above 50 degrees C) temperatures: heat-dissipating armor reduces it like other outside heat */
    public int reduceExtremeHeat(Entity target, int heat) {
        return reduceNonLocationHeat(target, heat);
    }

    /**
     * Does reflective armor cause modifiers for AP?
     *
     * @param reflectiveArmor true if the armor is reflective
     * @return true if reflective armor affects AP modifiers
     */
    public abstract boolean reflectiveAP(boolean reflectiveArmor);

    /**
     * Does this armor negate through-armor critical (TAC) checks? Asked only while the location struck has armor.
     *
     * @param armorType the type of armor
     * @return true if TAC is blocked
     */
    public abstract boolean blockTAC(int armorType);

    /** Modifier from hardened armor for this critical check. */
    public abstract int hardenedLocationCriticalModifier(HitData hit);

    /**
     * Do ablation and hardened armor keep their critical-hit protections (the ablation TAC block, the hardened -2)
     * against damage of this type?
     *
     * @param damageType the general damage type of the hit
     * @return true if those protections apply
     */
    public abstract boolean criticalProtectionsApplyAgainst(HitDamageType damageType);

    /**
     * Does damage that passes through a location destroyed before this attack keep its armor-piercing check?
     *
     * @return true if the check passes through such a location
     */
    public abstract boolean armorPiercingCheckPassesDestroyedLocation();

    /**
     * Ablation armor modifier for an applicable critical check while the struck location still has armor.
     *
     * @return the modifier to a critical check that still takes place
     */
    public abstract int ablationArmorPiercingModifier();

    /**
     * How does impact armor reduce damage. While the location has armor, impact-resistant armor reduces damage "by 1
     * point for every 3 points delivered (or fraction thereof), to a minimum of 1 damage point" (IO:AE p.82; Core
     * p.202 for physical attacks).
     *
     * @param entityId the entity ID
     * @param hit the hit data
     * @param damage the damage amount
     * @param reportVec vector of reports describing the damage reduction
     * @param damageType the type of damage
     * @return the reduced damage amount
     */
    public abstract int reduceImpactDamage(int entityId, HitData hit, int damage, Vector<Report> reportVec,
          HitDamageType damageType);

    protected static int reduceImpactDamageByThird(int entityId, int damage, Vector<Report> reportVec) {
        damage = Math.max(1, damage - (int) Math.ceil(damage / 3.0));
        Report report = new Report(6089);
        report.subject = entityId;
        report.indent(3);
        report.add(damage);
        reportVec.addElement(report);
        return damage;
    }

    /**
     * Reflective armor reduction
     *
     * @param damage the damage to be reduced
     * @return the reduced damage amount
     */
    public abstract int reflectiveArmorReduction(final int damage);


    /**
     * Reactive armor reduction
     *
     * @param damage the damage to be reduced
     * @return the reduced damage amount
     */
    public abstract int reactiveArmorReduction(final int damage);

    /**
     * Ballistic reinforced armor reduction
     *
     * @param damage the damage to be reduced
     * @return the reduced damage amount
     */
    public abstract int ballisticReinforcedArmorReduction(final int damage);

    /**
     * Does ballistic-reinforced armor reduce damage of this type?
     *
     * @param damageType the general damage type of the hit
     * @return true if {@link #ballisticReinforcedArmorReduction(int)} applies
     */
    public abstract boolean ballisticReinforcedArmorReduces(HitDamageType damageType);

    /**
     * Does ferro-lamellor armor reduce damage of this type?
     *
     * @param damageType the general damage type of the hit
     * @return true if the ferro-lamellor reduction applies
     */
    public abstract boolean ferroLamellorArmorReduces(HitDamageType damageType);

    /** Whether ablation armor on the struck facing negates a taser effect at impact. */
    public abstract boolean blocksAblationTaserEffect();
}
