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
    Game mockGame;
    Board mockBoard;
    Entity mockTractor;
    Entity mockTrailer;


    @BeforeEach
    void initialize() {
        mockTractor = mock(Entity.class);
        when(mockTractor.getId()).thenReturn(1);
        mockTrailer = mock(Entity.class);
        when(mockTrailer.getId()).thenReturn(2);
        mockGame = mock(Game.class);
        mockBoard = mock(Board.class);
        when(mockBoard.getHeight()).thenReturn(5);
        when(mockBoard.getWidth()).thenReturn(5);
        when(mockBoard.isLegalDeployment(any(Coords.class), any(Entity.class))).thenReturn(true);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getEntity(1)).thenReturn(mockTractor);
        when(mockGame.getEntity(2)).thenReturn(mockTrailer);
    }

    /**
     * @see TowLinkWarning#findCoordsForTractor(Game, Entity, Board)
     */
    @Nested
    class testFindCoordsForTractor {

        @Test
        void testNoTrailer() {
            // Arrange
            TowLinkWarning towLinkWarning = new TowLinkWarning();

            // Act
            List<Coords> coords = towLinkWarning.findCoordsForTractor(mockGame, mockTractor, mockBoard);


            // Assert
            assertNull(coords);
        }

        @Test
        void testTrailerNotDeployedYet() {
            // Arrange
            TowLinkWarning towLinkWarning = new TowLinkWarning();

            when(mockTractor.getTowing()).thenReturn(2);

            // Act
            List<Coords> coords = towLinkWarning.findCoordsForTractor(mockGame, mockTractor, mockBoard);


            // Assert
            assertEquals(25, coords.size());
        }

        @Test
        void testTrailerDeployed() {
            // Arrange
            TowLinkWarning towLinkWarning = new TowLinkWarning();

            when(mockTractor.getTowing()).thenReturn(2);
            when(mockTrailer.isDeployed()).thenReturn(true);
            when(mockTrailer.getPosition()).thenReturn(new Coords(3, 3));

            // Act
            List<Coords> coords = towLinkWarning.findCoordsForTractor(mockGame, mockTractor, mockBoard);


            // Assert
            assertEquals(7, coords.size());
        }

        void testLargeTrailerDeployed() {
            // Arrange
            TowLinkWarning towLinkWarning = new TowLinkWarning();

            when(mockTractor.getTowing()).thenReturn(2);
            when(mockTrailer.isDeployed()).thenReturn(true);
            when(mockTrailer.getPosition()).thenReturn(new Coords(3, 3));

            List<Coords> testCoords = null;

            try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
                compute.when(() -> Compute.stackingViolation(any(Game.class), anyInt(), any(Coords.class), anyBoolean())).thenReturn(mock(Entity.class));

                // Act
                testCoords = towLinkWarning.findCoordsForTractor(mockGame, mockTractor, mockBoard);
            }

            // Assert
            assertEquals(6, testCoords.size());
        }
    }

}
