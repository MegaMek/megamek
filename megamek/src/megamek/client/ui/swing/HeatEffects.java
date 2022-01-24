package megamek.client.ui.swing;

import megamek.client.ui.Messages;

public class HeatEffects {

    public static String getHeatEffects(int heat, boolean mtHeat, boolean hasTSM) {
        String whichOne = "HeatEffects";
        int maxheat = 30;
        if (hasTSM) {
            whichOne += ".tsm";
        }
        if (mtHeat) {
            if (heat >= 30) {
                whichOne += ".mt";
            }
            maxheat = 50;
        }
        whichOne += "." + Math.min(maxheat, heat);
        return Messages.getString(whichOne);
    }

}
