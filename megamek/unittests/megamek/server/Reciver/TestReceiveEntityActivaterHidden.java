package megamek.server.Reciver;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.net.Packet;
import megamek.server.Server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static megamek.server.receiver.ReceiveEntityActivaterHidden.receiveEntityActivateHidden;

@RunWith(value = JUnit4.class)
public class TestReceiveEntityActivaterHidden {


    @Test
    public void WhenReceiveEntityActivaterHiddenIsCAlled() {
        Server testServer = Mockito.mock(Server.class);
        Game  game = Mockito.mock(Game.class);
        Packet packet = Mockito.mock(Packet.class);
        int connectionId = 0 ;
        Entity e = Mockito.mock(Entity.class);
        Mockito.when(game.getEntity(Mockito.anyInt())).thenReturn(e);
        Mockito.when(testServer.getGame()).thenReturn(game);


        receiveEntityActivateHidden(testServer,packet,connectionId);
        Mockito.verify(testServer,Mockito.times(1)).entityUpdate(Mockito.anyInt());

    }
}
