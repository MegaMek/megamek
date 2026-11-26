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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.weapons.Weapon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a unit planning its fire through {@link MultiTargetFireControl}, such as a gun emplacement, tries a
 * missile launcher in Indirect mode when it has no direct shot. Before this it never did, so a missile emplacement
 * behind a building stayed silent even with a friendly unit spotting its target.
 */
class MultiTargetFireControlTest {

    private static final int INDIRECT_MODE_INDEX = 1;
    private static final int DIRECT_MODE_INDEX = 0;

    private MultiTargetFireControl fireControl;
    private Game game;
    private Entity shooter;
    private Entity target;
    private WeaponMounted launcher;
    private AmmoMounted ammo;
    private WeaponFireInfo noShot;
    private WeaponFireInfo indirectShot;

    @BeforeEach
    void setUp() {
        game = mock(Game.class);
        IHonorUtil honorUtil = mock(IHonorUtil.class);
        BehaviorSettings behaviorSettings = mock(BehaviorSettings.class);
        Princess owner = mock(Princess.class);
        when(owner.getGame()).thenReturn(game);
        when(owner.getHonorUtil()).thenReturn(honorUtil);
        when(owner.getBehaviorSettings()).thenReturn(behaviorSettings);

        shooter = mock(Entity.class);
        target = mock(Entity.class);
        ammo = mock(AmmoMounted.class);

        WeaponType launcherType = mock(WeaponType.class);
        when(launcherType.hasFlag(Weapon.F_MISSILE)).thenReturn(true);
        when(launcherType.getAmmoType()).thenReturn(AmmoType.AmmoTypeEnum.LRM);
        launcher = mock(WeaponMounted.class);
        when(launcher.getType()).thenReturn(launcherType);
        when(launcher.getBayWeapons()).thenReturn(List.of());
        when(launcher.hasModeType(Weapon.MODE_MISSILE_INDIRECT)).thenReturn(true);
        when(launcher.setMode(Weapon.MODE_MISSILE_INDIRECT)).thenReturn(INDIRECT_MODE_INDEX);
        when(launcher.setMode("")).thenReturn(DIRECT_MODE_INDEX);
        when(shooter.getAmmo(launcher)).thenReturn(List.of(ammo));

        noShot = mock(WeaponFireInfo.class);
        when(noShot.getExpectedDamage()).thenReturn(0.0);
        indirectShot = mock(WeaponFireInfo.class);

        fireControl = spy(new MultiTargetFireControl(owner));
        doReturn(List.of(target)).when(fireControl).getTargetableEnemyEntities(any(), any(), any());
    }

    @Test
    void aLauncherWithNoDirectShotFiresIndirectly() {
        when(launcher.curMode()).thenReturn(EquipmentMode.getMode(""));
        when(indirectShot.getExpectedDamage()).thenReturn(3.0);
        doReturn(noShot, indirectShot).when(fireControl)
              .buildWeaponFireInfo(shooter, target, launcher, ammo, game, false);

        WeaponFireInfo best = fireControl.getBestShot(shooter, launcher);

        assertSame(indirectShot, best);
        verify(launcher).setMode(Weapon.MODE_MISSILE_INDIRECT);
        verify(indirectShot).setUpdatedFiringMode(INDIRECT_MODE_INDEX);
    }

    @Test
    void aLauncherWithNoShotEitherWayIsPutBackAsItWas() {
        when(launcher.curMode()).thenReturn(EquipmentMode.getMode(""),
              EquipmentMode.getMode(Weapon.MODE_MISSILE_INDIRECT));
        when(indirectShot.getExpectedDamage()).thenReturn(0.0);
        doReturn(noShot, indirectShot).when(fireControl)
              .buildWeaponFireInfo(shooter, target, launcher, ammo, game, false);

        assertNull(fireControl.getBestShot(shooter, launcher));
        verify(launcher).setMode(Weapon.MODE_MISSILE_INDIRECT);
        verify(launcher).setMode("");
        verify(indirectShot, times(0)).setUpdatedFiringMode(anyInt());
    }

    @Test
    void aWeaponThatCannotFireIndirectlyIsLeftAlone() {
        when(launcher.hasModeType(Weapon.MODE_MISSILE_INDIRECT)).thenReturn(false);
        doReturn(noShot).when(fireControl).buildWeaponFireInfo(shooter, target, launcher, ammo, game, false);

        assertNull(fireControl.getBestShot(shooter, launcher));
        verify(launcher, times(0)).setMode(any());
    }
}
