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
								Compute
									.toHitKick(bot.game, entNum, bestAttack.target.getId(), bestAttack.type - 3)
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
							case CEntity.SIDE_FRONT :
								mod = (llarmor + rlarmor) / 2;
								break;
							case CEntity.SIDE_LEFT :
								mod = llarmor;
								break;
							case CEntity.SIDE_RIGHT :
								mod = rlarmor;
								break;
						}
					} else if (bestAttack.target instanceof Infantry) {
						mod = 0.0;
					} else if (bestAttack.target instanceof Tank) {
						switch (side) {
							case CEntity.SIDE_FRONT :
								mod =
									bestAttack.target.getArmor(Tank.LOC_FRONT)
										/ bestAttack.target.getOArmor(Tank.LOC_FRONT);
								break;
							case CEntity.SIDE_LEFT :
								mod =
									bestAttack.target.getArmor(Tank.LOC_LEFT)
										/ bestAttack.target.getOArmor(Tank.LOC_LEFT);
								break;
							case CEntity.SIDE_RIGHT :
								mod =
									bestAttack.target.getArmor(Tank.LOC_RIGHT)
										/ bestAttack.target.getOArmor(Tank.LOC_RIGHT);
								break;
							case CEntity.SIDE_REAR :
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
							Compute.toHitPunch(bot.game, en.getId(), bestAttack.target.getId(), PunchAttackAction.LEFT);
						if (toHit.getValue() != ToHitData.IMPOSSIBLE) {
							left = true;
						}
						toHit =
							Compute.toHitPunch(bot.game, en.getId(), bestAttack.target.getId(), PunchAttackAction.RIGHT);
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
			}
			entNum = bot.game.getNextEntityNum(entNum);
		}
		while (entNum != -1 && entNum != first);
		throw new RuntimeException("Error finding a unit that has a physical option");
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
		ToHitData odds = Compute.toHitPunch(game, from.getId(), to.getId(), PunchAttackAction.LEFT);
		if (odds.getValue() != ToHitData.IMPOSSIBLE) {
			damage = Compute.getPunchDamageFor(from, PunchAttackAction.LEFT);
			bestDmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
		}

		odds = Compute.toHitPunch(game, from.getId(), to.getId(), PunchAttackAction.RIGHT);
		if (odds.getValue() != ToHitData.IMPOSSIBLE) {
			damage = Compute.getPunchDamageFor(from, PunchAttackAction.RIGHT);
			dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
			if (dmg > 0 && bestDmg > 0) {
				bestType = PhysicalOption.PUNCH_BOTH;
				bestDmg += dmg;
			} else {
				bestType = PhysicalOption.PUNCH_RIGHT;
				bestDmg = dmg;
			}
		}

		odds = Compute.toHitKick(game, from.getId(), to.getId(), KickAttackAction.LEFT);
		if (odds.getValue() != ToHitData.IMPOSSIBLE) {
			damage = Compute.getKickDamageFor(from, KickAttackAction.LEFT);
			dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
			if (dmg > bestDmg) {
				bestType = PhysicalOption.KICK_LEFT;
				bestDmg = dmg;
			}
		}

		odds = Compute.toHitKick(game, from.getId(), to.getId(), KickAttackAction.RIGHT);
		if (odds.getValue() != ToHitData.IMPOSSIBLE) {
			damage = Compute.getKickDamageFor(from, KickAttackAction.RIGHT);
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