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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import megamek.common.CriticalSlot;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the destruction check on direct entity updates. Damage edits (for example a gamemaster
 * using the Edit Damage dialog in-game) bypass normal damage resolution, so {@code receiveEntityUpdate} must
 * detect a unit that cannot survive its edited state and destroy it.
 */
class TWGameManagerFatalDamageEditTest {

    private static final int UNIT_ID = 5;
    private static final int OWNER_CONNECTION_ID = 0;

    private TWGameManager gameManager;
    private Game game;
    private Entity mek;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();

        Player owner = new Player(OWNER_CONNECTION_ID, "Owner");
        owner.setTeam(1);
        game.addPlayer(OWNER_CONNECTION_ID, owner);

        mek = MMTestUtilities.getEntityForUnitTesting("Enforcer III ENF-6M", false);
        assertNotNull(mek, "Test unit could not be loaded");
        mek.setId(UNIT_ID);
        mek.setOwner(owner);
        game.addEntity(mek);

        game.setPhase(GamePhase.MOVEMENT);

        gameManager = mock(TWGameManager.class);
        doCallRealMethod().when(gameManager).setGame(any());
        doCallRealMethod().when(gameManager).handlePacket(anyInt(), any());
        gameManager.setGame(game);
    }

    /** Sends the (edited) unit back to the server the same way the Edit Damage dialog does. */
    private void sendEntityUpdate() {
        gameManager.handlePacket(OWNER_CONNECTION_ID, new Packet(PacketCommand.ENTITY_UPDATE, mek));
    }

    @Test
    void destroysMekWhenCenterTorsoIsGone() {
        mek.setInternal(IArmorState.ARMOR_DESTROYED, Mek.LOC_CENTER_TORSO);

        sendEntityUpdate();

        verify(gameManager).destroyEntity(mek, "damage", true);
    }

    @Test
    void destroysMekWhenEngineIsDestroyed() {
        mek.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, Mek.LOC_CENTER_TORSO, 3);

        sendEntityUpdate();

        verify(gameManager).destroyEntity(mek, "engine destruction", true);
    }

    @Test
    void destroysMekWhenCrewIsDead() {
        mek.getCrew().setHits(6, 0);

        sendEntityUpdate();

        verify(gameManager).destroyEntity(mek, "crew death", false);
    }

    @Test
    void survivesNonFatalDamage() {
        mek.setInternal(IArmorState.ARMOR_DESTROYED, Mek.LOC_LEFT_ARM);

        sendEntityUpdate();

        verify(gameManager, never()).destroyEntity(any(), anyString(), anyBoolean());
    }

    @Test
    void skipsDestructionCheckInTheLounge() {
        game.setPhase(GamePhase.LOUNGE);
        mek.setInternal(IArmorState.ARMOR_DESTROYED, Mek.LOC_CENTER_TORSO);

        sendEntityUpdate();

        verify(gameManager, never()).destroyEntity(any(), anyString(), anyBoolean());
    }
}
