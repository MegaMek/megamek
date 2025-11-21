/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.unitSelectorDialogs;

import static megamek.client.ui.entityreadout.ReadoutSections.*;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.WrapLayout;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.entityreadout.EntityReadout;
import megamek.client.ui.entityreadout.ReadoutSections;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;

/**
 * This class wraps the MekView / MekViewPanel and gives it a toolbar to choose font, open the MUL and copy the
 * contents.
 */
public class ConfigurableMekViewPanel extends JPanel {

    private final JComboBox<String> fontChooser;
    private final JToggleButton detailButton = new JToggleButton(Messages.getString("CMVPanel.detail"));
    private final JButton copyHtmlButton = new JButton(Messages.getString("CMVPanel.copyHTML"));
    private final JButton copyTextButton = new JButton(Messages.getString("CMVPanel.copyText"));
    private final JButton copyDiscordButton = new JButton(Messages.getString("CMVPanel.copyDiscord"));
    private final JButton mulButton = new JButton(Messages.getString("CMVPanel.MUL"));
    private final EntityReadoutPanel entityReadoutPanel = new EntityReadoutPanel();
    private final JComboBox<SectionFormat> sectionsChooser = new JComboBox<>();
    private int mulId;
    private Entity entity;
    private final JComponent menuPanel;

    enum SectionFormat {
        FULL(ReadoutSections.values()),
        IN_GAME(HEADLINE, BASE_DATA, SYSTEMS, LOADOUT, QUIRKS),
        NO_FLUFF(HEADLINE, TECH_LEVEL, AVAILABILITY, COST_SOURCE, BASE_DATA, SYSTEMS, LOADOUT, QUIRKS),
        NONCOMBAT(HEADLINE, TECH_LEVEL, AVAILABILITY, COST_SOURCE, FLUFF, INVALID);

        final ReadoutSections[] sections;

        SectionFormat(ReadoutSections... sections) {
            this.sections = sections;
        }
    }

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

        Arrays.stream(SectionFormat.values()).forEach(sectionsChooser::addItem);
        sectionsChooser.addActionListener(ev -> updateReadout());
        sectionsChooser.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                return super.getListCellRendererComponent(list,
                      Messages.getString("CMVPanel." + value),
                      index,
                      isSelected,
                      cellHasFocus);
            }
        });

        menuPanel = new UIUtil.FixedYPanel(new WrapLayout(FlowLayout.LEFT, 15, 10));
        JPanel fontChooserPanel = new JPanel();
        fontChooserPanel.add(new JLabel(Messages.getString("CMVPanel.font")));
        fontChooserPanel.add(fontChooser);
        menuPanel.add(fontChooserPanel);
        menuPanel.add(detailButton);
        menuPanel.add(copyHtmlButton);
        menuPanel.add(copyTextButton);
        menuPanel.add(copyDiscordButton);
        menuPanel.add(mulButton);
        menuPanel.add(sectionsChooser);

        add(menuPanel);
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
        return false; // for now
    }

    private boolean pilotBV(Entity entity) {
        return !entity.isUncrewed(); // for now
    }

    public void reset() {
        entityReadoutPanel.reset();
    }

    private void copyToClipboard(ViewFormatting formatting) {
        if (entity != null) {
            EntityReadout readout = EntityReadout.createReadout(entity, detail(), alternateCost());
            StringSelection stringSelection = new StringSelection(readout.getReadout(null,
                  formatting,
                  getSelectedSections()));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        }
    }

    private void updateReadout() {
        if (entity != null) {
            entityReadoutPanel.showEntity(entity, detail(), alternateCost(), !pilotBV(entity),
                  GUIPreferences.getInstance().getSummaryFont(), getSelectedSections());
        } else {
            entityReadoutPanel.reset();
        }
    }

    private List<ReadoutSections> getSelectedSections() {
        SectionFormat format = (SectionFormat) sectionsChooser.getSelectedItem();
        if (format == null) {
            format = SectionFormat.FULL;
        }
        return Arrays.asList(format.sections);
    }

    public void toggleMenu(boolean menuVisible) {
        menuPanel.setVisible(menuVisible);
    }
}
