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
package megamek.common.autoresolve.damage;

import megamek.common.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

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
            var entity = MekSummary.loadEntity(mek.getMekFullName());
            assert entity != null;
            entity.setCrew(new Crew(CrewType.SINGLE));
            entity.calculateBattleValue();
            entities.put(mek, entity);
        }
    }

    @Test
    void testDestroyLeg() {
        var entity = entities.get(MekType.OSR5D);
        var damageApplier = DamageApplierChooser.choose(entities.get(MekType.OSR5D));
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_LLEG, false, HitData.EFFECT_NONE), 1000);
        damageApplier.applyDamage(hitDetails);
        assertEquals(IArmorState.ARMOR_DESTROYED, entity.getInternal(Mek.LOC_LLEG));
    }

    @Test
    void testDestroyRightTorsoWithoutCase() {
        var atlas = entities.get(MekType.AS7KDC);
        var damageApplier = DamageApplierChooser.choose(atlas);
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_RT, false, HitData.EFFECT_NONE), 1000);
        damageApplier.applyDamage(hitDetails);
        assertEquals(IArmorState.ARMOR_DESTROYED, atlas.getInternal(Mek.LOC_RT));
        assertTrue(atlas.isLocationBlownOff(Mek.LOC_RARM));
    }

    @Test
    void testDestroyCenterTorso() {
        var shadowHawk = entities.get(MekType.SHD5D);
        var damageApplier = DamageApplierChooser.choose(shadowHawk);
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_CT, false, HitData.EFFECT_NONE), 1000);
        damageApplier.applyDamage(hitDetails);
        assertEquals(IArmorState.ARMOR_DESTROYED, shadowHawk.getInternal(Mek.LOC_CT));
        assertCriticalSlotsDestroyed(shadowHawk, Mek.LOC_CT);
        assertEquals(IEntityRemovalConditions.REMOVE_DEVASTATED, shadowHawk.getRemovalCondition());
    }

    @Test
    void testDestroySideTorsoWithCase() {
        var atlas = entities.get(MekType.AS7KDC);
        var damageApplier = DamageApplierChooser.choose(atlas);
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_RT, false, HitData.EFFECT_NONE), 1000);
        damageApplier.applyDamage(hitDetails);
        assertEquals(IArmorState.ARMOR_DESTROYED, atlas.getInternal(Mek.LOC_RT));
        assertCriticalSlotsDestroyed(atlas, Mek.LOC_RT);
        assertEquals(IEntityRemovalConditions.REMOVE_UNKNOWN, atlas.getRemovalCondition());
        assertTrue(atlas.isLocationBlownOff(Mek.LOC_RARM));
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
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_CT, true, HitData.EFFECT_NONE), 23);
        damageApplier.applyDamage(hitDetails);
        assertEquals(IArmorState.ARMOR_DESTROYED, enforcer.getArmor(Mek.LOC_CT, true));
        assertEquals(IArmorState.ARMOR_DESTROYED, enforcer.getInternal(Mek.LOC_CT));
        assertCriticalSlotsDestroyed(enforcer, Mek.LOC_CT);
        assertEquals(IEntityRemovalConditions.REMOVE_DEVASTATED, enforcer.getRemovalCondition());
    }

    @Test
    void testBlowOffArmorAndInternalsNoDestruction() {
        var enforcer = entities.get(MekType.ENF6M);
        var damageApplier = DamageApplierChooser.choose(enforcer);
        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_CT, true, HitData.EFFECT_NONE), 21);
        damageApplier.applyDamage(hitDetails);
        assertEquals(0, enforcer.getArmor(Mek.LOC_CT, true));
        assertEquals(2, enforcer.getInternal(Mek.LOC_CT));
        assertFalse(enforcer.isDestroyed());
        assertEquals(IEntityRemovalConditions.REMOVE_UNKNOWN, enforcer.getRemovalCondition());
    }

    @Test
    void testDestroyBothLegs() {
        var osiris = entities.get(MekType.OSR5D);
        var damageApplier = DamageApplierChooser.choose(osiris);

        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_LLEG, false, HitData.EFFECT_NONE), 1000);
        damageApplier.applyDamage(hitDetails);

        assertEquals(IArmorState.ARMOR_DESTROYED, osiris.getInternal(Mek.LOC_LLEG));
        assertCriticalSlotsDestroyed(osiris, Mek.LOC_LLEG);
    }

    @Test
    void testDamageOverflow() {
        var osiris = entities.get(MekType.OSR5D);
        var damageApplier = DamageApplierChooser.choose(osiris);

        var hitDetails = damageApplier.setupHitDetails(new HitData(Mek.LOC_LLEG, false, HitData.EFFECT_NONE), 1000);
        // damage will destroy leg, then left torso then center torso
        while (!osiris.isDestroyed()) {
            hitDetails = damageApplier.applyDamage(hitDetails);
        }

        assertEquals(IArmorState.ARMOR_DESTROYED, osiris.getInternal(Mek.LOC_LLEG));
        assertEquals(IArmorState.ARMOR_DESTROYED, osiris.getInternal(Mek.LOC_LT));
        assertEquals(IArmorState.ARMOR_DESTROYED, osiris.getInternal(Mek.LOC_CT));
        assertCriticalSlotsDestroyed(osiris, Mek.LOC_LLEG);
        assertCriticalSlotsDestroyed(osiris, Mek.LOC_LT);
        assertCriticalSlotsDestroyed(osiris, Mek.LOC_CT);
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
