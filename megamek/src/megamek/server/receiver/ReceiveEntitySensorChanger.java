package megamek.server.receiver;

import megamek.common.Entity;
import megamek.common.net.Packet;
import megamek.server.Server;

public class ReceiveEntitySensorChanger {
    /**
     * Receive and process an Entity Sensor Change Packet
     * @param server
     * @param c the packet to be processed
     */
    public static void receiveEntitySensorChange(Server server, Packet c) {
        int entityId = c.getIntValue(0);
        int sensorId = c.getIntValue(1);
        Entity e = server.getGame().getEntity(entityId);
        e.setNextSensor(e.getSensors().elementAt(sensorId));
    }
}
