/*
 * Created on Jul 27, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package megamek.common.weapons;

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
	WeaponType wtype;
	Mounted weapon;
	Entity ae;
	int roll;
	Targetable target;
	Entity entityTarget;

	public int getAttackerId() {
		return ae.getId();
	}
	public boolean cares(int phase) {
		if(matters && phase == Game.PHASE_FIRING) {
			return true;
		}
		return false;
	}

	
	public boolean handle(int phase) {
		StringBuffer phaseReport=game.getPhaseReport();
		int nShots = weapon.howManyShots();
		 final boolean targetInBuilding =
	          Compute.isInBuilding(game, entityTarget);

	      // Which building takes the damage?
	      Building bldg = game.board.getBuildingAt(target.getPosition());


	      // Report weapon attack and its to-hit value.
	      phaseReport.append("    ").append(wtype.getName()).append(" at ").append(
	          target.getDisplayName());
	      if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
	        phaseReport.append(", but the shot is impossible (").append(toHit.getDesc()).
	            append(")\n");
	        return false;
	      }
	      else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
	        phaseReport.append(", the shot is an automatic miss (").append(toHit.
	            getDesc()).append("), ");
	      }
	      else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
	        phaseReport.append(", the shot is an automatic hit (").append(toHit.
	            getDesc()).append("), ");
	      }
	      else {
	        phaseReport.append("; needs ").append(toHit.getValue()).append(", ");
	      }

	      // dice have been rolled, thanks
	      phaseReport.append("rolls ").append(roll).append(" : ");


	      // do we hit?
	      boolean bMissed = roll < toHit.getValue();


	        if (bMissed) {
	            // Report the miss.
	            
	                phaseReport.append("misses.\n");


	            // Shots that miss an entity can set fires.
	            // Infernos always set fires.  Otherwise
	            // Buildings can't be accidentally ignited,
	            // and some weapons can't ignite fires.
	            if ( entityTarget != null &&
	                     ( bldg == null &&
	                           wtype.getFireTN() != TargetRoll.IMPOSSIBLE ) ) {
	                tryIgniteHex(target.getPosition(), false, 11);
	            }

	            // BMRr, pg. 51: "All shots that were aimed at a target inside
	            // a building and miss do full damage to the building instead."
	            if ( !targetInBuilding ) {
	                return false;
	            }
	        }

	       

		// yeech.  handle damage. . different weapons do this in very different ways
	        int hits = 1, nCluster = 1, nSalvoBonus = 0;
	        int nDamPerHit = wtype.getDamage();
	        boolean bSalvo = false;
	        // ecm check is heavy, so only do it once
	        boolean bCheckedECM = false;
	        boolean bECMAffected = false;
	        boolean bMekStealthActive = false;
	        String sSalvoType = " shot(s) ";
	        boolean bAllShotsHit = false;

	        // All shots fired by a Streak SRM weapon, during
	        // a Mech Swarm hit, or at an adjacent building.
	        if ( ae.getSwarmTargetId() == waa.getTargetId() ) {
	            bAllShotsHit = true;
	        } 
	        if (nShots > 1) {
	            // this should handle multiple attacks from ultra and rotary ACs
	            bSalvo = true;
	            hits = nShots;
	            if ( !bAllShotsHit ) {
	                hits = Compute.missilesHit( hits );
	            }
	        }
	        // Some weapons double the number of hits scored.
	        if ( wtype.hasFlag(WeaponType.F_DOUBLE_HITS) ) {
	            hits *= 2;
	        }

	        // We've calculated how many hits.  At this point, any missed
	        // shots damage the building instead of the target.
	        if ( bMissed ) {
	            if ( targetInBuilding && bldg != null ) {


	                // Damage the building in one big lump.
	                

	                    // Only report if damage was done to the building.
	                    int toBldg = hits * nDamPerHit;
	                    if ( toBldg > 0 ) {
	                        phaseReport.append( "        " )
	                            .append( damageBuilding( bldg, toBldg ) )
	                            .append( "\n" );
	                    }

	            } // End rounds-hit

	            } // End missed-target-in-building
	            return false;

	         // End missed-target

	        // The building shields all units from a certain amount of damage.
	        // The amount is based upon the building's CF at the phase's start.
	        int bldgAbsorbs = 0;
	        if ( targetInBuilding && bldg != null ) {
	            bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
	        }

	        // All attacks (except from infantry weapons)
	        // during Mek Swarms hit the same location.
	        if (ae.getSwarmTargetId() == waa.getTargetId() ) {
	            nCluster = hits;
	        }

	        // Report the number of hits.  Infernos have their own reporting
	        else if (bSalvo) {
	            phaseReport.append( hits ).append( sSalvoType ).append( "hit" )
	                .append( toHit.getTableDesc() );
	            if (bECMAffected) {
	                phaseReport.append(" (ECM prevents bonus)");
	            }
	            else if (bMekStealthActive) {
	                phaseReport.append(" (active Stealth prevents bonus)");
	            }
	            if (nSalvoBonus > 0) {
	                phaseReport.append(" (w/ +")
	                    .append(nSalvoBonus)
	                    .append(" bonus)");
	            }
	            phaseReport.append(".");

	            
	        }

	        // Make sure the player knows when his attack causes no damage.
	        if ( hits == 0 ) {
	            phaseReport.append( "attack deals zero damage.\n" );
	        }

	        // for each cluster of hits, do a chunk of damage
	        while (hits > 0) {
	            int nDamage;
	            if (entityTarget != null) {
	                 HitData hit = entityTarget.rollHitLocation
	                     ( toHit.getHitTable(),
	                       toHit.getSideTable(),
	                       waa.getAimedLocation(),
	                       waa.getAimingMode() );


	                // Each hit in the salvo get's its own hit location.
	                if (!bSalvo) {
	                    phaseReport.append("hits" ).append( toHit.getTableDesc() ).append( " " ).
	                            append( entityTarget.getLocationAbbr(hit));
	                    if (hit.hitAimedLocation()) {
	                    	phaseReport.append("(hit aimed location)");
	                    }
	                }

	                // Special weapons do criticals instead of damage.
	                if ( nDamPerHit == WeaponType.DAMAGE_SPECIAL ) {
	                    // Do criticals.
	                    String specialDamage = criticalEntity( entityTarget, hit.getLocation() );

	                    // Replace "no effect" results with 4 points of damage.
	                    if ( specialDamage.endsWith(" no effect.") ) {
	                        // ASSUMPTION: buildings CAN'T absorb *this* damage.
	                        specialDamage = damageEntity(entityTarget, hit, 4);
	                    }
	                    else {
	                        specialDamage = "\n" + specialDamage;
	                    }

	                    // Report the result
	                    phaseReport.append( specialDamage );
	                }
	                else {
	                    // Resolve damage normally.
	                    nDamage = nDamPerHit * Math.min(nCluster, hits);

	                    // A building may be damaged, even if the squad is not.
	                    if ( bldgAbsorbs > 0 ) {
	                        int toBldg = Math.min( bldgAbsorbs, nDamage );
	                        nDamage -= toBldg;
	                        phaseReport.append( "\n        " )
	                            .append( damageBuilding( bldg, toBldg ) );
	                    }

	                    // A building may absorb the entire shot.
	                    if ( nDamage == 0 ) {
	                        phaseReport.append( "\n        " )
	                            .append( entityTarget.getDisplayName() )
	                            .append( " suffers no damage." );
	                    } 
	                }
	                hits -= nCluster;
	            }
	        } // Handle the next cluster.

	        phaseReport.append("\n");
		return false;
	}
	//Among other things, basically a refactored Server#preTreatWeaponAttack
	public WeaponHandler(ToHitData t, WeaponAttackAction w, Game g) {
		toHit=t;
		waa=w;
		game=g;
		ae=game.getEntity(waa.getEntityId());
		weapon = ae.getEquipment(waa.getWeaponId());
		wtype = (WeaponType)weapon.getType();
		target = game.getTarget(waa.getTargetType(),waa.getTargetId());
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
	        entityTarget = (Entity) target;
	    }
		
        
		
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
