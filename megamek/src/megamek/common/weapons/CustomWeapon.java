package megamek.common.weapons;

import megamek.common.IGame;
import megamek.common.ToHitData;
import megamek.common.WeaponLoader;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.logging.LogLevel;
import megamek.common.logging.Logger;
import megamek.server.Server;

/**
 * @author Wong Wing Lun (aka luiges90)
 *         Class for all custom weapons with weapon handler set.
 */
public class CustomWeapon extends Weapon {

    private static final long serialVersionUID = 1L;

    private static Logger logger = new Logger();
    private Class<?> handler;

    public CustomWeapon(String handler) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        Class<?> type;
        try {
            type = Class.forName("megamek.common.weapons." + handler.trim());
        } catch (ClassNotFoundException ex) {
            try {
                type = Class.forName("megamek.common.weapons.battlearmor."
                        + handler.trim());
            } catch (ClassNotFoundException ex2) {
                type = Class.forName("megamek.common.weapons.infantry."
                        + handler.trim());
            }
        }

        if (!AttackHandler.class.isAssignableFrom(type)) {
            throw new ClassCastException(
                    "Type " + type.getSimpleName() + " is not an AttackHandler.");
        }

        this.handler = type;
    }

    @Override
    public int getDamage(int range) {
        if (damage == WeaponType.DAMAGE_VARIABLE) {
            if (range <= shortRange) {
                return damageShort;
            }

            if (range <= mediumRange) {
                return damageMedium;
            }

            return damageLong;
        } else {
            return damage;
        }
    }

    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        try {
            return (AttackHandler) handler.getConstructor(ToHitData.class,
                    WeaponAttackAction.class,
                    IGame.class,
                    Server.class).newInstance(toHit, waa, game, server);
        } catch (Exception e) {
            logger.log(WeaponLoader.class,
                    "getCorrectHandler(ToHitData, WeaponAttackAction, IGame, Server)",
                    LogLevel.ERROR,
                    "Could not create an isntance of " + this.handler.getName());
        }
        return null;
    }

}
