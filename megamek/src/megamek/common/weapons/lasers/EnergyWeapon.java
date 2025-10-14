/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.lasers;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.EnergyWeaponHandler;
import megamek.common.weapons.lasers.innerSphere.ISBombastLaser;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since May 29, 2004
 */
public abstract class EnergyWeapon extends Weapon {
    @Serial
    private static final long serialVersionUID = 3128205629152612073L;

    public EnergyWeapon() {
        flags = flags.or(F_MEK_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON).or(F_PROTO_WEAPON).or(F_ENERGY);
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            return new EnergyWeaponHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Add modes for dialed-down damage according to TacOps, p.102
        // Adds a mode for each damage value down to zero; zero is included
        // as it is specifically mentioned in TacOps.
        // The bombast laser has its own rules with to-hit modifiers and does not
        // get additional dial-down
        if (!(this instanceof ISBombastLaser)) {
            if (gameOptions.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ENERGY_WEAPONS)) {
                int dmg = (damage == WeaponType.DAMAGE_VARIABLE) ? damageShort : damage;
                for (; dmg >= 0; dmg--) {
                    addMode("Damage " + dmg);
                }
                removeMode("");
                removeMode("Pulse");
            } else {
                int dmg = (damage == WeaponType.DAMAGE_VARIABLE) ? damageShort : damage;
                for (; dmg >= 0; dmg--) {
                    removeMode("Damage " + dmg);
                    removeMode("Pulse Damage " + dmg);
                }
            }
        }
    }
}
