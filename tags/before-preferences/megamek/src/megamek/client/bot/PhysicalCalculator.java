/*
 * MegaMek - Copyright (C) 2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.bot;

import java.util.Enumeration;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.Terrain;
import megamek.common.ToHitData;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PunchAttackAction;

public class PhysicalCalculator {

    PhysicalOption calculatePhysicalTurn(TestBot bot) {
        int entNum = bot.game.getFirstEntityNum();
        int first = entNum;
        do {
            // take the first entity that can do an attack
            Entity en = bot.game.getEntity(entNum);
            CEntity cen = bot.centities.get(en);
            PhysicalOption bestAttack = getBestPhysical(en, bot.game);
            if (bestAttack != null) {
                if (bestAttack.type == PhysicalOption.KICK_LEFT || bestAttack.type == PhysicalOption.KICK_RIGHT) {
                    int side =
                        CEntity.getThreatHitArc(
                                                bestAttack.target.getPosition(),
                                                bestAttack.target.getFacing(),
                                                en.getPosition());
                    double odds =
                        1.0
                        - (double) Compute.oddsAbove(
                                                     KickAttackAction
                                                     .toHit(bot.game, entNum, bestAttack.target, bestAttack.type - 3)
                                                     .getValue())
                        / 100;

                    // Meks can kick Vehicles and Infantry, too!
                    double mod = 1.0;
                    if (bestAttack.target instanceof Mech) {
                        double llarmor =
                            bestAttack.target.getArmor(Mech.LOC_LLEG) / bestAttack.target.getOArmor(Mech.LOC_LLEG);
                        double rlarmor =
                            bestAttack.target.getArmor(Mech.LOC_RLEG) / bestAttack.target.getOArmor(Mech.LOC_RLEG);
                        switch (side) {
                        case ToHitData.SIDE_FRONT :
                            mod = (llarmor + rlarmor) / 2;
                            break;
                        case ToHitData.SIDE_LEFT :
                            mod = llarmor;
                            break;
                        case ToHitData.SIDE_RIGHT :
                            mod = rlarmor;
                            break;
                        }
                    } else if (bestAttack.target instanceof Infantry) {
                        mod = 0.0;
                    } else if (bestAttack.target instanceof Tank) {
                        switch (side) {
                        case ToHitData.SIDE_FRONT :
                            mod =
                                bestAttack.target.getArmor(Tank.LOC_FRONT)
                                / bestAttack.target.getOArmor(Tank.LOC_FRONT);
                            break;
                        case ToHitData.SIDE_LEFT :
                            mod =
                                bestAttack.target.getArmor(Tank.LOC_LEFT)
                                / bestAttack.target.getOArmor(Tank.LOC_LEFT);
                            break;
                        case ToHitData.SIDE_RIGHT :
                            mod =
                                bestAttack.target.getArmor(Tank.LOC_RIGHT)
                                / bestAttack.target.getOArmor(Tank.LOC_RIGHT);
                            break;
                        case ToHitData.SIDE_REAR :
                            mod =
                                bestAttack.target.getArmor(Tank.LOC_REAR)
                                / bestAttack.target.getOArmor(Tank.LOC_REAR);
                            break;
                        }
                    }
                    double damage = 2 / (1 + mod) * bestAttack.expectedDmg;
                    double threat = .2 * en.getWeight() * odds * (1 - cen.base_psr_odds);
                    //check for head kick
                    Hex h = bot.game.board.getHex(bestAttack.target.getPosition());
                    Hex h1 = bot.game.board.getHex(en.getPosition());
                    if (h1.getElevation() > h.getElevation()) {
                        damage *= 2;
                    }
                    Enumeration e = bot.game.getEntities();
                    double temp_threat = 0;
                    int number = 0;
                    while (e.hasMoreElements()) {
                        Entity enemy = (Entity) e.nextElement();

                        if (null == enemy.getPosition())
                            continue;

                        if (!enemy.isProne() && enemy.getPosition().distance(en.getPosition()) < 3) {
                            if (enemy.isEnemyOf(en)) {
                                number++;
                            } else {
                                number--;
                            }
                        }
                    }
                    if (number > 0) {
                        threat += number * temp_threat;
                    }
                    if (!((damage > threat && !(cen.overall_armor_percent > .8 && odds > .9))
                          || (odds < .5 && cen.base_psr_odds > .5))) {
                        boolean left = false;
                        boolean right = false;
                        ToHitData toHit =
                            PunchAttackAction.toHit(bot.game, en.getId(), bestAttack.target, PunchAttackAction.LEFT);
                        if (toHit.getValue() != ToHitData.IMPOSSIBLE) {
                            left = true;
                        }
                        toHit =
                            PunchAttackAction.toHit(bot.game, en.getId(), bestAttack.target, PunchAttackAction.RIGHT);
                        if (toHit.getValue() != ToHitData.IMPOSSIBLE) {
                            right = true;
                        }
                        if (left) {
                            if (right) {
                                bestAttack.type = PhysicalOption.PUNCH_BOTH;
                            } else {
                                bestAttack.type = PhysicalOption.PUNCH_LEFT;
                            }
                        } else if (right) {
                            bestAttack.type = PhysicalOption.PUNCH_RIGHT;
                        } else {
                            return new PhysicalOption(en);
                        }
                    }
                }
                return bestAttack;

            } // End no-attack
            entNum = bot.game.getNextEntityNum(entNum);

        }
        while (entNum != -1 && entNum != first);

        // Didn't find any physical attack.
        return null;
    }

	PhysicalOption getBestPhysical(Entity entity, Game game) {
		// Infantry can't conduct physical attacks.
		if (entity instanceof Infantry) {
			return null;
		}

		// if you're charging, it's already declared
		if (entity.isCharging() || entity.isMakingDfa()) {
			return null;
		}

		PhysicalOption best = null;
		for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
			Entity target = (Entity) e.nextElement();

			if (target.equals(entity))
				continue;
			if (!target.isEnemyOf(entity))
				continue;
			if (null == target.getPosition())
				continue;
			PhysicalOption one = getBestPhysicalAttack(entity, target, game);
			if (one != null) {
				if (best == null || one.expectedDmg > best.expectedDmg) {
					best = one;
				}
			}
		}
        if(best == null) best = new PhysicalOption(entity);   
		return best;
	}

	PhysicalOption getBestPhysicalAttack(Entity from, Entity to, Game game) {
		double bestDmg = 0, dmg;
		int damage;
		int bestType = PhysicalOption.PUNCH_LEFT;

		// Infantry can't conduct physical attacks.
		if (from instanceof Infantry) {
			return null;
		}
		ToHitData odds = PunchAttackAction.toHit(game, from.getId(), to, PunchAttackAction.LEFT);
		if (odds.getValue() != ToHitData.IMPOSSIBLE) {
			damage = PunchAttackAction.getDamageFor(from, PunchAttackAction.LEFT);
			bestDmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
		}

		odds = PunchAttackAction.toHit(game, from.getId(), to, PunchAttackAction.RIGHT);
		if (odds.getValue() != ToHitData.IMPOSSIBLE) {
			damage = PunchAttackAction.getDamageFor(from, PunchAttackAction.RIGHT);
			dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
			if (dmg > 0 && bestDmg > 0) {
				bestType = PhysicalOption.PUNCH_BOTH;
				bestDmg += dmg;
			} else {
				bestType = PhysicalOption.PUNCH_RIGHT;
				bestDmg = dmg;
			}
		}

		odds = KickAttackAction.toHit(game, from.getId(), to, KickAttackAction.LEFT);
		if (odds.getValue() != ToHitData.IMPOSSIBLE) {
			damage = KickAttackAction.getDamageFor(from, KickAttackAction.LEFT);
			dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
			if (dmg > bestDmg) {
				bestType = PhysicalOption.KICK_LEFT;
				bestDmg = dmg;
			}
		}

		odds = KickAttackAction.toHit(game, from.getId(), to, KickAttackAction.RIGHT);
		if (odds.getValue() != ToHitData.IMPOSSIBLE) {
			damage = KickAttackAction.getDamageFor(from, KickAttackAction.RIGHT);
			dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
			if (dmg > bestDmg) {
				bestType = PhysicalOption.KICK_RIGHT;
				bestDmg = dmg;
			}
		}

		// Infantry in the open suffer double damage.
		if (to instanceof Infantry) {
			Hex e_hex = game.getBoard().getHex(to.getPosition());
			if (!e_hex.contains(Terrain.WOODS) && !e_hex.contains(Terrain.BUILDING)) {
				bestDmg *= 2;
			}
		}

		if (bestDmg > 0) {
			return new PhysicalOption(from, to, bestDmg, bestType);
		}
		return null;
	}
}
