/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.BLKTankFile;
import megamek.common.loaders.MtfFile;
import megamek.common.TechConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.util.BuildingBlock;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for cockpit modification equipment (DNI Cockpit Mod, EI Interface, Damage Interrupt Circuit)
 * to verify they are correctly saved and loaded from unit files.
 */
class CockpitModificationTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    // Helper method to convert a Mek to MTF format and reload it
    private Mek saveAndReloadMek(Mek mek) throws Exception {
        if (!mek.hasEngine() || mek.getEngine().getEngineType() == Engine.NONE) {
            mek.setWeight(20.0);
            mek.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));
        }
        String mtf = mek.getMtf();
        byte[] bytes = mtf.getBytes();
        InputStream inputStream = new ByteArrayInputStream(bytes);
        MtfFile loader = new MtfFile(inputStream);
        return (Mek) loader.getEntity();
    }

    // ========== DNI Cockpit Modification Tests ==========

    @Test
    void testMekDNICockpitModSaveAndLoad() throws Exception {
        // Create a Mek with DNI Cockpit Modification
        Mek mek = new BipedMek();
        MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
        mek.addEquipment(dniMod, Entity.LOC_NONE);

        // Verify it has the mod before save
        assertTrue(mek.hasDNICockpitMod(), "Mek should have DNI Cockpit Mod before save");

        // Save and reload
        Mek loadedMek = saveAndReloadMek(mek);

        // Verify it still has the mod after load
        assertTrue(loadedMek.hasDNICockpitMod(), "Mek should have DNI Cockpit Mod after load");
    }

    @Test
    void testMekWithoutDNICockpitMod() throws Exception {
        // Create a Mek without DNI Cockpit Modification
        Mek mek = new BipedMek();

        // Verify it doesn't have the mod
        assertFalse(mek.hasDNICockpitMod(), "Mek should not have DNI Cockpit Mod");

        // Save and reload
        Mek loadedMek = saveAndReloadMek(mek);

        // Verify it still doesn't have the mod after load
        assertFalse(loadedMek.hasDNICockpitMod(), "Mek should not have DNI Cockpit Mod after load");
    }

    // ========== EI Interface Tests ==========

    @Test
    void testMekEIInterfaceSaveAndLoad() throws Exception {
        // Create a Mek with EI Interface
        Mek mek = new BipedMek();
        MiscType eiInterface = (MiscType) EquipmentType.get("EIInterface");
        mek.addEquipment(eiInterface, Entity.LOC_NONE);

        // Verify it has the interface before save
        assertTrue(mek.hasEiCockpit(), "Mek should have EI Interface before save");

        // Save and reload
        Mek loadedMek = saveAndReloadMek(mek);

        // Verify it still has the interface after load
        assertTrue(loadedMek.hasEiCockpit(), "Mek should have EI Interface after load");
    }

    @Test
    void testMekWithoutEIInterface() throws Exception {
        // Create a Mek without EI Interface
        Mek mek = new BipedMek();

        // Verify it doesn't have the interface
        assertFalse(mek.hasEiCockpit(), "Mek should not have EI Interface");

        // Save and reload
        Mek loadedMek = saveAndReloadMek(mek);

        // Verify it still doesn't have the interface after load
        assertFalse(loadedMek.hasEiCockpit(), "Mek should not have EI Interface after load");
    }

    // ========== Damage Interrupt Circuit Tests ==========

    @Test
    void testMekDamageInterruptCircuitSaveAndLoad() throws Exception {
        // Create a Mek with Damage Interrupt Circuit
        Mek mek = new BipedMek();
        MiscType dic = (MiscType) EquipmentType.get("DamageInterruptCircuit");
        mek.addEquipment(dic, Entity.LOC_NONE);

        // Verify it has the circuit before save
        assertTrue(mek.hasDamageInterruptCircuit(), "Mek should have Damage Interrupt Circuit before save");

        // Save and reload
        Mek loadedMek = saveAndReloadMek(mek);

        // Verify it still has the circuit after load
        assertTrue(loadedMek.hasDamageInterruptCircuit(), "Mek should have Damage Interrupt Circuit after load");
    }

    @Test
    void testMekWithoutDamageInterruptCircuit() throws Exception {
        // Create a Mek without Damage Interrupt Circuit
        Mek mek = new BipedMek();

        // Verify it doesn't have the circuit
        assertFalse(mek.hasDamageInterruptCircuit(), "Mek should not have Damage Interrupt Circuit");

        // Save and reload
        Mek loadedMek = saveAndReloadMek(mek);

        // Verify it still doesn't have the circuit after load
        assertFalse(loadedMek.hasDamageInterruptCircuit(), "Mek should not have Damage Interrupt Circuit after load");
    }

    // ========== Multiple Modifications Test ==========

    @Test
    void testMekMultipleCockpitModsSaveAndLoad() throws Exception {
        // Create a Mek with multiple cockpit modifications
        Mek mek = new BipedMek();
        MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
        MiscType dic = (MiscType) EquipmentType.get("DamageInterruptCircuit");
        mek.addEquipment(dniMod, Entity.LOC_NONE);
        mek.addEquipment(dic, Entity.LOC_NONE);

        // Verify it has both mods before save
        assertTrue(mek.hasDNICockpitMod(), "Mek should have DNI Cockpit Mod before save");
        assertTrue(mek.hasDamageInterruptCircuit(), "Mek should have Damage Interrupt Circuit before save");
        assertFalse(mek.hasEiCockpit(), "Mek should not have EI Interface");

        // Save and reload
        Mek loadedMek = saveAndReloadMek(mek);

        // Verify it still has both mods after load
        assertTrue(loadedMek.hasDNICockpitMod(), "Mek should have DNI Cockpit Mod after load");
        assertTrue(loadedMek.hasDamageInterruptCircuit(), "Mek should have Damage Interrupt Circuit after load");
        assertFalse(loadedMek.hasEiCockpit(), "Mek should not have EI Interface after load");
    }

    // ========== Tank BLK Save/Load Tests ==========

    // Helper method to convert a Tank to BLK format and reload it
    private Tank saveAndReloadTank(Tank tank) throws Exception {
        BuildingBlock blk = BLKFile.getBlock(tank);
        BLKTankFile loader = new BLKTankFile(blk);
        return (Tank) loader.getEntity();
    }

    @Test
    void testTankDNICockpitModSaveAndLoad() throws Exception {
        // Create a Tank with DNI Cockpit Modification
        Tank tank = new Tank();
        tank.setChassis("Test");
        tank.setModel("Tank");
        tank.setWeight(20.0);
        tank.setMovementMode(EntityMovementMode.TRACKED);
        tank.setEngine(new Engine(100, Engine.NORMAL_ENGINE, Engine.TANK_ENGINE));
        tank.setOriginalWalkMP(5);
        tank.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        tank.setArmorTechLevel(TechConstants.T_INTRO_BOX_SET);
        tank.autoSetInternal();
        tank.initializeArmor(10, Tank.LOC_FRONT);
        tank.initializeArmor(10, Tank.LOC_RIGHT);
        tank.initializeArmor(10, Tank.LOC_LEFT);
        tank.initializeArmor(10, Tank.LOC_REAR);

        MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
        tank.addEquipment(dniMod, Entity.LOC_NONE);

        // Verify it has the mod before save
        assertTrue(tank.hasDNICockpitMod(), "Tank should have DNI Cockpit Mod before save");

        // Save and reload
        Tank loadedTank = saveAndReloadTank(tank);

        // Verify it still has the mod after load
        assertTrue(loadedTank.hasDNICockpitMod(), "Tank should have DNI Cockpit Mod after load");
    }

    @Test
    void testTankWithoutDNICockpitMod() throws Exception {
        // Create a Tank without DNI Cockpit Modification
        Tank tank = new Tank();
        tank.setChassis("Test");
        tank.setModel("Tank");
        tank.setWeight(20.0);
        tank.setMovementMode(EntityMovementMode.TRACKED);
        tank.setEngine(new Engine(100, Engine.NORMAL_ENGINE, Engine.TANK_ENGINE));
        tank.setOriginalWalkMP(5);
        tank.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        tank.setArmorTechLevel(TechConstants.T_INTRO_BOX_SET);
        tank.autoSetInternal();
        tank.initializeArmor(10, Tank.LOC_FRONT);
        tank.initializeArmor(10, Tank.LOC_RIGHT);
        tank.initializeArmor(10, Tank.LOC_LEFT);
        tank.initializeArmor(10, Tank.LOC_REAR);

        // Verify it doesn't have the mod
        assertFalse(tank.hasDNICockpitMod(), "Tank should not have DNI Cockpit Mod");

        // Save and reload
        Tank loadedTank = saveAndReloadTank(tank);

        // Verify it still doesn't have the mod after load
        assertFalse(loadedTank.hasDNICockpitMod(), "Tank should not have DNI Cockpit Mod after load");
    }
}
