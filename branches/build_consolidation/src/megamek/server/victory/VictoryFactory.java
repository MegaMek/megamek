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

/**
 * interface for VictoryFactories, ie. classes which construct Victory objects
 * or Victory object hierarchies based on a given string. The string might be
 * just a list of instructions, gibberish , or an url where to retrieve the real
 * information or a combination of these. Implementors must implement a publicly
 * accessible default constructor and must not store state outside of methods.
 * Also in general a Victory generated with this factory should not alter its
 * workings based on settings fetched during the game from some source. like
 * game options=) bad form=) Those options should be given in the victory-string
 * as a parameter.
 */
public interface VictoryFactory {
    /**
     * @param conditions - depending on the implementation describes either the
     *            conditions for Victory or a place where to get them.
     */
    public Victory createVictory(String victory);
}