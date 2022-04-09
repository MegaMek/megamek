package megamek.server.receiver;

import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.IBomber;
import megamek.common.enums.GamePhase;
import megamek.common.net.Packet;
import megamek.server.Server;
import megamek.server.ServerLobbyHelper;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static megamek.common.net.Packet.COMMAND_ENTITY_MULTIUPDATE;

public class ReceiveEntitySquadonAdder {
    /**
     * adds a squadron to the game
     * @param server
     * @param c the packet to be processed
     */
    @SuppressWarnings("unchecked")
    public static void receiveSquadronAdd(Server server, Packet c) {

        final FighterSquadron fs = (FighterSquadron) c.getObject(0);
        final Collection<Integer> fighters = (Collection<Integer>) c.getObject(1);
        if (fighters.isEmpty()) {
            return;
        }
        // Only assign an entity ID when the client hasn't.
        if (Entity.NONE == fs.getId()) {
            fs.setId(server.getFreeEntityId());
        }
        server.getGame().addEntity(fs);
        var formerCarriers = new HashSet<Entity>();

        for (int id : fighters) {
            Entity fighter = server.getGame().getEntity(id);
            if (null != fighter) {
                formerCarriers.addAll(ServerLobbyHelper.lobbyUnload(server.getGame(), List.of(fighter)));
                fs.load(fighter, false);
                fs.autoSetMaxBombPoints();
                fighter.setTransportId(fs.getId());
                // If this is the lounge, we want to configure bombs
                if (server.getGame().getPhase() == GamePhase.LOUNGE) {
                    ((IBomber) fighter).setBombChoices(fs.getBombChoices());
                }
                server.entityUpdate(fighter.getId());
            }
        }
        if (!formerCarriers.isEmpty()) {
            server.send(new Packet(COMMAND_ENTITY_MULTIUPDATE, formerCarriers));
        }
        server.send(server.createAddEntityPacket(fs.getId()));

    }
}
