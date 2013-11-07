package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import megamek.common.Configuration;

public class MegamekButton extends JButton {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3271105050872007863L;
	protected ImageIcon backgroundIcon;
	
	public MegamekButton(String text){
		super(text);
		setBorder(new MegamekButtonBorder(-1,-1,-1,-1));
		loadIcon();
	}
	
	 public void loadIcon(){
	        try {
	            java.net.URI imgURL = 
	                    new File(Configuration.widgetsDir(),
	                    "monitor_bg.png").toURI();
	            backgroundIcon = new ImageIcon(imgURL.toURL());
	        } catch (Exception e) {
	        	
	        }
	 }
	 
	 protected void paintComponent(Graphics g){
		int w = getWidth();
		int h = getHeight();
		int iW = backgroundIcon.getIconWidth();
		int iH = backgroundIcon.getIconHeight();
		for (int x = 0; x < w; x += iW) {
			for (int y = 0; y < h; y += iH) {
				g.drawImage(backgroundIcon.getImage(), x, y,
						backgroundIcon.getImageObserver());
			}
		}
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setColor(new Color(245,245,245));
		g2.drawString(getText(), 20, getHeight() / 2 + 5);
		g2.dispose();
	 }
	 
}
