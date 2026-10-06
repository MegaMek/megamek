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

package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import megamek.common.board.Coords;
import megamek.common.units.ConvInfantry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BridgeLayerState}: the carried-bridge Construction Factors per Bridge-Layer variant (TechManual
 * Bridge-Layer table), the board bridge terrain type each variant deploys, and the deploy lifecycle state machine.
 *
 * @author Claude Code (Opus 4.8)
 */
class BridgeLayerStateTest {

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    @Test
    @DisplayName("each variant initializes its carried bridge CF from the TechManual Bridge-Layer table (8 / 20 / 45)")
    void initialCFMatchesTheRulebookTable() {
        assertEquals(8, new BridgeLayerState(MiscType.createLightBridgeLayer()).getCurrentCF(),
              "Light Bridge-Layer carries a CF 8 bridge");
        assertEquals(20, new BridgeLayerState(MiscType.createMediumBridgeLayer()).getCurrentCF(),
              "Medium Bridge-Layer carries a CF 20 bridge");
        assertEquals(45, new BridgeLayerState(MiscType.createHeavyBridgeLayer()).getCurrentCF(),
              "Heavy Bridge-Layer carries a CF 45 bridge");
    }

    @Test
    @DisplayName("the three bridgelayer flags are recognized as bridgelayers; other equipment is not")
    void isBridgeLayerRecognizesEachVariant() {
        assertTrue(BridgeLayerState.isBridgeLayer(MiscType.createLightBridgeLayer()));
        assertTrue(BridgeLayerState.isBridgeLayer(MiscType.createMediumBridgeLayer()));
        assertTrue(BridgeLayerState.isBridgeLayer(MiscType.createHeavyBridgeLayer()));
        assertFalse(BridgeLayerState.isBridgeLayer(MiscType.createBulldozer()), "a bulldozer is not a bridgelayer");
        assertFalse(BridgeLayerState.isBridgeLayer(null), "null is not a bridgelayer");
    }

    @Test
    @DisplayName("each variant deploys its matching bridge terrain level: Light (1), Medium (2), Heavy (3)")
    void terrainBridgeTypeMapsVariantToTerrainLevel() {
        assertEquals(ConvInfantry.BRIDGE_TYPE_LIGHT,
              BridgeLayerState.terrainBridgeType(MiscType.createLightBridgeLayer()));
        assertEquals(ConvInfantry.BRIDGE_TYPE_MEDIUM,
              BridgeLayerState.terrainBridgeType(MiscType.createMediumBridgeLayer()));
        // Heavy uses terrain level 3 (BuildingType.HEAVY) so the placed bridge is labelled "Heavy", not "Medium".
        assertEquals(3, BridgeLayerState.terrainBridgeType(MiscType.createHeavyBridgeLayer()));
    }

    @Test
    @DisplayName("a new bridgelayer is carried (not deployed, mechanism intact, nothing pending)")
    void freshStateIsCarried() {
        BridgeLayerState state = new BridgeLayerState(MiscType.createLightBridgeLayer());
        assertFalse(state.isDeployed());
        assertFalse(state.isDeployMechanismDisabled());
        assertFalse(state.isDeployPending());
        assertNull(state.getDeployTarget());
        assertEquals(-1, state.getDeployDeclaredTurn());
    }

    @Test
    @DisplayName("setCurrentCF clamps to a minimum of 0 so a destroyed bridge never reports negative CF")
    void currentCFClampsAtZero() {
        BridgeLayerState state = new BridgeLayerState(MiscType.createLightBridgeLayer());
        state.setCurrentCF(-5);
        assertEquals(0, state.getCurrentCF());
    }

    @Test
    @DisplayName("startDeploy records the pending deployment; clearPendingDeploy resets it")
    void deployLifecycle() {
        BridgeLayerState state = new BridgeLayerState(MiscType.createMediumBridgeLayer());
        Coords target = new Coords(3, 4);

        state.startDeploy(target, 9, 7);
        assertTrue(state.isDeployPending());
        assertEquals(target, state.getDeployTarget());
        assertEquals(9, state.getDeployExits());
        assertEquals(7, state.getDeployDeclaredTurn());

        state.clearPendingDeploy();
        assertFalse(state.isDeployPending());
        assertNull(state.getDeployTarget());
        assertEquals(0, state.getDeployExits());
        assertEquals(-1, state.getDeployDeclaredTurn());
    }

    @Test
    @DisplayName("the state is serializable so a unit carrying a bridgelayer can be sent over the network/lobby")
    void stateIsSerializable() throws Exception {
        // Regression: an entity could not be added to the lobby because BridgeLayerState was not Serializable, so
        // serializing the unit's equipment for the network packet threw NotSerializableException.
        BridgeLayerState original = new BridgeLayerState(MiscType.createMediumBridgeLayer());
        original.startDeploy(new Coords(2, 5), 9, 4);
        original.setDeployMechanismDisabled(true);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(bytes)) {
            objectOutput.writeObject(original);
        }
        BridgeLayerState restored;
        try (ObjectInputStream objectInput = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (BridgeLayerState) objectInput.readObject();
        }

        assertEquals(original.getCurrentCF(), restored.getCurrentCF());
        assertTrue(restored.isDeployPending());
        assertEquals(new Coords(2, 5), restored.getDeployTarget());
        assertEquals(9, restored.getDeployExits());
        assertEquals(4, restored.getDeployDeclaredTurn());
        assertTrue(restored.isDeployMechanismDisabled());
    }
}
