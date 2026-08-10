/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import megamek.client.ui.util.UIUtil;
import megamek.common.ui.FastJScrollPane;

/** Sticky contextual help surface for a settings content host. */
public class SettingsHelpPanel extends JPanel {
    private static final String TITLE_KEY = "SettingsHelpPanel.title";
    private static final int HELP_PANEL_HEIGHT = 120;
    private static final int HELP_TEXT_VERTICAL_PADDING = 4;
    private static final int HELP_TEXT_HORIZONTAL_PADDING = 8;

    private final JEditorPane helpTextPane = new JEditorPane();

    public SettingsHelpPanel() {
        super(new BorderLayout());
        setName("settingsHelpPanel");
        Border frameBorder = UIManager.getBorder("ScrollPane.border");
        if (frameBorder == null) {
            frameBorder = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"),
                  UIUtil.scaleForGUI(1));
        }
        setBorder(new FlushTitledBorder(frameBorder, SettingsTextProvider.megaMek().getText(TITLE_KEY)));

        helpTextPane.setName("settingsHelpText");
        helpTextPane.setContentType("text/html");
        helpTextPane.setEditable(false);
        helpTextPane.setOpaque(false);
        helpTextPane.setBorder(new EmptyBorder(UIUtil.scaleForGUI(HELP_TEXT_VERTICAL_PADDING),
              UIUtil.scaleForGUI(HELP_TEXT_HORIZONTAL_PADDING),
              UIUtil.scaleForGUI(HELP_TEXT_VERTICAL_PADDING),
              UIUtil.scaleForGUI(HELP_TEXT_HORIZONTAL_PADDING)));
        helpTextPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        JScrollPane scrollPane = new FastJScrollPane(helpTextPane,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setName("settingsHelpScrollPane");
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);
        clearHelpText();
    }

    /** @deprecated settings help surfaces always use the shared localized title */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public SettingsHelpPanel(String ignoredTitle) {
        this();
    }

    public void setHelpText(String helpText) {
        if (helpText == null || helpText.isBlank()) {
            clearHelpText();
            return;
        }
        helpTextPane.setText(helpText);
        helpTextPane.setCaretPosition(0);
    }

    public void clearHelpText() {
        helpTextPane.setText("");
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        return new Dimension(preferred.width, UIUtil.scaleForGUI(HELP_PANEL_HEIGHT));
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension minimum = super.getMinimumSize();
        return new Dimension(minimum.width, UIUtil.scaleForGUI(HELP_PANEL_HEIGHT));
    }

    private static class FlushTitledBorder extends TitledBorder {
        private static final int EDGE_SPACING = 2;

        private FlushTitledBorder(Border border, String title) {
            super(border, title);
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            int edgeSpacing = UIUtil.scaleForGUI(EDGE_SPACING);
            super.paintBorder(component, graphics, x - edgeSpacing, y,
                  width + 2 * edgeSpacing, height + edgeSpacing);
        }
    }
}
