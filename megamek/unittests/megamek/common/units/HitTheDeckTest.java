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

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the "Hitting the Deck" infantry rule (TO:AR p.106).
 */
public class HitTheDeckTest extends GameBoardTestCase {

    static {
        initializeBoard("HIT_DECK_BOARD", """
              size 2 2
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              end""");
    }

    private static ConvInfantry getInfantry() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setId(2);
        infantry.setWeight(2.0f);
        infantry.initializeInternal(10, ConvInfantry.LOC_INFANTRY);
        return infantry;
    }

    @Nested
    @DisplayName("Movement legality")
    class MovementLegality {

        @Test
        @DisplayName("Hitting the deck as the only action is legal")
        void hittingTheDeckAloneIsLegal() {
            setBoard("HIT_DECK_BOARD");
            MovePath path = getMovePathFor(getInfantry(), MoveStepType.HIT_THE_DECK);
            assertTrue(path.isMoveLegal(), "Hitting the deck as the sole action should be legal");
        }

        @Test
        @DisplayName("A unit cannot move after hitting the deck")
        void cannotMoveAfterHittingTheDeck() {
            setBoard("HIT_DECK_BOARD");
            MovePath deckThenMove = getMovePathFor(getInfantry(),
                  MoveStepType.HIT_THE_DECK, MoveStepType.FORWARDS);
            assertFalse(deckThenMove.isMoveLegal(), "A unit may not move after hitting the deck");
        }

        @Test
        @DisplayName("A dug-in unit cannot also hit the deck")
        void cannotHitDeckWhileDugIn() {
            setBoard("HIT_DECK_BOARD");
            ConvInfantry infantry = getInfantry();
            infantry.setDugIn(Infantry.DUG_IN_COMPLETE);
            MovePath path = getMovePathFor(infantry, MoveStepType.HIT_THE_DECK);
            assertFalse(path.isMoveLegal(), "A unit that is dug in may not also hit the deck");
        }
    }

    @Nested
    @DisplayName("State machine")
    class StateMachine {

        private ConvInfantry stateInfantry() {
            ConvInfantry infantry = getInfantry();
            infantry.setGame(getGame());
            infantry.setCrew(new Crew(CrewType.INFANTRY_CREW));
            return infantry;
        }

        @Test
        @DisplayName("A unit that just hit the deck has no idle turns and cannot yet dig in")
        void freshlyOnDeckCannotDigIn() {
            ConvInfantry infantry = stateInfantry();
            infantry.setHitTheDeck(true);

            assertTrue(infantry.isHitTheDeck(), "Unit should be on the deck");
            assertEquals(0, infantry.getTurnsOnDeck(), "A unit that just hit the deck has no idle turns");
            assertFalse(infantry.canDigInFromDeck(), "A unit cannot immediately convert to dug in");
        }

        @Test
        @DisplayName("Idle turns accumulate, enabling the dig-in conversion after two turns")
        void idleTurnsEnableDigInConversion() {
            ConvInfantry infantry = stateInfantry();
            infantry.setHitTheDeck(true);

            infantry.newRound(1);
            assertEquals(1, infantry.getTurnsOnDeck(), "One idle turn should have elapsed");
            assertFalse(infantry.canDigInFromDeck(), "One idle turn is not enough to convert");

            infantry.newRound(2);
            assertEquals(2, infantry.getTurnsOnDeck(), "Two idle turns should have elapsed");
            assertTrue(infantry.canDigInFromDeck(), "Two idle turns allow conversion to dug in");
        }

        @Test
        @DisplayName("Firing breaks the idle streak required to dig in")
        void firingResetsIdleStreak() {
            ConvInfantry infantry = stateInfantry();
            infantry.setHitTheDeck(true);

            infantry.newRound(1);
            assertEquals(1, infantry.getTurnsOnDeck());

            infantry.setFiredWhileOnDeck(true);
            infantry.newRound(2);
            assertEquals(0, infantry.getTurnsOnDeck(), "Firing should reset the idle streak");
            assertFalse(infantry.canDigInFromDeck(), "After firing the unit cannot convert to dug in");
        }

        @Test
        @DisplayName("Clearing the deck status resets the idle counter")
        void clearingDeckResetsCounter() {
            ConvInfantry infantry = stateInfantry();
            infantry.setHitTheDeck(true);
            infantry.newRound(1);

            infantry.setHitTheDeck(false);

            assertFalse(infantry.isHitTheDeck(), "Unit should no longer be on the deck");
            assertEquals(0, infantry.getTurnsOnDeck(), "Leaving the deck resets the idle counter");
        }

        @Test
        @DisplayName("Loading into a transport clears the deck status")
        void transportClearsDeck() {
            ConvInfantry infantry = stateInfantry();
            infantry.setHitTheDeck(true);

            infantry.setTransportId(7);

            assertFalse(infantry.isHitTheDeck(), "A transported unit is no longer on the deck");
        }
    }
}
