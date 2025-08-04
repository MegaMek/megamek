/*
 * Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import megamek.client.ui.Messages;
import megamek.common.Configuration;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * A dialog of which exits are connected for terrain.
 */
public class ExitsDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -3126840102187553386L;
    JToggleButton cheExit0;
    JToggleButton cheExit1;
    JToggleButton cheExit2;
    JToggleButton cheExit3;
    JToggleButton cheExit4;
    JToggleButton cheExit5;
    private JLabel labBlank = new JLabel("                            ");
    private JPanel panNorth = new JPanel(new GridBagLayout());
    private JPanel panSouth = new JPanel(new GridBagLayout());
    private JPanel panWest = new JPanel(new BorderLayout());
    private JPanel panEast = new JPanel(new BorderLayout());
    private JPanel panExits = new JPanel(new BorderLayout());
    private JButton butDone = new JButton(Messages.getString("BoardEditor.Done"));

    public ExitsDialog(JFrame frame) {
        super(frame, Messages.getString("BoardEditor.SetExits"), true);
        setResizable(false);
        butDone.addActionListener(this);
        cheExit0 = setupTButton("ToggleEx", "0");
        cheExit1 = setupTButton("ToggleEx", "1");
        cheExit2 = setupTButton("ToggleEx", "2");
        cheExit3 = setupTButton("ToggleEx", "3");
        cheExit4 = setupTButton("ToggleEx", "4");
        cheExit5 = setupTButton("ToggleEx", "5");
        panNorth.add(cheExit0);
        panSouth.add(cheExit3);
        panWest.add(cheExit5, BorderLayout.NORTH);
        panWest.add(cheExit4, BorderLayout.SOUTH);
        panEast.add(cheExit1, BorderLayout.NORTH);
        panEast.add(cheExit2, BorderLayout.SOUTH);
        panExits.add(panNorth, BorderLayout.NORTH);
        panExits.add(panWest, BorderLayout.WEST);
        panExits.add(labBlank, BorderLayout.CENTER);
        panExits.add(panEast, BorderLayout.EAST);
        panExits.add(panSouth, BorderLayout.SOUTH);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panExits, BorderLayout.CENTER);
        getContentPane().add(butDone, BorderLayout.SOUTH);
        pack();
        setLocation(frame.getLocation().x + frame.getSize().width / 2
              - getSize().width / 2, frame.getLocation().y
              + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void setExits(int exits) {
        cheExit0.setSelected((exits & 1) != 0);
        cheExit1.setSelected((exits & 2) != 0);
        cheExit2.setSelected((exits & 4) != 0);
        cheExit3.setSelected((exits & 8) != 0);
        cheExit4.setSelected((exits & 16) != 0);
        cheExit5.setSelected((exits & 32) != 0);
    }

    public int getExits() {
        int exits = 0;
        exits |= cheExit0.isSelected() ? 1 : 0;
        exits |= cheExit1.isSelected() ? 2 : 0;
        exits |= cheExit2.isSelected() ? 4 : 0;
        exits |= cheExit3.isSelected() ? 8 : 0;
        exits |= cheExit4.isSelected() ? 16 : 0;
        exits |= cheExit5.isSelected() ? 32 : 0;
        return exits;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        setVisible(false);
    }

    /**
     * Sets up JToggleButtons
     */
    JToggleButton setupTButton(String iconName, String buttonName) {
        JToggleButton button = new JToggleButton(buttonName);

        // Get the normal icon
        File file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/" + iconName + ".png").getFile();
        Image imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null) {
            button.setIcon(new ImageIcon(imageButton));
            // When there is an icon, then the text can be removed
            button.setText("");
        }

        // Get the hover icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/" + iconName + "_H.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null) {
            button.setRolloverIcon(new ImageIcon(imageButton));
        }

        // Get the selected icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/" + iconName + "_S.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton != null) {
            button.setSelectedIcon(new ImageIcon(imageButton));
        }

        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createEmptyBorder());

        return button;
    }
}
