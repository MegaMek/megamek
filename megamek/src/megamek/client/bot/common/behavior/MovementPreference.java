package megamek.client.bot.common.behavior;

import static megamek.codeUtilities.MathUtility.clamp01;

public class MovementPreference {
    private float offensive = 1f;
    private float defensive = 1f;
    private float holdPosition = 1f;

    public void setAll(float offensive, float defensive, float holdPosition) {
        this.offensive = clamp01(offensive);
        this.defensive = clamp01(defensive);
        this.holdPosition = clamp01(holdPosition);
    }

    public float getOffensive() {
        return offensive;
    }

    public void setOffensive(float offensive) {
        this.offensive = clamp01(offensive);
    }

    public float getDefensive() {
        return defensive;
    }

    public void setDefensive(float defensive) {
        this.defensive = clamp01(defensive);
    }

    public float getHoldPosition() {
        return holdPosition;
    }

    public void setHoldPosition(float holdPosition) {
        this.holdPosition = clamp01(holdPosition);
    }

    public float getOffensiveWeight() {
        return offensive / getTotalWeight();
    }

    public float getDefensiveWeight() {
        return defensive / getTotalWeight();
    }

    public float getHoldPositionWeight() {
        return holdPosition / getTotalWeight();
    }

    private float getTotalWeight() {
        return Math.max(offensive + defensive + holdPosition, 0.1f);
    }

}
