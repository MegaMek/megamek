package megamek.client.util.widget;

import java.awt.*;

public class TransparentLabel extends PicMap{
    
    public final static int LEFT = -1;
    public final static int CENTER = 0;
    public final static int RIGHT = 1;
    
    private int align = 0;
    PMSimpleLabel l;
    
    public TransparentLabel( String s, FontMetrics fm, Color c, int al){
        super();
        l = new PMSimpleLabel(s, fm, c);
        addElement(l);
        l.moveTo(0, l.getSize().width - l.getDescent());
        setBackgroundOpaque(false);
        align = al;
        onResize();
    }
    
    public void setText(String s){
        l.setString(s);
        onResize();
        repaint();
    }
    
    public void onResize(){
        Rectangle r = getContentBounds();
        Dimension d = getSize();
        if(align < 0){
            setContentMargins(0,0,(d.width - r.width),0);
        } else if(align == 0) {
            setContentMargins((d.width - r.width)/2,0,(d.width - r.width)/2,0);
        } else if(align > 0) {
            setContentMargins((d.width - r.width),0,0,0);
        }
        
        r = getContentBounds();
        d = getSize();
    }
}