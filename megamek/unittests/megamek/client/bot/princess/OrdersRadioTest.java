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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import megamek.client.bot.Messages;
import megamek.client.bot.princess.OrdersRadio.RadioVoice;
import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OrdersRadio}: callsigns, the Clan and Inner Sphere voices, plain replies with radio chatter off,
 * and one call per lance per kind of event per round.
 */
class OrdersRadioTest {

    private Game game;
    private Princess princess;
    private BipedMek atlas;
    private BipedMek marauder;
    private Force commandLance;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        Player bot = new Player(1, "Lyran Allies");
        bot.setBot(true);
        game.addPlayer(1, bot);
        int lanceId = game.getForces().addTopLevelForce(Force.createToplevelForce("Command Lance", bot), bot);
        atlas = unit(10, bot, lanceId);
        marauder = unit(11, bot, lanceId);
        commandLance = game.getForces().getForce(lanceId);
        princess = spy(new Princess("Lyran Allies", UUID.randomUUID().toString(), 1));
        doReturn(game).when(princess).getGame();
        doReturn(List.<Entity>of(atlas, marauder)).when(princess).getEntitiesOwned();
        doNothing().when(princess).sendChat(anyString(), any(Level.class));
    }

    private BipedMek unit(int unitId, Player owner, int lanceId) {
        BipedMek mek = new BipedMek();
        mek.setId(unitId);
        mek.setOwner(owner);
        game.addEntity(mek);
        game.getForces().addEntity(mek, lanceId);
        return mek;
    }

    @Test
    void anInnerSphereCallsignIsTheLanceAndThePlace() {
        assertEquals("Command One", OrdersRadio.callsign(atlas, commandLance, RadioVoice.INNER_SPHERE));
        assertEquals("Command Two", OrdersRadio.callsign(marauder, commandLance, RadioVoice.INNER_SPHERE));
    }

    @Test
    void aClanCallsignNamesThePoint() {
        assertEquals("Command Lance, Point Two", OrdersRadio.callsign(marauder, commandLance, RadioVoice.CLAN));
    }

    @Test
    void theVoiceFollowsTheForceAndTheSetting() {
        assertEquals(RadioVoice.INNER_SPHERE, princess.getOrdersRadio().voice());

        princess.getOrdersRadio().setRadioSetting(OrdersRadio.RadioSetting.OFF);

        assertEquals(RadioVoice.PLAIN, princess.getOrdersRadio().voice());
    }

    @Test
    void aLanceCallsOncePerEventPerRound() {
        game.setCurrentRound(3);

        princess.getOrdersRadio().report(atlas, "arrived", "1508");
        princess.getOrdersRadio().report(marauder, "arrived", "1508");

        verify(princess, times(1)).sendChat("Command One: "
              + Messages.getString("Princess.radio.INNER_SPHERE.arrived", "1508"), Level.INFO);
        verify(princess, times(1)).sendChat(anyString(), any(Level.class));
    }

    @Test
    void aBotNamedForComStarTalksLikeComStar() {
        Princess comstar = spy(new Princess("ComStar Level II Alpha", UUID.randomUUID().toString(), 1));
        doReturn(List.<Entity>of(atlas)).when(comstar).getEntitiesOwned();

        assertEquals(RadioVoice.COMSTAR, comstar.getOrdersRadio().voice());
        assertEquals("Command Lance, Adept One", OrdersRadio.callsign(atlas, commandLance, RadioVoice.COMSTAR));
    }

    @Test
    void aVoiceThePlayerSetWins() {
        princess.getOrdersRadio().setRadioSetting(OrdersRadio.RadioSetting.CLAN);

        assertEquals(RadioVoice.CLAN, princess.getOrdersRadio().voice());
    }
}
