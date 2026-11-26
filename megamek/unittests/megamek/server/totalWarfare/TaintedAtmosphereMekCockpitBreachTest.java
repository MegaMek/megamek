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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests a Mek cockpit breach in toxic air, TO:AR p.54: the MekWarrior is killed, and that is the only effect of the
 * breach. The cockpit is wherever the Mek mounts it, so a torso-mounted cockpit is breached through the center torso.
 */
class TaintedAtmosphereMekCockpitBreachTest {

    private static final int TOXIC_AIR_PILOT_KILLED_REPORT = 7716;

    private TWGameManager gameManager;
    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = spy(new TWGameManager());
        doNothing().when(gameManager).send(any(Packet.class));
        doNothing().when(gameManager).sendServerChat(anyString());
        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
        game.setPhase(GamePhase.FIRING);
        // With auto-abandon on, a vacuum center torso breach also ejects the pilot; the air must kill them instead
        game.getOptions().getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_AUTO_ABANDON_UNIT).setValue(true);
    }

    private BipedMek prepareMek(int cockpitType) {
        BipedMek mek = new BipedMek();
        mek.setId(1);
        mek.setOwner(game.getPlayer(0));
        mek.setWeight(50);
        mek.setOriginalWalkMP(5);
        mek.setCockpitType(cockpitType);
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeInternal(15, location);
            mek.initializeArmor(30, location);
            if (mek.hasRearArmor(location)) {
                mek.initializeRearArmor(10, location);
            }
        }
        game.addEntity(mek);
        mek.setPosition(new Coords(1, 1));
        mek.setDeployed(true);
        return mek;
    }

    /** Strips the armor so the breach is certain, then makes the breach check for a location open to toxic air. */
    private Vector<Report> breachInToxicAir(Mek mek, int location) {
        mek.setArmor(0, location);
        mek.setLocationStatus(location, ILocationExposureStatus.TAINTED);
        return gameManager.breachCheck(mek, location, null);
    }

    private static boolean hasReport(Vector<Report> reports, int messageId) {
        for (Report report : reports) {
            if (report.messageId == messageId) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("A breached torso-mounted cockpit kills the MekWarrior instead of ejecting them")
    void torsoMountedCockpitBreachKillsTheMekWarrior() {
        BipedMek mek = prepareMek(Mek.COCKPIT_TORSO_MOUNTED);

        Vector<Report> reports = breachInToxicAir(mek, Mek.LOC_CENTER_TORSO);

        assertTrue(mek.getCrew().isDoomed(), "the air kills the MekWarrior in the breached cockpit");
        assertFalse(mek.getCrew().isEjected(), "a pilot killed by the air does not eject");
        assertTrue(mek.isDoomed(), "the Mek is lost with its MekWarrior");
        assertTrue(hasReport(reports, TOXIC_AIR_PILOT_KILLED_REPORT));
        assertEquals(ILocationExposureStatus.BREACHED, mek.getLocationStatus(Mek.LOC_CENTER_TORSO));
    }

    @Test
    @DisplayName("Only the cockpit breach has an effect, so the center torso engine is not knocked out")
    void torsoMountedCockpitBreachLeavesTheEngineWorking() {
        BipedMek mek = prepareMek(Mek.COCKPIT_TORSO_MOUNTED);
        for (int slot = 0; slot < 3; slot++) {
            mek.setCritical(Mek.LOC_CENTER_TORSO, slot, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_ENGINE));
        }

        breachInToxicAir(mek, Mek.LOC_CENTER_TORSO);

        assertEquals(0, mek.getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE,
              Mek.LOC_CENTER_TORSO), "a vacuum breach would count every engine slot in the location as hit");
    }

    @Test
    @DisplayName("A standard cockpit in the head still kills the MekWarrior when breached")
    void headCockpitBreachKillsTheMekWarrior() {
        BipedMek mek = prepareMek(Mek.COCKPIT_STANDARD);

        Vector<Report> reports = breachInToxicAir(mek, Mek.LOC_HEAD);

        assertTrue(mek.getCrew().isDoomed());
        assertTrue(mek.isDoomed());
        assertTrue(hasReport(reports, TOXIC_AIR_PILOT_KILLED_REPORT));
        assertEquals(ILocationExposureStatus.BREACHED, mek.getLocationStatus(Mek.LOC_HEAD));
    }
}
