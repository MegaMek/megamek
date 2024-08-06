/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs;

import megamek.server.scriptedevent.NarrativeDisplayProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This is the base class for dialogs related to the Story Arc, to help create a similar look and feel.
 * Inheriting classes must call initialize() in their constructors and override getMainPanel()
 */
public abstract class MMStoryDialog extends JDialog implements ActionListener {

    private JButton doneButton;
    private int imgWidth;
    private final NarrativeDisplayProvider storyPoint;

    public MMStoryDialog(final JFrame parent, NarrativeDisplayProvider sEvent) {
        super(parent, sEvent.header(), true);
        this.storyPoint = sEvent;
    }

    protected void initialize() {
        setLayout(new BorderLayout());
        add(getButtonPanel(), BorderLayout.SOUTH);
        add(getMainPanel(), BorderLayout.CENTER);

        setDialogSize();
        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);
    }

    private JPanel getButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout());

        doneButton = new JButton("Done");
        doneButton.addActionListener(this);
        buttonPanel.add(doneButton, BorderLayout.LINE_END);

        return buttonPanel;
    }

    protected abstract Container getMainPanel();

    protected NarrativeDisplayProvider getStoryPoint() {
        return storyPoint;
    }

    protected JPanel getImagePanel() {
        JPanel imagePanel = new JPanel(new BorderLayout());

        imgWidth = 0;
        Image img = getStoryPoint().splashImage();
        if (getStoryPoint().portrait() != null) {
            img = getStoryPoint().portrait();
        }

        if (null != img) {
            ImageIcon icon = new ImageIcon(img);
            imgWidth = icon.getIconWidth();
            JLabel imgLbl = new JLabel();
            imgLbl.setIcon(icon);
            imagePanel.add(imgLbl, BorderLayout.CENTER);
//            if(null != p) {
//                //add a caption
//                imagePanel.add(new JLabel(p.getTitle(), SwingConstants.CENTER), BorderLayout.PAGE_END);
//            }
        }

        //we can grab and put here in an image panel
        return imagePanel;
    }

    protected void setDialogSize() {

        int width = 400+imgWidth;
        int height = 400;
        setMinimumSize(new Dimension(width, height));
        setPreferredSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (doneButton.equals(e.getSource())) {
            this.setVisible(false);
        }
    }
}
