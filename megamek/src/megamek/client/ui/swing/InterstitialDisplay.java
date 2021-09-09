/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package megamek.client.ui.swing;

import java.util.ArrayList;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.MegamekButton;

/**
 * A mostly empty display to show up in between phases.
 */
public class InterstitialDisplay extends StatusBarPhaseDisplay {


    public enum Type { END_OF_ROUND, END_OF_GAME; }


    public InterstitialDisplay(ClientGUI clientgui, Type type) {
        super(clientgui);
        switch (type) {
            case END_OF_ROUND:
                setupStatusBar(Messages.getString("InterstitialDisplay.roundStatus")); //$NON-NLS-1$
                butDone.setText(Messages.getString("InterstitialDisplay.roundButton")); //$NON-NLS-1$
                break;
            case END_OF_GAME:
                setupStatusBar(Messages.getString("InterstitialDisplay.gameStatus")); //$NON-NLS-1$
                butDone.setText(Messages.getString("InterstitialDisplay.gameButton")); //$NON-NLS-1$
                break;
        }

        setupButtonPanel();
    }

    @Override
    public void ready() {
        this.clientgui.getClient().sendDone(true);
    }

    @Override
    protected ArrayList<MegamekButton> getButtonList() {
        return new ArrayList<MegamekButton>();
    }

    @Override
    public void removeAllListeners() {
        // noop
    }

    @Override
    public void clear() {
        // noop
    }

}
