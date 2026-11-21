package megamek.common.rules.core;
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

import java.util.EnumSet;

import megamek.common.ToHitData;
import megamek.common.equipment.AmmoType;
import megamek.common.rules.RulesAmmo;
import megamek.server.totalWarfare.TWDamageManager;

public class CoreRulesAmmo extends RulesAmmo {

    /**
     * {@inheritDoc} Core p.191 prints AC/2, LAC/2, PAC/2: -3; AC/5, LAC/5, PAC/4: -2; AC/10, AC/20, PAC/8: -1, and
     * improved autocannons use the value of their caliber. Weapons and calibers Core does not print keep the Total
     * Warfare modifier.
     */
    @Override
    public int armorPiercingMod(AmmoType inType) {
        boolean corePrintedWeapon = (inType.getAmmoType() == AmmoType.AmmoTypeEnum.AC)
              || (inType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_IMP)
              || (inType.getAmmoType() == AmmoType.AmmoTypeEnum.LAC)
              || (inType.getAmmoType() == AmmoType.AmmoTypeEnum.PAC);
        if (!corePrintedWeapon) {
            return super.armorPiercingMod(inType);
        }
        return switch (inType.getRackSize()) {
            case 2 -> -3;
            case 4, 5 -> -2;
            case 8, 10, 20 -> -1;
            default -> super.armorPiercingMod(inType);
        };
    }

    /**
     * {@inheritDoc} Core armor-piercing ammunition has no attack modifier, improved autocannons included (Core p.191).
     */
    @Override
    public void armorPiercingAttackMod(AmmoType.AmmoTypeEnum ammoType, ToHitData toHit, boolean AP) {
        // Do nothing: there is no attack modifier
    }

    /**
     * {@inheritDoc} Reduce to-hit if the target has a narc pod and not under ECM. Core p.193
     */
    @Override
    public void narcHomingTarget(ToHitData toHit) {
        toHit.addModifier(-1, "target has a narc pod");
    }

    /**
     * {@inheritDoc} Acid (AX) missiles are -1 on the cluster roll. Core p.192
     */
    @Override
    public int getAXMissileModifier() {
        return -1;
    }

    /**
     * {@inheritDoc} Acid (AX) missiles do more damage to some kinds of armor. Core p.192
     */
    @Override
    public int getAXMissileDamage(int armor, TWDamageManager.ModsInfo mods, int damage) {
        if ((mods.ferroLamellorArmor || mods.ballisticArmor || mods.reactiveArmor || mods.reflectiveArmor)
              && armor > 2) {
            damage = 3;
        }

        return damage;
    }

    /**
     * {@inheritDoc} Semi-guided can reduce terrain modifiers. Core p.193
     */
    @Override
    public int getSemiGuidedAdjustment(int modifierValue, boolean movementMod, boolean terrainMod) {
        // Semi guided reduces terrain modifiers by up to 2 (minimum 0)
        if (terrainMod) {
            if (modifierValue >= 2) {
                return 2;
            } else if (modifierValue == 1) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * {@inheritDoc} Semi-guided when tag is present ignores partial cover except from water or buildings. Core p.193
     */
    @Override
    public boolean semiGuidedIgnoresCover() {
        return true;
    }

    /**
     * {@inheritDoc} Semi-guided changes number of missiles depending on tag and direct/indirect. Core p.193
     */
    @Override
    public int getSemiGuidedNMissiles(boolean taggedTarget, boolean indirect) {
        if (!taggedTarget) {
            return -1;
        } else if (!indirect && taggedTarget) {
            return 2;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * The printed Core Rulebook prices no special munitions: ammo slots can be switched to any munition the weapon can
     * use (Core p.190), and "no other alternate ammunition types in the Core Rulebook, including Semi-Guided LRMs,
     * currently have an additional BV surcharge" beyond the TAG surcharge for Homing Arrow IV (Core errata v0.2 p.1).
     * This applies to the munition and launcher pairs Core prints: Inner Sphere caseless (AC, LAC, RAC; p.192) and AX
     * (SRM, MML; p.192), Thunder (LRM, MML, NLRM, Thunderbolt; pp.186, 193) and Thunder IV (Arrow IV; p.191). Other
     * munitions keep their Total Warfare BV.
     *
     * @param munition AmmoType of the item being considered
     * @return the munition's BV
     */
    @Override
    public double getAmmoBVAdjusted(final AmmoType munition) {
        if ((munition != null) && (munition.getBaseAmmo() != null) && isPricedLikeStandardInCore(munition)) {
            return munition.getBaseAmmo().getBaseBV();
        }
        return super.getAmmoBVAdjusted(munition);
    }

    private static boolean isPricedLikeStandardInCore(AmmoType munition) {
        EnumSet<AmmoType.Munitions> munitions = munition.getMunitionType();
        // Core prints caseless (p.192) and AX (p.192) ammunition for the Inner Sphere only
        boolean innerSphere = !munition.isClan();
        return switch (munition.getAmmoType()) {
            case AC, LAC, AC_ROTARY -> innerSphere && munitions.contains(AmmoType.Munitions.M_CASELESS);
            case SRM -> innerSphere && munitions.contains(AmmoType.Munitions.M_AX_HEAD);
            // An MML uses the SRM and LRM munitions (Core p.186)
            case MML -> (innerSphere && munitions.contains(AmmoType.Munitions.M_AX_HEAD))
                  || munitions.contains(AmmoType.Munitions.M_THUNDER);
            case LRM, NLRM, TBOLT_10, TBOLT_15, TBOLT_20 -> munitions.contains(AmmoType.Munitions.M_THUNDER);
            case ARROW_IV -> munitions.contains(AmmoType.Munitions.M_FASCAM);
            default -> false;
        };
    }
}
