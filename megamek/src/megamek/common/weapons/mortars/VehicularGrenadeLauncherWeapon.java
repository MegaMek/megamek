/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.mortars;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.HexTarget;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.VGLWeaponHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public abstract class VehicularGrenadeLauncherWeapon extends AmmoWeapon {
    @Serial
    private static final long serialVersionUID = 3343394645568467135L;

    public VehicularGrenadeLauncherWeapon() {
        super();

        heat = 1;
        damage = 0;
        ammoType = AmmoType.AmmoTypeEnum.VGL;
        rackSize = 1;
        minimumRange = 0;
        shortRange = 1;
        mediumRange = 1;
        longRange = 1;
        extremeRange = 1;
        tonnage = 0.5;
        criticalSlots = 1;
        flags = flags.or(F_MEK_WEAPON).or(F_PROTO_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON)
              .or(F_BALLISTIC).or(F_ONE_SHOT).or(F_VGL);
        explosive = false;
        bv = 15.0;
        cost = 10000;
        rulesRefs = "127, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false)
              .setClanAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.game.Game)
     */
    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game, TWGameManager manager) {
        try {
            return new VGLWeaponHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;

    }

    public static Targetable getTargetHex(Mounted<?> weapon, int weaponID) {
        Entity owner = weapon.getEntity();
        int facing;

        facing = owner.isSecondaryArcWeapon(weaponID) ? owner.getSecondaryFacing() : owner.getFacing();
        facing = (facing + weapon.getFacing()) % 6;

        // attempt to target first the "correct" automatic coordinates.
        Coords c = owner.getPosition().translated(facing);
        if (owner.getGame().getBoard().contains(c)) {
            return new HexTarget(c, Targetable.TYPE_HEX_CLEAR);
        }

        // then one hex clockwise
        c = owner.getPosition().translated((facing + 1) % 6);
        if (owner.getGame().getBoard().contains(c)) {
            return new HexTarget(c, Targetable.TYPE_HEX_CLEAR);
        }

        // then one hex counterclockwise
        c = owner.getPosition().translated((facing - 1) % 6);
        if (owner.getGame().getBoard().contains(c)) {
            return new HexTarget(c, Targetable.TYPE_HEX_CLEAR);
        }

        // default to the "correct" coordinates even though they're off board
        c = owner.getPosition().translated(facing);
        return new HexTarget(c, Targetable.TYPE_HEX_CLEAR);
    }
}
