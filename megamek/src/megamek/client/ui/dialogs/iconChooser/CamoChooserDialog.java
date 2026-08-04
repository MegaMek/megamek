/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.iconChooser;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;

import megamek.client.ui.Messages;
import megamek.client.ui.WrapLayout;
import megamek.client.ui.buttons.DialogButton;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.battlefieldSupport.OverlayStyle;
import megamek.common.battlefieldSupport.StripeDirection;
import megamek.common.units.Entity;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;

/**
 * This dialog allows players to select the camouflage pattern (or colour) used by their units. It automatically fills
 * itself with all the PlayerColour Enum colours and all the camouflage icons in the Camouflage directory.
 *
 * @see AbstractIconChooserDialog
 */
public class CamoChooserDialog extends AbstractIconChooserDialog {
    //region Variable Declarations
    private boolean useDefault = false;
    private JSlider rotationSlider;
    private JSlider scaleSlider;
    private final Camouflage originalCamo;
    private final List<Entity> previewEntities = new ArrayList<>();
    private final List<EntityImagePanel> previewPanels = new ArrayList<>();
    private JPanel previewContainer;

    // Battlefield Support Asset marker-overlay controls, shown only when a displayed unit is an asset.
    private JPanel overlayPanel;
    private JButton overlayColorButton;
    private JComboBox<StripeDirection> stripeDirectionCombo;
    private JComboBox<OverlayStyle> styleCombo;
    private boolean assetOverlayMode = false;
    private Color overlayColor = Camouflage.DEFAULT_ASSET_OVERLAY_COLOR;
    //endregion Variable Declarations

    //region Constructors
    public CamoChooserDialog(final JFrame frame, final @Nullable AbstractIcon camouflage) {
        this(frame, camouflage, false);
    }

    public CamoChooserDialog(final JFrame frame, final @Nullable AbstractIcon camouflage,
          final boolean canHaveIndividualCamouflage) {
        super(frame, "CamoChooserDialog", "CamoChoiceDialog.select_camo_pattern",
              new CamoChooserPanel(frame, camouflage, canHaveIndividualCamouflage), true);
        originalCamo = (Camouflage) camouflage;

        // Ensure the dialog is wide/tall enough to show the (asset) camo controls without scrolling, even if a smaller
        // size was remembered from a previous session before those controls existed.
        Dimension minimumSize = new Dimension(900, 760);
        setMinimumSize(minimumSize);
        if ((getWidth() < minimumSize.width) || (getHeight() < minimumSize.height)) {
            setSize(Math.max(getWidth(), minimumSize.width), Math.max(getHeight(), minimumSize.height));
            setLocationRelativeTo(frame);
        }
    }
    //endregion Constructors

    //region Getters/Setters
    public boolean isUseDefault() {
        return useDefault;
    }

    /** Sets a single unit to preview in the dialog. See {@link #setDisplayedEntities(List)}. */
    public void setDisplayedEntity(Entity entity) {
        setDisplayedEntities((entity == null) ? List.of() : List.of(entity));
    }

    /**
     * Sets the units to preview in the dialog. Up to two are shown; when the selection mixes assets and non-assets, one
     * of each is chosen so both looks are visible, otherwise the first two are used. The asset marker-overlay controls
     * are revealed when any displayed unit is a Battlefield Support Asset.
     *
     * @param entities the candidate units to preview (may be empty)
     */
    public void setDisplayedEntities(List<Entity> entities) {
        previewEntities.clear();
        previewEntities.addAll(choosePreviewEntities(entities));
        assetOverlayMode = previewEntities.stream().anyMatch(e -> e instanceof BattlefieldSupportAsset);
        if (overlayPanel != null) {
            overlayPanel.setVisible(assetOverlayMode);
        }
        rebuildPreviewPanels();
        updatePreview();
    }

