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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.HitData;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link BridgeLayerLogic}: the Bridge-Layer (AVLB) rules helper that locates the deployable mount, redirects
 * hits to the carried bridge (including the Support Vehicle turret rule), and blocks weapon fire in the bridge's
 * location until it is deployed. TM p.242 / TW.
 *
 * @author Claude Code (Opus 4.8)
 */
class BridgeLayerLogicTest {

    private static final int MEK_TORSO = Mek.LOC_RIGHT_TORSO;

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    private static MiscMounted bridgeLayerMount(int location, BridgeLayerState state) {
        MiscMounted mount = mock(MiscMounted.class);
        when(mount.getBridgeLayerState()).thenReturn(state);
        when(mount.getLocation()).thenReturn(location);
        when(mount.isInoperable()).thenReturn(false);
        when(mount.isMissing()).thenReturn(false);
        return mount;
    }

    private static Mek quadMekCarrying(MiscMounted mount) {
        Mek mek = mock(Mek.class);
        doReturn(List.of(mount)).when(mek).getMisc();
        when(mek.getShortName()).thenReturn("Test Quad");
        when(mek.isSupportVehicle()).thenReturn(false);
        return mek;
    }

    private static Tank tankCarrying(MiscMounted mount, boolean supportVehicle) {
        Tank tank = mock(Tank.class);
        doReturn(List.of(mount)).when(tank).getMisc();
        when(tank.getShortName()).thenReturn("Test Tank");
        when(tank.isSupportVehicle()).thenReturn(supportVehicle);
        return tank;
    }

    @Test
    @DisplayName("a carried, functional bridgelayer is reported as deployable")
    void carriedBridgeLayerIsDeployable() {
        MiscMounted mount = bridgeLayerMount(MEK_TORSO, new BridgeLayerState(MiscType.createLightBridgeLayer()));
        Mek mek = quadMekCarrying(mount);
        assertSame(mount, BridgeLayerLogic.getDeployableBridgeLayer(mek));
    }

    @Test
    @DisplayName("a deployed bridge is no longer deployable")
    void deployedBridgeLayerIsNotDeployable() {
        BridgeLayerState state = new BridgeLayerState(MiscType.createLightBridgeLayer());
        state.setDeployed(true);
        Mek mek = quadMekCarrying(bridgeLayerMount(MEK_TORSO, state));
        assertNull(BridgeLayerLogic.getDeployableBridgeLayer(mek));
    }

    @Test
    @DisplayName("a destroyed bridge (CF 0) is no longer deployable")
    void destroyedBridgeLayerIsNotDeployable() {
        BridgeLayerState state = new BridgeLayerState(MiscType.createLightBridgeLayer());
        state.setCurrentCF(0);
        Mek mek = quadMekCarrying(bridgeLayerMount(MEK_TORSO, state));
        assertNull(BridgeLayerLogic.getDeployableBridgeLayer(mek));
    }

    @Test
    @DisplayName("a hit to the bridge's location is absorbed by the carried bridge; other locations are not")
    void hitToBridgeLocationRedirects() {
        MiscMounted mount = bridgeLayerMount(MEK_TORSO, new BridgeLayerState(MiscType.createLightBridgeLayer()));
        Mek mek = quadMekCarrying(mount);
        assertSame(mount, BridgeLayerLogic.getBridgeLayerForHit(mek, new HitData(MEK_TORSO)));
        assertNull(BridgeLayerLogic.getBridgeLayerForHit(mek, new HitData(Mek.LOC_LEFT_TORSO)),
              "a hit to a different location is not absorbed by the bridge");
    }

    @Test
    @DisplayName("on a Support Vehicle, both the bridge's own location and the turret are absorbed")
    void supportVehicleRedirectsMountLocationAndTurret() {
        MiscMounted mount = bridgeLayerMount(Tank.LOC_RIGHT, new BridgeLayerState(MiscType.createLightBridgeLayer()));
        Tank supportTank = tankCarrying(mount, true);
        assertSame(mount, BridgeLayerLogic.getBridgeLayerForHit(supportTank, new HitData(Tank.LOC_RIGHT)),
              "an SV hit to the bridge's own (right side) location hits the bridge - the Prometheus case");
        assertSame(mount, BridgeLayerLogic.getBridgeLayerForHit(supportTank, new HitData(Tank.LOC_TURRET)),
              "an SV turret hit also hits the bridge");
        assertNull(BridgeLayerLogic.getBridgeLayerForHit(supportTank, new HitData(Tank.LOC_FRONT)),
              "a hit to an unrelated location is not absorbed");
    }

    @Test
    @DisplayName("on a combat vehicle, a hit to the bridge's own location is absorbed")
    void combatVehicleHitToMountLocationRedirects() {
        MiscMounted mount = bridgeLayerMount(Tank.LOC_TURRET, new BridgeLayerState(MiscType.createLightBridgeLayer()));
        Tank combatTank = tankCarrying(mount, false);
        assertSame(mount, BridgeLayerLogic.getBridgeLayerForHit(combatTank, new HitData(Tank.LOC_TURRET)));
        assertNull(BridgeLayerLogic.getBridgeLayerForHit(combatTank, new HitData(Tank.LOC_FRONT)));
    }

    @Test
    @DisplayName("a carried bridge blocks weapons in its location; a deployed or destroyed bridge does not")
    void weaponFireBlockTracksBridgeState() {
        BridgeLayerState state = new BridgeLayerState(MiscType.createLightBridgeLayer());
        Mek mek = quadMekCarrying(bridgeLayerMount(MEK_TORSO, state));

        assertTrue(BridgeLayerLogic.isWeaponLocationBlockedByCarriedBridge(mek, MEK_TORSO),
              "weapons in the bridge's location are blocked while it is carried");
        assertFalse(BridgeLayerLogic.isWeaponLocationBlockedByCarriedBridge(mek, Mek.LOC_LEFT_TORSO),
              "weapons in other locations are not blocked");

        state.setDeployed(true);
        assertFalse(BridgeLayerLogic.isWeaponLocationBlockedByCarriedBridge(mek, MEK_TORSO),
              "once deployed, the location's weapons may fire again");
    }
}
