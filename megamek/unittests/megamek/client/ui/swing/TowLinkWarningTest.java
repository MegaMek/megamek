/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.client.ui.swing;

import megamek.client.ui.swing.phaseDisplay.TowLinkWarning;
import megamek.common.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TowLinkWarningTest {
    final int TRACTOR_ID = 1;
    final int TRAILER_ID = 2;
    final int TRAILER_2_ID = 3;
    final int TRAILER_3_ID = 4;
    final int TRAILER_4_ID = 5;

    Game mockGame;
    Board mockBoard;
    Entity mockTractor;
    Entity mockTrailer;

    @BeforeEach
    void initialize() {
        mockTractor = mock(Entity.class);
        when(mockTractor.getId()).thenReturn(TRACTOR_ID);
        mockTrailer = mock(Entity.class);
        when(mockTrailer.getId()).thenReturn(TRAILER_ID);
        mockGame = mock(Game.class);
        mockBoard = mock(Board.class);
        when(mockBoard.getHeight()).thenReturn(5);
        when(mockBoard.getWidth()).thenReturn(5);
        when(mockBoard.isLegalDeployment(any(Coords.class), any(Entity.class))).thenReturn(true);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getEntity(TRACTOR_ID)).thenReturn(mockTractor);
        when(mockGame.getEntity(TRAILER_ID)).thenReturn(mockTrailer);
        when(mockGame.hasEntity(TRACTOR_ID)).thenReturn(true);
        when(mockGame.hasEntity(TRAILER_ID)).thenReturn(true);

        when(mockTractor.isDeployed()).thenReturn(false);
        when(mockTrailer.isDeployed()).thenReturn(false);

        when(mockTractor.getTowing()).thenReturn(Entity.NONE);
        when(mockTrailer.getTowing()).thenReturn(Entity.NONE);

        when(mockTractor.getTowedBy()).thenReturn(Entity.NONE);
        when(mockTrailer.getTowedBy()).thenReturn(Entity.NONE);
    }

    /**
     * @see TowLinkWarning#findValidDeployCoordsForTractorTrailer(Game, Entity, Board)
     */
    @Nested
    class testFindCoordsForTractor {

        @Test
        void testNoTrailer() {
            // Arrange

            // Act
            List<Coords> coords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTractor, mockBoard);

            // Assert
            // Empty list - It's not a tractor or trailer!
            assertEquals(0, coords.size());
        }

        @Test
        void testTrailerNotDeployedYet() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);

            // Act
            List<Coords> coords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTractor, mockBoard);


            // Assert
            assertEquals(25, coords.size());
        }

        @Test
        void testTrailerDeployed() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);
            when(mockTrailer.isDeployed()).thenReturn(true);
            when(mockTrailer.getPosition()).thenReturn(new Coords(3, 3));

            // Act
            List<Coords> coords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTractor, mockBoard);


            // Assert
            assertEquals(1, coords.size());
        }

        @Test
        void testLargeTrailerDeployed() {
            // Arrange

            mockTrailer = mock(LargeSupportTank.class);
            when(mockTrailer.getId()).thenReturn(TRAILER_ID);
            when(mockTrailer.isDeployed()).thenReturn(true);
            when(mockGame.getEntity(TRAILER_ID)).thenReturn(mockTrailer);

            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);
            when(mockTrailer.isDeployed()).thenReturn(true);
            when(mockTrailer.getPosition()).thenReturn(new Coords(3, 3));

            List<Coords> testCoords = null;

            try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
                compute.when(() -> Compute.stackingViolation(any(Game.class), anyInt(), any(Coords.class), anyBoolean())).thenReturn(mock(Entity.class));

                // Act
                testCoords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTractor, mockBoard);
            }

            // Assert
            assertEquals(6, testCoords.size());
        }
    }

    /**
     * @see TowLinkWarning#findValidDeployCoordsForTractorTrailer(Game, Entity, Board)
     */
    @Nested
    class testFindCoordsForTrailer {

        @Test
        void testNoTractor() {
            // Arrange

            // Act
            List<Coords> coords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTrailer, mockBoard);

            // Assert
            // Empty list - It's not a tractor or trailer!
            assertEquals(0, coords.size());
        }

        @Test
        void testTractorNotDeployedYet() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);

            // Act
            List<Coords> coords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTrailer, mockBoard);

            // Assert
            assertEquals(25, coords.size());
        }

        @Test
        void testTractorDeployed() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);

            when(mockTractor.isDeployed()).thenReturn(true);
            when(mockTractor.getPosition()).thenReturn(new Coords(3, 3));

            // Act
            List<Coords> coords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTrailer, mockBoard);

            // Assert
            assertEquals(1, coords.size());
        }

        @Test
        void testLargeTrailerAndTractorDeployed() {
            // Arrange
            mockTrailer = mock(LargeSupportTank.class);
            when(mockTrailer.getId()).thenReturn(TRAILER_ID);
            when(mockTrailer.isDeployed()).thenReturn(false);
            when(mockGame.getEntity(TRAILER_ID)).thenReturn(mockTrailer);

            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);

            when(mockTractor.isDeployed()).thenReturn(true);
            when(mockTractor.getPosition()).thenReturn(new Coords(3, 3));

            List<Coords> testCoords = null;

            try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
                compute.when(() -> Compute.stackingViolation(any(Game.class), anyInt(), any(Coords.class), anyBoolean())).thenReturn(mock(Entity.class));

                // Act
                testCoords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTrailer, mockBoard);
            }

            // Assert
            assertEquals(6, testCoords.size());
        }
    }
    @Nested
    class TractorTrainTests {
        // Let's mock another trailer and put it at the end of the train
        Entity mockTrailer2;
        Entity mockTrailer3;
        Entity mockTrailer4;

        @BeforeEach
        void setUp() {
            mockTrailer2 = mock(Entity.class);
            when(mockTrailer2.getId()).thenReturn(TRAILER_2_ID);
            when(mockGame.getEntity(mockTrailer2.getId())).thenReturn(mockTrailer2);
            when(mockGame.hasEntity(mockTrailer2.getId())).thenReturn(true);

            mockTrailer3 = mock(Entity.class);
            when(mockTrailer3.getId()).thenReturn(TRAILER_3_ID);
            when(mockGame.getEntity(mockTrailer3.getId())).thenReturn(mockTrailer3);
            when(mockGame.hasEntity(mockTrailer3.getId())).thenReturn(true);

            mockTrailer4 = mock(Entity.class);
            when(mockTrailer4.getId()).thenReturn(TRAILER_4_ID);
            when(mockGame.getEntity(mockTrailer4.getId())).thenReturn(mockTrailer4);
            when(mockGame.hasEntity(mockTrailer4.getId())).thenReturn(true);

            when(mockTrailer2.isDeployed()).thenReturn(false);
            when(mockTrailer3.isDeployed()).thenReturn(false);
            when(mockTrailer4.isDeployed()).thenReturn(false);

            when(mockTrailer2.getTowing()).thenReturn(Entity.NONE);
            when(mockTrailer3.getTowing()).thenReturn(Entity.NONE);
            when(mockTrailer4.getTowing()).thenReturn(Entity.NONE);

            when(mockTrailer2.getTowedBy()).thenReturn(Entity.NONE);
            when(mockTrailer3.getTowedBy()).thenReturn(Entity.NONE);
            when(mockTrailer4.getTowedBy()).thenReturn(Entity.NONE);
        }

        @Test
        void test2TrailersLastTrailerDeployed() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowing()).thenReturn(TRAILER_2_ID);

            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);
            when(mockTrailer2.getTowedBy()).thenReturn(TRAILER_ID);

            when(mockTractor.getAllTowedUnits()).thenReturn(Arrays.asList(TRAILER_ID, TRAILER_2_ID));

            when(mockTrailer2.isDeployed()).thenReturn(true);
            when(mockTrailer2.getPosition()).thenReturn(new Coords(3, 3));

            // Act
            List<Coords> coords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTractor, mockBoard);

            // Assert
            // This tractor should be suggested to deploy in any location adjacent to the current deployed trailer - 6 hexes
            assertEquals(6, coords.size());
        }

        @Test
        void test3TrailersLastTrailerDeployed() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowing()).thenReturn(TRAILER_2_ID);
            when(mockTrailer2.getTowing()).thenReturn(TRAILER_3_ID);

            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);
            when(mockTrailer2.getTowedBy()).thenReturn(TRAILER_ID);
            when(mockTrailer3.getTowedBy()).thenReturn(TRAILER_2_ID);

            when(mockTractor.getAllTowedUnits()).thenReturn(Arrays.asList(TRAILER_ID, TRAILER_2_ID, TRAILER_3_ID));

            when(mockTrailer3.isDeployed()).thenReturn(true);
            when(mockTrailer3.getPosition()).thenReturn(new Coords(3, 3));

            // Act
            List<Coords> coords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTractor, mockBoard);

            // Assert
            // These trailers stack, so trailer 3 and 2 will be in one hex, and the tractor and trailer 1 in the adjacent hex
            assertEquals(6, coords.size());
        }

        @Test
        void test4TrailersLastTrailerDeployed() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowing()).thenReturn(TRAILER_2_ID);
            when(mockTrailer2.getTowing()).thenReturn(TRAILER_3_ID);
            when(mockTrailer3.getTowing()).thenReturn(TRAILER_4_ID);

            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);
            when(mockTrailer2.getTowedBy()).thenReturn(TRAILER_ID);
            when(mockTrailer3.getTowedBy()).thenReturn(TRAILER_2_ID);
            when(mockTrailer4.getTowedBy()).thenReturn(TRAILER_3_ID);

            when(mockTractor.getAllTowedUnits()).thenReturn(Arrays.asList(TRAILER_ID, TRAILER_2_ID, TRAILER_3_ID, TRAILER_4_ID));

            when(mockTrailer4.isDeployed()).thenReturn(true);
            when(mockTrailer4.getPosition()).thenReturn(new Coords(3, 3));

            // Act
            List<Coords> coords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTractor, mockBoard);

            // Assert
            // These trailers stack, so trailer 3 and 2 will be in one hex, and the tractor and trailer 1 in the adjacent hex
            assertEquals(19, coords.size());
        }

        @Test
        void testDeployTrailer4WithTrailer2Deployed() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowing()).thenReturn(TRAILER_2_ID);
            when(mockTrailer2.getTowing()).thenReturn(TRAILER_3_ID);
            when(mockTrailer3.getTowing()).thenReturn(TRAILER_4_ID);

            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);
            when(mockTrailer2.getTowedBy()).thenReturn(TRAILER_ID);
            when(mockTrailer3.getTowedBy()).thenReturn(TRAILER_2_ID);
            when(mockTrailer4.getTowedBy()).thenReturn(TRAILER_3_ID);

            when(mockTractor.getAllTowedUnits()).thenReturn(Arrays.asList(TRAILER_ID, TRAILER_2_ID, TRAILER_3_ID, TRAILER_4_ID));

            when(mockTrailer2.isDeployed()).thenReturn(true);
            when(mockTrailer2.getPosition()).thenReturn(new Coords(3, 3));

            // Act
            List<Coords> coords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTrailer4, mockBoard);

            // Assert
            assertEquals(6, coords.size());
        }

        @Test
        void testDeployTrailerMultiDeployed() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowing()).thenReturn(TRAILER_2_ID);
            when(mockTrailer2.getTowing()).thenReturn(TRAILER_3_ID);
            when(mockTrailer3.getTowing()).thenReturn(TRAILER_4_ID);

            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);
            when(mockTrailer2.getTowedBy()).thenReturn(TRAILER_ID);
            when(mockTrailer3.getTowedBy()).thenReturn(TRAILER_2_ID);
            when(mockTrailer4.getTowedBy()).thenReturn(TRAILER_3_ID);

            when(mockTractor.getAllTowedUnits()).thenReturn(Arrays.asList(TRAILER_ID, TRAILER_2_ID, TRAILER_3_ID, TRAILER_4_ID));

            when(mockTractor.isDeployed()).thenReturn(true);
            when(mockTractor.getPosition()).thenReturn(new Coords(3, 3));

            when(mockTrailer4.isDeployed()).thenReturn(true);
            when(mockTrailer4.getPosition()).thenReturn(new Coords(3, 1));

            // Act
            List<Coords> coords = TowLinkWarning.findValidDeployCoordsForTractorTrailer(mockGame, mockTrailer2, mockBoard);

            // Assert
            assertEquals(1, coords.size());
        }
    }

    /**
     * @see TowLinkWarning#findTowLinkIssues(Game, Entity, Board)
     */
    @Nested
    class FindTowLinkIssues {
        @Test
        void testTowLinkIssuesNoIssues() {
            // Act
            List<Coords> coords = TowLinkWarning.findTowLinkIssues(mockGame, mockTrailer, mockBoard);

            // Assert
            // Every hex is okay
            assertEquals(0, coords.size());
        }

        @Test
        void testTowLinkTrailerNoIssuesNotDeployed() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);

            // Act
            List<Coords> coords = TowLinkWarning.findTowLinkIssues(mockGame, mockTrailer, mockBoard);

            // Assert
            // Every hex is okay
            assertEquals(0, coords.size());
        }


        @Test
        void testTowLinkDeployedTractorIssues() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);

            when(mockTractor.isDeployed()).thenReturn(true);
            when(mockTractor.getPosition()).thenReturn(new Coords(3,3));

            // Act
            List<Coords> coords = TowLinkWarning.findTowLinkIssues(mockGame, mockTrailer, mockBoard);

            // Assert
            // Only one okay hex
            assertEquals(24, coords.size());
        }

        @Test
        void testTowLinkTractorInCoordTrailerCantDeploy() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);

            when(mockTractor.isDeployed()).thenReturn(true);
            when(mockTractor.getPosition()).thenReturn(new Coords(3,3));

            when(mockTrailer.isLocationProhibited(new Coords(3,3))).thenReturn(true);

            // Act
            List<Coords> coords = TowLinkWarning.findTowLinkIssues(mockGame, mockTrailer, mockBoard);

            // Assert
            //No good hexes - bad tractor spot!
            assertEquals(25, coords.size());
        }

        @Test
        void testTowLinkWarningLargeTrailer() {
            // Arrange
            mockTrailer = mock(LargeSupportTank.class);
            when(mockTrailer.getId()).thenReturn(TRAILER_ID);
            when(mockTrailer.isDeployed()).thenReturn(true);
            when(mockGame.getEntity(TRAILER_ID)).thenReturn(mockTrailer);

            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);
            when(mockTractor.isDeployed()).thenReturn(true);
            when(mockTractor.getPosition()).thenReturn(new Coords(3, 3));

            // Act
            List<Coords> coords = TowLinkWarning.findTowLinkIssues(mockGame, mockTrailer, mockBoard);

            // Assert
            // There should be 6 available hexes: 25 - 6 adjacent = 19 valid coords
            assertEquals(19, coords.size());
        }
    }

    /**
     * Inspired by Luana's suggestion to add a limit to how many times the
     * do while loops while search for a tractor/trailer. This will test
     * invalid situations to ensure they fail in an expected manner.
     */
    @Nested
    class TestInvalidUseCases {

        @Test
        void testInfiniteLoops() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowing()).thenReturn(TRACTOR_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);
            when(mockTractor.getTowedBy()).thenReturn(TRAILER_ID);

            // Act / Assert
            // This should throw a stack overflow error.
            Throwable exception = assertThrows(StackOverflowError.class, () -> TowLinkWarning.findTowLinkIssues(mockGame, mockTrailer, mockBoard));
        }
    }
}
