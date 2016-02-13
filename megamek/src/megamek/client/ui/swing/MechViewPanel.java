/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * MechViewPanel.java
 *
 * Created on November 2, 2009 by Jay Lawson
 */

package megamek.client.ui.swing;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import megamek.client.ui.swing.util.FluffImageHelper;
import megamek.common.Entity;
import megamek.common.MechView;


public class MechViewPanel extends JPanel {
    
    /**
     * 
     */
    private static final long serialVersionUID = 2438490306644271135L;
    
    private JTextPane txtMek;
    private JLabel lblMek;
    private JScrollPane scrMek;
    private Icon icon;
    
    public final static int DEFAULT_WIDTH = 350;
    public final static int DEFAULT_HEIGHT = 600;
    
    public MechViewPanel() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT,true);
    }
    
    public MechViewPanel(int width, int height) {
        this(width,height,true);
    }
    
    public MechViewPanel(int width, int height, boolean noBorder) {
 
        setBackground(Color.WHITE);
        
        icon = null;
        
        txtMek = new JTextPane();
        ReportDisplay.setupStylesheet(txtMek);
        txtMek.setText("");
        txtMek.setEditable(false);
        txtMek.setBorder(null);
        txtMek.setPreferredSize(new Dimension(width, height));
        lblMek = new JLabel();
        lblMek.setText("");
        lblMek.setVerticalAlignment(SwingConstants.TOP);
        scrMek = new JScrollPane(txtMek);
        if (noBorder){
            scrMek.setBorder(null);
        }
        scrMek.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        scrMek.setPreferredSize(new Dimension(width, height));
        scrMek.setMinimumSize(new Dimension(width, height));
        GridBagConstraints c;
        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 0.0;
        c.weighty = 1.0;
        gridbag.setConstraints(scrMek, c);
        add(scrMek);

        c.insets = new Insets(0,0,0,0);
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        gridbag.setConstraints(lblMek, c);
        add(lblMek);
    }
    
    public void setMech(Entity entity, MechView mechView){
        reset();        
        txtMek.setText(mechView.getMechReadout());
        txtMek.setCaretPosition(0);
        Image image = FluffImageHelper.getFluffImage(entity);
        icon = null;
        if(null != image) {
            // We don't want this window to be too big, so scale large images
            if (image.getWidth(this) > DEFAULT_WIDTH)
            {
                double aspect_ratio = (float)image.getWidth(this) / image.getHeight(this);
                image = image.getScaledInstance(DEFAULT_WIDTH, 
                        (int)(DEFAULT_WIDTH/aspect_ratio), Image.SCALE_FAST);
            }            
            icon = new ImageIcon(image);
            lblMek.setIcon(icon);
        }  
    }
    
    public void setMech(Entity entity) {
        MechView mechView = new MechView(entity, false);
        setMech(entity,mechView);
    }
    
    public void setMech(Entity entity, boolean useAlternateCost) {
        MechView mechView = new MechView(entity, false, useAlternateCost);
        setMech(entity,mechView);
    }
    
    public void reset() {
        txtMek.setText("");
        lblMek.setIcon(null);
    }
    
    public int getBestWidth() {
        int width = DEFAULT_WIDTH;
        if(null != icon) {
            width += icon.getIconWidth() + 20;
        }
        return width;
    }
    
    public int getBestHeight() {
        int height = DEFAULT_HEIGHT;
        if(null != icon) {
            height = Math.max(height, icon.getIconHeight());
        }
        return height;
    }
    
}