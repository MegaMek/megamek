package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;

public class MechSlotLabel extends PicMap {

    /**
     * 
     */
    private static final long serialVersionUID = 5601930871313914270L;

    // Margins - used to draw 3D box
    private static final int MARGIN_WIDTH = 2;

    private BackGroundDrawer bgd = new BackGroundDrawer(null);

    public MechSlotLabel(String s, FontMetrics fm, Image im, Color textColor,
            Color bgColor) {
        super();
        PMPicArea pa = new PMPicArea(im);
        pa.setCursor(Cursor.getDefaultCursor());
        addElement(pa);
        PMSimpleLabel l = new PMSimpleLabel(s, fm, textColor);
        addElement(l);
        l.moveTo(pa.getBounds().width + 5, (pa.getBounds().height - l
                .getBounds().height)
                / 2 + l.getSize().height - l.getDescent());
        setContentMargins(MARGIN_WIDTH, MARGIN_WIDTH, MARGIN_WIDTH,
                MARGIN_WIDTH);
        setBackground(bgColor);
        addBgDrawer(bgd);
        drawBGImage();
    }

    private void drawBGImage() {
        Dimension d = getSize();
        int w = d.width;
        int h = d.height;
        Image BGImage = createImage(w, h);
        if (BGImage == null)
            return;
        Graphics g = BGImage.getGraphics();
        g.setColor(Color.green.darker().darker());
        g.fillRect(0, 0, w, h);
        g.setColor(Color.green.darker());
        g.fillRect(w - 2, 0, 2, h);
        g.fillRect(0, h - 2, w, 2);
        g.setColor(Color.green.darker().darker().darker());
        g.fillRect(0, 0, w, 2);
        g.fillRect(0, 0, 2, h);
        g.dispose();
        bgd.setImage(BGImage);
    }

    public void onResize() {
        drawBGImage();
    }
}