    /** @return up to two units to preview: one asset + one non-asset when the list has both, else the first two. */
    private static List<Entity> choosePreviewEntities(List<Entity> entities) {
        Entity asset = entities.stream().filter(e -> e instanceof BattlefieldSupportAsset).findFirst().orElse(null);
        Entity nonAsset = entities.stream().filter(e -> !(e instanceof BattlefieldSupportAsset)).findFirst()
              .orElse(null);
        List<Entity> chosen = new ArrayList<>();
        if ((asset != null) && (nonAsset != null)) {
            chosen.add(asset);
            chosen.add(nonAsset);
        } else {
            entities.stream().limit(2).forEach(chosen::add);
        }
        return chosen;
    }

    public void setUseDefault(final boolean useDefault) {
        this.useDefault = useDefault;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected JPanel createButtonPanel() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));

        rotationSlider = new JSlider(-180, 180);
        rotationSlider.setMajorTickSpacing(90);
        rotationSlider.setMinorTickSpacing(10);
        rotationSlider.setPaintTicks(true);
        rotationSlider.setSnapToTicks(true);
        rotationSlider.addChangeListener(e -> updatePreview());
        rotationSlider.setPaintLabels(true);

        scaleSlider = new JSlider(3, 15);
        scaleSlider.setSnapToTicks(true);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setMajorTickSpacing(1);
        scaleSlider.setPaintLabels(true);
        Hashtable<Integer, JComponent> labelTable = new Hashtable<>();
        labelTable.put(5, new JLabel("0.5"));
        labelTable.put(10, new JLabel("1"));
        labelTable.put(15, new JLabel("1.5"));
        scaleSlider.setLabelTable(labelTable);
        scaleSlider.addChangeListener(e -> updatePreview());

        previewContainer = new JPanel();

        JPanel rotationPanel = new JPanel();
        rotationPanel.add(Box.createHorizontalStrut(20));
        rotationPanel.add(new JLabel(Messages.getString("CamoChoiceDialog.rotation") + ":"));
        rotationPanel.add(rotationSlider);

        JPanel scalePanel = new JPanel();
        scalePanel.add(Box.createHorizontalStrut(30));
        scalePanel.add(new JLabel(Messages.getString("CamoChoiceDialog.scale") + ":"));
        scalePanel.add(scaleSlider);

        overlayPanel = createOverlayPanel();

        // The preview(s) and the rotation/scale sliders sit on one row; the (wider set of) asset marker controls get
        // their own row below so they don't force the dialog to scroll horizontally.
        JPanel topRow = new JPanel();
        topRow.add(previewContainer);
        topRow.add(rotationPanel);
        topRow.add(scalePanel);
        topRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        overlayPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel modifierPanel = new JPanel();
        modifierPanel.setLayout(new BoxLayout(modifierPanel, BoxLayout.PAGE_AXIS));
        modifierPanel.add(topRow);
        modifierPanel.add(overlayPanel);

        JScrollPane modifierScrollPane = new JScrollPane(modifierPanel,
              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        var okButton = new DialogButton(Messages.getString("Ok.text"));
        okButton.addActionListener(this::okButtonActionPerformed);
        getRootPane().setDefaultButton(okButton);

        var cancelButton = new DialogButton(Messages.getString("Cancel.text"));
        cancelButton.addActionListener(this::cancelActionPerformed);

        var refreshButton = new DialogButton(Messages.getString("RefreshDirectory.text"));
        refreshButton.setToolTipText(Messages.getString("RefreshDirectory.toolTipText"));
        refreshButton.addActionListener(evt -> getChooser().refreshDirectory());

        var parentCamoButton = new DialogButton(Messages.getString("CamoChoiceDialog.btnParent.text"));
        parentCamoButton.setToolTipText(Messages.getString("CamoChoiceDialog.btnParent.toolTipText"));
        parentCamoButton.addActionListener(evt -> {
            setUseDefault(true);
            okButtonActionPerformed(evt);
        });
        parentCamoButton.setEnabled(getChooser().canHaveIndividualCamouflage());

        final JPanel buttonPanel = new JPanel(new WrapLayout(FlowLayout.RIGHT));
        buttonPanel.setName("buttonPanel");
        buttonPanel.add(parentCamoButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(Box.createHorizontalStrut(30));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        container.add(modifierScrollPane);
        container.add(buttonPanel);

        return container;
    }
    //endregion Initialization

    @Override
    protected CamoChooserPanel getChooser() {
        return (CamoChooserPanel) super.getChooser();
    }

    @Override
    public Camouflage getSelectedItem() {
        Camouflage result = new Camouflage();
        if (!isUseDefault() && (super.getSelectedItem() != null)) {
            result = ((Camouflage) super.getSelectedItem()).clone();
        }
        result.setScale(scaleSlider.getValue());
        result.setRotationAngle(rotationSlider.getValue());
        // The asset marker overlay rides on the camo, so a unit that uses this camo (individually or via the player)
        // shares the overlay. Non-asset units simply ignore it.
        result.setOverlayColor(overlayColor);
        result.setOverlayDirection((StripeDirection) stripeDirectionCombo.getSelectedItem());
        result.setOverlayStyle((OverlayStyle) styleCombo.getSelectedItem());
        return result;
    }

    @Override
    public void setVisible(boolean b) {
        if ((originalCamo != null) && b) {
            rotationSlider.setValue(originalCamo.getRotationAngle());
            scaleSlider.setValue(originalCamo.getScale());
            overlayColor = originalCamo.getOverlayColor();
            overlayColorButton.setBackground(overlayColor);
            stripeDirectionCombo.setSelectedItem(originalCamo.getOverlayDirection());
            styleCombo.setSelectedItem(originalCamo.getOverlayStyle());
        }
        if (b) {
            getChooser().getImageList().addListSelectionListener(e -> updatePreview());
        }
        super.setVisible(b);
    }

    /** Rebuilds the preview panels to match the chosen preview entities (at most two). */
    private void rebuildPreviewPanels() {
        if (previewContainer == null) {
            return;
        }
        previewContainer.removeAll();
        previewPanels.clear();
        for (Entity ignored : previewEntities) {
            EntityImagePanel panel = new EntityImagePanel(null, null);
            previewPanels.add(panel);
            previewContainer.add(panel);
        }
        previewContainer.revalidate();
        previewContainer.repaint();
    }

    private void updatePreview() {
        Camouflage camo = getSelectedItem();
        if (camo == null) {
            return;
        }
        for (int i = 0; i < previewEntities.size() && i < previewPanels.size(); i++) {
            previewPanels.get(i).updateDisplayedEntity(previewEntities.get(i), camo);
        }
    }

    /** Builds the (initially hidden) Battlefield Support Asset marker-overlay controls: color, direction and style. */
    private JPanel createOverlayPanel() {
        JPanel panel = new JPanel();
        panel.add(Box.createHorizontalStrut(20));
        panel.add(new JLabel(Messages.getString("CamoChoiceDialog.overlayLabel") + ":"));

        styleCombo = new JComboBox<>(OverlayStyle.values());
        styleCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof OverlayStyle style) {
                    setText(Messages.getString("CamoChoiceDialog.overlayStyle." + style.name()));
                }
                return this;
            }
        });
        styleCombo.setToolTipText(Messages.getString("CamoChoiceDialog.overlayStyle.tooltip"));
        styleCombo.addActionListener(e -> updatePreview());
        panel.add(styleCombo);

        stripeDirectionCombo = new JComboBox<>(StripeDirection.values());
        stripeDirectionCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof StripeDirection direction) {
                    setText(Messages.getString("CamoChoiceDialog.overlayDirection." + direction.name()));
                }
                return this;
            }
        });
        stripeDirectionCombo.addActionListener(e -> updatePreview());
        panel.add(stripeDirectionCombo);

        overlayColorButton = new JButton(Messages.getString("CamoChoiceDialog.overlayColor"));
        overlayColorButton.setOpaque(true);
        overlayColorButton.setBackground(overlayColor);
        overlayColorButton.addActionListener(e -> chooseOverlayColor());
        panel.add(overlayColorButton);

        panel.setVisible(false);
        return panel;
    }

    private void chooseOverlayColor() {
        Color chosen = JColorChooser.showDialog(this, Messages.getString("CamoChoiceDialog.overlayColor"), overlayColor);
        if (chosen != null) {
            overlayColor = chosen;
            overlayColorButton.setBackground(chosen);
            updatePreview();
        }
    }
}
