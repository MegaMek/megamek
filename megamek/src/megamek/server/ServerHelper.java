package megamek.server;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Terrains;
import megamek.common.options.OptionsConstants;

/**
 * This class contains computations carried out by the Server class.
 * Methods put in here should be static and self-contained. 
 * @author NickAragua
 *
 */
public class ServerHelper {
    /**
     * Determines if the given entity is an infantry unit in the given hex is "in the open" 
     * (and thus subject to double damage from attacks)
     * @param te Target entity.
     * @param te_hex Hex where target entity is located.
     * @param game Game being played.
     * @param isPlatoon Whether the target unit is a platoon.
     * @param ammoExplosion Whether we're considering a "big boom" ammo explosion from tacops.
     * @param ignoreInfantryDoubleDamage Whether we should ignore double damage to infantry.
     * @return
     */
    public static boolean infantryInOpen(Entity te, IHex te_hex, IGame game, 
            boolean isPlatoon, boolean ammoExplosion, boolean ignoreInfantryDoubleDamage) {
        
        if (isPlatoon && !te.isDestroyed() && !te.isDoomed() && !ignoreInfantryDoubleDamage
                && (((Infantry) te).getDugIn() != Infantry.DUG_IN_COMPLETE)
                && !te.hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
            te_hex = game.getBoard().getHex(te.getPosition());
            if ((te_hex != null) && !te_hex.containsTerrain(Terrains.WOODS) && !te_hex.containsTerrain(Terrains.JUNGLE)
                    && !te_hex.containsTerrain(Terrains.ROUGH) && !te_hex.containsTerrain(Terrains.RUBBLE)
                    && !te_hex.containsTerrain(Terrains.SWAMP) && !te_hex.containsTerrain(Terrains.BUILDING)
                    && !te_hex.containsTerrain(Terrains.FUEL_TANK) && !te_hex.containsTerrain(Terrains.FORTIFIED)
                    && (!te.hasAbility(OptionsConstants.INFANTRY_URBAN_GUERRILLA))
                    && (!te_hex.containsTerrain(Terrains.PAVEMENT) || !te_hex.containsTerrain(Terrains.ROAD))
                    && !ammoExplosion) {
                return true;
            }
        }
        
        return false;
    }
}
