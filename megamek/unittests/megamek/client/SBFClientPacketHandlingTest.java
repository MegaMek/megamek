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

package megamek.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import megamek.common.actions.EntityAction;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.strategicBattleSystems.SBFFormation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Regression tests for SBF packet switch isolation and ordered multi-packet handling. */
class SBFClientPacketHandlingTest {

    private static final int FORMATION_ID = 17;

    private SBFClient client;

    @BeforeEach
    void setUp() {
        client = new SBFClient("Test", "localhost", 0);
    }

    @Test
    void unitInvisibleIsHandledWithoutFallingThroughToActions() {
        client.getGame().addUnit(formation());

        boolean handled = client.handleGameSpecificPacket(
              new Packet(PacketCommand.UNIT_INVISIBLE, FORMATION_ID));

        assertTrue(handled);
        assertTrue(client.getGame().getFormation(FORMATION_ID).isEmpty());
    }

    @Test
    void actionsAreHandledAndReplaceTheAuthoritativeList() {
        EntityAction staleAction = mock(EntityAction.class);
        EntityAction replacementAction = mock(EntityAction.class);
        client.getGame().addAction(staleAction);

        boolean handled = client.handleGameSpecificPacket(
              new Packet(PacketCommand.ACTIONS, new ArrayList<>(List.of(replacementAction))));

        assertTrue(handled);
        assertEquals(List.of(replacementAction), client.getGame().getActionsVector());
    }

    @Test
    void nestedPacketsAreAppliedInWireOrder() {
        SBFFormation formation = formation();
        client.handlePacket(new Packet(PacketCommand.MULTI_PACKET, new ArrayList<>(List.of(
              new Packet(PacketCommand.ENTITY_UPDATE, formation),
              new Packet(PacketCommand.UNIT_INVISIBLE, FORMATION_ID)))));

        assertTrue(client.getGame().getFormation(FORMATION_ID).isEmpty());

        client.handlePacket(new Packet(PacketCommand.MULTI_PACKET, new ArrayList<>(List.of(
              new Packet(PacketCommand.UNIT_INVISIBLE, FORMATION_ID),
              new Packet(PacketCommand.ENTITY_UPDATE, formation)))));

        assertFalse(client.getGame().getFormation(FORMATION_ID).isEmpty());
    }

    private SBFFormation formation() {
        SBFFormation formation = new SBFFormation();
        formation.setId(FORMATION_ID);
        return formation;
    }
}
