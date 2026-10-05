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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import megamek.client.bot.princess.FireControl.FireControlType;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.units.VTOL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Kill chance against vehicles (issue #9178): before this, every non-Mek target had none. */
class VehicleKillChanceTest {

    private static final double DELTA = 0.0001;
    private static final int TO_HIT = 6;
    private static final Coords SHOOTER_HEX = new Coords(10, 10);
    private static final Coords TARGET_HEX = new Coords(10, 19);
    // the shooter is due north of the target: facing north, the target is shot from the front
    private static final int FACING_NORTH = 0;
    private static final int FACING_SOUTH = 3;

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5 })
    void everyShotLandsSomewhereOnAVehicleWithNoTurret(int direction) {
        assertEquals(1.0, sumOfLocationOdds(direction, false, false), DELTA);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5 })
    void everyShotLandsSomewhereOnAVehicleWithTurrets(int direction) {
        assertEquals(1.0, sumOfLocationOdds(direction, true, false), DELTA);
        assertEquals(1.0, sumOfLocationOdds(direction, true, true), DELTA);
    }

    @Test
    void aShotFromTheFrontMostlyHitsTheFront() {
        assertEquals(28d / 36, ProbabilityCalculator.getVehicleHitProbability(0, Tank.LOC_FRONT, false, false), DELTA);
        assertEquals(22d / 36, ProbabilityCalculator.getVehicleHitProbability(0, Tank.LOC_FRONT, true, false), DELTA);
        assertEquals(6d / 36, ProbabilityCalculator.getVehicleHitProbability(0, Tank.LOC_TURRET, true, false), DELTA);
        // a 5 and a 9 from the front both land on the left, as Tank.rollHitLocation rolls them
        assertEquals(8d / 36, ProbabilityCalculator.getVehicleHitProbability(0, Tank.LOC_LEFT, false, false), DELTA);
    }

    @Test
    void aShotFromTheSideHitsThatSideThenTheFrontAndRear() {
        assertEquals(28d / 36, ProbabilityCalculator.getVehicleHitProbability(2, Tank.LOC_RIGHT, false, false), DELTA);
        assertEquals(4d / 36, ProbabilityCalculator.getVehicleHitProbability(2, Tank.LOC_FRONT, false, false), DELTA);
        assertEquals(4d / 36, ProbabilityCalculator.getVehicleHitProbability(2, Tank.LOC_REAR, false, false), DELTA);
    }

    @Test
    void aWeaponThatCanDestroyAnyLocationKillsWheneverItHits() {
        // 20 damage at 72% to hit is 14.4 expected, more than any location's 8 armor and 5 structure
        Tank truck = vehicle(Tank.class, 8, 5, false);
        WeaponFireInfo shot = shotAt(truck, 20, FACING_NORTH);

        double hitChance = Compute.oddsAbove(TO_HIT) / 100;
        assertEquals(hitChance, shot.getKillProbability(), DELTA);
    }

    @Test
    void aTurretTooToughToDestroyIsLeftOut() {
        Tank tank = vehicle(Tank.class, 8, 5, true);
        when(tank.getArmor(Tank.LOC_TURRET)).thenReturn(40);
        WeaponFireInfo shot = shotAt(tank, 20, FACING_NORTH);

        double hitChance = Compute.oddsAbove(TO_HIT) / 100;
        assertEquals((30d / 36) * hitChance, shot.getKillProbability(), DELTA);
    }

    @Test
    void aWeaponTooWeakForAnyLocationHasNoKillChance() {
        Tank tank = vehicle(Tank.class, 30, 10, false);
        WeaponFireInfo shot = shotAt(tank, 20, FACING_SOUTH);

        assertEquals(0, shot.getKillProbability(), DELTA);
    }

    @Test
    void aVtolKeepsItsOwnHitTableAndIsLeftAsItWas() {
        VTOL vtol = vehicle(VTOL.class, 2, 2, false);
        WeaponFireInfo shot = shotAt(vtol, 20, FACING_NORTH);

        assertEquals(0, shot.getKillProbability(), DELTA);
    }

    private static double sumOfLocationOdds(int direction, boolean hasTurret, boolean hasDualTurret) {
        double sum = 0;
        for (int location = Tank.LOC_FRONT; location <= Tank.LOC_TURRET_2; location++) {
            sum += ProbabilityCalculator.getVehicleHitProbability(direction, location, hasTurret, hasDualTurret);
        }
        return sum;
    }

    private static <T extends Tank> T vehicle(Class<T> type, int armor, int internal, boolean hasTurret) {
        T vehicle = mock(type);
        when(vehicle.getPosition()).thenReturn(TARGET_HEX);
        when(vehicle.getId()).thenReturn(2);
        when(vehicle.getTargetType()).thenReturn(Targetable.TYPE_ENTITY);
        when(vehicle.getDisplayName()).thenReturn("Sherpa Armored Truck");
        when(vehicle.locations()).thenReturn(hasTurret ? 6 : 5);
        when(vehicle.hasNoTurret()).thenReturn(!hasTurret);
        when(vehicle.hasNoDualTurret()).thenReturn(true);
        when(vehicle.isLocationBad(anyInt())).thenReturn(false);
        when(vehicle.getArmor(anyInt())).thenReturn(armor);
        when(vehicle.getInternal(anyInt())).thenReturn(internal);
        return vehicle;
    }

    private static WeaponFireInfo shotAt(Tank target, int damage, int targetFacing) {
        Game game = mock(Game.class);
        ToHitData toHit = mock(ToHitData.class);
        when(toHit.getValue()).thenReturn(TO_HIT);
        Princess princess = mock(Princess.class);
        when(princess.getFireControl(FireControlType.Basic)).thenReturn(mock(FireControl.class));
        when(princess.getMaxWeaponRange(any(Entity.class))).thenReturn(21);

        BipedMek shooter = mock(BipedMek.class);
        when(shooter.getPosition()).thenReturn(SHOOTER_HEX);
        when(shooter.getId()).thenReturn(1);
        when(shooter.getDisplayName()).thenReturn("Masakari");
        EntityState shooterState = mock(EntityState.class);
        when(shooterState.getPosition()).thenReturn(SHOOTER_HEX);
        EntityState targetState = mock(EntityState.class);
        when(targetState.getPosition()).thenReturn(TARGET_HEX);
        when(targetState.getFacing()).thenReturn(targetFacing);

        WeaponType weaponType = mock(WeaponType.class);
        when(weaponType.getDamage()).thenReturn(damage);
        WeaponMounted weapon = mock(WeaponMounted.class);
        EquipmentMode mode = mock(EquipmentMode.class);
        when(mode.getName()).thenReturn("");
        when(weapon.getType()).thenReturn(weaponType);
        when(weapon.curMode()).thenReturn(mode);
        when(weapon.getDesc()).thenReturn("ER PPC");
        when(shooter.getEquipmentNum(eq(weapon))).thenReturn(3);
        when(shooter.getEquipment(anyInt())).thenReturn((Mounted) weapon);
        WeaponAttackAction attack = mock(WeaponAttackAction.class);
        when(attack.getEntity(any(Game.class))).thenReturn(shooter);

        WeaponFireInfo shot = spy(new WeaponFireInfo(princess));
        shot.setShooter(shooter);
        shot.setShooterState(shooterState);
        shot.setTarget(target);
        shot.setTargetState(targetState);
        shot.setWeapon(weapon);
        shot.setGame(game);
        doReturn(toHit).when(shot).calcToHit();
        doReturn(attack).when(shot).buildWeaponAttackAction();
        doReturn(new double[] { damage, 0D, 0D }).when(shot).computeExpectedDamage();
        shot.initDamage(null, false, true, null);
        return shot;
    }
}
