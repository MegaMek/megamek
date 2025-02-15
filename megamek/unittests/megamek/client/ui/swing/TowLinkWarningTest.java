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

import megamek.common.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TowLinkWarningTest {
    final int TRACTOR_ID = 1;
    final int TRAILER_ID = 2;

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
    }

    /**
     * @see TowLinkWarning#findCoordsForTractor(Game, Entity, Board)
     */
    @Nested
    class testFindCoordsForTractor {

        @Test
        void testNoTrailer() {
            // Arrange

            // Act
            List<Coords> coords = TowLinkWarning.findCoordsForTractor(mockGame, mockTractor, mockBoard);

            // Assert
            assertNull(coords);
        }

        @Test
        void testTrailerNotDeployedYet() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);

            // Act
            List<Coords> coords = TowLinkWarning.findCoordsForTractor(mockGame, mockTractor, mockBoard);


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
            List<Coords> coords = TowLinkWarning.findCoordsForTractor(mockGame, mockTractor, mockBoard);


            // Assert
            assertEquals(1, coords.size());
        }

        @Test
        void testLargeTrailerDeployed() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);
            when(mockTrailer.isDeployed()).thenReturn(true);
            when(mockTrailer.getPosition()).thenReturn(new Coords(3, 3));

            List<Coords> testCoords = null;

            try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
                compute.when(() -> Compute.stackingViolation(any(Game.class), anyInt(), any(Coords.class), anyBoolean())).thenReturn(mock(Entity.class));

                // Act
                testCoords = TowLinkWarning.findCoordsForTractor(mockGame, mockTractor, mockBoard);
            }

            // Assert
            assertEquals(5, testCoords.size());
        }
    }

    /**
     * @see TowLinkWarning#findCoordsForTrailer(Game, Entity, Board)
     */
    @Nested
    class testFindCoordsForTrailer {

        @Test
        void testNoTractor() {
            // Arrange

            // Act
            List<Coords> coords = TowLinkWarning.findCoordsForTrailer(mockGame, mockTrailer, mockBoard);

            // Assert
            assertNull(coords);
        }

        @Test
        void testTractorNotDeployedYet() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);

            // Act
            List<Coords> coords = TowLinkWarning.findCoordsForTrailer(mockGame, mockTrailer, mockBoard);

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
            List<Coords> coords = TowLinkWarning.findCoordsForTrailer(mockGame, mockTrailer, mockBoard);

            // Assert
            assertEquals(1, coords.size());
        }

        @Test
        void testLargeTrailerAndTractorDeployed() {
            // Arrange
            when(mockTractor.getTowing()).thenReturn(TRAILER_ID);
            when(mockTrailer.getTowedBy()).thenReturn(TRACTOR_ID);

            when(mockTractor.isDeployed()).thenReturn(true);
            when(mockTractor.getPosition()).thenReturn(new Coords(3, 3));

            List<Coords> testCoords = null;

            try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
                compute.when(() -> Compute.stackingViolation(any(Game.class), anyInt(), any(Coords.class), anyBoolean())).thenReturn(mock(Entity.class));

                // Act
                testCoords = TowLinkWarning.findCoordsForTrailer(mockGame, mockTrailer, mockBoard);
            }

            // Assert
            assertEquals(6, testCoords.size());
        }
    }

}
