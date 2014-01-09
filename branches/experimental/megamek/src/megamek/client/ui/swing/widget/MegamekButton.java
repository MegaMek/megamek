/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006,2007,2008 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
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
package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import megamek.common.Configuration;

public class MegamekButton extends JButton {
	
	/**
	 * @author arlith
	 */
	private static final long serialVersionUID = -3271105050872007863L;
	protected ImageIcon backgroundIcon;
	protected ImageIcon backgroundPressedIcon;
	
	boolean isPressed = false;
	boolean isMousedOver = false;
	
	public MegamekButton(String text, String component){
		super(text);
		setBorder(new MegamekBorder(SkinXMLHandler.getSkin(component,true)));
		loadIcon(SkinXMLHandler.getSkin(component,true));
	}
	
	public MegamekButton(String text){
		super(text);
		setBorder(new MegamekBorder(SkinXMLHandler.defaultButton));
		loadIcon(SkinXMLHandler.getSkin(SkinXMLHandler.defaultButton));
	}
	
	public MegamekButton(){
		super();
		setBorder(new MegamekBorder(SkinXMLHandler.defaultButton));
		loadIcon(SkinXMLHandler.getSkin(SkinXMLHandler.defaultButton));
	}
	
	 public void loadIcon(SkinSpecification spec){
	        try {
	        	if (spec.backgrounds.size() <2){
	        		System.out.println("Error: skin specification for a " +
	        				"Megamek Button does not contain at least " +
	        				"2 background images!");
	        	}
	            java.net.URI imgURL = 
	                    new File(Configuration.widgetsDir(),
	                    		spec.backgrounds.get(0)).toURI();
	            backgroundIcon = new ImageIcon(imgURL.toURL());
	            imgURL = 
	                    new File(Configuration.widgetsDir(),
	                    		spec.backgrounds.get(1)).toURI();
	            backgroundPressedIcon = new ImageIcon(imgURL.toURL());
	        } catch (Exception e) {
	        	System.out.println("Error: loading background icons for " +
	        			"a Megamekbutton!");
	        	System.out.println("Error: " + e.getMessage());
	        }
	 }
	 
	 protected void processMouseEvent(MouseEvent e){
		if (e.getID() == MouseEvent.MOUSE_EXITED){
			isMousedOver = false;
			repaint();
		} else if (e.getID() == MouseEvent.MOUSE_ENTERED) {
			isMousedOver = true;
		} else if (e.getID() == MouseEvent.MOUSE_PRESSED) {
			isPressed = true;
		} else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
			isPressed = false;
		}
		super.processMouseEvent(e);
	 }
	 
	 protected void paintComponent(Graphics g){
		int w = getWidth();
		int h = getHeight();
		int iW = isPressed ? backgroundPressedIcon.getIconWidth() : 
			backgroundIcon.getIconWidth();
		int iH = isPressed ? backgroundPressedIcon.getIconHeight() : 
			backgroundIcon.getIconHeight();
		for (int x = 0; x < w; x += iW) {
			for (int y = 0; y < h; y += iH) {
				if (isPressed){
					g.drawImage(backgroundPressedIcon.getImage(), x, y,
							backgroundPressedIcon.getImageObserver());
				} else {
					g.drawImage(backgroundIcon.getImage(), x, y,
							backgroundIcon.getImageObserver());
				}
			}
		}
		
		JLabel textLabel = new JLabel(getText(),SwingConstants.CENTER);
		textLabel.setSize(getSize());
		if (this.isEnabled()){
			if (isMousedOver){
				Font font = textLabel.getFont();
				// same font but bold
				Font boldFont = new Font(font.getFontName(), Font.BOLD, 
						font.getSize()+2);
				textLabel.setFont(boldFont);
				textLabel.setForeground(new Color(255,255,0));
			} else {
				textLabel.setForeground(new Color(250,250,250));
			}
		} else {
			textLabel.setForeground(new Color(128,128,128));
		}
		textLabel.paint(g);
	 }
	 
	 public String toString(){
		 return getActionCommand();
	 }
	 
}
