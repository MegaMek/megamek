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

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.CriticalSlot;
import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that sensor damage disables Enhanced Imaging, IO p.69. Regressions for #8738 (the EI 1-hex active probe kept
 * working with the sensors destroyed) and #8739 (a torso-mounted cockpit Mek kept its EI benefits with its centre torso
 * sensors destroyed).
 */
class EnhancedImagingSensorsTest {

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
        game.getOptions()
              .getOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE)
              .setValue(OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING);
    }

    private BipedMek eiMek(boolean torsoMountedCockpit) throws LocationFullException {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setWeight(50);
        Crew crew = new Crew(CrewType.SINGLE);
        crew.getOptions().getOption(OptionsConstants.MD_EI_IMPLANT).setValue(true);
        mek.setCrew(crew);
        mek.setOwner(game.getPlayer(0));
        mek.autoSetInternal();
        if (torsoMountedCockpit) {
            mek.setCockpitType(Mek.COCKPIT_TORSO_MOUNTED);
            mek.addTorsoMountedCockpit(false);
        } else {
            mek.addCockpit();
        }
        mek.addEquipment((MiscType) EquipmentType.get("EIInterface"), Mek.LOC_HEAD);
        return mek;
    }

    /** Destroys the first sensor slot in the location, as a resolved critical hit leaves it. */
    private static void destroyFirstSensorSlot(Mek mek, int location) {
        for (int slot = 0; slot < mek.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot criticalSlot = mek.getCritical(location, slot);
            if ((criticalSlot != null)
                  && (criticalSlot.getType() == CriticalSlot.TYPE_SYSTEM)
                  && (criticalSlot.getIndex() == Mek.SYSTEM_SENSORS)) {
                criticalSlot.setHit(true);
                criticalSlot.setDestroyed(true);
                return;
            }
        }
        throw new IllegalStateException("No sensor slot in location " + location);
    }

    @Test
    void eiProbeWorksWithUndamagedSensors() throws LocationFullException {
        BipedMek mek = eiMek(false);

        assertTrue(mek.hasBAP(false));
        assertEquals(1, mek.getBAPRange());
    }

    @Test
    void headSensorHitRemovesEiProbe() throws LocationFullException {
        BipedMek mek = eiMek(false);
        destroyFirstSensorSlot(mek, Mek.LOC_HEAD);

        assertFalse(mek.hasBAP(false), "The EI probe needs undamaged sensors");
        assertEquals(Entity.NONE, mek.getBAPRange());
    }

    @Test
    void protoMekHeadHitRemovesEiProbe() {
        ProtoMek protoMek = new ProtoMek();
        protoMek.setGame(game);
        protoMek.setWeight(5);
        protoMek.setCrew(new Crew(CrewType.SINGLE));
        protoMek.setOwner(game.getPlayer(0));
        protoMek.autoSetInternal();
        assertTrue(protoMek.hasBAP(false));

        protoMek.getCritical(ProtoMek.LOC_HEAD, 0).setHit(true);

        assertFalse(protoMek.hasBAP(false), "The EI probe needs undamaged sensors");
        assertEquals(Entity.NONE, protoMek.getBAPRange());
    }

    @Test
    void torsoMountedCockpitKeepsEiWithUndamagedSensors() throws LocationFullException {
        BipedMek mek = eiMek(true);

        assertTrue(mek.hasActiveEiCockpit());
    }

    @Test
    void torsoMountedCockpitLosesEiToCentreTorsoSensorHit() throws LocationFullException {
        BipedMek mek = eiMek(true);
        destroyFirstSensorSlot(mek, Mek.LOC_CENTER_TORSO);

        assertFalse(mek.hasActiveEiCockpit(), "A torso-mounted cockpit also has sensors in the centre torso");
        assertFalse(mek.hasBAP(false));
    }
}
