/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.mortars;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.mekMortar.MekMortarAirburstHandler;
import megamek.common.weapons.handlers.mekMortar.MekMortarAntiPersonnelHandler;
import megamek.common.weapons.handlers.mekMortar.MekMortarFlareHandler;
import megamek.common.weapons.handlers.mekMortar.MekMortarHandler;
import megamek.common.weapons.handlers.mekMortar.MekMortarSmokeHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jason Tighe
 */
public abstract class MekMortarWeapon extends AmmoWeapon {

    @Serial
    private static final long serialVersionUID = -4887277242270179970L;

    public MekMortarWeapon() {
        super();
        ammoType = AmmoType.AmmoTypeEnum.MEK_MORTAR;
        damage = DAMAGE_BY_CLUSTER_TABLE;
        atClass = CLASS_MORTAR;
        flags = flags.or(F_MEK_MORTAR).or(F_MORTAR_TYPE_INDIRECT).or(F_MEK_WEAPON).or(F_MISSILE).or(F_TANK_WEAPON);
        infDamageClass = WEAPON_CLUSTER_MISSILE;
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit,
          WeaponAttackAction waa, Game game, TWGameManager manager) {
        try {
            AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId())
                  .getEquipment(waa.getWeaponId()).getLinked().getType();
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_AIRBURST)) {
                return new MekMortarAirburstHandler(toHit, waa, game, manager);
            } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_ANTI_PERSONNEL)) {
                return new MekMortarAntiPersonnelHandler(toHit, waa, game, manager);
            } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_FLARE)) {
                return new MekMortarFlareHandler(toHit, waa, game, manager);
            } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_SEMIGUIDED)) {
                // Semi-Guided works like shaped-charge, but can benefit from tag
                return new MekMortarHandler(toHit, waa, game, manager);
            } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD)) {
                return new MekMortarSmokeHandler(toHit, waa, game, manager);
            }
            // If it doesn't match other types, it's the default armor-piercing
            return new MekMortarHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;

    }

    @Override
    public double getBattleForceDamage(int range) {
        if (range > getLongRange()) {
            return 0;
        }
        double damage = Compute.calculateClusterHitTableAmount(7, getRackSize()) * 2;
        if ((range == AlphaStrikeElement.SHORT_RANGE) && (getMinimumRange() > 0)) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
    }

    @Override
    public boolean hasIndirectFire() {
        return true;
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Indirect Fire
        if (gameOptions.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            addMode("");
            addMode("Indirect");
        } else {
            removeMode("");
            removeMode("Indirect");
        }
    }

    @Override
    public String getSortingName() {
        return "Mek Mortar " + rackSize;
    }
}
