/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panes;

import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.ViewFormatting;
import megamek.client.ui.WrapLayout;
import megamek.client.ui.mekview.MekView;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.MekViewPanel;
import megamek.client.ui.swing.util.FontHandler;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;

/**
 * This class wraps the MekView / MekViewPanel and gives it a toolbar to choose font, open the MUL
 * and copy the contents.
 */
public class ConfigurableMekViewPanel extends JPanel {

    private final JComboBox<String> fontChooser;
    private final JButton copyHtmlButton = new JButton(Messages.getString("CMVPanel.copyHTML"));
    private final JButton copyTextButton = new JButton(Messages.getString("CMVPanel.copyText"));
    private final JButton mulButton = new JButton(Messages.getString("CMVPanel.MUL"));
    private final MekViewPanel mekViewPanel = new MekViewPanel();
    private int mulId;
    private Entity entity;

    /**
     * Constructs a panel with the given unit to display.
     *
     * @param entity The Entity to display
     */
    public ConfigurableMekViewPanel(@Nullable Entity entity) {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        fontChooser = new JComboBox<>(new Vector<>(FontHandler.getAvailableNonSymbolFonts()));
        fontChooser.addItem("");
        fontChooser.addActionListener(ev -> updateFont());
        fontChooser.setSelectedItem(GUIPreferences.getInstance().getSummaryFont());

        copyHtmlButton.addActionListener(ev -> copyToClipboard(ViewFormatting.HTML));
        copyTextButton.addActionListener(ev -> copyToClipboard(ViewFormatting.NONE));
        // todo: create a copyDiscordButton
        // The implementer of the Discord export cared only about the MML UI.

        mulButton.addActionListener(ev -> UIUtil.showMUL(mulId, this));
        mulButton.setToolTipText("Show the Master Unit List entry for this unit. Opens a browser window.");

        var chooserLine = new UIUtil.FixedYPanel(new WrapLayout(FlowLayout.LEFT, 15, 10));
        JPanel fontChooserPanel = new JPanel();
        fontChooserPanel.add(new JLabel(Messages.getString("CMVPanel.font")));
        fontChooserPanel.add(fontChooser);
        chooserLine.add(fontChooserPanel);
        chooserLine.add(copyHtmlButton);
        chooserLine.add(copyTextButton);
        chooserLine.add(mulButton);

        add(chooserLine);
        add(mekViewPanel);
        setEntity(entity);
    }

    /** Construct a new panel without a unit to display. */
    public ConfigurableMekViewPanel() {
        this(null);
    }

    /**
     * Set the panel to display the given element.
     *
     * @param entity The Entity to display
     */
    public void setEntity(@Nullable Entity entity) {
        this.entity = entity;
        mulId = (entity != null) ? entity.getMulId() : -1;
        mulButton.setEnabled(mulId > 0);
        copyTextButton.setEnabled(entity != null);
        copyHtmlButton.setEnabled(entity != null);
        if (entity != null) {
            mekViewPanel.setMek(entity, GUIPreferences.getInstance().getSummaryFont());
        } else {
            mekViewPanel.reset();
        }
    }

    /** Set the card to use a newly selected font. */
    private void updateFont() {
        if (entity != null) {
            String selectedItem = (String) fontChooser.getSelectedItem();
            if ((selectedItem == null) || selectedItem.isBlank()) {
                mekViewPanel.setMek(entity, MMConstants.FONT_SANS_SERIF);
                GUIPreferences.getInstance().setSummaryFont("");
            } else {
                mekViewPanel.setMek(entity, selectedItem);
                GUIPreferences.getInstance().setSummaryFont(selectedItem);
            }
        }
    }

    public void reset() {
        mekViewPanel.reset();
    }

    private void copyToClipboard(ViewFormatting formatting) {
        if (entity != null) {
            MekView mekView = new MekView(entity, false, false, formatting);
            StringSelection stringSelection = new StringSelection(mekView.getMekReadout());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        }
    }
}
