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
import java.util.Collections;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IGameOptions;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.EnergyWeaponHandler;
import megamek.common.weapons.handlers.InsulatedLaserWeaponHandler;
import megamek.common.weapons.handlers.PulseLaserWeaponHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Sep 2, 2004
 */
public abstract class LaserWeapon extends EnergyWeapon {
    @Serial
    private static final long serialVersionUID = -9210696480919833245L;

    public LaserWeapon() {
        super();
        flags = flags.or(F_LASER).or(F_DIRECT_FIRE);
        ammoType = AmmoType.AmmoTypeEnum.NA;

        atClass = CLASS_LASER;
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Add Dazzle mode first (before Pulse modes) for Gothic BattleTech
        // This ensures Pulse modes remain last for proper filtering
        if (gameOptions.booleanOption(megamek.common.options.OptionsConstants.ADVANCED_COMBAT_GOTHIC_DAZZLE_MODE)) {
            if (!hasModes()) {
                addMode("");
            }
            addMode("Dazzle");
        }

        // Add Pulse modes last so they can be filtered by getModesCount()
        if (!(this instanceof PulseLaserWeapon)) {
            if (!hasModes()) {
                addMode("");
                addMode("Pulse");
            } else {
                for (var mode : Collections.list(getModes())) {
                    if (!mode.getName().contains("Pulse") && !mode.getName().isEmpty()) {
                        addMode("Pulse " + mode.getName());
                    }
                }
            }
        }
    }

    @Override
    public int getModesCount(Mounted<?> mounted) {
        Mounted<?> linkedBy = mounted.getLinkedBy();
        if ((linkedBy instanceof MiscMounted)
              && !linkedBy.isInoperable()
              && ((MiscMounted) linkedBy).getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
            return super.getModesCount();
        } else if (this instanceof PulseLaserWeapon) {
            return super.getModesCount();
        }

        if (hasModes()) {
            //Only works if laser pulse module's "Pulse" modes are added last.
            synchronized (modes) {
                return (int) modes.stream().filter(mode -> !mode.getName().startsWith("Pulse")).count();
            }
        }
        return super.getModesCount();
    }

    @Override
    public int getToHitModifier(@Nullable Mounted<?> mounted) {
        if ((mounted == null) || !(mounted.getLinkedBy() instanceof MiscMounted) || !mounted.getLinkedBy()
              .getType()
              .hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
            return super.getToHitModifier(mounted);
        }
        return mounted.curMode().getName().startsWith("Pulse") ? -2 : 0;
    }

    @Override
    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        try {
            Mounted<?> linkedBy = waa.getEntity(game).getEquipment(waa.getWeaponId()).getLinkedBy();
            if ((linkedBy != null) && !linkedBy.isInoperable()) {
                if (linkedBy.getType().hasFlag(MiscType.F_LASER_INSULATOR)) {
                    return new InsulatedLaserWeaponHandler(toHit, waa, game, manager);
                } else if (linkedBy.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    return new PulseLaserWeaponHandler(toHit, waa, game, manager);
                }
            }

            return new EnergyWeaponHandler(toHit, waa, game, manager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }
}
