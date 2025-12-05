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

package megamek.common.weapons.autoCannons;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IGameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.RapidFireACWeaponHandler;
import megamek.common.weapons.handlers.ac.ACAPHandler;
import megamek.common.weapons.handlers.ac.ACCaselessHandler;
import megamek.common.weapons.handlers.ac.ACFlakHandler;
import megamek.common.weapons.handlers.ac.ACFlechetteHandler;
import megamek.common.weapons.handlers.ac.ACIncendiaryHandler;
import megamek.common.weapons.handlers.ac.ACTracerHandler;
import megamek.common.weapons.handlers.ac.ACWeaponHandler;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

/**
 * N.B. This class is overridden for AC/2, AC/5, AC/10, AC/10, NOT ultras/LB/RAC. (No difference between ACWeapon and
 * AmmoWeapon except the ability to use special ammunition (precision, AP, etc.))
 *
 * @author Andrew Hunter
 */
public abstract class ACWeapon extends AmmoWeapon {
    @Serial
    private static final long serialVersionUID = 1537808266032711407L;

    public ACWeapon() {
        super();
        flags = flags.or(F_DIRECT_FIRE).or(F_BALLISTIC).or(F_MEK_WEAPON).or(F_AERO_WEAPON).or(F_TANK_WEAPON);
        ammoType = AmmoType.AmmoTypeEnum.AC;
        explosive = true; // when firing incendiary ammo
        atClass = CLASS_AC;
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager gameManager) {
        try {
            Entity entity = game.getEntity(waa.getEntityId());

            if (entity != null) {
                Object mountedEquipment = entity.getEquipment(waa.getWeaponId()).getLinked().getType();

                if (mountedEquipment instanceof AmmoType ammoType) {
                    Mounted<?> weapon = entity.getEquipment(waa.getWeaponId());

                    // Auto-hit attacks (spawned from rapid-fire parent) skip rapid-fire routing
                    // to prevent infinite recursion and allow special ammo handlers to process hits
                    boolean isAutoHit = (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS);

                    if (!isAutoHit && weapon.curMode().equals("Rapid")) {
                        return new RapidFireACWeaponHandler(toHit, waa, game, gameManager);
                    }

                    // PLAYTEST3 AP Ammo
                    if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARMOR_PIERCING) || ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARMOR_PIERCING_PLAYTEST)) {
                        return new ACAPHandler(toHit, waa, game, gameManager);
                    }

                    if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLECHETTE)) {
                        return new ACFlechetteHandler(toHit, waa, game, gameManager);
                    }

                    if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY_AC)) {
                        return new ACIncendiaryHandler(toHit, waa, game, gameManager);
                    }

                    if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_TRACER)) {
                        return new ACTracerHandler(toHit, waa, game, gameManager);
                    }

                    if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLAK)) {
                        return new ACFlakHandler(toHit, waa, game, gameManager);
                    }

                    if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_CASELESS)) {
                        return new ACCaselessHandler(toHit, waa, game, gameManager);
                    }
                }
            }

            return new ACWeaponHandler(toHit, waa, game, gameManager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }

    @Override
    public int getDamage() {
        int dmg = super.getDamage();
        if ((dmg != 5) && (dmg != 2)) {
            return dmg;
        }

        if (Server.getServerInstance() != null) {
            IOption increasedAc = Server.getServerInstance().getGame()
                  .getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_INCREASED_AC_DMG);
            if ((increasedAc != null) && increasedAc.booleanValue()) {
                dmg++;
            }
        }

        return dmg;
    }

    @Override
    public double getBattleForceDamage(int range) {
        double damage = 0;
        if (range <= getLongRange()) {
            damage = getRackSize();
            if ((range == AlphaStrikeElement.SHORT_RANGE) && (getMinimumRange() > 0)) {
                damage = adjustBattleForceDamageForMinRange(damage);
            }
        }
        return damage / 10.0;
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_AC;
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Modes for allowing standard and light AC rapid fire
        IOption rapidAc = gameOptions.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RAPID_AC);
        if ((rapidAc != null) && rapidAc.booleanValue()) {
            addMode("");
            addMode(Weapon.MODE_AC_RAPID);
        } else {
            removeMode("");
            removeMode(Weapon.MODE_AC_RAPID);
        }
    }
}
