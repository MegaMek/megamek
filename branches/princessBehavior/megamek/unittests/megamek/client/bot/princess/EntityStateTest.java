/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot.princess;

import junit.framework.TestCase;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.MovePath;
import megamek.common.Targetable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/8/13 1:39 PM
 */
@RunWith(JUnit4.class)
public class EntityStateTest {

    @Test
    public void testInitFromEntity() {
        // Test a normal entity that just ran.
        Coords expectedCoords = new Coords(10, 10);
        int expectedFacing = 3;
        int expectedDistance = 6;
        int expectedHeat = 4;
        int expectedSecondaryFacing = 4;
        EntityMovementType expectedMoveType = EntityMovementType.MOVE_RUN;
        Entity mockEntity = Mockito.mock(Entity.class);
        Mockito.when(mockEntity.getPosition()).thenReturn(expectedCoords);
        Mockito.when(mockEntity.getFacing()).thenReturn(expectedFacing);
        Mockito.when(mockEntity.getDeltaDistance()).thenReturn(expectedDistance);
        Mockito.when(mockEntity.getHeat()).thenReturn(expectedHeat);
        Mockito.when(mockEntity.isProne()).thenReturn(false);
        Mockito.when(mockEntity.isHullDown()).thenReturn(false);
        Mockito.when(mockEntity.isImmobile()).thenReturn(false);
        Mockito.when(mockEntity.getMoved()).thenReturn(expectedMoveType);
        Mockito.when(mockEntity.getSecondaryFacing()).thenReturn(expectedSecondaryFacing);
        EntityState testEntityState = new EntityState();
        testEntityState.init(mockEntity);
        TestCase.assertEquals(expectedCoords, testEntityState.getPosition());
        TestCase.assertEquals(expectedFacing, testEntityState.getFacing());
        TestCase.assertEquals(expectedDistance, testEntityState.getHexesMoved());
        TestCase.assertEquals(expectedHeat, testEntityState.getHeat());
        TestCase.assertEquals(expectedSecondaryFacing, testEntityState.getSecondaryFacing());
        TestCase.assertFalse(testEntityState.isProne());
        TestCase.assertFalse(testEntityState.isImmobile());
        TestCase.assertFalse(testEntityState.isJumping());
        TestCase.assertEquals(expectedMoveType, testEntityState.getMovementType());

        // Test an entity that just jumped.
        expectedMoveType = EntityMovementType.MOVE_JUMP;
        Mockito.when(mockEntity.getMoved()).thenReturn(expectedMoveType);
        testEntityState.init(mockEntity);
        TestCase.assertEquals(expectedCoords, testEntityState.getPosition());
        TestCase.assertEquals(expectedFacing, testEntityState.getFacing());
        TestCase.assertEquals(expectedDistance, testEntityState.getHexesMoved());
        TestCase.assertEquals(expectedHeat, testEntityState.getHeat());
        TestCase.assertEquals(expectedSecondaryFacing, testEntityState.getSecondaryFacing());
        TestCase.assertFalse(testEntityState.isProne());
        TestCase.assertFalse(testEntityState.isImmobile());
        TestCase.assertTrue(testEntityState.isJumping());
        TestCase.assertEquals(expectedMoveType, testEntityState.getMovementType());

        // Test a prone entity.
        expectedMoveType = EntityMovementType.MOVE_WALK;
        Mockito.when(mockEntity.getMoved()).thenReturn(expectedMoveType);
        Mockito.when(mockEntity.isProne()).thenReturn(true);
        testEntityState.init(mockEntity);
        TestCase.assertEquals(expectedCoords, testEntityState.getPosition());
        TestCase.assertEquals(expectedFacing, testEntityState.getFacing());
        TestCase.assertEquals(expectedDistance, testEntityState.getHexesMoved());
        TestCase.assertEquals(expectedHeat, testEntityState.getHeat());
        TestCase.assertEquals(expectedSecondaryFacing, testEntityState.getSecondaryFacing());
        TestCase.assertTrue(testEntityState.isProne());
        TestCase.assertFalse(testEntityState.isImmobile());
        TestCase.assertFalse(testEntityState.isJumping());
        TestCase.assertEquals(expectedMoveType, testEntityState.getMovementType());

        // Test a hull down entity.
        Mockito.when(mockEntity.isProne()).thenReturn(false);
        Mockito.when(mockEntity.isHullDown()).thenReturn(true);
        testEntityState.init(mockEntity);
        TestCase.assertEquals(expectedCoords, testEntityState.getPosition());
        TestCase.assertEquals(expectedFacing, testEntityState.getFacing());
        TestCase.assertEquals(expectedDistance, testEntityState.getHexesMoved());
        TestCase.assertEquals(expectedHeat, testEntityState.getHeat());
        TestCase.assertEquals(expectedSecondaryFacing, testEntityState.getSecondaryFacing());
        TestCase.assertTrue(testEntityState.isProne());
        TestCase.assertFalse(testEntityState.isImmobile());
        TestCase.assertFalse(testEntityState.isJumping());
        TestCase.assertEquals(expectedMoveType, testEntityState.getMovementType());
    }

