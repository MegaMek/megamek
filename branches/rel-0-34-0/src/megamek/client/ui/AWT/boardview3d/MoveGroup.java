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

import java.util.Enumeration;
import javax.media.j3d.BranchGroup;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MoveStep;

/**
 *
 * @author jwalt
 */
class MoveGroup extends BranchGroup {
    IGame game;
    ViewTransform currentView;
    MovePath cur;

    public MoveGroup(IGame g, ViewTransform v) {
        game = g;
        currentView = v;
        setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        setPickable(false);
    }
    
    public void clear() {
        for (Enumeration<?> e = getAllChildren(); e.hasMoreElements();) {
            ((BranchGroup)e.nextElement()).detach();
        }
        cur = null;
    }

    public void set(MovePath md) {
        clear();
        if (md == null) return;
        cur = md;
        IBoard gboard = game.getBoard();
        int count = 0;
        for (Enumeration<?> i = md.getSteps(); i.hasMoreElements();) {
            MoveStep step = (MoveStep)i.nextElement();
            addChild(new MoveStepModel(step, count++, gboard.getHex(step.getPosition()), currentView));
        }
    }

    void setView(ViewTransform v) {
        currentView = v;
        set(cur);
    }

}
