package megamek.server.totalwarfare;

import megamek.common.*;
import megamek.server.totalwarfare.Target.ArtilleryHexHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import megamek.common.force.Forces;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class ArtilleryHexHandlerTest {

    private TWGameManager gameManager;
    private Game mockGame;

    @BeforeEach
    void setUp() {
        gameManager = new TWGameManager();
        mockGame = mock(Game.class);
        when(mockGame.getAttacksVector()).thenReturn(new Vector<>());
        when(mockGame.getForces()).thenReturn(mock(Forces.class));
        gameManager.setGame(mockGame);
    }

    @Test
    void testHandleReturnsReportsForHexArtillery() {
        // Préparation
        Coords targetCoords = new Coords(3, 3);
        Targetable mockTarget = mock(Targetable.class);
        when(mockTarget.getTargetType()).thenReturn(Targetable.TYPE_HEX_ARTILLERY);
        when(mockTarget.getPosition()).thenReturn(targetCoords);

        Entity attacker = mock(Entity.class);
        when(attacker.getId()).thenReturn(42);

        Entity entityInHex = mock(Entity.class);
        when(entityInHex.getId()).thenReturn(99);
        when(entityInHex.getElevation()).thenReturn(0);

        Crew mockCrew = mock(Crew.class);
        when(mockCrew.getSize()).thenReturn(1);
        when(mockCrew.getNickname()).thenReturn("Mocky");
        when(entityInHex.getCrew()).thenReturn(mockCrew);


        Board mockBoard = mock(Board.class);
        Hex mockHex = mock(Hex.class);
        when(mockHex.terrainLevel(Terrains.BLDG_ELEV)).thenReturn(1);

        when(mockBoard.getHex(targetCoords)).thenReturn(mockHex);
        when(mockGame.getBoard()).thenReturn(mockBoard);

        Building mockBuilding = mock(Building.class);
        when(mockBoard.getBuildingAt(targetCoords)).thenReturn(mockBuilding);

        Vector<Entity> entities = new Vector<>();
        entities.add(entityInHex);
        when(mockGame.getEntitiesVector(targetCoords)).thenReturn(entities);

        ArtilleryHexHandler handler = new ArtilleryHexHandler(gameManager);

        // Action
        Vector<Report> reports = handler.handle(mockTarget, mockGame, attacker, 2, 42);

        // Vérification
        assertFalse(reports.isEmpty(), "Le rapport d'artillerie ne devrait pas être vide.");
    }
}
