/*
 * Created on Jul 27, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package megamek.common.weapons;

import megamek.common.AttackHandler;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.weapons.*;

/**
 * @author Andrew Hunter
 * A basic, simple attack handler.  May or may not work for your purposes.
 */
public class WeaponHandler implements AttackHandler {
	boolean matters;
	ToHitData toHit;
	Game game;
	WeaponAttackAction waa;
	int roll;

	
	public boolean cares(int phase) {
		if(matters && phase == Game.PHASE_FIRING) {
			return true;
		}
		return false;
	}

	
	public boolean handle(int phase) {
		if(roll>=toHit.getValue()) {
			//hits
		} else {
			//misses
		}
		
		return false;
	}
	//Among other things, basically a refactored Server#preTreatWeaponAttack
	public WeaponHandler(ToHitData t, WeaponAttackAction w, Game g) {
		toHit=t;
		waa=w;
		game=g;
		Entity ae=game.getEntity(waa.getEntityId());
		Mounted weapon = ae.getEquipment(waa.getWeaponId());
		WeaponType wtype = (WeaponType)weapon.getType();
		
        
		
		if(toHit.getValue()==TargetRoll.AUTOMATIC_FAIL || toHit.getValue()==TargetRoll.IMPOSSIBLE) {
			matters=false;
		} else {
			matters=true;
		}
		roll=Compute.d6(2);
		if(!(toHit.getValue()==TargetRoll.IMPOSSIBLE)) {

	        ae.heatBuildup += (wtype.getHeat());
		}
		weapon.setUsedThisRound(true);
		
		
	}

}
