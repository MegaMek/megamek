package megamek.server.victory;
import java.util.*;
import megamek.common.IGame;
/**
 *  This is the original implementation of victory moved under the 
 *  new infrastructure
 */
public class SpaghettiVictory
implements Victory
{
    public Victory.Result victory(
                        ReportListener rl,
                        IGame game)
    {
        Victory.Result ret=null;
        
		if (game.isForceVictory()) {
			int victoryPlayerId = game.getVictoryPlayerId();
			int victoryTeam = game.getVictoryTeam();
			Vector<Player> players = game.getPlayersVector();
			boolean forceVictory = true;

			// Individual victory.
			if (victoryPlayerId != Player.PLAYER_NONE) {
				for (int i = 0; i < players.size(); i++) {
					Player player = players.elementAt(i);

					if (player.getId() != victoryPlayerId
							&& !player.isObserver()) {
						if (!player.admitsDefeat()) {
							forceVictory = false;
							break;
						}
					}
				}
			}
			// Team victory.
			if (victoryTeam != Player.TEAM_NONE) {
				for (int i = 0; i < players.size(); i++) {
					Player player = players.elementAt(i);

					if (player.getTeam() != victoryTeam && !player.isObserver()) {
						if (!player.admitsDefeat()) {
							forceVictory = false;
							break;
						}
					}
				}
			}

			if (forceVictory) {
				return true;
			}

			for (int i = 0; i < players.size(); i++) {
				Player player = players.elementAt(i);
				player.setAdmitsDefeat(false);
			}

			cancelVictory();
		}//end forcevictory

		if (!game.gameTimerIsExpired()
				&& !game.getOptions().booleanOption("check_victory")) {
			return false;
		}

		// check all players/teams for aliveness
		int playersAlive = 0;
		Player lastPlayer = null;
		boolean oneTeamAlive = false;
		int lastTeam = Player.TEAM_NONE;
		boolean unteamedAlive = false;
		for (Enumeration<Player> e = game.getPlayers(); e.hasMoreElements();) {
			Player player = e.nextElement();
			int team = player.getTeam();
			if (game.getLiveDeployedEntitiesOwnedBy(player) <= 0) {
				continue;
			}
			// we found a live one!
			playersAlive++;
			lastPlayer = player;
			// check team
			if (team == Player.TEAM_NONE) {
				unteamedAlive = true;
			} else if (lastTeam == Player.TEAM_NONE) {
				// possibly only one team alive
				oneTeamAlive = true;
				lastTeam = team;
			} else if (team != lastTeam) {
				// more than one team alive
				oneTeamAlive = false;
				lastTeam = team;
			}
		}

		// check if there's one player alive
		if (playersAlive < 1) {
			game.setVictoryPlayerId(Player.PLAYER_NONE);
			game.setVictoryTeam(Player.TEAM_NONE);
			return true;
		} else if (playersAlive == 1) {
			if (lastPlayer != null && lastPlayer.getTeam() == Player.TEAM_NONE) {
				// individual victory
				game.setVictoryPlayerId(lastPlayer.getId());
				game.setVictoryTeam(Player.TEAM_NONE);
				return true;
			}
		}

		// did we only find one live team?
		if (oneTeamAlive && !unteamedAlive) {
			// team victory
			game.setVictoryPlayerId(Player.PLAYER_NONE);
			game.setVictoryTeam(lastTeam);
			return true;
		}

		// now check for detailed victory conditions...
		Hashtable<Integer, Integer> winPlayers = new Hashtable<Integer, Integer>();
		Hashtable<Integer, Integer> winTeams = new Hashtable<Integer, Integer>();

		// BV related victory conditions
		if (game.getOptions().booleanOption("use_bv_destroyed")
				|| game.getOptions().booleanOption("use_bv_ratio")) {
			HashSet<Integer> doneTeams = new HashSet<Integer>();
			for (Enumeration<Player> e = game.getPlayers(); e.hasMoreElements();) {
				Player player = e.nextElement();
				if (player.isObserver())
					continue;
				int fbv = 0;
				int ebv = 0;
				int eibv = 0;
				int team = player.getTeam();
				if (team != Player.TEAM_NONE) {
					if (doneTeams.contains(team))
						continue; // skip if already
					doneTeams.add(team);
				}

				for (Enumeration<Player> f = game.getPlayers(); f
						.hasMoreElements();) {
					Player other = f.nextElement();
					if (other.isObserver())
						continue;
					if (other.isEnemyOf(player)) {
						ebv += other.getBV();
						eibv += other.getInitialBV();
					} else {
						fbv += other.getBV();
					}
				}

				if (game.getOptions().booleanOption("use_bv_ratio")) {
					if (ebv == 0
							|| (100 * fbv) / ebv >= game.getOptions()
									.intOption("bv_ratio_percent")) {
						Report r = new Report(7100, Report.PUBLIC);
						if (team == Player.TEAM_NONE) {
							r.add(player.getName());
							Integer vc = winPlayers.get(player.getId());
							if (vc == null)
								vc = new Integer(0);
							winPlayers.put(player.getId(), vc + 1);
						} else {
							r.add("Team " + team);
							Integer vc = winTeams.get(team);
							if (vc == null)
								vc = new Integer(0);
							winTeams.put(team, vc + 1);
						}
						r.add(ebv == 0 ? 9999 : (100 * fbv) / ebv);
						addReport(r);
					}
				}
				if (game.getOptions().booleanOption("use_bv_destroyed")) {
					if ((ebv * 100) / eibv <= 100 - game.getOptions()
							.intOption("bv_destroyed_percent")) {
						Report r = new Report(7105, Report.PUBLIC);
						if (team == Player.TEAM_NONE) {
							r.add(player.getName());
							Integer vc = winPlayers.get(player.getId());
							if (vc == null)
								vc = new Integer(0);
							winPlayers.put(player.getId(), vc + 1);
						} else {
							r.add("Team " + team);
							Integer vc = winTeams.get(team);
							if (vc == null)
								vc = new Integer(0);
							winTeams.put(team, vc + 1);
						}
						r.add(100 - ((ebv * 100) / eibv));
						addReport(r);
					}
				}
			}
		}

		// Commander killed victory condition
		if (game.getOptions().booleanOption("commander_killed")) {
			// check all players/teams for aliveness
			playersAlive = 0;
			lastPlayer = null;
			oneTeamAlive = false;
			lastTeam = Player.TEAM_NONE;
			unteamedAlive = false;
			for (Enumeration<Player> e = game.getPlayers(); e.hasMoreElements();) {
				Player player = e.nextElement();
				int team = player.getTeam();
				if (game.getLiveCommandersOwnedBy(player) <= 0) {
					continue;
				}
				// we found a live one!
				playersAlive++;
				lastPlayer = player;
				// check team
				if (team == Player.TEAM_NONE) {
					unteamedAlive = true;
				} else if (lastTeam == Player.TEAM_NONE) {
					// possibly only one team alive
					oneTeamAlive = true;
					lastTeam = team;
				} else if (team != lastTeam) {
					// more than one team alive
					oneTeamAlive = false;
					lastTeam = team;
				}
			}

			// check if there's one player alive
			if (playersAlive < 1) {
				for (Player p : game.getPlayersVector()) {
					Integer vc = winPlayers.get(p.getId());
					if (vc == null)
						vc = new Integer(0);
					winPlayers.put(p.getId(), vc + 1);
				}
				for (Team t : game.getTeamsVector()) {
					Integer vc = winTeams.get(t.getId());
					if (vc == null)
						vc = new Integer(0);
					winTeams.put(t.getId(), vc + 1);
				}
			} else if (playersAlive == 1) {
				if (lastPlayer != null
						&& lastPlayer.getTeam() == Player.TEAM_NONE) {
					// individual victory
					Integer vc = winPlayers.get(lastPlayer.getId());
					if (vc == null)
						vc = new Integer(0);
					winPlayers.put(lastPlayer.getId(), vc + 1);
				}
			}

			// did we only find one live team?
			if (oneTeamAlive && !unteamedAlive) {
				Integer vc = winTeams.get(lastTeam);
				if (vc == null)
					vc = new Integer(0);
				winTeams.put(lastTeam, vc + 1);
			}
		}

		// Any winners?
		int wonPlayer = Player.PLAYER_NONE;
		int wonTeam = Player.TEAM_NONE;
		boolean draw = false;
		for (Map.Entry<Integer, Integer> e : winPlayers.entrySet()) {
			if (e.getValue() >= game.getOptions().intOption(
					"achieve_conditions")) {
				if (wonPlayer != Player.PLAYER_NONE)
					draw = true;
				wonPlayer = e.getKey();
				Report r = new Report(7200, Report.PUBLIC);
				r.add(game.getPlayer(wonPlayer).getName());
				addReport(r);
			}
		}
		for (Map.Entry<Integer, Integer> e : winTeams.entrySet()) {
			if (e.getValue() >= game.getOptions().intOption(
					"achieve_conditions")) {
				if (wonTeam != Player.TEAM_NONE
						|| wonPlayer != Player.PLAYER_NONE)
					draw = true;
				wonTeam = e.getKey();
				Report r = new Report(7200, Report.PUBLIC);
				r.add("Team " + wonTeam);
				addReport(r);
			}
		}
		if (draw) {
			game.setVictoryPlayerId(Player.PLAYER_NONE);
			game.setVictoryTeam(Player.TEAM_NONE);
			return true;
		}
		if (wonPlayer != Player.PLAYER_NONE) {
			// individual victory
			game.setVictoryPlayerId(wonPlayer);
			game.setVictoryTeam(Player.TEAM_NONE);
			return true;
		}
		if (wonTeam != Player.TEAM_NONE) {
			// team victory
			game.setVictoryPlayerId(Player.PLAYER_NONE);
			game.setVictoryTeam(wonTeam);
			return true;
		}

		// If noone has won...
		if (game.gameTimerIsExpired()) {
			game.setVictoryPlayerId(Player.PLAYER_NONE);
			game.setVictoryTeam(Player.TEAM_NONE);

			return true;
		}

		return false;    
    }
}