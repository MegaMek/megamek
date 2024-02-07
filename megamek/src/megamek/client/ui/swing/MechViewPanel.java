/*
 * MCopyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
import megamek.client.ui.swing.util.UIUtil.FixedXPanel;
import megamek.common.Entity;
import megamek.common.MechView;
import megamek.common.Report;
import megamek.common.templates.TROView;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * @author Jay Lawson
 * @since November 2, 2009
 */
public class MechViewPanel extends JPanel {

    private static final long serialVersionUID = 2438490306644271135L;

    private JTextPane txtMek = new JTextPane();
    private JLabel lblMek = new JLabel();
    private JScrollPane scrMek;

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
        textPanel.setMinimumSize(new Dimension(width, height));
        textPanel.setPreferredSize(new Dimension(width, height));
        textPanel.add(scrMek);

        var fluffPanel = new FixedXPanel();
        fluffPanel.setMinimumSize(new Dimension(width, height));
        fluffPanel.setPreferredSize(new Dimension(width, height));
        fluffPanel.add(lblMek);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
        p.add(textPanel);
        p.add(fluffPanel);
        p.add(Box.createHorizontalGlue());
        JScrollPane sp = new JScrollPane(p);
        setLayout(new BorderLayout());
        add(sp);
        addMouseWheelListener(wheelForwarder);
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

    public void setMech(Entity entity, boolean useAlternateCost) {
        MechView mechView = new MechView(entity, false, useAlternateCost);
        setMech(entity, mechView);
    }

    public void setMech(Entity entity, String fontName) {
        MechView mechView = new MechView(entity, false, false);
        setMech(entity, mechView, fontName);
    }

    private void setFluffImage(Entity entity) {
        Image image = entity.getFluffImage();
        // Scale down to the default width if the image is wider than that
        if (null != image) {
            if (image.getWidth(this) > DEFAULT_WIDTH) {
                image = image.getScaledInstance(DEFAULT_WIDTH, -1, Image.SCALE_SMOOTH);
            }          
            lblMek.setIcon(new ImageIcon(image));
        } else {
            lblMek.setIcon(null);
        }
    }

    public void reset() {
        txtMek.setText("");
        lblMek.setIcon(null);
    }

    /** Forwards a mouse wheel scroll on the fluff image or free space to the TRO entry. */ 
    MouseWheelListener wheelForwarder = e -> {
        MouseWheelEvent converted = (MouseWheelEvent) SwingUtilities.convertMouseEvent(MechViewPanel.this, e, scrMek);
        for (MouseWheelListener listener : scrMek.getMouseWheelListeners()) {
            listener.mouseWheelMoved(converted);
        }
    };
}