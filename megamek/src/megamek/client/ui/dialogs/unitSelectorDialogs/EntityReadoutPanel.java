/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2009-2026 The MegaMek Team. All Rights Reserved.
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

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import megamek.client.ui.FluffImageTooltip;
import megamek.client.ui.Messages;
import megamek.client.ui.entityreadout.EntityReadout;
import megamek.client.ui.entityreadout.ReadoutSections;
import megamek.client.ui.util.FluffImageHelper;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.Configuration;
import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.common.templates.TROView;
import megamek.common.units.Entity;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

/**
 * @author Jay Lawson
 * @since November 2, 2009
 */
public class EntityReadoutPanel extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(EntityReadoutPanel.class);

    private final int TOOLTIP_MAX_SIZE = 85;

    private final JTextPane readoutTextComponent = new JTextPane();
    private final JScrollPane scrollPane = new JScrollPane(readoutTextComponent);

    private final JLabel fluffImageLabel = new JLabel();
    private final List<FluffImageHelper.FluffImageRecord> fluffImageList = new ArrayList<>();
    private int fluffImageIndex = 0;
    private final JButton nextImageButton = new JButton(">");
    private final JButton prevImageButton = new JButton("<");
    private final JLabel imageInfoLabel = new JLabel("", JLabel.CENTER);

    public static final int DEFAULT_WIDTH = 360;

    /** The vertical gap between the fluff image and the info line below it, before GUI scaling. */
    private static final int IMAGE_INFO_GAP = 10;

    private static final String PLACEHOLDER_IMAGE_NAME =
            new File(Configuration.fluffImagesDir(), "fluff_placeholder.png").getPath();
    private static final Image PLACEHOLDER_IMAGE = readPlaceHolderImage();

    public EntityReadoutPanel() {
        this(-1, -1);
    }

    public EntityReadoutPanel(int width, int height) {
        Report.setupStylesheet(readoutTextComponent);
        readoutTextComponent.setEditable(false);
        readoutTextComponent.setBorder(new EmptyBorder(5, 10, 0, 0));
        if (width != -1) {
            readoutTextComponent.setMinimumSize(new Dimension(width, height));
            readoutTextComponent.setPreferredSize(new Dimension(width, height));
        }

        readoutTextComponent.addHyperlinkListener(pe -> {
            if (HyperlinkEvent.EventType.ACTIVATED == pe.getEventType()) {

                boolean isHttpAddress = pe.getURL().toString().startsWith("http");
                if (isHttpAddress) {
                    UIUtil.browse(pe.getURL().toString(), this);
                } else {
                    String reference = pe.getDescription();
                    if (reference != null && reference.startsWith("#")) {
                        reference = reference.substring(1);
                        String finalReference = reference;
                        SwingUtilities.invokeLater(() -> readoutTextComponent.scrollToReference(finalReference));
                    }
                }
            }
        });

        // Add mouse motion listener to show tooltips for links.
        readoutTextComponent.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int pos = readoutTextComponent.viewToModel2D(e.getPoint());
                if (pos >= 0 && readoutTextComponent.getDocument() instanceof HTMLDocument doc) {
                    Element element = doc.getCharacterElement(pos);
                    while (element != null) {
                        Object spanAttr = element.getAttributes().getAttribute(HTML.Tag.SPAN);
                        if (spanAttr instanceof SimpleAttributeSet attrs) {
                            String title = (String) attrs.getAttribute(HTML.Attribute.TITLE);
                            // Found a tooltip, line-wrap it
                            readoutTextComponent.setToolTipText(StringUtil.wrapLines(title, TOOLTIP_MAX_SIZE));
                            return;
                        }
                        // Try to recurse up the tree for an element with a title
                        element = element.getParentElement();
                    }
                }
                // No tooltip found, clear.
                readoutTextComponent.setToolTipText(null);
            }
        });
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        scrollPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        var textPanel = new JPanel(new GridLayout(1, 1));
        if (width != -1) {
            textPanel.setMinimumSize(new Dimension(width, height));
            textPanel.setPreferredSize(new Dimension(width, height));
        }
        textPanel.add(scrollPane);

        prevImageButton.setToolTipText(Messages.getString("EntityReadoutPanel.previousImage.toolTipText"));
        nextImageButton.setToolTipText(Messages.getString("EntityReadoutPanel.nextImage.toolTipText"));

        var imageControlsPanel = new UIUtil.FixedYPanel(new FlowLayout());
        imageControlsPanel.add(prevImageButton);
        imageControlsPanel.add(nextImageButton);

        imageControlsPanel.setAlignmentX(CENTER_ALIGNMENT);
        fluffImageLabel.setAlignmentX(CENTER_ALIGNMENT);
        imageInfoLabel.setAlignmentX(CENTER_ALIGNMENT);

        Box fluffPanel = Box.createVerticalBox();
        fluffPanel.setAlignmentY(TOP_ALIGNMENT);
        fluffPanel.add(imageControlsPanel);
        fluffPanel.add(fluffImageLabel);
        fluffPanel.add(Box.createVerticalStrut(UIUtil.scaleForGUI(IMAGE_INFO_GAP)));
        fluffPanel.add(imageInfoLabel);

        Box readoutAndFluffPanel = Box.createHorizontalBox();
        readoutAndFluffPanel.add(textPanel);
        readoutAndFluffPanel.add(fluffPanel);
        readoutAndFluffPanel.add(Box.createHorizontalGlue());
        setLayout(new BorderLayout());
        add(readoutAndFluffPanel);
        addMouseWheelListener(wheelForwarder);

        nextImageButton.addActionListener(event -> showNextFluffImage());
        prevImageButton.addActionListener(event -> showPrevFluffImage());
    }

    public void showEntity(Entity entity, EntityReadout mekView) {
        readoutTextComponent.setText(mekView.getFullReadout());
        readoutTextComponent.setCaretPosition(0);
        setFluffImage(entity);
    }

    public void showEntity(Entity entity, EntityReadout mekView, String fontName) {
        readoutTextComponent.setText(mekView.getFullReadout(fontName, ViewFormatting.HTML));
        readoutTextComponent.setCaretPosition(0);
        setFluffImage(entity);
    }

    public void showEntity(Entity entity, EntityReadout mekView, String fontName,
          Collection<ReadoutSections> sections) {
        showReadout(mekView, fontName, sections);
        setFluffImage(entity);
    }

    private void showReadout(EntityReadout readout, String fontName, Collection<ReadoutSections> sections) {
        readoutTextComponent.setText(readout.getReadout(fontName, ViewFormatting.HTML, sections));
        readoutTextComponent.setCaretPosition(0);
    }

    public void showEntity(Entity entity, TROView troView) {
        readoutTextComponent.setText(troView.processTemplate());
        readoutTextComponent.setCaretPosition(0);
        setFluffImage(entity);
    }

    public void showEntity(Entity entity, boolean useAlternateCost) {
        EntityReadout mekView = EntityReadout.createReadout(entity, false, useAlternateCost);
        showEntity(entity, mekView);
    }

    public void showEntity(Entity entity, String fontName) {
        EntityReadout mekView = EntityReadout.createReadout(entity,
              false,
              false,
              (entity.isUncrewed())
        );
        showEntity(entity, mekView, fontName);
    }

    public void showEntity(Entity entity, boolean showDetail, boolean useAlternateCost,
          boolean ignorePilotBV, String fontName, Collection<ReadoutSections> sections) {

        EntityReadout mekView = EntityReadout.createReadout(entity, showDetail, useAlternateCost,
              ignorePilotBV);
        showEntity(entity, mekView, fontName, sections);
    }

    /**
     * Shows the given image as the fluff image, scaled down to {@link #DEFAULT_WIDTH} if it is wider than that.
     *
     * @param image The image to show, or {@code null} to clear the fluff image
     */
    private void displayFluffImage(@Nullable Image image) {
        if (image == null) {
            fluffImageLabel.setIcon(null);
            fluffImageLabel.setToolTipText(null);
            return;
        }
        Image displayedImage = image;
        if (displayedImage.getWidth(this) > DEFAULT_WIDTH) {
            displayedImage = displayedImage.getScaledInstance(DEFAULT_WIDTH, -1, Image.SCALE_SMOOTH);
        }
        fluffImageLabel.setIcon(new ImageIcon(displayedImage));
    }

    private void setFluffImage(@Nullable Entity entity) {
        fluffImageList.clear();
        fluffImageIndex = 0;

        boolean isSpritesOnly = PreferenceManager.getClientPreferences().getSpritesOnly();
        if (isSpritesOnly || (entity == null)) {
            nextImageButton.setEnabled(false);
            prevImageButton.setEnabled(false);
            imageInfoLabel.setText("");
            displayFluffImage(null);
            return;
        }

        fluffImageList.addAll(FluffImageHelper.getFluffRecords(entity));
        boolean hasMultipleImages = fluffImageList.size() > 1;
        nextImageButton.setEnabled(hasMultipleImages);
        prevImageButton.setEnabled(hasMultipleImages);
        showNextFluffImage();
    }

    public void reset() {
        readoutTextComponent.setText("");
        fluffImageList.clear();
        fluffImageIndex = 0;
        nextImageButton.setEnabled(false);
        prevImageButton.setEnabled(false);
        imageInfoLabel.setText("");
        displayFluffImage(null);
    }

    /** Forwards a mouse wheel scroll on the fluff image or free space to the TRO entry. */
    MouseWheelListener wheelForwarder = event -> {
        MouseWheelEvent converted = (MouseWheelEvent) SwingUtilities.convertMouseEvent(EntityReadoutPanel.this, event,
              scrollPane);
        for (MouseWheelListener listener : scrollPane.getMouseWheelListeners()) {
            listener.mouseWheelMoved(converted);
        }
    };

    private void showNextFluffImage() {
        changeFluffImageIndex(1);
    }

    private void showPrevFluffImage() {
        changeFluffImageIndex(-1);
    }

    private void changeFluffImageIndex(int delta) {
        fluffImageIndex += delta;
        if (fluffImageIndex >= fluffImageList.size()) {
            fluffImageIndex = 0;
        }
        if (fluffImageIndex < 0) {
            fluffImageIndex = fluffImageList.size() - 1;
        }
        boolean hasImageAtIndex = (fluffImageIndex >= 0) && (fluffImageIndex < fluffImageList.size());
        if (!hasImageAtIndex) {
            LOGGER.debug("[FluffImages] No fluff image available; showing the placeholder image.");
            displayFluffImage(PLACEHOLDER_IMAGE);
            imageInfoLabel.setText("");
            return;
        }

        FluffImageHelper.FluffImageRecord record = fluffImageList.get(fluffImageIndex);
        try {
            displayFluffImage(record.getImage());
        } catch (IOException exception) {
            LOGGER.warn("[FluffImages] Could not load fluff image {}", record.file(), exception);
            displayFluffImage(null);
            imageInfoLabel.setText(Messages.getString("EntityReadoutPanel.imageLoadError"));
            return;
        }
        String imageInfo = FluffImageTooltip.getTooltip(record);
        fluffImageLabel.setToolTipText(imageInfo);
        imageInfoLabel.setText((imageInfo != null) ? imageInfo : "");
    }

    private static @Nullable Image readPlaceHolderImage() {
        File placeholderFile = new File(PLACEHOLDER_IMAGE_NAME);
        if (!placeholderFile.exists()) {
            LOGGER.debug("[FluffImages] No placeholder image at {}; units without fluff will show no image.",
                  PLACEHOLDER_IMAGE_NAME);
            return null;
        }
        try {
            return ImageIO.read(placeholderFile);
        } catch (IOException exception) {
            LOGGER.warn("[FluffImages] Could not read the placeholder image {}", PLACEHOLDER_IMAGE_NAME, exception);
            return null;
        }
    }
}
