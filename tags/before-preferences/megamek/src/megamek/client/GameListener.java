/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.client;

public interface GameListener
    extends java.util.EventListener
{
    public void gamePlayerChat(GameEvent e);
    public void gamePlayerStatusChange(GameEvent e);
    public void gameTurnChange(GameEvent e);
    public void gamePhaseChange(GameEvent e);
    public void gameNewEntities(GameEvent e);
    public void gameNewSettings(GameEvent e);
    public void gameDisconnected(GameEvent e);
    public void gameBoardChanged(GameEvent e);
    public void gameEnd(GameEvent e);
    public void gameReport(GameEvent e);
    public void gameMapQuery(GameEvent e);
}
