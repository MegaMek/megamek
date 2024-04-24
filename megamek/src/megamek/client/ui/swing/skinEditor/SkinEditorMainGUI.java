/*
 * MegaMek - Copyright (C) 2000-2004, 2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2015 Nicholas Walczak (walczak@cs.umn.edu)
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
package megamek.client.ui.swing.skinEditor;

import megamek.client.TimerSingleton;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.*;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class SkinEditorMainGUI extends JPanel implements WindowListener {
    private static final long serialVersionUID = 5625499617779156289L;

    private static final String FILENAME_ICON_16X16 = "megamek-icon-16x16.png";
    private static final String FILENAME_ICON_32X32 = "megamek-icon-32x32.png";
    private static final String FILENAME_ICON_48X48 = "megamek-icon-48x48.png";
    private static final String FILENAME_ICON_256X256 = "megamek-icon-256x256.png";

    // a frame, to show stuff in
    private JFrame frame;

    // A menu bar to contain all actions.
    private JDialog skinSpecEditorD;
    private SkinSpecEditor skinSpecEditor;


    public SkinEditorMainGUI() {
        super(new BorderLayout());
    }

    /**
     * Initializes a number of things about this frame.
     */
    private void initializeFrame() {
        frame = new JFrame(Messages.getString("ClientGUI.title"));

        Rectangle virtualBounds = UIUtil.getVirtualBounds();
        int x, y, w, h;
        if (GUIPreferences.getInstance().getWindowSizeHeight() != 0) {
            x = GUIPreferences.getInstance().getWindowPosX();
            y = GUIPreferences.getInstance().getWindowPosY();
            w = GUIPreferences.getInstance().getWindowSizeWidth();
            h = GUIPreferences.getInstance().getWindowSizeHeight();
            if ((x < virtualBounds.getMinX())
                    || ((x + w) > virtualBounds.getMaxX())) {
                x = 0;
            }
            if ((y < virtualBounds.getMinY())
                    || ((y + h) > virtualBounds.getMaxY())) {
                y = 0;
            }
            if (w > virtualBounds.getWidth()) {
                w = (int) virtualBounds.getWidth();
            }
            if (h > virtualBounds.getHeight()) {
                h = (int) virtualBounds.getHeight();
            }
            frame.setLocation(x, y);
            frame.setSize(w, h);
        } else {
            frame.setSize(800, 600);
        }
        frame.setMinimumSize(new Dimension(640, 480));

        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        List<Image> iconList = new ArrayList<>();
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_16X16)
                        .toString()));
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_32X32)
                        .toString()));
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_48X48)
                        .toString()));
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_256X256)
                        .toString()));
        frame.setIconImages(iconList);
    }

    /**
     * Lays out the frame by setting this Client object to take up the full
     * frame display area.
     */
    private void layoutFrame() {
        frame.setTitle(Messages.getString("MegaMek.SkinEditor.label")
                + Messages.getString("ClientGUI.clientTitleSuffix"));
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this, BorderLayout.CENTER);
        frame.validate();
    }

    public void updateBorder() {
        frame.validate();
    }

    /**
     * Have the client register itself as a listener wherever it's needed.
     * <p>
     * According to
     * http://www-106.ibm.com/developerworks/java/library/j-jtp0618.html it is a
     * major bad no-no to perform these registrations before the constructor
     * finishes, so this function has to be called after the <code>Client</code>
     * is created.
     */
    public void initialize() {

        initializeFrame();

        layoutFrame();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                frame.setVisible(false);
                saveSettings();
                die();
            }
        });

        skinSpecEditor = new SkinSpecEditor(this);

        skinSpecEditorD = new JDialog(frame,
                Messages.getString("SkinEditor.SkinEditorDialog.Title"), false);
        skinSpecEditorD.setSize(640, 480);
        skinSpecEditorD.setResizable(true);

        skinSpecEditorD.addWindowListener(this);
        skinSpecEditorD.add(skinSpecEditor);

        frame.pack();
        frame.setVisible(false);
        skinSpecEditorD.setVisible(true);
    }

    /**
     * Saves the current settings to the cfg file.
     */
    void saveSettings() {
        // save frame location
        GUIPreferences.getInstance().setWindowPosX(frame.getLocation().x);
        GUIPreferences.getInstance().setWindowPosY(frame.getLocation().y);
        GUIPreferences.getInstance().setWindowSizeWidth(frame.getSize().width);
        GUIPreferences.getInstance().setWindowSizeHeight(frame.getSize().height);
    }

    /**
     * Shuts down threads and sockets
     */
    void die() {
        frame.removeAll();
        frame.setVisible(false);
        try {
            frame.dispose();
        } catch (Throwable t) {
            LogManager.getLogger().error("", t);
        }

        TimerSingleton.getInstance().killTimer();
    }


    /**
     * Sets the visibility of the entity display window
     */
    public void setDisplayVisible(boolean visible) {
        skinSpecEditorD.setVisible(visible);
        if (visible) {
            frame.requestFocus();
        }
    }

    //
    // WindowListener
    //
    @Override
    public void windowActivated(WindowEvent evt) {
        // TODO: this is a kludge to fix a window iconify issue
        // For some reason when I click on the window button, the main UI
        // window doesn't deiconify. This fix doesn't allow me to iconify the
        // window by clicking the window button, but it's better than the
        // alternative
        frame.setState(Frame.NORMAL);
    }

    @Override
    public void windowClosed(WindowEvent evt) {

    }

    @Override
    public void windowClosing(WindowEvent evt) {
        if (evt.getWindow().equals(skinSpecEditorD)) {
            setDisplayVisible(false);
            die();
        }
    }

    @Override
    public void windowDeactivated(WindowEvent evt) {

    }

    @Override
    public void windowDeiconified(WindowEvent evt) {
        // TODO : this is a kludge to fix a window iconify issue
        // For some reason when I click on the window button, the main UI
        // window doesn't deiconify. This fix doesn't allow me to iconify the
        // window by clicking the window button, but it's better than the
        // alternative
        frame.setState(Frame.NORMAL);
    }

    @Override
    public void windowIconified(WindowEvent evt) {

    }

    @Override
    public void windowOpened(WindowEvent evt) {

    }

    /**
     * @return the frame this client is displayed in
     */
    public JFrame getFrame() {
        return frame;
    }
}
