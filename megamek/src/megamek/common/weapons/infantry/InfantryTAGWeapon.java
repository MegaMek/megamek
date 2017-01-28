/**
 * 
 */
package megamek.common.weapons.infantry;

import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.TAGHandler;
import megamek.server.Server;

/**
 * TAG for conventional infantry. Rules not found in TacOps 2nd printing are in this forum post:
 * http://bg.battletech.com/forums/index.php?topic=5902.0
 * 
 * @author Neoancient
 *
 */
public class InfantryTAGWeapon extends InfantryWeapon {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4986981464279987117L;

	public InfantryTAGWeapon() {
        super();
        flags = flags.andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON)
        		.andNot(F_TANK_WEAPON).andNot(F_BA_WEAPON).andNot(F_PROTO_WEAPON)
                .or(F_TAG).or(F_NO_FIRES).or(F_INF_ENCUMBER);

        techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        name = "Infantry TAG";
        setInternalName("InfantryTAG");
        addLookupName("Infantry TAG");
        damage = 0;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        cost = 40000;
        introDate = 2600;
        extinctDate = 2835;
        reintroDate = 3037;
        techLevel.put(2600, techLevel.get(3071));
        availRating = new int[] { RATING_F, RATING_X, RATING_E };
        techRating = RATING_E;
	}

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new TAGHandler(toHit, waa, game, server);
    }
}
