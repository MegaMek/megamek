package megamek.client.ui.swing.lobby;

import java.awt.Frame;
import java.text.MessageFormat;

import javax.swing.JOptionPane;

/** Contains static methods that show common info/error messages for the lobby. */
public final class LobbyErrors {
    
    private static final String SINGLE_OWNER = "For this action, the selected units must have a single owner.";
    private static final String CONFIG_ENEMY = "Cannot configure units of other players except units of your bots.";
    private static final String VIEW_HIDDEN = "Cannot view or set details on hidden units.";
    private static final String SINGLE_UNIT = "Cannot {0} for more than one unit at a time.";
    private static final String TEN_UNITS = "Please select fewer than 10 units.";
    private static final String HEAT_TRACKING = "Cannot apply a heat setting to units that do not track heat.";
    private static final String ONLY_MEKS = "This setting can only be applied to Meks.";
    private static final String ONLY_FTEAMMATE = "Can only reassign a force to a teammate when reassigning without units.";
    private static final String ENTITY_OR_FORCE = "Please select either only forces or only units.";
    private static final String FORCE_EMPTY = "Please select only empty forces.";
    

    public static void showSingleOwnerRequired(Frame owner) {
        JOptionPane.showMessageDialog(owner, SINGLE_OWNER);
    }

    public static void showCannotConfigEnemies(Frame owner) {
        JOptionPane.showMessageDialog(owner, CONFIG_ENEMY);
    }
    
    public static void showCannotViewHidden(Frame owner) {
        JOptionPane.showMessageDialog(owner, VIEW_HIDDEN);
    }
    
    public static void showSingleUnit(Frame owner, String action) {
        JOptionPane.showMessageDialog(owner, MessageFormat.format(SINGLE_UNIT, action));
    }
    
    public static void showTenUnits(Frame owner) {
        JOptionPane.showMessageDialog(owner, TEN_UNITS);
    }
    
    public static void showHeatTracking(Frame owner) {
        JOptionPane.showMessageDialog(owner, HEAT_TRACKING);
    }
    
    public static void showOnlyMeks(Frame owner) {
        JOptionPane.showMessageDialog(owner, ONLY_MEKS);
    }
    
    public static void showOnlyTeammate(Frame owner) {
        JOptionPane.showMessageDialog(owner, ONLY_FTEAMMATE);
    }
    
    public static void showOnlyEntityOrForce(Frame owner) {
        JOptionPane.showMessageDialog(owner, ENTITY_OR_FORCE);
    }
    
    public static void showOnlyEmptyForce(Frame owner) {
        JOptionPane.showMessageDialog(owner, FORCE_EMPTY);
    }


}
