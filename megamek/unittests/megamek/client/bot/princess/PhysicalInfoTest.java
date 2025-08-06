/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import megamek.common.BipedMek;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Mek;
import megamek.common.QuadMek;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PunchAttackAction;
import org.junit.jupiter.api.Test;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 2/27/14 3:23 PM
 */
class PhysicalInfoTest {

    private static final double TOLERANCE = 0.0001;

    @Test
    void testInitDamage() {
        Princess mockPrincess = mock(Princess.class);

        FireControl mockFireControl = mock(FireControl.class);
        when(mockPrincess.getFireControl(any(Entity.class))).thenReturn(mockFireControl);

        ToHitData mockToHit = mock(ToHitData.class);
        when(mockFireControl.guessToHitModifierPhysical(any(Entity.class), any(EntityState.class),
              any(Targetable.class), any(EntityState.class), any(PhysicalAttackType.class),
              any(Game.class)))
              .thenReturn(mockToHit);
        when(mockToHit.getValue()).thenReturn(7);

        Entity mockShooter = mock(BipedMek.class);
        when(mockShooter.getId()).thenReturn(1);
        when(mockShooter.getWeight()).thenReturn(50.0);

        EntityState mockShooterState = mock(EntityState.class);

        Mek mockTarget = mock(BipedMek.class);
        when(mockTarget.isLocationBad(anyInt())).thenReturn(false);
        when(mockTarget.getArmor(anyInt(), eq(false))).thenReturn(10);
        when(mockTarget.getArmor(anyInt(), eq(true))).thenReturn(5);
        when(mockTarget.getInternal(anyInt())).thenReturn(6);

        EntityState mockTargetState = mock(EntityState.class);

        Game mockGame = mock(Game.class);

        PhysicalInfo testPhysicalInfo = spy(new PhysicalInfo(mockPrincess));
        testPhysicalInfo.setShooter(mockShooter);
        testPhysicalInfo.setTarget(mockTarget);
        doNothing().when(testPhysicalInfo).setDamageDirection(any(EntityState.class), nullable(Coords.class));
        doReturn(1).when(testPhysicalInfo).getDamageDirection();

        PhysicalAttackType punch = PhysicalAttackType.LEFT_PUNCH;
        PhysicalAttackType kick = PhysicalAttackType.LEFT_KICK;

        PunchAttackAction punchAction = mock(PunchAttackAction.class);
        doReturn(punchAction).when(testPhysicalInfo).buildAction(eq(punch), anyInt(), any(Targetable.class));
        when(punchAction.toHit(any(Game.class))).thenReturn(mockToHit);

        KickAttackAction kickAction = mock(KickAttackAction.class);
        doReturn(kickAction).when(testPhysicalInfo).buildAction(eq(kick), anyInt(), any(Targetable.class));
        when(kickAction.toHit(any(Game.class))).thenReturn(mockToHit);

        // Test a vanilla punch.
        testPhysicalInfo.setShooter(mockShooter);
        testPhysicalInfo.setAttackType(punch);
        testPhysicalInfo.initDamage(punch, mockShooterState, mockTargetState, true, mockGame);
        assertEquals(0.583, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        assertEquals(5.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        assertEquals(0.0099, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getKillProbability(), TOLERANCE);
        assertEquals(5.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);

        // Test a vanilla kick.
        testPhysicalInfo.setShooter(mockShooter);
        testPhysicalInfo.setAttackType(kick);
        testPhysicalInfo.initDamage(kick, mockShooterState, mockTargetState, true, mockGame);
        assertEquals(0.583, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        assertEquals(10.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        assertEquals(0.0099, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getKillProbability(), TOLERANCE);
        assertEquals(10.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);

        // Make the puncher heavier.
        when(mockShooter.getWeight()).thenReturn(100.0);
        testPhysicalInfo.setShooter(mockShooter);
        testPhysicalInfo.setAttackType(punch);
        testPhysicalInfo.initDamage(punch, mockShooterState, mockTargetState, true, mockGame);
        assertEquals(0.583, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        assertEquals(10.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        assertEquals(0.0099, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getKillProbability(), TOLERANCE);
        assertEquals(10.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);

        // Give the target less armor and internals
        when(mockTarget.isLocationBad(anyInt())).thenReturn(false);
        when(mockTarget.getArmor(anyInt(), eq(false))).thenReturn(6);
        when(mockTarget.getArmor(anyInt(), eq(true))).thenReturn(3);
        when(mockTarget.getInternal(anyInt())).thenReturn(3);
        when(mockShooter.getWeight()).thenReturn(100.0);
        testPhysicalInfo.setShooter(mockShooter);
        testPhysicalInfo.setAttackType(punch);
        testPhysicalInfo.initDamage(punch, mockShooterState, mockTargetState, true, mockGame);
        assertEquals(0.583, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        assertEquals(10.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        assertEquals(0.5929, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        assertEquals(0.1943, testPhysicalInfo.getKillProbability(), TOLERANCE);
        assertEquals(10.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);

        // Test a non-biped trying to punch.
        testPhysicalInfo.setShooter(mock(QuadMek.class));
        testPhysicalInfo.initDamage(punch, mockShooterState, mockTargetState, true, mockGame);
        assertEquals(0.0, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getKillProbability(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);

        // Test not being able to hit.
        when(mockToHit.getValue()).thenReturn(13);
        testPhysicalInfo.initDamage(punch, mockShooterState, mockTargetState, true, mockGame);
        assertEquals(0.0, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getKillProbability(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);

        // Test a non-mek.
        testPhysicalInfo.setShooter(mock(Tank.class));
        testPhysicalInfo.initDamage(punch, mockShooterState, mockTargetState, true, mockGame);
        assertEquals(0.0, testPhysicalInfo.getProbabilityToHit(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getMaxDamage(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getExpectedCriticals(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getKillProbability(), TOLERANCE);
        assertEquals(0.0, testPhysicalInfo.getExpectedDamageOnHit(), TOLERANCE);
    }
}
