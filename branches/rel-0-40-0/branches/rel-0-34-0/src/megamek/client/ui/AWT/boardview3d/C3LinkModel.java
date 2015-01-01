/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
 * 
 * This file (C) 2008 Jörg Walter <j.walter@syntax-k.de>
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

package megamek.client.ui.AWT.boardview3d;

import javax.vecmath.*;
import megamek.client.ui.AWT.util.PlayerColors;
import megamek.common.Entity;
import megamek.common.IGame;

class C3LinkModel extends ConnectionModel {
    Entity src, dst;

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != getClass()) return false;
        C3LinkModel m = (C3LinkModel)o;
        return  src.getId() == m.src.getId() && dst.getId() == m.dst.getId();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + src.hashCode();
        hash = 71 * hash + dst.hashCode();
        return hash;
    }

    public C3LinkModel(Entity a, Entity b, IGame game, Point3d source) {
        super(
            a.getPosition(),
            b.getPosition(),
            game.getBoard().getHex(a.getPosition()).getElevation()+a.absHeight(),
            game.getBoard().getHex(b.getPosition()).getElevation()+b.absHeight(),
            source,
            new Color3f(PlayerColors.getColor(a.getOwner().getColorIndex())),
            .8f
        );
            
        if (a.getId() < b.getId()) {
            src = a;
            dst = b;
        } else {
            src = b;
            dst = a;
        }
    }
}
