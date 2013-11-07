package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.awt.Graphics;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;

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
		
		JLabel textLabel = new JLabel(getText());
		textLabel.setSize(getPreferredSize());
		textLabel.setForeground(new Color(250,250,250));
		int x = Math.max(0, (int)(getWidth()/2.0 + 0.5) - 
				(int)(textLabel.getWidth()/2.0 + 0.5));
		int y = Math.max(0, (int)(getHeight()/2.0 + 0.5) - 
				(int)(textLabel.getHeight()/2.0 + 0.5));
		Graphics g2 = g.create(x,y,getWidth()-x,getHeight()-y);
		textLabel.paint(g2);
		g2.dispose();
	 }
	 
}
