/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import megamek.client.ui.Messages;

/**
 * Created by User: Deric "Netzilla" Page (deric dot page at usa dot net)
 * A confirmation dialog for saving changes to a Princess bot that will
 * also ask if changes should be saved to a target list preset 
 */
public class SavePrincessDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = -8941569745045361519L;
    
    JLabel question;
    JCheckBox saveTargets;
    JButton yesButton;
    JButton noButton;
    boolean save = false;
    boolean saveTargetList = false;

    public SavePrincessDialog(Container parent) {
        initGUI(parent);
    }

    private void initGUI(Container parent) {
        setLayout(new GridLayout(3, 1));
        setModal(true);

        JPanel questionPanel = new JPanel();
        question = new JLabel(Messages.getString("SavePrincessDialog.question"));
        questionPanel.add(question);
        add(questionPanel);

        JPanel saveTargetsPanel = new JPanel();
        saveTargets = new JCheckBox(Messages.getString("SavePrincessDialog.saveTargets"));
        saveTargetsPanel.add(saveTargets);
        add(saveTargetsPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        yesButton = new JButton(Messages.getString("Yes"));
        yesButton.addActionListener(this);
        buttonPanel.add(yesButton);

        noButton = new JButton(Messages.getString("No"));
        noButton.addActionListener(this);
        buttonPanel.add(noButton);

        add(buttonPanel);

        pack();
        validate();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (yesButton.equals(e.getSource())) {
            save = true;
            saveTargetList = saveTargets.isSelected();
            setVisible(false);
        } else if (noButton.equals(e.getSource())) {
            save = false;
            saveTargetList = false;
            setVisible(false);
        }
        System.out.println("Save: " + save + "\tSave Target List: " + saveTargetList);
    }

    public boolean doSave() {
        return save;
    }

    public boolean doSaveTargets() {
        return saveTargetList;
    }
}
