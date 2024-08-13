/*
 * MCopyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021, 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import megamek.client.ui.swing.util.FluffImageHelper;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.MechView;
import megamek.common.Report;
import megamek.common.templates.TROView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * @author Jay Lawson
 */
public class MechViewPanel extends JPanel {

    private final JTextPane txtMek = new JTextPane();
    private JScrollPane scrMek;

    private final JLabel fluffImageLabel = new JLabel();
    private final List<FluffImageHelper.FluffImageRecord> fluffImageList = new ArrayList<>();
    private int fluffImageIndex = 0;
    private final JButton nextImageButton = new JButton("   >   ");
    private final JButton prevImageButton = new JButton("   <   ");
    private final JLabel imageInfoLabel = new JLabel("", JLabel.CENTER);

    public static final int DEFAULT_WIDTH = 360;
    public static final int DEFAULT_HEIGHT = 600;

    public MechViewPanel() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, true);
    }

    public MechViewPanel(int width, int height, boolean noBorder) {
        Report.setupStylesheet(txtMek);
        txtMek.setEditable(false);
        txtMek.setBorder(new EmptyBorder(5, 10, 0, 0));
        txtMek.setMinimumSize(new Dimension(width, height));
        txtMek.setPreferredSize(new Dimension(width, height));
        txtMek.addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
                UIUtil.browse(e.getURL().toString(), this);
            }
        });
        scrMek = new JScrollPane(txtMek);
        scrMek.getVerticalScrollBar().setUnitIncrement(16);

        if (noBorder) {
            scrMek.setBorder(null);
        }
        scrMek.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        var textPanel = new JPanel(new GridLayout(1, 1));
        textPanel.setAlignmentY(0);
        textPanel.setMinimumSize(new Dimension(width, height));
        textPanel.setPreferredSize(new Dimension(width, height));
        textPanel.add(scrMek);

        var imageControlsPanel = new UIUtil.FixedYPanel(new FlowLayout());
        imageControlsPanel.add(prevImageButton);
        imageControlsPanel.add(nextImageButton);

        imageControlsPanel.setAlignmentX(0.5f);
        fluffImageLabel.setAlignmentX(0.5f);
        imageInfoLabel.setAlignmentX(0.5f);

        Box fluffPanel = Box.createVerticalBox();
        fluffPanel.setAlignmentY(0);
        fluffPanel.add(imageControlsPanel);
        fluffPanel.add(fluffImageLabel);
        fluffPanel.add(imageInfoLabel);

        Box p = Box.createHorizontalBox();
        p.add(textPanel);
        p.add(fluffPanel);
        p.add(Box.createHorizontalGlue());
        JScrollPane sp = new JScrollPane(p);
        setLayout(new BorderLayout());
        add(sp);
        addMouseWheelListener(wheelForwarder);

        nextImageButton.addActionListener(e -> showNextFluffImage());
        prevImageButton.addActionListener(e -> showPrevFluffImage());
    }

    public void setMech(Entity entity, MechView mechView) {
        txtMek.setText(mechView.getMechReadout());
        txtMek.setCaretPosition(0);
        setFluffImage(entity);
    }

    public void setMech(Entity entity, MechView mechView, String fontName) {
        txtMek.setText(mechView.getMechReadout(fontName));
        txtMek.setCaretPosition(0);
        setFluffImage(entity);
    }

    public void setMech(Entity entity, TROView troView) {
        txtMek.setText(troView.processTemplate());
        txtMek.setCaretPosition(0);
        setFluffImage(entity);
    }

    @SuppressWarnings("unused") // Used in MHQ
    public void setMech(Entity entity, boolean useAlternateCost) {
        MechView mechView = new MechView(entity, false, useAlternateCost);
        setMech(entity, mechView);
    }

    public void setMech(Entity entity, String fontName) {
        MechView mechView = new MechView(entity, false, false);
        setMech(entity, mechView, fontName);
    }

    private void setFluffImage(Entity entity) {
        fluffImageList.clear();
        fluffImageList.addAll(FluffImageHelper.getFluffRecords(entity));
        fluffImageIndex = 0;
        nextImageButton.setEnabled(fluffImageList.size() > 1);
        prevImageButton.setEnabled(fluffImageList.size() > 1);
        showNextFluffImage();
    }

    private void setFluffImage(Image image) {
        // Scale down to the default width if the image is wider than that
        if (null != image) {
            if (image.getWidth(this) > DEFAULT_WIDTH) {
                image = image.getScaledInstance(DEFAULT_WIDTH, -1, Image.SCALE_SMOOTH);
            }
            fluffImageLabel.setIcon(new ImageIcon(image));
        } else {
            fluffImageLabel.setIcon(null);
            fluffImageLabel.setToolTipText(null);
        }
    }

    public void reset() {
        txtMek.setText("");
        fluffImageList.clear();
        setFluffImage((Entity) null);
    }

    /** Forwards a mouse wheel scroll on the fluff image or free space to the TRO entry. */
    private final MouseWheelListener wheelForwarder = e -> {
        MouseWheelEvent converted = (MouseWheelEvent) SwingUtilities.convertMouseEvent(MechViewPanel.this, e, scrMek);
        for (MouseWheelListener listener : scrMek.getMouseWheelListeners()) {
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
        if ((fluffImageIndex >= 0) && (fluffImageIndex < fluffImageList.size())) {
            try {
                FluffImageHelper.FluffImageRecord record = fluffImageList.get(fluffImageIndex);
                setFluffImage(record.getImage());
                imageInfoLabel.setText(prepareLabelText(record.file()));
                fluffImageLabel.setToolTipText(FluffImageTooltip.getTooltip(record));
            } catch (IOException ex) {
                setFluffImage((Image) null);
                imageInfoLabel.setText("Error loading fluff image");
            }
        } else {
            setFluffImage((Image) null);
            imageInfoLabel.setText("");
        }
    }

    private String prepareLabelText(File file) {
        String labelText = "";
        String labelInfo = file.toString();
        if (labelInfo.contains("__")) {
            labelText = labelInfo.substring(labelInfo.lastIndexOf("__") + 2);
        }
        if (labelText.contains(".")) {
            labelText = labelText.substring(0, labelText.lastIndexOf("."));
        }
        return labelText;
    }
}