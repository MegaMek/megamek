package megamek.server.resolver;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.Weapon;
import megamek.server.Server;

import java.util.Enumeration;

public class ResolveOnlyWeaponAttacks {
    /**
     * Called during the fire phase to resolve all (and only) weapon attacks
     * @param server
     * @param game
     */
    public static void resolveOnlyWeaponAttacks(Server server, Game game) {
        // loop through received attack actions, getting attack handlers
        for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
            EntityAction ea = i.nextElement();
            if (ea instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) ea;
                Entity ae = game.getEntity(waa.getEntityId());
                Mounted m = ae.getEquipment(waa.getWeaponId());
                Weapon w = (Weapon) m.getType();
                // Track attacks original target, for things like swarm LRMs
                waa.setOriginalTargetId(waa.getTargetId());
                waa.setOriginalTargetType(waa.getTargetType());
                AttackHandler ah = w.fire(waa, game, server);
                if (ah != null) {
                    ah.setStrafing(waa.isStrafing());
                    ah.setStrafingFirstShot(waa.isStrafingFirstShot());
                    game.addAttack(ah);
                }
            }
        }
        // and clear the attacks Vector
        game.resetActions();
    }
}
