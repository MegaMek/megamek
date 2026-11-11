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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
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

import jakarta.annotation.Nullable;
import megamek.client.ui.FluffImageTooltip;
import megamek.client.ui.Messages;
import megamek.client.ui.entityreadout.EntityReadout;
import megamek.client.ui.entityreadout.ReadoutSections;
import megamek.client.ui.util.FluffImageHelper;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.Configuration;
import megamek.common.Report;
import megamek.common.preference.PreferenceManager;
import megamek.common.templates.TROView;
import megamek.common.units.Entity;
import megamek.common.util.ImageUtil;
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

    /** Recently shown fluff images of the current unit by list index, least recently shown first. */
    private final Map<Integer, ShownFluffImage> recentFluffImages = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, ShownFluffImage> eldest) {
            return size() > RECENT_FLUFF_IMAGE_COUNT;
        }
    };
    private final JButton nextImageButton = new JButton(">");
    private final JButton previousImageButton = new JButton("<");
    private final JLabel imageInfoLabel = new JLabel("", JLabel.CENTER);

    public static final int DEFAULT_WIDTH = 360;

    /** How many recently shown fluff images of the current unit are kept, so that browsing back is instant. */
    private static final int RECENT_FLUFF_IMAGE_COUNT = 8;

    /** The vertical gap between the fluff image and the info line below it, before GUI scaling. */
    private static final int IMAGE_INFO_GAP = 10;

    private static final String PLACEHOLDER_IMAGE_NAME =
            new File(Configuration.fluffImagesDir(), "fluff_placeholder.png").getPath();
    private static final Image PLACEHOLDER_IMAGE = readPlaceHolderImage();

    /**
     * Finds and reads fluff images away from the UI thread. Both need disk access, which can be slow, e.g. when the
     * user directory is on a cloud-synced drive, and would otherwise delay every unit selection. A single thread is
     * enough, as work for a unit that is no longer shown is skipped.
     */
    private static final ExecutorService FLUFF_IMAGE_LOADER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Fluff Image Loader");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Increased whenever another unit (or none) is shown, so background fluff image work for a unit that is no longer
     * shown can be recognized and dropped. Only changed on the UI thread.
     */
    private final AtomicInteger fluffGeneration = new AtomicInteger();

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

        JPanel textPanel = new JPanel(new GridLayout(1, 1));
        if (width != -1) {
            textPanel.setMinimumSize(new Dimension(width, height));
            textPanel.setPreferredSize(new Dimension(width, height));
        }
        textPanel.add(scrollPane);

        previousImageButton.setToolTipText(Messages.getString("EntityReadoutPanel.previousImage.toolTipText"));
        nextImageButton.setToolTipText(Messages.getString("EntityReadoutPanel.nextImage.toolTipText"));

        UIUtil.FixedYPanel imageControlsPanel = new UIUtil.FixedYPanel(new FlowLayout());
        imageControlsPanel.add(previousImageButton);
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
        previousImageButton.addActionListener(event -> showPreviousFluffImage());
    }

    public void showEntity(Entity entity, EntityReadout mekView) {
        setReadoutText(mekView.getFullReadout());
        setFluffImage(entity);
    }

    public void showEntity(Entity entity, EntityReadout mekView, String fontName) {
        setReadoutText(mekView.getFullReadout(fontName, ViewFormatting.HTML));
        setFluffImage(entity);
    }

    public void showEntity(Entity entity, EntityReadout mekView, String fontName,
          Collection<ReadoutSections> sections) {
        showReadout(mekView, fontName, sections);
        setFluffImage(entity);
    }

    private void showReadout(EntityReadout readout, String fontName, Collection<ReadoutSections> sections) {
        setReadoutText(readout.getReadout(fontName, ViewFormatting.HTML, sections));
    }

    /**
     * Shows the given HTML in a fresh document. Reusing one HTML document for many readouts makes its style cache
     * grow, and every later setText gets slower (tens of ms per unit after browsing a few units).
     */
    private void setReadoutText(String html) {
        readoutTextComponent.setDocument(readoutTextComponent.getEditorKit().createDefaultDocument());
        readoutTextComponent.setText(html);
        readoutTextComponent.setCaretPosition(0);
    }

    public void showEntity(Entity entity, TROView troView) {
        setReadoutText(troView.processTemplate());
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
        fluffImageLabel.setIcon(new ImageIcon(scaleToPanelWidth(image)));
    }

    /**
     * Scales the image down to {@link #DEFAULT_WIDTH}, keeping its aspect ratio. Images that are narrower, or whose
     * size is not known yet, are returned unchanged. This draws the image once at the new size, which is several times
     * faster than {@link Image#getScaledInstance} with {@link Image#SCALE_SMOOTH} on large fluff images.
     *
     * @param image The image to scale
     *
     * @return The scaled image, or the given image if it needs no scaling
     */
    private static Image scaleToPanelWidth(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        if ((width <= DEFAULT_WIDTH) || (height <= 0)) {
            return image;
        }
        return ImageUtil.getScaledImage(image, DEFAULT_WIDTH, height * DEFAULT_WIDTH / width);
    }

    private void setFluffImage(@Nullable Entity entity) {
        int generation = startNewFluffGeneration();

        boolean isSpritesOnly = PreferenceManager.getClientPreferences().getSpritesOnly();
        if (isSpritesOnly || (entity == null)) {
            displayFluffImage(null);
            return;
        }

        // Until the unit's own images are found, show the same placeholder that units without fluff images show
        displayFluffImage(PLACEHOLDER_IMAGE);
        FLUFF_IMAGE_LOADER.execute(() -> {
            if (generation != fluffGeneration.get()) {
                return;
            }
            List<FluffImageHelper.FluffImageRecord> records;
            try {
                records = FluffImageHelper.getFluffRecords(entity);
            } catch (RuntimeException exception) {
                LOGGER.warn("[FluffImages] Could not find fluff images for {}", entity.getShortName(), exception);
                return;
            }
            SwingUtilities.invokeLater(() -> {
                if (generation != fluffGeneration.get()) {
                    return;
                }
                fluffImageList.addAll(records);
                boolean hasMultipleImages = fluffImageList.size() > 1;
                nextImageButton.setEnabled(hasMultipleImages);
                previousImageButton.setEnabled(hasMultipleImages);
                // Show the first image, not the next one - stepping by 1 here would open on the second image
                changeFluffImageIndex(0);
            });
        });
    }

    /**
     * Forgets the fluff images of the previously shown unit, including any that are still being found or read.
     *
     * @return The new fluff generation
     */
    private int startNewFluffGeneration() {
        fluffImageList.clear();
        recentFluffImages.clear();
        fluffImageIndex = 0;
        nextImageButton.setEnabled(false);
        previousImageButton.setEnabled(false);
        imageInfoLabel.setText("");
        return fluffGeneration.incrementAndGet();
    }

    public void reset() {
        setReadoutText("");
        startNewFluffGeneration();
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

    private void showPreviousFluffImage() {
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

        ShownFluffImage shownImage = recentFluffImages.get(fluffImageIndex);
        if (shownImage != null) {
            showFluffImage(shownImage);
            return;
        }

        // Keep showing the current image until the new one is read; reading can take a moment on slow drives
        int generation = fluffGeneration.get();
        int index = fluffImageIndex;
        FluffImageHelper.FluffImageRecord record = fluffImageList.get(index);
        FLUFF_IMAGE_LOADER.execute(() -> {
            if (generation != fluffGeneration.get()) {
                return;
            }
            ShownFluffImage loadedImage = loadFluffImage(record);
            SwingUtilities.invokeLater(() -> {
                if (generation != fluffGeneration.get()) {
                    return;
                }
                if (loadedImage != null) {
                    recentFluffImages.put(index, loadedImage);
                }
                if (fluffImageIndex == index) {
                    showFluffImage(loadedImage);
                }
            });
        });
    }

    /**
     * Reads the image of the record and scales it to the panel width. Called away from the UI thread.
     *
     * @return The image ready to be shown, or {@code null} if it could not be read
     */
    private static @Nullable ShownFluffImage loadFluffImage(FluffImageHelper.FluffImageRecord record) {
        try {
            Image image = record.getImage();
            return new ShownFluffImage((image != null) ? scaleToPanelWidth(image) : null,
                  FluffImageTooltip.getTooltip(record));
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[FluffImages] Could not load fluff image {}", record.file(), exception);
            return null;
        }
    }

    /**
     * Shows the given fluff image with its information, or an error message if the image could not be read.
     *
     * @param shownImage The image to show, or {@code null} when it could not be read
     */
    private void showFluffImage(@Nullable ShownFluffImage shownImage) {
        if (shownImage == null) {
            displayFluffImage(null);
            imageInfoLabel.setText(Messages.getString("EntityReadoutPanel.imageLoadError"));
            return;
        }
        displayFluffImage(shownImage.image());
        fluffImageLabel.setToolTipText(shownImage.info());
        imageInfoLabel.setText((shownImage.info() != null) ? shownImage.info() : "");
    }

    /**
     * A fluff image as shown in this panel, kept so that browsing back to it needs no second read from disk.
     *
     * @param image The image, already scaled to the panel width, or {@code null} if there is none
     * @param info  The image information shown below the image and as its tooltip, or {@code null} if there is none
     */
    private record ShownFluffImage(@Nullable Image image, @Nullable String info) {}

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
