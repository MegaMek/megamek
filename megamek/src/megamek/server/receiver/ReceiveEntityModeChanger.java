package megamek.server.receiver;

import megamek.common.*;
import megamek.common.net.Packet;
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;

public class ReceiveEntityModeChanger {
    /**
     * receive and process an entity mode change packet
     *
     * @param server
     * @param c the packet to be processed
     * @param connIndex the id for connection that received the packet.
     */
    public static void receiveEntityModeChange(Server server, Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        int equipId = c.getIntValue(1);
        int mode = c.getIntValue(2);
        Entity e = server.getGame().getEntity(entityId);
        if (!Objects.equals(e.getOwner(), server.getPlayer(connIndex))) {
            return;
        }
        Mounted m = e.getEquipment(equipId);

        if (m == null) {
            return;
        }

        try {
            // Check for BA dumping body mounted missile launchers
            if ((e instanceof BattleArmor) && (!m.isMissing())
                    && m.isBodyMounted()
                    && m.getType().hasFlag(WeaponType.F_MISSILE)
                    && (m.getLinked() != null)
                    && (m.getLinked().getUsableShotsLeft() > 0)
                    && (mode <= 0)) {
                m.setPendingDump(mode == -1);
            // a mode change for ammo means dumping or hot loading
            } else if ((m.getType() instanceof AmmoType)
                && !m.getType().hasInstantModeSwitch() && (mode < 0
                            || mode == 0 && m.isPendingDump())) {
                m.setPendingDump(mode == -1);
            } else if ((m.getType() instanceof WeaponType) && m.isDWPMounted()
                       && (mode <= 0)) {
                m.setPendingDump(mode == -1);
            } else {
                if (!m.setMode(mode)) {
                    String message = e.getShortName() + ": " + m.getName() + ": " + e.getLocationName(m.getLocation())
                            + " trying to compensate";
                    LogManager.getLogger().error(message);
                    server.sendServerChat(message);
                    e.setGameOptions();

                    if (!m.setMode(mode)) {
                        message = e.getShortName() + ": " + m.getName() + ": " + e.getLocationName(m.getLocation())
                                + " unable to compensate";
                        LogManager.getLogger().error(message);
                        server.sendServerChat(message);
                    }

                }
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

    }
}
