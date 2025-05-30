/*
 * Copyright (c) 2013-2020, 2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.helpDialogs;

import java.awt.event.KeyEvent;
import java.net.URL;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import megamek.client.ui.clientGUI.CloseAction;
import megamek.logging.MMLogger;

/**
 * This is a basic help dialog that can display HTML pages and also reacts to hyperlink clicks.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @author Simon (Juliez)
 */
public class HelpDialog extends JDialog {
    private final static MMLogger logger = MMLogger.create(HelpDialog.class);

    protected static final String CLOSE_ACTION = "closeAction";

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    public HelpDialog(String title, URL helpURL, JFrame parent) {
        super(parent, title, true);

        JEditorPane mainView = new JEditorPane();
        mainView.setEditable(false);
        try {
            mainView.setPage(helpURL);
        } catch (Exception ex) {
            logger.debug("Help File Not Found: {}", helpURL);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }

        // Listen for the user clicking on hyperlinks.
        mainView.addHyperlinkListener(e -> {
            try {
                if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
                    mainView.setPage(e.getURL());
                }
            } catch (Exception ex) {
                logger.error(ex, "");
                JOptionPane.showMessageDialog(this, ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        });

        JScrollPane scrollPane = new JScrollPane(mainView);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(scrollPane);

        // Escape keypress
        final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, CLOSE_ACTION);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, CLOSE_ACTION);
        getRootPane().getActionMap().put(CLOSE_ACTION, new CloseAction(this));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
    }
}
