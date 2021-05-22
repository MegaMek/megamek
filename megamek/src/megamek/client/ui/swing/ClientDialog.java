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

package megamek.client.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import megamek.client.ui.swing.util.UIUtil;

/**
 * A MegaMek Dialog box.
 */
public class ClientDialog extends JDialog {

    private static final long serialVersionUID = 6154951760485853883L;
    
    private static final double TASKBAR_SIZE = .05;
    private static final int CONTAINER_BUFFER = 10;

    protected JFrame owner = null;
    private boolean isScaling = false;

    /** 
     * Creates a basic ClientDialog.
     * @see JDialog#JDialog(java.awt.Frame, String) 
     */
    public ClientDialog(JFrame owner, String title) {
        super(owner, title);
        this.owner = owner;
    }

    /** 
     * Creates a ClientDialog with modality as given by modal.
     * @see JDialog#JDialog(java.awt.Frame, String, boolean) 
     */
    public ClientDialog(JFrame owner, String title, boolean modal) {
        super(owner, title, modal);
        this.owner = owner;
    }
    
    /** 
     * Creates a ClientDialog with modality as given by modal. This dialog
     * will automatically scale with the current GUI scaling value. Results of
     * this may vary with the complexity of the dialog; manual scaling will
     * often be better.
     * @see JDialog#JDialog(java.awt.Frame, String, boolean) 
     */
    public ClientDialog(JFrame owner, String title, boolean modal, boolean scale) {
        super(owner, title, modal);
        this.owner = owner;
        isScaling = scale;
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
    public void setLocationAndSize(Dimension desiredDimension) {
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
    
    /** Center the dialog within the owner frame.  */
    public void center() {
        if (owner == null) {
            return;
        }
        
        setLocation(owner.getLocation().x + (owner.getSize().width / 2) 
              - (getSize().width / 2),
              owner.getLocation().y + (owner.getSize().height / 2) 
              - (getSize().height / 2));
    }
    
    /** 
     * Adds a row (line) with the two JComponents <code>label, secondC</code>
     * to the given <code>panel</code>, using constraints c. The label will be
     * right-aligned, the secondC left-aligned to bring them close together. 
     * Only useful for simple panels with GridBagLayout.
     */
    public void addOptionRow(JPanel targetP, GridBagConstraints c, JLabel label, Component secondC) {
        int oldGridW = c.gridwidth;
        int oldAnchor = c.anchor;
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        targetP.add(label, c);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        targetP.add(secondC, c);
        
        c.gridwidth = oldGridW;
        c.anchor = oldAnchor;
    }
    
    /** 
     * Adds a spacer row (line) to the given <code>panel</code>, 
     * using constraints c. Only useful for simple panels with GridBagLayout.
     */
    public void addSpacerRow(JPanel targetP, GridBagConstraints c, int vGap) {
        int oldGridW = c.gridwidth;
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        targetP.add(Box.createVerticalStrut(vGap), c);
        
        c.gridwidth = oldGridW;
    }
    
    @Override
    public void setVisible(boolean b) {
        if (isScaling && b) {
            guiScale();
            super.setVisible(true);
        } else {
            super.setVisible(b);
        }
    }
    
    /** 
     * Applies the GUI Scaling on the event thread as demanded
     * by {@link java.awt.Container#getComponent(int)}. 
     */
    public void guiScale() {
        UIUtil.adjustDialog(getContentPane());
        pack();
        center();
    }

}
