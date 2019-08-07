/*
* ZellbrigenAdjudicatorTests.java
*
* Copyright (C) 2019 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class ZellbrigenAdjudicatorTests {

    private Entity getTestEntity(int id, long entityType) {
        Entity mockEntity;
        if ((entityType & Entity.ETYPE_MECH) == entityType) {
            mockEntity = Mockito.mock(Mech.class);
        } else if ((entityType & Entity.ETYPE_INFANTRY) == entityType) {
            mockEntity = Mockito.mock(Infantry.class);
        } else if ((entityType & Entity.ETYPE_TANK) == entityType) {
            mockEntity = Mockito.mock(Tank.class);
        } else if ((entityType & Entity.ETYPE_AERO) == entityType) {
            mockEntity = Mockito.mock(Aero.class);
        } else if ((entityType & Entity.ETYPE_PROTOMECH) == entityType) {
            mockEntity = Mockito.mock(Protomech.class);
        } else {
            Assert.fail("Cannot mock entity of type " + entityType);
            return null;
        }
        Mockito.when(mockEntity.getId()).thenReturn(id);
        Mockito.when(mockEntity.hasETypeFlag(Mockito.eq(entityType))).thenReturn(true);
        return mockEntity;
    }

    @Test
    public void IfYouMakeADishonorableShotYouAreDezgra() {
        ZellbrigenAdjudicator zellbrigen = new ZellbrigenAdjudicator();

        Entity entity1 = getTestEntity(1, Entity.ETYPE_MECH);
        Entity entity2 = getTestEntity(2, Entity.ETYPE_MECH);
        Entity entity3 = getTestEntity(3, Entity.ETYPE_MECH);
        Entity entity4 = getTestEntity(4, Entity.ETYPE_MECH);

        // Entity 1 is firing at Entity 2
        zellbrigen.trackDeclaredTarget(entity1, entity2);

        // Entity 2 should be able to fire at Entity 1
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity2, entity1));
        
        // But Entity 3 should not be able to fire at Entity 1 or Entity 2
        Assert.assertFalse(zellbrigen.isHonorableTarget(entity3, entity1));
        Assert.assertFalse(zellbrigen.isHonorableTarget(entity3, entity2));

        // Entity 3 decides to fire at Entity 2 anyways...
        zellbrigen.trackDeclaredTarget(entity3, entity2);

        // ...so Zellbrigen is now broken...
        Assert.assertTrue(zellbrigen.isZellbrigenBroken());

        // ...and they are dezgra
        Assert.assertEquals(1, zellbrigen.getDishonorableUnitIds().size());
        Assert.assertTrue(zellbrigen.getDishonorableUnitIds().contains(entity3.getId()));

        // Now Entity 1, Entity 2, and Entity 4 should all be able to shoot
        // at Entity 3
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity1, entity3));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity2, entity3));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity4, entity3));
    }

    @Test
    public void YouCanShootAnybodyIfZellbrigenIsBroken() {
        ZellbrigenAdjudicator zellbrigen = new ZellbrigenAdjudicator();
        zellbrigen.setIsZellbrigenBroken(true);

        Entity entity1 = getTestEntity(1, Entity.ETYPE_MECH);
        Entity entity2 = getTestEntity(2, Entity.ETYPE_MECH);
        Entity entity3 = getTestEntity(3, Entity.ETYPE_MECH);
        Entity entity4 = getTestEntity(4, Entity.ETYPE_MECH);

        // Entity 1 is firing at Entity 2
        zellbrigen.trackDeclaredTarget(entity1, entity2);

        // Entity 3 is firing at Entity 4
        zellbrigen.trackDeclaredTarget(entity3, entity4);

        // But because we've broken Zellbrigen, everybody can shoot at everybody!
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity1, entity2));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity1, entity3));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity1, entity4));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity2, entity1));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity2, entity3));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity2, entity4));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity3, entity1));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity3, entity2));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity3, entity4));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity4, entity1));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity4, entity2));
        Assert.assertTrue(zellbrigen.isHonorableTarget(entity4, entity3));
    }

    @Test
    public void YouCanAlwaysTargetInfantryAndArmor() {
        ZellbrigenAdjudicator zellbrigen = new ZellbrigenAdjudicator();

        Entity mech1 = getTestEntity(1, Entity.ETYPE_MECH);
        Entity infantry = getTestEntity(2, Entity.ETYPE_INFANTRY);
        Entity tank = getTestEntity(3, Entity.ETYPE_TANK);
        Entity mech2 = getTestEntity(4, Entity.ETYPE_MECH);

        // The mechs can shoot at infantry and tanks (and each other)
        Assert.assertTrue(zellbrigen.isHonorableTarget(mech1, infantry));
        Assert.assertTrue(zellbrigen.isHonorableTarget(mech1, tank));
        Assert.assertTrue(zellbrigen.isHonorableTarget(mech1, mech2));
        Assert.assertTrue(zellbrigen.isHonorableTarget(mech2, mech1));
        Assert.assertTrue(zellbrigen.isHonorableTarget(mech2, infantry));
        Assert.assertTrue(zellbrigen.isHonorableTarget(mech2, tank));

        // The mech can declare shooting infantry...
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(mech1, infantry));
        
        // ...and tanks
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(mech1, tank));

        // ...and that other mech
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(mech1, mech2));

        // ...and that other mech can shoot back
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(mech2, mech1));

        // ...and the other mech cannot shoot at them.
        Entity mech3 = getTestEntity(5, Entity.ETYPE_MECH);
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(mech3, mech1));
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(mech3, mech2));

        // ...but the other mechs can also declare shooting at the infantry and tanks
        Assert.assertTrue(zellbrigen.isHonorableTarget(mech2, infantry));
        Assert.assertTrue(zellbrigen.isHonorableTarget(mech2, tank));
        Assert.assertTrue(zellbrigen.isHonorableTarget(mech3, tank));
        Assert.assertTrue(zellbrigen.isHonorableTarget(mech3, tank));
    }

    @Test
    public void InfantryAndArmorCanTargetAnything() {
        ZellbrigenAdjudicator zellbrigen = new ZellbrigenAdjudicator();

        Entity mech1 = getTestEntity(1, Entity.ETYPE_MECH);
        Entity infantry = getTestEntity(2, Entity.ETYPE_INFANTRY);
        Entity tank = getTestEntity(3, Entity.ETYPE_TANK);
        Entity mech2 = getTestEntity(4, Entity.ETYPE_MECH);

        // The mechs are shooting at each other...
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(mech1, mech2));
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(mech2, mech1));

        // ...but the infantry and armor can target whomever they please!
        Assert.assertTrue(zellbrigen.isHonorableTarget(infantry, mech1));
        Assert.assertTrue(zellbrigen.isHonorableTarget(infantry, tank));
        Assert.assertTrue(zellbrigen.isHonorableTarget(infantry, mech2));
        Assert.assertTrue(zellbrigen.isHonorableTarget(tank, mech1));
        Assert.assertTrue(zellbrigen.isHonorableTarget(tank, infantry));
        Assert.assertTrue(zellbrigen.isHonorableTarget(tank, mech2));

        // And after they declare the target, they can keep declaring others!
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(infantry, mech1));
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(infantry, tank));
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(infantry, mech2));
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(tank, mech1));
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(tank, infantry));
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(tank, mech2));
    }

    @Test
    public void YouCanDeclareMultipleTargets() {
        ZellbrigenAdjudicator zellbrigen = new ZellbrigenAdjudicator();

        Entity entity1 = getTestEntity(1, Entity.ETYPE_MECH);
        Entity entity2 = getTestEntity(2, Entity.ETYPE_MECH);
        Entity entity3 = getTestEntity(3, Entity.ETYPE_MECH);

        // Mech1 can target Mech2 and Mech3 (because they're strong like Ox.)
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(entity1, entity2));
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(entity1, entity3));

        // Mech2 and Mech3 can both target Mech1 (because Mech1 is foolish.)
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(entity2, entity1));
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(entity3, entity1));

        // ...but not each other
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity2, entity3));
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity3, entity2));

        Entity entity4 = getTestEntity(4, Entity.ETYPE_MECH);
        
        // Mech 4 cannot target any of them without becoming dezgra.
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity4, entity1));
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity4, entity2));
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity4, entity3));

        // And Mechs 2, and 3 cannot target 4 without this becoming a giant free-for-all
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity2, entity4));
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity3, entity4));

        // ...but if Mech 1 had a hankering for more action, they could target another mech...
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(entity1, entity4));

        // ...at which point Mech 4 can certainly target Mech 1
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(entity4, entity1));

        // ...but Mechs 2, 3, and 4 still could not target one another
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity2, entity3));
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity2, entity4));
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity3, entity2));
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity3, entity4));
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity4, entity2));
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity4, entity3));

        // Two new mechs enter the party...
        Entity entity5 = getTestEntity(5, Entity.ETYPE_MECH);
        Entity entity6 = getTestEntity(6, Entity.ETYPE_MECH);

        // ...and one of them declares a target
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(entity5, entity6));

        // ...then mech 1 should NOT be able to enjoin either of these mechs...
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity1, entity5));
        Assert.assertFalse(zellbrigen.tryDeclaringHonorableTarget(entity1, entity6));

        // ...but mech 6 can definitely party with mech 5
        Assert.assertTrue(zellbrigen.tryDeclaringHonorableTarget(entity6, entity5));
    }

    // ...this Zellbrigen stuff SEEMS simple, but it is not.
}