    @Test
    public void testInitFromTargetable() {
        // Buildings and the like really only vary in position.
        Targetable mockTarget = Mockito.mock(Targetable.class);
        Coords expectedCoords = new Coords(10, 10);
        Mockito.when(mockTarget.getPosition()).thenReturn(expectedCoords);
        EntityState testEntityState = new EntityState();
        testEntityState.init(mockTarget);
        TestCase.assertEquals(expectedCoords, testEntityState.getPosition());
        TestCase.assertEquals(0, testEntityState.getFacing());
        TestCase.assertEquals(0, testEntityState.getHexesMoved());
        TestCase.assertEquals(0, testEntityState.getHeat());
        TestCase.assertEquals(0, testEntityState.getSecondaryFacing());
        TestCase.assertFalse(testEntityState.isProne());
        TestCase.assertTrue(testEntityState.isImmobile());
        TestCase.assertFalse(testEntityState.isJumping());
        TestCase.assertEquals(EntityMovementType.MOVE_NONE, testEntityState.getMovementType());
    }

    @Test
    public void testInitFromMovePath() {
        // Test a running move path.
        MovePath mockMovePath = Mockito.mock(MovePath.class);
        Coords expectedCoords = new Coords(10, 10);
        Mockito.when(mockMovePath.getFinalCoords()).thenReturn(expectedCoords);
        int expectedFacing = 3;
        Mockito.when(mockMovePath.getFinalFacing()).thenReturn(expectedFacing);
        int expectedDistance = 5;
        Mockito.when(mockMovePath.getHexesMoved()).thenReturn(expectedDistance);
        Entity mockEntity = Mockito.mock(Entity.class);
        boolean expectedImmobile = false;
        Mockito.when(mockEntity.isImmobile()).thenReturn(expectedImmobile);
        int baseHeat = 8;
        int expectedHeat = 10;
        Mockito.when(mockEntity.getHeat()).thenReturn(baseHeat);
        Mockito.when(mockMovePath.getEntity()).thenReturn(mockEntity);
        EntityMovementType expectedMovementType = EntityMovementType.MOVE_RUN;
        Mockito.when(mockMovePath.getLastStepMovementType()).thenReturn(expectedMovementType);
        boolean expectedProne = false;
        Mockito.when(mockMovePath.getFinalProne()).thenReturn(expectedProne);
        Mockito.when(mockMovePath.getFinalHullDown()).thenReturn(expectedProne);
        boolean expectedJump = false;
        Mockito.when(mockMovePath.isJumping()).thenReturn(expectedJump);
        EntityState testEntityState = new EntityState();
        testEntityState.init(mockMovePath);
        TestCase.assertEquals(expectedCoords, testEntityState.getPosition());
        TestCase.assertEquals(expectedFacing, testEntityState.getFacing());
        TestCase.assertEquals(expectedDistance, testEntityState.getHexesMoved());
        TestCase.assertEquals(expectedHeat, testEntityState.getHeat());
        TestCase.assertEquals(expectedFacing, testEntityState.getSecondaryFacing());
        TestCase.assertFalse(testEntityState.isProne());
        TestCase.assertFalse(testEntityState.isImmobile());
        TestCase.assertFalse(testEntityState.isJumping());
        TestCase.assertEquals(expectedMovementType, testEntityState.getMovementType());

        // Test a walking move path.
        expectedHeat = 9;
        expectedMovementType = EntityMovementType.MOVE_WALK;
        Mockito.when(mockMovePath.getLastStepMovementType()).thenReturn(expectedMovementType);
        testEntityState.init(mockMovePath);
        TestCase.assertEquals(expectedCoords, testEntityState.getPosition());
        TestCase.assertEquals(expectedFacing, testEntityState.getFacing());
        TestCase.assertEquals(expectedDistance, testEntityState.getHexesMoved());
        TestCase.assertEquals(expectedHeat, testEntityState.getHeat());
        TestCase.assertEquals(expectedFacing, testEntityState.getSecondaryFacing());
        TestCase.assertFalse(testEntityState.isProne());
        TestCase.assertFalse(testEntityState.isImmobile());
        TestCase.assertFalse(testEntityState.isJumping());
        TestCase.assertEquals(expectedMovementType, testEntityState.getMovementType());

        // Test a jumping move path.
        expectedHeat = 13;
        expectedMovementType = EntityMovementType.MOVE_JUMP;
        Mockito.when(mockMovePath.getLastStepMovementType()).thenReturn(expectedMovementType);
        Mockito.when(mockMovePath.isJumping()).thenReturn(true);
        testEntityState.init(mockMovePath);
        TestCase.assertEquals(expectedCoords, testEntityState.getPosition());
        TestCase.assertEquals(expectedFacing, testEntityState.getFacing());
        TestCase.assertEquals(expectedDistance, testEntityState.getHexesMoved());
        TestCase.assertEquals(expectedHeat, testEntityState.getHeat());
        TestCase.assertEquals(expectedFacing, testEntityState.getSecondaryFacing());
        TestCase.assertFalse(testEntityState.isProne());
        TestCase.assertFalse(testEntityState.isImmobile());
        TestCase.assertTrue(testEntityState.isJumping());
        TestCase.assertEquals(expectedMovementType, testEntityState.getMovementType());

        // Test a short (2-hex) jumping move path.
        expectedHeat = 11;
        expectedMovementType = EntityMovementType.MOVE_JUMP;
        Mockito.when(mockMovePath.getLastStepMovementType()).thenReturn(expectedMovementType);
        Mockito.when(mockMovePath.isJumping()).thenReturn(true);
        expectedDistance = 2;
        Mockito.when(mockMovePath.getHexesMoved()).thenReturn(expectedDistance);
        testEntityState.init(mockMovePath);
        TestCase.assertEquals(expectedCoords, testEntityState.getPosition());
        TestCase.assertEquals(expectedFacing, testEntityState.getFacing());
        TestCase.assertEquals(expectedDistance, testEntityState.getHexesMoved());
        TestCase.assertEquals(expectedHeat, testEntityState.getHeat());
        TestCase.assertEquals(expectedFacing, testEntityState.getSecondaryFacing());
        TestCase.assertFalse(testEntityState.isProne());
        TestCase.assertFalse(testEntityState.isImmobile());
        TestCase.assertTrue(testEntityState.isJumping());
        TestCase.assertEquals(expectedMovementType, testEntityState.getMovementType());

        // Test a stationary prone move path.
        expectedHeat = 8;
        expectedMovementType = EntityMovementType.MOVE_NONE;
        Mockito.when(mockMovePath.getLastStepMovementType()).thenReturn(expectedMovementType);
        Mockito.when(mockMovePath.isJumping()).thenReturn(false);
        expectedDistance = 0;
        Mockito.when(mockMovePath.getHexesMoved()).thenReturn(expectedDistance);
        expectedProne = true;
        Mockito.when(mockMovePath.getFinalProne()).thenReturn(expectedProne);
        testEntityState.init(mockMovePath);
        TestCase.assertEquals(expectedCoords, testEntityState.getPosition());
        TestCase.assertEquals(expectedFacing, testEntityState.getFacing());
        TestCase.assertEquals(expectedDistance, testEntityState.getHexesMoved());
        TestCase.assertEquals(expectedHeat, testEntityState.getHeat());
        TestCase.assertEquals(expectedFacing, testEntityState.getSecondaryFacing());
        TestCase.assertTrue(testEntityState.isProne());
        TestCase.assertFalse(testEntityState.isImmobile());
        TestCase.assertFalse(testEntityState.isJumping());
        TestCase.assertEquals(expectedMovementType, testEntityState.getMovementType());
    }
}
