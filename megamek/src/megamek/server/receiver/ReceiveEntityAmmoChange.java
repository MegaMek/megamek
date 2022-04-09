package megamek.server.receiver;

import megamek.common.*;
import megamek.common.net.Packet;
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;

public class ReceiveEntityAmmoChange {
    /**
     * Receive a packet that contains an Entity ammo change
     *
     * @param server
     * @param c the packet to be processed
     * @param connIndex the id for connection that received the packet.
     */
    public static void receiveEntityAmmoChange(Server server, Packet c, int connIndex) {
        int entityId = c.getIntValue(0);
        int weaponId = c.getIntValue(1);
        int ammoId = c.getIntValue(2);
        Entity e = server.getGame().getEntity(entityId);

        // Did we receive a request for a valid Entity?
        if (null == e) {
            LogManager.getLogger().error("Could not find entity# " + entityId);
            return;
        }
        Player player = server.getPlayer(connIndex);
        if ((null != player) && (!Objects.equals(e.getOwner(), player))) {
            LogManager.getLogger().error("Player " + player.getName() + " does not own the entity " + e.getDisplayName());
            return;
        }

        // Make sure that the entity has the given equipment.
        Mounted mWeap = e.getEquipment(weaponId);
        Mounted mAmmo = e.getEquipment(ammoId);
        if (null == mAmmo) {
            LogManager.getLogger().error("Entity " + e.getDisplayName() + " does not have ammo #" + ammoId);
            return;
        }
        if (!(mAmmo.getType() instanceof AmmoType)) {
            LogManager.getLogger().error("Item #" + ammoId + " of entity " + e.getDisplayName()
                    + " is a " + mAmmo.getName() + " and not ammo.");
            return;
        }
        if (null == mWeap) {
            LogManager.getLogger().error("Entity " + e.getDisplayName() + " does not have weapon #" + weaponId);
            return;
        }
        if (!(mWeap.getType() instanceof WeaponType)) {
            LogManager.getLogger().error("Item #" + weaponId + " of entity " + e.getDisplayName()
                    + " is a " + mWeap.getName() + " and not a weapon.");
            return;
        }
        if (((WeaponType) mWeap.getType()).getAmmoType() == AmmoType.T_NA) {
            LogManager.getLogger().error("Item #" + weaponId + " of entity " + e.getDisplayName()
                    + " is a " + mWeap.getName() + " and does not use ammo.");
            return;
        }
        if (mWeap.getType().hasFlag(WeaponType.F_ONESHOT)
                && !mWeap.getType().hasFlag(WeaponType.F_DOUBLE_ONESHOT)) {
            LogManager.getLogger().error("Item #" + weaponId + " of entity " + e.getDisplayName()
                    + " is a " + mWeap.getName() + " and cannot use external ammo.");
            return;
        }

        // Load the weapon.
        e.loadWeapon(mWeap, mAmmo);
    }
}
