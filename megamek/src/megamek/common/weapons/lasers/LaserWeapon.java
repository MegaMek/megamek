/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.lasers;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.MiscMounted;
import megamek.common.options.IGameOptions;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.EnergyWeaponHandler;
import megamek.common.weapons.InsulatedLaserWeaponHandler;
import megamek.common.weapons.PulseLaserWeaponHandler;
import megamek.server.totalwarfare.TWGameManager;

import java.util.Collections;

/**
 * @author Andrew Hunter
 * @since Sep 2, 2004
 */
public abstract class LaserWeapon extends EnergyWeapon {
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

        if (!(this instanceof PulseLaserWeapon)) {
            if (!hasModes()) {
                addMode("");
                addMode("Pulse");
            }
            else {
                for (var mode : Collections.list(getModes())) {
                    if(!mode.getName().contains("Pulse") && !mode.getName().isEmpty()) {
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
        } else if(this instanceof PulseLaserWeapon) {
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
        if ((mounted == null) || !(mounted.getLinkedBy() instanceof MiscMounted) || !mounted.getLinkedBy().getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
            return super.getToHitModifier(mounted);
        }
        return mounted.curMode().getName().startsWith("Pulse") ? -2 : 0;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
            TWGameManager manager) {
        Mounted<?> linkedBy = waa.getEntity(game).getEquipment(waa.getWeaponId()).getLinkedBy();
        if ((linkedBy != null) && !linkedBy.isInoperable()) {
            if (linkedBy.getType().hasFlag(MiscType.F_LASER_INSULATOR)) {
                return new InsulatedLaserWeaponHandler(toHit, waa, game, manager);
            } else if (linkedBy.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                return new PulseLaserWeaponHandler(toHit, waa, game, manager);
            }
        }
        return new EnergyWeaponHandler(toHit, waa, game, manager);
    }
}
