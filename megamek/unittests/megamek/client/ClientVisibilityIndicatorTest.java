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

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import megamek.common.Player;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@code Client.receiveEntityVisibilityIndicator} only fires a {@link GameEntityChangeEvent} when the
 * visibility state in fact changed. The server re-sends unchanged indicators wholesale in double-blind games; firing an
 * event for each one made the UI redo entity sprites and images for every packet.
 */
class ClientVisibilityIndicatorTest {

    private Client client;
    private Entity entity;
    private final AtomicInteger entityChangeEvents = new AtomicInteger();

    @BeforeEach
    void setUp() {
        client = new Client("Test Player", "localhost", 1234);
        // Visibility indicators matter in double-blind games; without the option, the visibility getters
        // report true unconditionally
        client.getGame().getOptions().getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND).setValue(true);
        entity = new BipedMek();
        client.getGame().addEntity(entity, false);
        client.getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gameEntityChange(GameEntityChangeEvent e) {
                entityChangeEvents.incrementAndGet();
            }
        });
    }

    private Packet visibilityPacket(boolean everSeen, boolean visible, boolean detected,
          Vector<Player> whoCanSee, Vector<Player> whoCanDetect) {
        return new Packet(PacketCommand.ENTITY_VISIBILITY_INDICATOR,
              entity.getId(), everSeen, visible, detected, whoCanSee, whoCanDetect);
    }

    @Test
    void firesEventWhenVisibilityChanges() throws Exception {
        client.receiveEntityVisibilityIndicator(
              visibilityPacket(true, true, false, new Vector<>(), new Vector<>()));

        assertEquals(1, entityChangeEvents.get());
        assertTrue(entity.isEverSeenByEnemy());
        assertTrue(entity.isVisibleToEnemy());
        assertFalse(entity.isDetectedByEnemy());
    }

    @Test
    void doesNotFireEventWhenNothingChanged() throws Exception {
        Vector<Player> whoCanSee = new Vector<>();
        whoCanSee.add(new Player(0, "Spotter"));

        client.receiveEntityVisibilityIndicator(
              visibilityPacket(true, true, false, whoCanSee, new Vector<>()));
        assertEquals(1, entityChangeEvents.get());

        // Identical state re-sent by the server: state must be applied but no event fired
        Vector<Player> sameWhoCanSee = new Vector<>();
        sameWhoCanSee.add(new Player(0, "Spotter"));
        client.receiveEntityVisibilityIndicator(
              visibilityPacket(true, true, false, sameWhoCanSee, new Vector<>()));

        assertEquals(1, entityChangeEvents.get(),
              "An indicator that carries no change must not fire a GameEntityChangeEvent");
    }

    @Test
    void firesEventAgainWhenAFlagChanges() throws Exception {
        client.receiveEntityVisibilityIndicator(
              visibilityPacket(true, true, false, new Vector<>(), new Vector<>()));
        client.receiveEntityVisibilityIndicator(
              visibilityPacket(true, true, false, new Vector<>(), new Vector<>()));
        assertEquals(1, entityChangeEvents.get());

        client.receiveEntityVisibilityIndicator(
              visibilityPacket(true, true, true, new Vector<>(), new Vector<>()));

        assertEquals(2, entityChangeEvents.get());
        assertTrue(entity.isDetectedByEnemy());
    }

    @Test
    void firesEventWhenWhoCanSeeChanges() throws Exception {
        client.receiveEntityVisibilityIndicator(
              visibilityPacket(true, true, false, new Vector<>(), new Vector<>()));
        assertEquals(1, entityChangeEvents.get());

        Vector<Player> whoCanSee = new Vector<>();
        whoCanSee.add(new Player(3, "New Spotter"));
        client.receiveEntityVisibilityIndicator(
              visibilityPacket(true, true, false, whoCanSee, new Vector<>()));

        assertEquals(2, entityChangeEvents.get());
        assertEquals(whoCanSee, entity.getWhoCanSee());
    }

    @Test
    void withoutDoubleBlindVisibilityFlagsAreNotObservableChanges() throws Exception {
        // Without double-blind, isVisibleToEnemy/isDetectedByEnemy report true unconditionally, so packets
        // that only toggle those flags change nothing a listener could observe
        client.getGame().getOptions().getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND).setValue(false);

        client.receiveEntityVisibilityIndicator(
              visibilityPacket(false, true, true, new Vector<>(), new Vector<>()));
        assertEquals(0, entityChangeEvents.get());

        client.receiveEntityVisibilityIndicator(
              visibilityPacket(false, false, false, new Vector<>(), new Vector<>()));
        assertEquals(0, entityChangeEvents.get());

        // everSeenByEnemy is a plain field and thus observable regardless of double-blind
        client.receiveEntityVisibilityIndicator(
              visibilityPacket(true, false, false, new Vector<>(), new Vector<>()));
        assertEquals(1, entityChangeEvents.get());
    }
}
