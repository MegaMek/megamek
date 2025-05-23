/*
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common;

import megamek.common.weapons.DamageType;

/**
 * Record class for passing damage application information to a Damage Manager's damageEntity()
 * method.
 *
 * This moves a lot of boilerplate and chained function call responsibility out of TWGameManager
 * in preparation for replacing those overloaded functions with a single function, and having callers
 * instantiate DamageInfo
 *
 *
 * @param entity        the target entity that will suffer damage
 * @param hit           the hit data for the location hit
 * @param damage        the amount of damage to apply
 * @param ammoExplosion ammo explosion type damage is applied directly to the IS, hurts the pilot, causes
 *                      auto-ejects, and can blow the unit to smithereens
 * @param damageType    The DamageType of the attack (mostly used by specialty armor handling).
 * @param damageIS      Should the target location's internal structure be damaged directly?
 * @param areaSatArty   Is the damage from an area saturating artillery attack / AE damage?
 * @param throughFront  Is the damage coming through the hex the unit is facing?  For Cowl, etc.
 * @param underWater    Is the damage coming from an underwater attack?
 * @param nukeS2S       is this a ship-to-ship nuke?
 *
 */
public record DamageInfo(
        Entity entity,
        HitData hit,
        int damage,
        boolean ammoExplosion,
        DamageType damageType,
        boolean damageIS,
        boolean areaSatArty,
        boolean throughFront,
        boolean underWater,
        boolean nukeS2S
    ) {

    public DamageInfo(
          Entity entity,
          HitData hit,
          int damage
    ){
        this(
              entity,
              hit,
              damage,
              false,
              DamageType.NONE,
              false,
              false
        );
    }

    public DamageInfo(
          Entity entity,
          HitData hit,
          int damage,
          boolean ammoExplosion
    ){
        this(
              entity,
              hit,
              damage,
              ammoExplosion,
              DamageType.NONE,
              false,
              false
        );
    }

    public DamageInfo(
          Entity entity,
          HitData hit,
          int damage,
          boolean ammoExplosion,
          DamageType bFrag,
          boolean damageIS
    ){
        this(
              entity,
              hit,
              damage,
              ammoExplosion,
              bFrag,
              damageIS,
              false
        );
    }

    public DamageInfo(
          Entity entity,
          HitData hit,
          int damage,
          boolean ammoExplosion,
          DamageType bFrag,
          boolean damageIS,
          boolean areaSatArty
    ){
        this(
              entity,
              hit,
              damage,
              ammoExplosion,
              bFrag,
              damageIS,
              areaSatArty,
              true
        );
    }

    public DamageInfo(
          Entity entity,
          HitData hit,
          int damage,
          boolean ammoExplosion,
          DamageType bFrag,
          boolean damageIS,
          boolean areaSatArty,
          boolean throughFront
    ){
        this(
              entity,
              hit,
              damage,
              ammoExplosion,
              bFrag,
              damageIS,
              areaSatArty,
              throughFront,
              false,
              false
        );
    }

    public DamageInfo(
          Entity entity,
          HitData hit,
          int damage,
          boolean ammoExplosion,
          DamageType bFrag,
          boolean damageIS,
          boolean areaSatArty,
          boolean throughFront,
          boolean underWater
    ) {
        this(
              entity,
              hit,
              damage,
              ammoExplosion,
              bFrag,
              damageIS,
              areaSatArty,
              throughFront,
              underWater,
              false
        );
    }
}
