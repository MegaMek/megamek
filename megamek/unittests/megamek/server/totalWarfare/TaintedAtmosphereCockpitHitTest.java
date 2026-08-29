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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Vector;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.units.BipedMek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests the extra crew hit a caustic atmosphere inflicts through a damaged cockpit, TO:AR p.54.
 * <p>
 * A playtest read as confusing because the same hit that damages a cockpit can also breach it, which in toxic air
 * kills the crew and destroys the unit. The air then burned the crew of the wreck, printing that it got in followed
 * by "is already dead, so no damage is dealt" - two lines that say nothing about what killed the unit.
 */
class TaintedAtmosphereCockpitHitTest {

    private TWGameManager gameManager;
    private Game game;
    private TaintedAtmosphereHandler handler;

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendServerChat(anyString());

        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
        game.setPhase(GamePhase.FIRING);
        handler = new TaintedAtmosphereHandler(gameManager);
    }

    private BipedMek mekInAir(AtmosphericTaint atmosphericTaint) {
        game.getPlanetaryConditions().setAtmosphericTaint(atmosphericTaint);
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setOwner(game.getPlayer(0));
        mek.setId(1);
        game.addEntity(mek);
        return mek;
    }

    private static boolean reportsTheAirGettingIn(Vector<Report> reports) {
        return reports.stream().anyMatch(report -> report.messageId == 7710);
    }

    @Test
    @DisplayName("Caustic air burns the crew through a damaged cockpit")
    void causticAirBurnsTheCrew() {
        BipedMek mek = mekInAir(AtmosphericTaint.CAUSTIC_TAINTED);

        assertTrue(reportsTheAirGettingIn(handler.resolveExtraCockpitCrewHit(mek)),
              "a live crew in a damaged cockpit takes the extra hit");
    }

    @Test
    @DisplayName("A destroyed unit is not burned a second time")
    void aDestroyedUnitIsNotBurned() {
        BipedMek mek = mekInAir(AtmosphericTaint.CAUSTIC_TOXIC);
        mek.setDestroyed(true);

        assertFalse(reportsTheAirGettingIn(handler.resolveExtraCockpitCrewHit(mek)),
              "the same hit already breached the cockpit and destroyed the unit - saying the air got into the "
                    + "wreck only buries what actually killed it");
    }

    @Test
    @DisplayName("A crew already lost is not burned again")
    void aCrewAlreadyLostIsNotBurned() {
        BipedMek mek = mekInAir(AtmosphericTaint.CAUSTIC_TOXIC);
        mek.getCrew().setDead(true);

        assertFalse(reportsTheAirGettingIn(handler.resolveExtraCockpitCrewHit(mek)),
              "there is nobody left for the air to burn");
    }

    @Test
    @DisplayName("Air that is not caustic burns nobody")
    void nonCausticAirBurnsNobody() {
        BipedMek mek = mekInAir(AtmosphericTaint.RADIOLOGICAL_TAINTED);

        assertFalse(reportsTheAirGettingIn(handler.resolveExtraCockpitCrewHit(mek)),
              "only caustic air gets into the cockpit and burns the crew");
    }
}
