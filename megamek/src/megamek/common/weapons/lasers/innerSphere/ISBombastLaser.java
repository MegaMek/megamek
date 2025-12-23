/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.lasers.innerSphere;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.BombastLaserWeaponHandler;
import megamek.common.weapons.lasers.LaserWeapon;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jason Tighe
 * @since Sep 12, 2004
 */
public class ISBombastLaser extends LaserWeapon {
    @Serial
    private static final long serialVersionUID = 3379805005243042138L;

    public ISBombastLaser() {
        super();
        name = "Bombast Laser";
        setInternalName(name);
        addLookupName("IS Bombast Laser");
        addLookupName("ISBombastLaser");
        String[] modeStrings = { "Damage 12", "Damage 11", "Damage 10",
                                 "Damage 9", "Damage 8", "Damage 7" };
        setModes(modeStrings);
        heat = 12;
        damage = 12;
        shortRange = 5;
        mediumRange = 10;
        longRange = 15;
        extremeRange = 20;
        waterShortRange = 3;
        waterMediumRange = 6;
        waterLongRange = 9;
        waterExtremeRange = 12;
        tonnage = 7.0;
        criticalSlots = 3;
        bv = 137;
        cost = 200000;
        shortAV = 12;
        medAV = 12;
        maxRange = RANGE_MED;
        flags = flags.or(F_BOMBAST_LASER).andNot(F_PROTO_WEAPON);
        rulesRefs = "132, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(3064, 3085).setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
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
            return new BombastLaserWeaponHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;

    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        return (range <= AlphaStrikeElement.MEDIUM_RANGE) ? 1.02 : 0;
    }
}
