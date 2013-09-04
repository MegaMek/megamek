package megamek.common;
 
import java.io.Serializable;

 /**
  * Some static methods for called shots
  */
public class CalledShot implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 8746351140726246311L;
    
     //locations for called shots
    public static final int CALLED_NONE = 0;
    public static final int CALLED_HIGH = 1;
    public static final int CALLED_LOW = 2;
    public static final int CALLED_LEFT = 3;
    public static final int CALLED_RIGHT = 4;
    public static final int CALLED_NUM = 5;
     
    private int current;
     private static final String[] calledLocNames = {"", "HIGH", "LOW", "LEFT", "RIGHT"};
     
    public CalledShot() {
        current = CALLED_NONE;
    }
    
    public String getDisplayableName() {
        if(current >= CALLED_NUM) {
             return "Unknown";
        }
        return calledLocNames[current];
    }
    
    public int switchCalledShot() {
        current = current + 1;
        if(current >= CALLED_NUM) {
            current = CALLED_NONE;
        }
        return current;   
     }
    
    public int getCall() {
        return current;
    }

    public String isValid(Targetable target) {
 
        if(current == CALLED_NONE) {
            return null;
         }
         
        if(!(target instanceof Entity)) {
            return "called shots on entities only";
        }
        Entity te = (Entity)target;
        if(te instanceof Infantry || te instanceof Protomech) {
            return "no called shots on infantry/protomeks";
        }
        //only meks can be high high or low
        if(!(te instanceof Mech) && (current == CALLED_HIGH)) {
            return "called shots (high) only on Meks"; 
        }
        if(!(te instanceof Mech) && (current == CALLED_LOW)) {
            return "called shots (low) only on Meks"; 
        }

        return null;
    }
    
    public void reset() {
        current = CALLED_NONE;
    }
}
