package megamek.client.ui.swing.widget;

import java.awt.Color;
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
	 * 
	 */
	private static final long serialVersionUID = -3271105050872007863L;
	protected ImageIcon backgroundIcon;
	protected ImageIcon backgroundPressedIcon;
	
	boolean isPressed = false;
	
	public MegamekButton(String text){
		super(text);
		setBorder(new MegamekButtonBorder(-1,-1,-1,-1));
		loadIcon();
	}
	
	public MegamekButton(){
		super();
		setBorder(new MegamekButtonBorder(-1,-1,-1,-1));
		loadIcon();
	}
	
	 public void loadIcon(){
	        try {
	            java.net.URI imgURL = 
	                    new File(Configuration.widgetsDir(),
	                    "monitor_bg.png").toURI();
	            backgroundIcon = new ImageIcon(imgURL.toURL());
	            imgURL = 
	                    new File(Configuration.widgetsDir(),
	                    "monitor_bg_pressed.png").toURI();
	            backgroundPressedIcon = new ImageIcon(imgURL.toURL());
	        } catch (Exception e) {
	        	
	        }
	 }
	 
	 protected void processMouseEvent(MouseEvent e){
		if (e.getID() == MouseEvent.MOUSE_EXITED){
			repaint();
			e.consume();
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
		int iW = backgroundIcon.getIconWidth();
		int iH = backgroundIcon.getIconHeight();
		for (int x = 0; x < w; x += iW) {
			for (int y = 0; y < h; y += iH) {
				if (isPressed){
					g.drawImage(backgroundPressedIcon.getImage(), x, y,
							backgroundIcon.getImageObserver());
				} else {
					g.drawImage(backgroundIcon.getImage(), x, y,
							backgroundIcon.getImageObserver());
				}
			}
		}
		
		JLabel textLabel = new JLabel(getText(),SwingConstants.CENTER);
		textLabel.setSize(getSize());
		if (this.isEnabled()){
			if (getMousePosition(true) != null){
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
