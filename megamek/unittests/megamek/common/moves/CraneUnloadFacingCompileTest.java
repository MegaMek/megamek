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
package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.units.BipedMek;
import megamek.common.units.Dropship;
import megamek.common.units.VTOL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The server compiles every move path it receives, which rebuilds each step. The facing chosen for crane unloading
 * rides in the step's additional data, so it must survive that rebuild; before the fix a step with a target and a hex
 * lost it and the unit came out facing the carrier's direction instead.
 */
class CraneUnloadFacingCompileTest extends GameBoardTestCase {

    private static final Coords CARRIER_HEX = new Coords(0, 0);
    private static final Coords UNLOAD_HEX = new Coords(0, 1);
    private static final int SOUTH = 3;

    static {
        initializeBoard("CRANE_UNLOAD_BOARD", """
              size 1 2
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              end""");
    }

    @Test
    @DisplayName("Compiling a crane unloading step keeps the facing the player chose")
    void compileKeepsCraneUnloadFacing() {
        setBoard("CRANE_UNLOAD_BOARD");
        Player player = new Player(0, "Player");
        getGame().addPlayer(player.getId(), player);

        Dropship carrier = new Dropship();
        carrier.setId(20);
        carrier.setOwner(player);
        getGame().addEntity(carrier);
        carrier.setDeployed(true);
        carrier.setPosition(CARRIER_HEX);

        VTOL carriedVtol = new VTOL();
        carriedVtol.setId(30);
        carriedVtol.setOwner(player);
        getGame().addEntity(carriedVtol);

        MovePath movePath = new MovePath(getGame(), carrier);
        movePath.addStep(MoveStepType.UNLOAD_BY_CRANE, carriedVtol, UNLOAD_HEX,
              Map.of(MoveStep.CRANE_UNLOAD_FACING_KEY, SOUTH));

        movePath.compile(getGame(), carrier, false);

        MoveStep compiledStep = movePath.getLastStep();
        assertEquals(MoveStepType.UNLOAD_BY_CRANE, compiledStep.getType(), "The crane unloading step should remain");
        assertEquals(UNLOAD_HEX, compiledStep.getTargetPosition(), "The unloading hex should survive compiling");
        assertEquals(SOUTH, compiledStep.getAdditionalData(MoveStep.CRANE_UNLOAD_FACING_KEY),
              "The chosen facing should survive compiling");
    }

    @Test
    @DisplayName("Compiling an ordinary unload step keeps the dismount facing the player chose (TW p.91)")
    void compileKeepsDismountFacing() {
        setBoard("CRANE_UNLOAD_BOARD");
        Player player = new Player(0, "Player");
        getGame().addPlayer(player.getId(), player);

        Dropship carrier = new Dropship();
        carrier.setId(20);
        carrier.setOwner(player);
        getGame().addEntity(carrier);
        carrier.setDeployed(true);
        carrier.setPosition(CARRIER_HEX);

        BipedMek carriedMek = new BipedMek();
        carriedMek.setId(31);
        carriedMek.setOwner(player);
        getGame().addEntity(carriedMek);

        MovePath movePath = new MovePath(getGame(), carrier);
        movePath.addStep(MoveStepType.UNLOAD, carriedMek, UNLOAD_HEX, Map.of(MoveStep.UNLOAD_FACING_KEY, SOUTH));

        movePath.compile(getGame(), carrier, false);

        MoveStep compiledStep = movePath.getLastStep();
        assertEquals(MoveStepType.UNLOAD, compiledStep.getType(), "The unload step should remain");
        assertEquals(SOUTH, compiledStep.getAdditionalData(MoveStep.UNLOAD_FACING_KEY),
              "The chosen dismount facing should survive compiling");
    }
}
