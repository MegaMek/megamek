package megamek.server.receiver;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.net.Packet;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

import java.util.Enumeration;
import java.util.Vector;

public class ReceiveEntityGamerOptions {
    /**
     * Sets game options, providing that the player has specified the password
     * correctly.
     *
     * @return true if any options have been successfully changed.
     */
    public static boolean receiveGameOptions(Server server, Packet packet, int connId) {
        Player player = server.getGame().getPlayer(connId);
        // Check player
        if (null == player) {
            LogManager.getLogger().error("Server does not recognize player at connection " + connId);
            return false;
        }

        // check password
        if ((server.password != null) && (server.password.length() > 0) && !server.password.equals(packet.getObject(0))) {
            server.sendServerChat(connId, "The password you specified to change game options is incorrect.");
            return false;
        }

        if (server.getGame().getPhase().isDuringOrAfter(GamePhase.DEPLOYMENT)) {
            return false;
        }

        int changed = 0;

        for (Enumeration<?> i = ((Vector<?>) packet.getObject(1)).elements(); i.hasMoreElements(); ) {
            IBasicOption option = (IBasicOption) i.nextElement();
            IOption originalOption = server.getGame().getOptions().getOption(option.getName());

            if (originalOption == null) {
                continue;
            }

            String message = "Player " + player.getName() + " changed option \"" +
                    originalOption.getDisplayableName() + "\" to " + option.getValue().toString() + '.';
            server.sendServerChat(message);
            originalOption.setValue(option.getValue());
            changed++;
        }

        // Set proper RNG
        Compute.setRNG(server.getGame().getOptions().intOption(OptionsConstants.BASE_RNG_TYPE));

        if (changed > 0) {
            for (Entity en : server.getGame().getEntitiesVector()) {
                en.setGameOptions();
            }
            server.entityAllUpdate();
            return true;
        }
        return false;
    }
}
