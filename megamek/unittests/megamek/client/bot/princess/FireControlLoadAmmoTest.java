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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Issue 9376: launchers that share an ammo bin must not be planned to fire more shots than the bin holds. The server
 * reloads a launcher whose bin ran dry from any other bin, so an SRM Carrier planned on three standard shots fired its
 * other launchers with Dead-Fire and Heat-Seeking ammo that needed a 13 or 14.
 */
class FireControlLoadAmmoTest {

    private static final String SRM_6 = "ISSRM6";
    private static final String STANDARD_AMMO = "IS Ammo SRM-6";
    private static final String DEAD_FIRE_AMMO = "IS Ammo SRM-6 Dead-Fire";
    private static final String TANDEM_CHARGE_AMMO = "IS Ammo SRM-6 Tandem-Charge";
    private static final int PLANNED_TO_HIT = 12;

    private Princess owner;
    private FireControl fireControl;
    private BipedMek shooter;
    private Entity target;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        owner = mock(Princess.class);
        fireControl = spy(new FireControl(owner));
        shooter = new BipedMek();
        shooter.setChassis("Test");
        shooter.setModel("SRM");
        target = mock(Entity.class);
    }

    @Test
    @DisplayName("a second launcher on an empty shared bin is dropped rather than fired with ammo that cannot hit")
    void secondLauncherDroppedWhenOnlyWorseAmmoIsLeft() throws LocationFullException {
        WeaponMounted firstLauncher = addLauncher(Mek.LOC_LEFT_TORSO);
        WeaponMounted secondLauncher = addLauncher(Mek.LOC_RIGHT_TORSO);
        AmmoMounted standard = addBin(STANDARD_AMMO, 1);
        AmmoMounted deadFire = addBin(DEAD_FIRE_AMMO, 3);
        shooter.loadAllWeapons();
        doReturn(PLANNED_TO_HIT).when(fireControl).toHitWith(any(), eq(standard));
        doReturn(14).when(fireControl).toHitWith(any(), eq(deadFire));

        WeaponFireInfo firstShot = plannedShot(firstLauncher, standard);
        WeaponFireInfo secondShot = plannedShot(secondLauncher, standard);
        FiringPlan plan = planOf(firstShot, secondShot);

        fireControl.loadAmmo(shooter, plan);

        assertEquals(1, plan.size(), "only one shot of standard ammo is left, so only one launcher should fire");
        assertSame(firstShot, plan.getFirst());
        assertSame(standard, firstLauncher.getLinkedAmmo());
        verify(owner, never()).sendAmmoChange(anyInt(), eq(shooter.getEquipmentNum(secondLauncher)), anyInt(),
              anyInt(), anyInt());
    }

    @Test
    @DisplayName("a second launcher on an empty shared bin switches to other ammo that can still hit")
    void secondLauncherSwitchesToAmmoThatCanStillHit() throws LocationFullException {
        WeaponMounted firstLauncher = addLauncher(Mek.LOC_LEFT_TORSO);
        WeaponMounted secondLauncher = addLauncher(Mek.LOC_RIGHT_TORSO);
        AmmoMounted standard = addBin(STANDARD_AMMO, 1);
        AmmoMounted deadFire = addBin(DEAD_FIRE_AMMO, 3);
        AmmoMounted tandemCharge = addBin(TANDEM_CHARGE_AMMO, 2);
        shooter.loadAllWeapons();
        doReturn(PLANNED_TO_HIT).when(fireControl).toHitWith(any(), eq(standard));
        doReturn(14).when(fireControl).toHitWith(any(), eq(deadFire));
        doReturn(PLANNED_TO_HIT).when(fireControl).toHitWith(any(), eq(tandemCharge));

        WeaponFireInfo firstShot = plannedShot(firstLauncher, standard);
        WeaponFireInfo secondShot = plannedShot(secondLauncher, standard);
        FiringPlan plan = planOf(firstShot, secondShot);

        fireControl.loadAmmo(shooter, plan);

        assertEquals(2, plan.size(), "the Tandem-Charge shot can still hit");
        assertSame(standard, firstLauncher.getLinkedAmmo());
        assertSame(tandemCharge, secondLauncher.getLinkedAmmo(), "Dead-Fire needs a 14 and should be passed over");
        verify(secondShot.getAction()).setAmmoId(shooter.getEquipmentNum(tandemCharge));
        verify(secondShot).setAmmo(tandemCharge);
    }

    @Test
    @DisplayName("of the other bins, the launcher takes the one easiest to hit with")
    void secondLauncherTakesEasiestOtherAmmo() throws LocationFullException {
        WeaponMounted firstLauncher = addLauncher(Mek.LOC_LEFT_TORSO);
        WeaponMounted secondLauncher = addLauncher(Mek.LOC_RIGHT_TORSO);
        AmmoMounted standard = addBin(STANDARD_AMMO, 1);
        AmmoMounted deadFire = addBin(DEAD_FIRE_AMMO, 3);
        AmmoMounted tandemCharge = addBin(TANDEM_CHARGE_AMMO, 2);
        shooter.loadAllWeapons();
        doReturn(8).when(fireControl).toHitWith(any(), eq(standard));
        doReturn(10).when(fireControl).toHitWith(any(), eq(deadFire));
        doReturn(9).when(fireControl).toHitWith(any(), eq(tandemCharge));

        FiringPlan plan = planOf(plannedShot(firstLauncher, standard), plannedShot(secondLauncher, standard));

        fireControl.loadAmmo(shooter, plan);

        assertEquals(2, plan.size());
        assertSame(tandemCharge, secondLauncher.getLinkedAmmo(), "Tandem-Charge needs a 9, Dead-Fire a 10");
    }

    @Test
    @DisplayName("other ammo is not used when its chance to hit is below what the launcher's ammo is worth")
    void secondLauncherDroppedWhenOtherAmmoFailsConservation() throws LocationFullException {
        WeaponMounted firstLauncher = addLauncher(Mek.LOC_LEFT_TORSO);
        WeaponMounted secondLauncher = addLauncher(Mek.LOC_RIGHT_TORSO);
        AmmoMounted standard = addBin(STANDARD_AMMO, 1);
        AmmoMounted deadFire = addBin(DEAD_FIRE_AMMO, 3);
        shooter.loadAllWeapons();
        doReturn(8).when(fireControl).toHitWith(any(), eq(standard));
        // a 9 hits about 28% of the time, short of the 50% this launcher's last shots are held for
        doReturn(9).when(fireControl).toHitWith(any(), eq(deadFire));
        when(owner.calcAmmoConservation(shooter)).thenReturn(Map.of(firstLauncher, 0.5, secondLauncher, 0.5));

        FiringPlan plan = planOf(plannedShot(firstLauncher, standard), plannedShot(secondLauncher, standard));

        fireControl.loadAmmo(shooter, plan);

        assertEquals(1, plan.size());
        assertSame(standard, firstLauncher.getLinkedAmmo());
    }

    @Test
    @DisplayName("launchers sharing a bin with enough shots all fire from it")
    void launchersShareBinWithEnoughShots() throws LocationFullException {
        WeaponMounted firstLauncher = addLauncher(Mek.LOC_LEFT_TORSO);
        WeaponMounted secondLauncher = addLauncher(Mek.LOC_RIGHT_TORSO);
        AmmoMounted standard = addBin(STANDARD_AMMO, 2);
        addBin(DEAD_FIRE_AMMO, 3);
        shooter.loadAllWeapons();
        doReturn(PLANNED_TO_HIT).when(fireControl).toHitWith(any(), eq(standard));

        FiringPlan plan = planOf(plannedShot(firstLauncher, standard), plannedShot(secondLauncher, standard));

        fireControl.loadAmmo(shooter, plan);

        assertEquals(2, plan.size());
        assertSame(standard, firstLauncher.getLinkedAmmo());
        assertSame(standard, secondLauncher.getLinkedAmmo());
    }

    private WeaponMounted addLauncher(int location) throws LocationFullException {
        EquipmentType launcherType = EquipmentType.get(SRM_6);
        assertNotNull(launcherType, SRM_6);
        return (WeaponMounted) shooter.addEquipment(launcherType, location);
    }

    private AmmoMounted addBin(String ammoName, int shots) throws LocationFullException {
        EquipmentType ammoType = EquipmentType.get(ammoName);
        assertNotNull(ammoType, ammoName);
        AmmoMounted bin = (AmmoMounted) shooter.addEquipment(ammoType, Mek.LOC_CENTER_TORSO);
        bin.setShotsLeft(shots);
        return bin;
    }

    /**
     * A shot the plan scored with the given ammo and accepted at {@link #PLANNED_TO_HIT}. The preferred ammo is the
     * planned ammo, as it is when nothing about the target calls for a special munition.
     */
    private WeaponFireInfo plannedShot(WeaponMounted launcher, AmmoMounted plannedAmmo) {
        WeaponFireInfo shot = mock(WeaponFireInfo.class);
        when(shot.getWeapon()).thenReturn(launcher);
        when(shot.getAmmo()).thenReturn(plannedAmmo);
        when(shot.getShooter()).thenReturn(shooter);
        when(shot.getTarget()).thenReturn(target);
        when(shot.getToHit()).thenReturn(new ToHitData(PLANNED_TO_HIT, "planned"));
        WeaponAttackAction action = mock(WeaponAttackAction.class);
        when(shot.getAction()).thenReturn(action);
        doReturn(plannedAmmo).when(fireControl).getPreferredAmmo(eq(shooter), eq(target), eq(launcher), any());
        return shot;
    }

    private FiringPlan planOf(WeaponFireInfo... shots) {
        FiringPlan plan = new FiringPlan(target);
        for (WeaponFireInfo shot : shots) {
            plan.add(shot);
        }
        return plan;
    }
}
