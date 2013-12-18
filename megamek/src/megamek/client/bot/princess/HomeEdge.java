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
package megamek.client.bot.princess;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : Deric "Netzilla" Page (deric dot page at usa dot net
 * @version $Id$
 * @since : 8/17/13 10:37 PM
 */
public enum HomeEdge {
    NORTH(0),
    SOUTH(1),
    WEST(2),
    EAST(3);

    private int index;

    HomeEdge(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static HomeEdge getHomeEdge(int index) {
        for (HomeEdge he : values()) {
            if (he.getIndex() == index) {
                return he;
            }
        }
        return null;
    }
}
