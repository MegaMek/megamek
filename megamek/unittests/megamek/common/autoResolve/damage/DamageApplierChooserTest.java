/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.autoResolve.damage;

import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import megamek.common.CriticalSlot;
import megamek.common.HitData;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class DamageApplierChooserTest {
    private enum MekType {
        ENF6M("Enforcer III ENF-6M"),
        SHD5D("Shadow Hawk SHD-5D"),
        AS7KDC("Atlas AS7-K-DC"),
        OSR5D("Osiris OSR-5D");
        private final String mekFullName;

        MekType(String mekFullName) {
            this.mekFullName = mekFullName;
        }

        public String getMekFullName() {
            return mekFullName;
        }
    }

    private Map<MekType, Entity> entities;

    @BeforeAll
    public static void setupClass() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    public void setup() throws IOException {
        entities = new EnumMap<>(MekType.class);
        for (var mek : MekType.values()) {
            Entity entity = getEntityForUnitTesting(mek.getMekFullName(), false);
            assertNotNull(entity, mek.getMekFullName() + " not found");
            entity.setCrew(new Crew(CrewType.SINGLE));
            entity.calculateBattleValue();
            entities.put(mek, entity);
        }
    }

    @Test
    void testDestroyLeg() {
        var entity = entities.get(MekType.OSR5D);
        var damageApplier = DamageApplierChooser.choose(entities.get(MekType.OSR5D));
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_LEFT_LEG, false, HitData.EFFECT_NONE), 1000);
        damageApplier.applyDamage(hitDetails);
        assertEquals(IArmorState.ARMOR_DESTROYED, entity.getInternal(Mek.LOC_LEFT_LEG));
    }

    @Test
    void testDestroyRightTorsoWithoutCase() {
        var atlas = entities.get(MekType.AS7KDC);
        var damageApplier = DamageApplierChooser.choose(atlas);
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_RIGHT_TORSO, false, HitData.EFFECT_NONE),
              1000);
        damageApplier.applyDamage(hitDetails);
        assertEquals(IArmorState.ARMOR_DESTROYED, atlas.getInternal(Mek.LOC_RIGHT_TORSO));
        assertTrue(atlas.isLocationBlownOff(Mek.LOC_RIGHT_ARM));
    }

    @Test
    void testDestroyCenterTorso() {
        var shadowHawk = entities.get(MekType.SHD5D);
        var damageApplier = DamageApplierChooser.choose(shadowHawk);
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_CENTER_TORSO, false, HitData.EFFECT_NONE),
              1000);
        damageApplier.applyDamage(hitDetails);
        assertEquals(IArmorState.ARMOR_DESTROYED, shadowHawk.getInternal(Mek.LOC_CENTER_TORSO));
        assertCriticalSlotsDestroyed(shadowHawk, Mek.LOC_CENTER_TORSO);
        assertEquals(IEntityRemovalConditions.REMOVE_DEVASTATED, shadowHawk.getRemovalCondition());
    }

    @Test
    void testDestroySideTorsoWithCase() {
        var atlas = entities.get(MekType.AS7KDC);
        var damageApplier = DamageApplierChooser.choose(atlas);
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_RIGHT_TORSO, false, HitData.EFFECT_NONE),
              1000);
        damageApplier.applyDamage(hitDetails);
        assertEquals(IArmorState.ARMOR_DESTROYED, atlas.getInternal(Mek.LOC_RIGHT_TORSO));
        assertCriticalSlotsDestroyed(atlas, Mek.LOC_RIGHT_TORSO);
        assertEquals(IEntityRemovalConditions.REMOVE_UNKNOWN, atlas.getRemovalCondition());
        assertTrue(atlas.isLocationBlownOff(Mek.LOC_RIGHT_ARM));
    }

    @Test
    void testDestroyHead() {
        var enforcer = entities.get(MekType.ENF6M);
        var damageApplier = DamageApplierChooser.choose(enforcer);
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_HEAD, false, HitData.EFFECT_NONE), 1000);
        damageApplier.applyDamage(hitDetails);
        assertEquals(IArmorState.ARMOR_DESTROYED, enforcer.getInternal(Mek.LOC_HEAD));
        assertCriticalSlotsDestroyed(enforcer, Mek.LOC_HEAD);
        assertEquals(IEntityRemovalConditions.REMOVE_EJECTED, enforcer.getRemovalCondition());
    }

    @Test
    void testDestroyBackArmor() {
        var enforcer = entities.get(MekType.ENF6M);
        var damageApplier = DamageApplierChooser.choose(enforcer);
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_CENTER_TORSO, true, HitData.EFFECT_NONE),
              23);
        damageApplier.applyDamage(hitDetails);
        assertEquals(IArmorState.ARMOR_DESTROYED, enforcer.getArmor(Mek.LOC_CENTER_TORSO, true));
        assertEquals(IArmorState.ARMOR_DESTROYED, enforcer.getInternal(Mek.LOC_CENTER_TORSO));
        assertCriticalSlotsDestroyed(enforcer, Mek.LOC_CENTER_TORSO);
        assertEquals(IEntityRemovalConditions.REMOVE_DEVASTATED, enforcer.getRemovalCondition());
    }

    @Test
    void testBlowOffArmorAndInternalsNoDestruction() {
        var enforcer = entities.get(MekType.ENF6M);
        var damageApplier = DamageApplierChooser.choose(enforcer);
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_CENTER_TORSO, true, HitData.EFFECT_NONE),
              21);
        damageApplier.applyDamage(hitDetails);
        assertEquals(0, enforcer.getArmor(Mek.LOC_CENTER_TORSO, true));
        assertEquals(2, enforcer.getInternal(Mek.LOC_CENTER_TORSO));
        assertFalse(enforcer.isDestroyed());
        assertEquals(IEntityRemovalConditions.REMOVE_UNKNOWN, enforcer.getRemovalCondition());
    }

    @Test
    void testDestroyBothLegs() {
        var osiris = entities.get(MekType.OSR5D);
        var damageApplier = DamageApplierChooser.choose(osiris);

        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_LEFT_LEG, false, HitData.EFFECT_NONE), 1000);
        damageApplier.applyDamage(hitDetails);

        assertEquals(IArmorState.ARMOR_DESTROYED, osiris.getInternal(Mek.LOC_LEFT_LEG));
        assertCriticalSlotsDestroyed(osiris, Mek.LOC_LEFT_LEG);
    }

    @Test
    void testDamageOverflow() {
        var osiris = entities.get(MekType.OSR5D);
        var damageApplier = DamageApplierChooser.choose(osiris);

        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_LEFT_LEG, false, HitData.EFFECT_NONE), 1000);
        // damage will destroy leg, then left torso then center torso
        while (!osiris.isDestroyed()) {
            hitDetails = damageApplier.applyDamage(hitDetails);
        }

        assertEquals(IArmorState.ARMOR_DESTROYED, osiris.getInternal(Mek.LOC_LEFT_LEG));
        assertEquals(IArmorState.ARMOR_DESTROYED, osiris.getInternal(Mek.LOC_LEFT_TORSO));
        assertEquals(IArmorState.ARMOR_DESTROYED, osiris.getInternal(Mek.LOC_CENTER_TORSO));
        assertCriticalSlotsDestroyed(osiris, Mek.LOC_LEFT_LEG);
        assertCriticalSlotsDestroyed(osiris, Mek.LOC_LEFT_TORSO);
        assertCriticalSlotsDestroyed(osiris, Mek.LOC_CENTER_TORSO);
    }

    private static void assertCriticalSlotsDestroyed(Entity osiris, int loc) {
        assertAll(osiris.getCriticalSlots(loc).stream()
              .filter(Objects::nonNull)
              .filter(CriticalSlot::isEverHittable)
              .map(criticalSlot -> (Executable) (() -> assertTrue(criticalSlot.isDestroyed(),
                    "Equipment " + criticalSlot + " should be destroyed"))
              ).toList());
    }
}
