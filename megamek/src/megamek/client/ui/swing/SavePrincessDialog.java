/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * Created with IntelliJ IDEA.
 * User: Deric
 * Date: 10/20/12
 * Time: 1:03 PM
 */
public class SavePrincessDialog extends JDialog implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = -8941569745045361519L;
    JLabel question;
    JCheckBox saveTargets;
    JButton yesButton;
    JButton noButton;
    boolean save = false;
    boolean saveTargetList = false;
    ResourceBundle bundle = ResourceBundle.getBundle("megamek.client.messages");

    public SavePrincessDialog(Container parent) {
        initGUI(parent);
    }

    private void initGUI(Container parent) {
        setLayout(new GridLayout(3, 1));
        setModal(true);

        JPanel questionPanel = new JPanel();
        question = new JLabel(bundle.getString("SavePrincessDialog.question"));
        questionPanel.add(question);
        add(questionPanel);

        JPanel saveTargetsPanel = new JPanel();
        saveTargets = new JCheckBox(bundle.getString("SavePrincessDialog.saveTargets"));
        saveTargetsPanel.add(saveTargets);
        add(saveTargetsPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        yesButton = new JButton(bundle.getString("Yes"));
        yesButton.addActionListener(this);
        buttonPanel.add(yesButton);

        noButton = new JButton(bundle.getString("No"));
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
