/*
 * Created on Jul 27, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.server.Server;
import megamek.common.actions.*;
import megamek.common.weapons.*;

/**
 * @author Andrew Hunter
 * A basic, simple attack handler.  May or may not work for any particular weapon; must be overloaded to support
 * special rules.  
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
	Server server;

	public int getAttackerId() {
		return ae.getId();
	}
	public boolean cares(int phase) {
		if(phase == Game.PHASE_FIRING) {
			return true;
		}
		return false;
	}
	public void setServer(Server server) {
		this.server=server;
	}

	
	public boolean handle(int phase) {
		if(!this.cares(phase)) {
			return true;
		}
		Entity entityTarget=(target.getTargetType()==Targetable.TYPE_ENTITY) ? (Entity)target : null;
		StringBuffer phaseReport=game.getPhaseReport();
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
	            // Buildings can't be accidentally ignited,
	            // and some weapons can't ignite fires.
	            if ( entityTarget != null &&
	                     ( bldg == null &&
	                           wtype.getFireTN() != TargetRoll.IMPOSSIBLE ) ) {
	                server.tryIgniteHex(target.getPosition(), false, 11);
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
	        String sSalvoType = " shot(s) ";
	        boolean bAllShotsHit = false;
	       

	        // We've calculated how many hits.  At this point, any missed
	        // shots damage the building instead of the target.
	        if ( bMissed ) {
	            if ( targetInBuilding && bldg != null ) {


	                // Damage the building in one big lump.
	                

	                    // Only report if damage was done to the building.
	                    int toBldg = hits * nDamPerHit;
	                    if ( toBldg > 0 ) {
	                        phaseReport.append( "        " )
	                            .append( server.damageBuilding( bldg, toBldg ) )
	                            .append( "\n" );
	                    }

	             // End rounds-hit

	            } // End missed-target-in-building
	            return false;

	        } // End missed-target

	        // The building shields all units from a certain amount of damage.
	        // The amount is based upon the building's CF at the phase's start.
	        int bldgAbsorbs = 0;
	        if ( targetInBuilding && bldg != null ) {
	            bldgAbsorbs = (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
	        }



	        // Make sure the player knows when his attack causes no damage.
	        if ( hits == 0 ) {
	            phaseReport.append( "attack deals zero damage.\n" );
	        }

	        // for each cluster of hits, do a chunk of damage
	        while (hits > 0) {

	            int nDamage;
//	          targeting a hex for igniting
	            if( target.getTargetType() == Targetable.TYPE_HEX_IGNITE ||
	                target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) {
	                if ( !bSalvo ) {
	                    phaseReport.append("hits!");
	                }
	                // We handle Inferno rounds above.
	                int tn = wtype.getFireTN();
	                if (tn != TargetRoll.IMPOSSIBLE) {
	                    if ( bldg != null ) {
	                        tn += bldg.getType() - 1;
	                    }
	                    phaseReport.append( "\n" );
	                    server.tryIgniteHex( target.getPosition(), false, tn, true );
	                }
	                return false;
	            }

	            // targeting a hex for clearing
	            if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {

	                nDamage = nDamPerHit * hits;
	                
	                phaseReport.append("hits!");
	                phaseReport.append("    Terrain takes " ).append( nDamage ).append( " damage.\n");

	                // Any clear attempt can result in accidental ignition, even
	                // weapons that can't normally start fires.  that's weird.
	                // Buildings can't be accidentally ignited.
	                if ( bldg != null &&
	                     server.tryIgniteHex(target.getPosition(), false, 9) ) {
	                    return false;
	                }

	                int tn = 14 - nDamage;
	                server.tryClearHex(target.getPosition(), tn);

	                return false;
	            }
//	          Targeting a building.
	            if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {

	                // The building takes the full brunt of the attack.
	                nDamage = nDamPerHit * hits;
	                if ( !bSalvo ) {
	                    phaseReport.append( "hits." );
	                }
	                phaseReport.append( "\n        " )
	                    .append( server.damageBuilding( bldg, nDamage ) )
	                    .append( "\n" );

	                // Damage any infantry in the hex.
	                server.damageInfantryIn( bldg, nDamage );

	                // And we're done!
	                return false;
	            }
	            if (entityTarget != null) {
	            	
	                 HitData hit = entityTarget.rollHitLocation
	                     ( toHit.getHitTable(),
	                       toHit.getSideTable(),
	                       waa.getAimedLocation(),
	                       waa.getAimingMode() );


	                // Each hit in the salvo get's its own hit location.
	                    phaseReport.append("hits" ).append( toHit.getTableDesc() ).append( " " ).
	                            append( entityTarget.getLocationAbbr(hit));
	                    if (hit.hitAimedLocation()) {
	                    	phaseReport.append("(hit aimed location)");
	                    }
	                
	                    // Resolve damage normally.
	                    nDamage = nDamPerHit * Math.min(nCluster, hits);

	                    // A building may be damaged, even if the squad is not.
	                    if ( bldgAbsorbs > 0 ) {
	                        int toBldg = Math.min( bldgAbsorbs, nDamage );
	                        nDamage -= toBldg;
	                        phaseReport.append( "\n        " )
	                            .append( server.damageBuilding( bldg, toBldg ) );
	                    }

	                    // A building may absorb the entire shot.
	                    if ( nDamage == 0 ) {
	                        phaseReport.append( "\n        " )
	                            .append( entityTarget.getDisplayName() )
	                            .append( " suffers no damage." );
	                    } else {
	                    	if(server==null) { System.out.println("serverN");}
	                    	if(phaseReport==null) { System.out.println("phaseReportN");}
	                    	if(entityTarget==null) { System.out.println("entityTargetN");}	                    	
	                    	phaseReport.append( server.damageEntity(entityTarget, hit, nDamage) );
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
