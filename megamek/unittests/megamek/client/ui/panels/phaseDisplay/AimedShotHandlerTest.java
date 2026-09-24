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
package megamek.client.ui.panels.phaseDisplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests which targets a targeting computer may aim at, and what the aimed-shot location mask allows.
 * <p>
 * A targeting computer cannot pick a hit location on a ProtoMek at all - they are too small (TW p.185) - and cannot
 * aim at a Mek's head (TW p.143).
 */
@DisplayName("AimedShotHandler targeting-computer aiming")
class AimedShotHandlerTest {

    private Game game;
    private FiringDisplay firingDisplay;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
        firingDisplay = mock(FiringDisplay.class);
    }

    /** An attacker with aimed-shot capability from TCP + VDNI (IO p.81), so no targeting computer has to be mounted. */
    private BipedMek createAimingAttacker() {
        BipedMek mek = createMek(1, "Test Attacker", new Coords(0, 0), 3);
        mek.getCrew().getOptions().getOption(OptionsConstants.MD_TRIPLE_CORE_PROCESSOR).setValue(true);
        mek.getCrew().getOptions().getOption(OptionsConstants.MD_VDNI).setValue(true);
        return mek;
    }

    private BipedMek createMek(int id, String chassis, Coords position, int facing) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(id);
        mek.setChassis(chassis);
        mek.setModel("Standard");
        mek.setWeight(50);
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(game.getPlayer(0));
        mek.autoSetInternal();
        mek.setPosition(position);
        mek.setFacing(facing);
        return mek;
    }

    private ProtoMek createProtoMekTarget() {
        ProtoMek protoMek = new ProtoMek();
        protoMek.setGame(game);
        protoMek.setId(2);
        protoMek.setChassis("Test ProtoMek");
        protoMek.setModel("Standard");
        protoMek.setWeight(5);
        protoMek.setCrew(new Crew(CrewType.SINGLE));
        protoMek.setOwner(game.getPlayer(0));
        protoMek.autoSetInternal();
        protoMek.setPosition(new Coords(0, 2));
        protoMek.setFacing(0);
        return protoMek;
    }

    private AimedShotHandler handlerAimingAt(Entity target) {
        when(firingDisplay.currentEntity()).thenReturn(createAimingAttacker());
        when(firingDisplay.getTarget()).thenReturn(target);
        AimedShotHandler handler = new AimedShotHandler(firingDisplay);
        handler.setAimingMode();
        return handler;
    }

    @Test
    @DisplayName("A targeting computer cannot aim at a ProtoMek at all")
    void targetingComputerCannotAimAtProtoMek() {
        AimedShotHandler handler = handlerAimingAt(createProtoMekTarget());

        assertTrue(handler.getAimingMode().isNone(),
              "a ProtoMek is too small for a targeting computer to pick a hit location (TW p.185)");
    }

    @Test
    @DisplayName("A targeting computer can aim at a Mek, but not at its head")
    void targetingComputerAimsAtMekButNotHead() {
        BipedMek target = createMek(2, "Test Target", new Coords(0, 2), 0);
        AimedShotHandler handler = handlerAimingAt(target);

        assertTrue(handler.getAimingMode().isTargetingComputer(), "the attacker should be aiming with a TC");
        boolean[] mask = handler.createEnabledMask(target.getLocationNames().length);
        assertFalse(mask[Mek.LOC_HEAD], "the Mek head should be disabled under a targeting computer (TW p.143)");
        assertTrue(mask[Mek.LOC_CENTER_TORSO], "the Mek center torso should stay selectable");
    }
}
