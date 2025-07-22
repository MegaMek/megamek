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
package megamek.client.ui.dialogs.unitSelectorDialogs;

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
import javax.swing.JToggleButton;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.WrapLayout;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.UIUtil;
import megamek.common.Entity;
import megamek.client.ui.unitreadout.EntityReadout;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.annotations.Nullable;

/**
 * This class wraps the MekView / MekViewPanel and gives it a toolbar to choose font, open the MUL
 * and copy the contents.
 */
public class ConfigurableMekViewPanel extends JPanel {

    private final JComboBox<String> fontChooser;
    private final JToggleButton detailButton = new JToggleButton(Messages.getString("CMVPanel.detail"));
    private final JButton copyHtmlButton = new JButton(Messages.getString("CMVPanel.copyHTML"));
    private final JButton copyTextButton = new JButton(Messages.getString("CMVPanel.copyText"));
    private final JButton copyDiscordButton = new JButton(Messages.getString("CMVPanel.copyDiscord"));
    private final JButton mulButton = new JButton(Messages.getString("CMVPanel.MUL"));
    private final EntityReadoutPanel entityReadoutPanel = new EntityReadoutPanel();
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
        copyDiscordButton.addActionListener(ev -> copyToClipboard(ViewFormatting.DISCORD));
        detailButton.addActionListener(ev -> updateReadout());

        mulButton.addActionListener(ev -> UIUtil.showMUL(mulId, this));
        mulButton.setToolTipText("Show the Master Unit List entry for this unit. Opens a browser window.");

        var chooserLine = new UIUtil.FixedYPanel(new WrapLayout(FlowLayout.LEFT, 15, 10));
        JPanel fontChooserPanel = new JPanel();
        fontChooserPanel.add(new JLabel(Messages.getString("CMVPanel.font")));
        fontChooserPanel.add(fontChooser);
        chooserLine.add(fontChooserPanel);
        chooserLine.add(detailButton);
        chooserLine.add(copyHtmlButton);
        chooserLine.add(copyTextButton);
        chooserLine.add(copyDiscordButton);
        chooserLine.add(mulButton);

        add(chooserLine);
        add(entityReadoutPanel);
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
        copyDiscordButton.setEnabled(entity != null);
        detailButton.setEnabled((entity != null) && entity.usesWeaponBays());
        updateReadout();
    }

    /** Set the card to use a newly selected font. */
    private void updateFont() {
        if (entity != null) {
            String selectedItem = (String) fontChooser.getSelectedItem();
            if ((selectedItem == null) || selectedItem.isBlank()) {
                entityReadoutPanel.showEntity(entity, MMConstants.FONT_SANS_SERIF);
                GUIPreferences.getInstance().setSummaryFont("");
            } else {
                entityReadoutPanel.showEntity(entity, selectedItem);
                GUIPreferences.getInstance().setSummaryFont(selectedItem);
            }
        }
    }

    private boolean detail() {
        return detailButton.isSelected();
    }

    private boolean alternateCost() {
        // for now
        return false;
    }

    private boolean pilotBV(Entity entity) {
        // for now
        return entity.getCrew() != null;
    }

    public void reset() {
        entityReadoutPanel.reset();
    }

    private void copyToClipboard(ViewFormatting formatting) {
        if (entity != null) {
            EntityReadout readout = EntityReadout.createReadout(entity, detail(), alternateCost(), formatting);
            StringSelection stringSelection = new StringSelection(readout.getReadout(null, formatting));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        }
    }

    private void updateReadout() {
        if (entity != null) {
            entityReadoutPanel.showEntity(entity, detail(), alternateCost(), !pilotBV(entity),
                  ViewFormatting.HTML, GUIPreferences.getInstance().getSummaryFont());
        } else {
            entityReadoutPanel.reset();
        }
    }
}
