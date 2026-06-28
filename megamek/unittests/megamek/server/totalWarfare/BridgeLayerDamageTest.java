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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.equipment.BridgeLayerState;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.server.totalWarfare.TWDamageManager.ModsInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link TWDamageManager#applyBridgeLayerAbsorption} - the carried Bridge-Layer (AVLB) damage-absorption math (TM
 * p.242 / TW): CF reduction, fall-through once the bridge is destroyed, the crit-disables-mechanism rule, and the cases
 * the bridge does not protect against. The location-resolution itself (which bridge for which hit) is covered by
 * {@code BridgeLayerLogicTest}; here the unit simply carries a single bridge in the hit location so the absorption
 * logic is exercised.
 *
 * @author Claude Code (Opus 4.8)
 */
class BridgeLayerDamageTest {

    private final TWDamageManager damageManager = new TWDamageManager();

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    /** A carried bridge with the given current CF. */
    private static BridgeLayerState carriedBridge(int currentCF) {
        BridgeLayerState state = new BridgeLayerState(MiscType.createHeavyBridgeLayer());
        state.setCurrentCF(currentCF);
        return state;
    }

    /** A unit carrying a single bridge in {@link Tank#LOC_RIGHT}, backed by the given state. */
    private static Entity unitWithBridge(BridgeLayerState state) {
        MiscMounted mount = mock(MiscMounted.class);
        when(mount.getBridgeLayerState()).thenReturn(state);
        when(mount.getLocation()).thenReturn(Tank.LOC_RIGHT);
        when(mount.isMissing()).thenReturn(false);
        Entity entity = mock(Entity.class);
        when(entity.getMisc()).thenReturn(List.of(mount));
        when(entity.getShortName()).thenReturn("Test Bridgelayer");
        // addDesc() on the absorption report reads several crew fields, so use a real (default) crew.
        when(entity.getCrew()).thenReturn(new Crew(CrewType.SINGLE));
        return entity;
    }

    private static boolean hasReport(Vector<Report> reports, int messageId) {
        return reports.stream().anyMatch(report -> report.messageId == messageId);
    }

    @Test
    @DisplayName("a hit smaller than CF is fully absorbed; CF drops by the damage and no damage falls through")
    void partialAbsorptionReducesCF() {
        BridgeLayerState state = carriedBridge(45);
        Entity entity = unitWithBridge(state);
        Vector<Report> reports = new Vector<>();

        int remaining = damageManager.applyBridgeLayerAbsorption(entity, new HitData(Tank.LOC_RIGHT), 10, false,
              new ModsInfo(), reports);

        assertEquals(0, remaining, "all 10 damage absorbed");
        assertEquals(35, state.getCurrentCF(), "CF reduced by the damage (45 -> 35)");
        assertTrue(hasReport(reports, 4296), "an absorption report (4296) is added");
        assertFalse(state.isDeployed());
    }

    @Test
    @DisplayName("a hit larger than the remaining CF destroys the bridge and the leftover damage falls through")
    void overkillDestroysBridgeAndFallsThrough() {
        BridgeLayerState state = carriedBridge(5);
        Entity entity = unitWithBridge(state);
        Vector<Report> reports = new Vector<>();

        int remaining = damageManager.applyBridgeLayerAbsorption(entity, new HitData(Tank.LOC_RIGHT), 10, false,
              new ModsInfo(), reports);

        assertEquals(5, remaining, "only the 5 CF is absorbed; the other 5 falls through to the location");
        assertEquals(0, state.getCurrentCF(), "the bridge is exhausted");
        assertTrue(hasReport(reports, 4297), "a destroyed report (4297) is added");
    }

    @Test
    @DisplayName("a critical hit while the bridge is carried disables the deploy mechanism and is suppressed")
    void criticalDisablesMechanismAndIsSuppressed() {
        BridgeLayerState state = carriedBridge(45);
        Entity entity = unitWithBridge(state);
        Vector<Report> reports = new Vector<>();
        ModsInfo mods = new ModsInfo();
        mods.crits = 1;
        mods.specCrits = 1;
        HitData critHit = new HitData(Tank.LOC_RIGHT, false, HitData.EFFECT_CRITICAL);

        damageManager.applyBridgeLayerAbsorption(entity, critHit, 10, false, mods, reports);

        assertTrue(state.isDeployMechanismDisabled(), "the crit disables the deploy mechanism");
        assertEquals(0, mods.crits, "the location crit is suppressed");
        assertEquals(0, mods.specCrits, "the location special crit is suppressed");
        assertTrue(hasReport(reports, 4298), "a mechanism-disabled report (4298) is added");
        assertEquals(35, state.getCurrentCF(), "the crit hit still reduces CF normally");
    }

    @Test
    @DisplayName("a hit to a location with no bridge is not absorbed")
    void noBridgeForHitLeavesDamageUnchanged() {
        Entity entity = mock(Entity.class);
        when(entity.getShortName()).thenReturn("Test");
        when(entity.getMisc()).thenReturn(List.of());
        Vector<Report> reports = new Vector<>();

        int remaining = damageManager.applyBridgeLayerAbsorption(entity, new HitData(Tank.LOC_FRONT), 10, false,
              new ModsInfo(), reports);

        assertEquals(10, remaining, "all damage passes through when no bridge covers the hit");
        assertFalse(hasReport(reports, 4296));
    }

    @Test
    @DisplayName("an ammo explosion is not absorbed by the carried bridge")
    void ammoExplosionNotAbsorbed() {
        BridgeLayerState state = carriedBridge(45);
        Entity entity = unitWithBridge(state);
        Vector<Report> reports = new Vector<>();

        int remaining = damageManager.applyBridgeLayerAbsorption(entity, new HitData(Tank.LOC_RIGHT), 10, true,
              new ModsInfo(), reports);

        assertEquals(10, remaining, "ammo-explosion damage is not absorbed");
        assertEquals(45, state.getCurrentCF(), "CF is untouched by an ammo explosion");
    }

    @Test
    @DisplayName("damage applied directly to internal structure is not absorbed")
    void internalStructureDamageNotAbsorbed() {
        BridgeLayerState state = carriedBridge(45);
        Entity entity = unitWithBridge(state);
        Vector<Report> reports = new Vector<>();
        ModsInfo mods = new ModsInfo();
        mods.damageIS = true;

        int remaining = damageManager.applyBridgeLayerAbsorption(entity, new HitData(Tank.LOC_RIGHT), 10, false,
              mods, reports);

        assertEquals(10, remaining, "internal-structure damage is not absorbed");
        assertEquals(45, state.getCurrentCF(), "CF is untouched by internal damage");
    }
}
