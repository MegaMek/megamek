/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.dialog.imageChooser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.icons.AbstractIcon;

/**
 * Creates a dialog that allows players to select a directory from
 * a directory tree and choose an image from the images in that directory.
 * Subclasses must provide the getItems() method that translates
 * a given category (directory) selected in the tree to a list
 * of items (images) to show in the list.
 * Subclasses can provide getSearchedItems() that translates a given search
 * String to the list of "found" items. If this is provided, showSearch(true)
 * should be called in the constructor to show the search panel.
 */
public abstract class AbstractIconChooserDialog extends JDialog {
    //region Variable Declarations
    private static final long serialVersionUID = -7836502700465322620L;
    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private AbstractIconChooser chooser;

    /** True when the user canceled. */
    private boolean wasCanceled = false;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Creates a dialog that allows players to choose a directory from
     * a directory tree and an image from the images in that directory.
     *
     * @param parent The Window (dialog or frame) hosting this dialog
     * @param title the dialog title
     * @param chooser the icon chooser display panel
     */
    public AbstractIconChooserDialog(Window parent, String title, AbstractIconChooser chooser) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        this.chooser = chooser;

        initialize();
    }
    //endregion Constructors

    //region Initialization
    private void initialize() {
        setLayout(new BorderLayout());
        add(getChooser(), BorderLayout.CENTER);
        add(buttonPanel(), BorderLayout.PAGE_END);

        // Size and position of the dialog
        setMinimumSize(new Dimension(480, 240));
        setSize(GUIP.getImageChoiceSizeWidth(), GUIP.getImageChoiceSizeHeight());
        setLocation(GUIP.getImageChoicePosX(), GUIP.getImageChoicePosY());

        // Make the close "X" cancel the dialog
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });
        chooser.imageList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    select();
                }
            }
        });
    }

    /** Constructs the bottom panel with the Okay and Cancel buttons. */
    protected JPanel buttonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));

        JButton btnOkay = new JButton(Messages.getString("Okay"));
        btnOkay.addActionListener(evt -> select());
        panel.add(btnOkay);

        JButton btnCancel = new JButton(Messages.getString("Cancel"));
        btnCancel.addActionListener(evt -> cancel());
        panel.add(btnCancel);

        JButton btnRefresh = new JButton(Messages.getString("AbstractIconChooserDialog.btnRefresh"));
        btnRefresh.addActionListener(evt -> getChooser().refreshDirectory());
        panel.add(btnRefresh);

        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        return panel;
    }
    //endregion Initialization

    //region Getters/Setters
    protected AbstractIconChooser getChooser() {
        return chooser;
    }

    public AbstractIcon getSelectedItem() {
        return getChooser().getSelectedItem();
    }

    public int getSelectedIndex() {
        return getChooser().getSelectedIndex();
    }
    //endregion Getters/Setters

    /** Activates the dialog and returns if the user cancelled. */
    public int showDialog() {
        wasCanceled = false;
        setVisible(true);
        // After returning from the modal dialog, save settings the return whether it was cancelled or not...
        saveWindowSettings();
        return wasCanceled ? JOptionPane.CANCEL_OPTION : JOptionPane.OK_OPTION;
    }

    /** Called when the Okay button is pressed */
    protected void select() {
        wasCanceled = false;
        setVisible(false);
    }

    protected void cancel() {
        wasCanceled = true;
        setVisible(false);
    }

    /** Saves the position, size and split of the dialog. */
    private void saveWindowSettings() {
        GUIP.setValue(GUIPreferences.IMAGE_CHOOSER_POS_X, getLocation().x);
        GUIP.setValue(GUIPreferences.IMAGE_CHOOSER_POS_Y, getLocation().y);
        GUIP.setValue(GUIPreferences.IMAGE_CHOOSER_SIZE_WIDTH, getSize().width);
        GUIP.setValue(GUIPreferences.IMAGE_CHOOSER_SIZE_HEIGHT, getSize().height);
        getChooser().saveWindowSettings();
    }
}
