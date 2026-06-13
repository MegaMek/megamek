/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.prototypes.innerSphere;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.SmallPulseLaserPrototypeHandler;
import megamek.common.weapons.lasers.PulseLaserWeapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class ISPulseLaserSmallPrototype extends PulseLaserWeapon {
    @Serial
    private static final long serialVersionUID = 2977404162226570144L;

    public ISPulseLaserSmallPrototype() {
        super();
        this.name = "Prototype Small Pulse Laser";
        this.setInternalName("ISSmallPulseLaserPrototype");
        this.addLookupName("IS Prototype Small Pulse Laser");
        this.addLookupName("ISSmall Pulse Laser Prototype");
        shortName = "Small Pulse Laser (P)";
        this.heat = 2;
        this.damage = 3;
        this.infDamageClass = WeaponType.WEAPON_BURST_2D6;
        this.toHitModifier = -1;
        this.shortRange = 1;
        this.mediumRange = 2;
        this.longRange = 3;
        this.extremeRange = 4;
        this.waterShortRange = 1;
        this.waterMediumRange = 2;
        this.waterLongRange = 2;
        this.waterExtremeRange = 2;
        this.tonnage = 1.0;
        this.criticalSlots = 1;
        this.bv = 11;
        this.cost = 80000;
        this.shortAV = 3;
        this.maxRange = RANGE_SHORT;
        this.atClass = CLASS_POINT_DEFENSE;
        this.flags = flags.or(F_BURST_FIRE).or(F_PROTOTYPE);
        rulesRefs = "67, IO:AE";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2595, DATE_NONE, DATE_NONE, 2609, DATE_NONE)
              .setISApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.game.Game,
     * megamek.server.Server)
     */
    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            return new SmallPulseLaserPrototypeHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }

    @Override
    public int getAlphaStrikeHeat() {
        return 4;
    }
}
