/**
 * 
 */
package megamek.common;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.Serializable;

/**
 * @author dirk
 *
 */
public class SpecialHexDisplay implements Serializable{
    public enum Type {
        ARTILLERY_AUTOHIT   ("data/images/hexes/artyauto.gif"),
        ARTILLERY_ADJUSTED  ("data/images/hexes/artyadj.gif"),
        ARTILERY_INCOMING   ("data/images/hexes/artyinc.gif"),
        ARTILEY_TARGET      ("data/images/hexes/artytarget.gif"),
        ARTILERY_HIT        ("data/images/hexes/artyhit.gif");
    
        private Image defaultImage;
        private String defaultImagePath;

        Type(String iconPath) {
            defaultImagePath = iconPath;
        }
        
        public void init(Toolkit toolkit) {
            toolkit.getImage(defaultImagePath);
        }
        
        public Image getDefaultImage() {
            return defaultImage;
        }

        public void setDefaultImage(Image defaultImage) {
            this.defaultImage = defaultImage;
        }
    };
    
    private String info;
    private Type type;
    private int round;
    
    public static int NO_ROUND = -99;
    
    public SpecialHexDisplay(Type type) {
        this.type = type;
        round = NO_ROUND;
    }
    
    public SpecialHexDisplay(Type type, String info) {
        this.type = type;
        this.info = info;
        round = NO_ROUND;
    }
    
    public SpecialHexDisplay(Type type, int round, String info) {
        this.type = type;
        this.info = info;
        this.round = round;
    }
    
    public boolean thisRound(int round) {
        if(NO_ROUND == this.round) {
            return true;
        }
        return round == this.round;
    }
    
    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
