package megamek.common;

import megamek.common.enums.GamePhase;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.verifier.TestEntity;
import megamek.server.GameManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DropShipMekLoadTest {

    @Test
    public void test() throws Exception {
        MechSummaryCache instance = MechSummaryCache.getInstance(true);
        Mech atlas = (Mech) instance.getMech("Atlas AS7-D").loadEntity();
        atlas.setId(2);
        Dropship leopard = (Dropship) instance.getMech("Leopard (2537)").loadEntity();
        leopard.setId(1);

        Game game = new Game();
        game.setPhase(GamePhase.LOUNGE);
        game.addPlayer(0, new Player(0, "TestPlayer"));
        game.addEntity(atlas);
        game.addEntity(leopard);

        GameManager gm = mock(GameManager.class);
        doNothing().when(gm).entityUpdate(anyInt());
        when(gm.getGame()).thenReturn(game);
        doCallRealMethod().when(gm).setGame(any(Game.class));
        doCallRealMethod().when(gm).handlePacket(anyInt(), any(Packet.class));
        gm.setGame(game);

        Packet packet = new Packet(PacketCommand.ENTITY_LOAD, atlas.getId(), leopard.getId(), -1);
        gm.handlePacket(0, packet);

        doAssertions(leopard, atlas);

        leopard.setAltitude(0);

        doAssertions(leopard, atlas);
    }

    private void doAssertions(Entity leopard, Entity atlas) {
        StringBuffer errors = new StringBuffer();
        assertTrue(isValid(leopard, errors), "Leopard is not valid after loading Atlas, errors: " + errors);
        assertTrue(isValid(atlas, errors), "Atlas is not valid after loading onto Leopard, errors: " + errors);
        assertEquals(atlas.getTransportId(), leopard.getId(),
                "Carrier ID " + atlas.getTransportId() + " wrong, should be " + leopard.getId());
        assertEquals(1, leopard.getLoadedUnits().size(),
                "Loaded unit list size" + leopard.getLoadedUnits().size() + " wrong, should be 1");
        assertNull(atlas.getPosition(), "Loaded Atlas position is " + atlas.getPosition() + ", should be null");
    }

    private boolean isValid(Entity entity, StringBuffer errors) {
        return TestEntity.getEntityVerifier(entity).correctEntity(errors);
    }
}
