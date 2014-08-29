/*
 * MegaMek - Copyright (C) 2007-2008 Ben Mazur (bmazur@sev.org)
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
package megamek.server.victory;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.Player;
import megamek.common.Report;

/**
 * Implements a kill count victory condition.  Victory is achieved if a team (or
 * a player with no team) achieves more kills than the set amount.  If multiple
 * teams/players achieve the kill condition in a turn, victory is awarded to the
 * player/team with the highest kill count.
 */
public class KillCountVictory implements Victory, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -6622529899835634696L;
    protected int killCondition;

    public KillCountVictory(int kc) {
        killCondition = kc;
    }

    public Victory.Result victory(IGame game, HashMap<String, Object> ctx) {
        boolean victory = false;
        VictoryResult vr = new VictoryResult(true);
        // Stores the number of kills for each team
        Hashtable<Integer,Integer> killsTeam = new Hashtable<Integer,Integer>();
        // Stores the number of kills for players no on a team
        Hashtable<Integer,Integer> killsPlayer = new Hashtable<Integer,Integer>();
        for (Enumeration<Entity> wreckedEnum = game.getWreckedEntities();
                wreckedEnum.hasMoreElements();)
        {
            Entity wreck = wreckedEnum.nextElement();
            Entity killer = game.getEntity(wreck.getKillerId());
            
            if (killer == null){
                continue;
            }            
            
            int team = killer.getOwner().getTeam();
            // Friendly fire doesn't count
            if (team == wreck.getOwner().getTeam()){
                continue;
            }
            if (team != Player.TEAM_NONE){
                Integer kills = killsTeam.get(team);
                if (kills == null){
                    kills = 1;
                } else {
                    kills++;
                }
                killsTeam.put(team, kills);
            } else {
                Integer player = killer.getOwner().getId();
                // Friendly fire doesn't count
                if (wreck.getOwner().getId() == player){
                    continue;
                }
                Integer kills = killsPlayer.get(player);
                if (kills == null){
                    kills = 1;
                } else {
                    kills++;
                }
                killsPlayer.put(player, kills);
            }
        }
            
        boolean teamHasHighestKills = true;
        int highestKillsId = -1;
        int killCount = 0;
        for (Integer killer : killsTeam.keySet()){
            if (killsTeam.get(killer) > killCount){
                highestKillsId = killer;
                killCount = killsTeam.get(killer);
            }
        }
        
        for (Integer killer : killsPlayer.keySet()){
            if (killsTeam.get(killer) > killCount){
                highestKillsId = killer;
                killCount = killsPlayer.get(killer);
                teamHasHighestKills = false;
            }
        }
        
        if (killCount >= killCondition){
            Report r = new Report(7106, Report.PUBLIC);
            victory = true;
            if (teamHasHighestKills) {
                r.add("Team " + highestKillsId);
                vr.addTeamScore(highestKillsId, 1.0);                
            } else {
                IPlayer winner = game.getPlayer(highestKillsId);
                r.add(winner.getName());
                vr.addPlayerScore(winner.getId(), 1.0);
            }
            r.add(killCount);
            vr.addReport(r);
        }
        
        if (victory)
            return vr;
        return new SimpleNoResult();
    }
}