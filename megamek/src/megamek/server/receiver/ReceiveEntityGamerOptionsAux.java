package megamek.server.receiver;

import megamek.common.net.Packet;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.server.Processor.MovementProcessor;
import megamek.server.Server;
import megamek.server.ServerBoardHelper;

import java.util.Enumeration;
import java.util.Vector;

public class ReceiveEntityGamerOptionsAux {
    /**
     * Performs the additional processing of the received options after the the
     * <code>receiveGameOptions<code> done its job; should be called after
     * <code>receiveGameOptions<code> only if the <code>receiveGameOptions<code>
     * returned <code>true</code>
     *
     * @param server
     * @param packet the packet to be processed
     */
    public static void receiveGameOptionsAux(Server server, Packet packet) {
        for (Enumeration<?> i = ((Vector<?>) packet.getObject(1)).elements(); i.hasMoreElements(); ) {
            IBasicOption option = (IBasicOption) i.nextElement();
            IOption originalOption = server.getGame().getOptions().getOption(option.getName());
            if (originalOption != null) {
                if ("maps_include_subdir".equals(originalOption.getName())) {
                    server.mapSettings.setBoardsAvailableVector(ServerBoardHelper.scanForBoards(server.mapSettings));
                    server.mapSettings.removeUnavailable();
                    server.mapSettings.setNullBoards(MovementProcessor.DEFAULT_BOARD);
                    server.send(server.createMapSettingsPacket());
                }
            }
        }

    }
}
