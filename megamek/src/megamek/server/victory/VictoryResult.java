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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

import megamek.common.Player;
import megamek.common.Report;

/**
 * quick implementation of a Victory.Result stores player scores and a flag if
 * game-ending victory is achieved or not
 */
public class VictoryResult implements Victory.Result {
    protected boolean victory;
    protected Throwable tr;
    protected ArrayList<Report> reports = new ArrayList<Report>();
    protected HashMap<Integer, Double> playerScore = new HashMap<Integer, Double>();
    protected HashMap<Integer, Double> teamScore = new HashMap<Integer, Double>();
    protected double hiScore = 0;

    public VictoryResult(boolean win) {
        this.victory = win;
        tr = new Throwable();
    }

    public int getWinningPlayer() {
        double max = Double.MIN_VALUE;
        int maxPlayer = Player.PLAYER_NONE;
        boolean draw = false;
        for (int i : playerScore.keySet()) {
            if (playerScore.get(i) == max) {
                draw = true;
            }
            if (playerScore.get(i) > max) {
                draw = false;
                max = playerScore.get(i);
                maxPlayer = i;
            }
        }
        if (draw)
            return Player.PLAYER_NONE;
        return maxPlayer;
    }

    public int getWinningTeam() {
        double max = Double.MIN_VALUE;
        int maxTeam = Player.TEAM_NONE;
        boolean draw = false;
        ;
        for (int i : teamScore.keySet()) {
            if (teamScore.get(i) == max) {
                draw = true;
            }
            if (teamScore.get(i) > max) {
                draw = false;
                max = teamScore.get(i);
                maxTeam = i;
            }
        }
        if (draw)
            return Player.TEAM_NONE;
        return maxTeam;
    }

    protected void updateHiScore() {
        // used to calculate winner
        hiScore = Double.MIN_VALUE;
        for (Double d : playerScore.values()) {
            if (d > hiScore)
                hiScore = d;
        }
        for (Double d : teamScore.values()) {
            if (d > hiScore)
                hiScore = d;
        }
    }

    public void addPlayerScore(int id, double score) {
        playerScore.put(id, score);
        updateHiScore();
    }

    public void addTeamScore(int id, double score) {
        teamScore.put(id, score);
        updateHiScore();
    }

    public boolean isWinningPlayer(int id) {
        double d = getPlayerScore(id);
        // two decimal compare..
        return ((d * 100) % 100) == ((hiScore * 100) % 100);
    }

    public boolean isWinningTeam(int id) {
        double d = getTeamScore(id);
        // two decimal compare..
        return ((d * 100) % 100) == ((hiScore * 100) % 100);
    }

    public boolean victory() {
        return victory;
    }

    public void setVictory(boolean b) {
        this.victory = b;
    }

    public double getPlayerScore(int id) {
        if (playerScore.get(id) == null)
            return 0.0;
        else
            return playerScore.get(id);
    }

    public int[] getPlayers() {
        return intify(playerScore.keySet().toArray(new Integer[0]));
    }

    public double getTeamScore(int id) {
        if (teamScore.get(id) == null)
            return 0.0;
        else
            return teamScore.get(id);
    }

    public int[] getTeams() {
        return intify(teamScore.keySet().toArray(new Integer[0]));
    }

    public void addReport(Report r) {
        reports.add(r);
    }

    public ArrayList<Report> getReports() {
        return reports;
    }

    protected String getTrace() {
        StringWriter sw = new StringWriter();
        PrintWriter pr = new PrintWriter(sw);
        tr.printStackTrace(pr);
        pr.flush();
        return sw.toString();
    }

    private int[] intify(Integer[] ar) {
        int[] ret = new int[ar.length];
        for (int i = 0; i < ar.length; i++)
            ret[i] = ar[i];
        return ret;
    }

    public String toString() {
        return "victory provided to you by:" + getTrace();
    }

    public boolean isDraw() {
        return (getWinningPlayer() == Player.PLAYER_NONE && getWinningTeam() == Player.TEAM_NONE);
    }
}