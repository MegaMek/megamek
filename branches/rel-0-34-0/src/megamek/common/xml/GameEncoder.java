/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common.xml;

import gd.xml.tiny.ParsedXML;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.GameTurn;
import megamek.common.IGame;
import megamek.common.Minefield;
import megamek.common.PilotingRollData;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.actions.EntityAction;

/**
 * Objects of this class can encode a <code>IGame</code> object as XML into an
 * output writer and decode one from a parsed XML node. It is used for saving
 * games into a version- neutral format and may someday become the mechanism for
 * handling advance scenario options. Please note that <em>order of the
 * XML elements matters</em>
 * because I'm trying to be efficient, not flexible.
 * 
 * @author James Damour <suvarov454@users.sourceforge.net>
 */
public class GameEncoder {

    /**
     * Encode a <code>IGame</code> object to an output writer.
     * 
     * @param game - the <code>IGame</code> to be encoded. This value must not
     *            be <code>null</code>.
     * @param out - the <code>Writer</code> that will receive the XML. This
     *            value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IOException</code> if there's any error on write.
     */
    public static void encode(IGame game, Writer out) throws IOException {
        Enumeration<?> iter; // used when marching through a list of sub-elements
        Coords coords;

        // First, validate our input.
        if (null == game) {
            throw new IllegalArgumentException("The game is null.");
        }
        if (null == out) {
            throw new IllegalArgumentException("The writer is null.");
        }

        // Start the XML stream for this game.
        out.write("<game version=\"1.0\">");

        // Encode the game options.
        if (null != game.getOptions()) {
            GameOptionsEncoder.encode(game.getOptions(), out);
        }

        // Encode the board.
        if (null != game.getBoard()) {
            BoardEncoder.encode(game.getBoard(), out);
        }

        // Encode the minefields.
        iter = game.getMinedCoords();
        if (iter.hasMoreElements()) {
            out.write("<minefields>");
            while (iter.hasMoreElements()) {
                coords = (Coords) iter.nextElement();
                Enumeration<?> fields = game.getMinefields(coords).elements();
                while (fields.hasMoreElements()) {
                    MinefieldEncoder.encode((Minefield) fields.nextElement(),
                            out);
                }
            }
            out.write("</minefields>");
        }

        // Encode the players.
        iter = game.getPlayers();
        if (iter.hasMoreElements()) {
            out.write("<players>");
            while (iter.hasMoreElements()) {
                PlayerEncoder.encode((Player) iter.nextElement(), out);
            }
            out.write("</players>");
        }

        // Encode the teams.
        iter = game.getTeams();
        if (iter.hasMoreElements()) {
            out.write("<teams>");
            while (iter.hasMoreElements()) {
                TeamEncoder.encode((Team) iter.nextElement(), out);
            }
            out.write("</teams>");
        }

        // Encode the in-game entities.
        iter = game.getEntities();
        if (iter.hasMoreElements()) {
            out.write("<entities set=\"IN-GAME\">");
            while (iter.hasMoreElements()) {
                EntityEncoder.encode((Entity) iter.nextElement(), out);
            }
            out.write("</entities>");
        }

        // Encode the out-of-game entities.
        iter = game.getOutOfGameEntitiesVector().elements();
        if (iter.hasMoreElements()) {
            out.write("<entities set=\"OUT-GAME\">");
            while (iter.hasMoreElements()) {
                EntityEncoder.encode((Entity) iter.nextElement(), out);
            }
            out.write("</entities>");
        }

        // Encode the game turns.
        iter = game.getTurns();
        if (iter.hasMoreElements()) {
            out.write("<turns>");
            while (iter.hasMoreElements()) {
                GameTurnEncoder.encode((GameTurn) iter.nextElement(), out);
            }
            out.write("</turns>");
        }

        // Encode the actions.
        iter = game.getActions();
        if (iter.hasMoreElements()) {
            out.write("<actions>");
            while (iter.hasMoreElements()) {
                EntityActionEncoder.encode((EntityAction) iter.nextElement(),
                        out);
            }
            out.write("</actions>");
        }

        // Encode the PSRs.
        iter = game.getPSRs();
        if (iter.hasMoreElements()) {
            out.write("<PSRs>");
            while (iter.hasMoreElements()) {
                PilotingRollDataEncoder.encode((PilotingRollData) iter
                        .nextElement(), out);
            }
            out.write("</PSRs>");
        }

        // Encode the game's data.
        out.write("<gameData ");
        out.write("windDirection=\"");
        out.write(game.getPlanetaryConditions().getWindDirection());
        out.write("\" roundCount=\"");
        out.write(game.getRoundCount());
        out.write("\" phase=\"");
        out.write(game.getPhase().ordinal());
        out.write("\" lastPhase=\"");
        out.write(game.getLastPhase().ordinal());
        out.write("\" forceVictory=\"");
        out.write(game.isForceVictory() ? "true" : "false");
        out.write("\" victoryPlayerId=\"");
        out.write(game.getVictoryPlayerId());
        out.write("\" victoryTeam=\"");
        out.write(game.getVictoryTeam());
        out.write("\" />");

        // Finish the XML stream for this game.
        out.write("</game>");
    }

    /**
     * Decode a <code>IGame</code> object from the passed node.
     * 
     * @param node - the <code>ParsedXML</code> node for this object. This
     *            value must not be <code>null</code>.
     * @return the <code>IGame</code> object based on the node.
     * @throws <code>IllegalArgumentException</code> if the node is
     *             <code>null</code>.
     * @throws <code>IllegalStateException</code> if the node does not contain
     *             a valid <code>IGame</code>.
     */
    public static IGame decode(ParsedXML node) {
        return null;
    }

}
