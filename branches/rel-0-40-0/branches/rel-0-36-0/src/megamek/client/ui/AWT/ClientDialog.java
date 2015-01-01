/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

/**
 * A MegaMek Dialog box.
 */
public class ClientDialog extends Dialog {

    /**
     * 
     */
    private static final long serialVersionUID = -1640475498623472521L;
    private static final double TASKBAR_SIZE = .05;
    private static final int CONTAINER_BUFFER = 10;

    private Frame owner = null;

    /**
     * @param owner - the <code>Frame</code> that owns this dialog.
     * @param title - the title of this Dialog window
     */
    public ClientDialog(Frame owner, String title) {
        super(owner, title);
        this.owner = owner;
    }

    public ClientDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        this.owner = owner;
    }

    /**
     * Set the size and location to something sane (always within the screen).
     * We try to fit the dialog in the middle of its owner, if it is smaller,
     * but allow it to eclipse the parent if it is larger, still keeping all on
     * the screen.
     * 
     * @param desiredX the desired width of this dialog (you might not get it)
     * @param desiredY the desired height of this dialog (you might not get it)
     */
    public void setLocationAndSize(int desiredX, int desiredY) {
        setLocationAndSize(new Dimension(desiredX, desiredY));
    }

    /**
     * Set the size and location to something sane (always within the screen).
     * We try to fit the dialog in the middle of its owner, if it is smaller,
     * but allow it to eclipse the parent if it is larger, still keeping all on
     * the screen.
     * 
     * @param desiredDimension the desired dimension of this dialog (you might
     *            not get it)
     */
    protected void setLocationAndSize(Dimension desiredDimension) {
        int height, width;

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Dimension screenSize = new Dimension(gd.getDisplayMode().getWidth(), gd.getDisplayMode().getHeight());

        width = Math.min(desiredDimension.width + CONTAINER_BUFFER,
                screenSize.width);
        height = Math.min(desiredDimension.height + CONTAINER_BUFFER,
                screenSize.height);

        // Shrink the dialog if it will go bigger than page:
        // A border is used to account for things like the windows taskbar.
        // Sadly, the true size of the taskbar cannot be found without making
        // a native call, so 5% is just a guess. It should probably be
        // a configuration setting so people can modify it according to
        // their OS setup.
        int screenBorder = Math.max((int) (screenSize.width * TASKBAR_SIZE),
                (int) (screenSize.height * TASKBAR_SIZE));
        if (height == screenSize.height)
            height = screenSize.height - 2 * screenBorder;
        if (width == screenSize.width)
            width = screenSize.width - 2 * screenBorder;

        setSize(width, height);
        setLocationRelativeTo(owner);
    }

    }
