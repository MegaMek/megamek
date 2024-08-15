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

import megamek.client.ui.Messages;
import megamek.client.ui.swing.dialog.DialogButton;
import megamek.client.ui.swing.util.UIUtil;
import megamek.server.scriptedevent.NarrativeDisplayProvider;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * This is the base class for MM dialogs that have a similar look to Story Arc dialogs. Instead of the
 * StoryArc object they use the generalized NarrativeDisplayProvider interface. StoryArc objects could
 * eventually be adapted to use the same interface.
 * Inheriting classes must call initialize() in their constructors and override getMainPanel()
 */
public abstract class MMStoryDialog extends JDialog {

    protected static final String CLOSE_ACTION = "closeAction";

    private int imgWidth = 0;
    private int imgHeight = 450;
    private final NarrativeDisplayProvider storyPoint;

    public MMStoryDialog(JFrame parent, NarrativeDisplayProvider sEvent) {
        super(parent, sEvent.header(), true);
        this.storyPoint = sEvent;
        // Escape keypress
        final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, CLOSE_ACTION);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, CLOSE_ACTION);
        getRootPane().getActionMap().put(CLOSE_ACTION, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent evt) {
                close();
            }
        });
    }

    protected void initialize() {
        setLayout(new BorderLayout());
        add(getMainPanel(), BorderLayout.CENTER);
        add(getButtonPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);
    }

    private JPanel getButtonPanel() {
        JButton okButton = new DialogButton(Messages.getString("Ok.text"));
        okButton.addActionListener(e -> close());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
                new UIUtil.ScaledEmptyBorder(10, 0, 10, 0)));

        Box verticalBox = Box.createVerticalBox();
        verticalBox.add(Box.createVerticalGlue());
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.add(Box.createHorizontalGlue());
        verticalBox.add(panel);
        verticalBox.add(Box.createVerticalGlue());

        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        rightButtonPanel.add(okButton);
        getRootPane().setDefaultButton(okButton);

        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(verticalBox);
        buttonPanel.add(rightButtonPanel);
        return buttonPanel;
    }

    protected abstract Container getMainPanel();

    protected NarrativeDisplayProvider getStoryPoint() {
        return storyPoint;
    }

    protected JPanel getImagePanel() {
        JPanel imagePanel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(imgWidth, imgHeight);
            }
        };

        Image img = storyPoint.splashImage();
        if (storyPoint.portrait() != null) {
            img = storyPoint.portrait();
        }

        if (null != img) {
            ImageIcon icon = new ImageIcon(img);
            imgWidth = icon.getIconWidth();
            imgHeight = icon.getIconHeight();
            JLabel imgLbl = new JLabel();
            imgLbl.setIcon(icon);
            imagePanel.add(imgLbl, BorderLayout.CENTER);
        }

        //we can grab and put here in an image panel
        return imagePanel;
    }

    protected void close() {
        setVisible(false);
        dispose();
    }
}
