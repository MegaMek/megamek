package megamek.common.actions;
import megamek.*;
import megamek.common.*;
import java.io.Serializable;
public class ArtilleryAttackAction
implements Serializable
{
    private WeaponAttackAction waa;
    public int turnsTilHit;
    public ArtilleryAttackAction(WeaponAttackAction waa,Game game) {
        this.waa=waa;
        int distance=Compute.effectiveDistance(game, waa.getEntity(game),waa.getTarget(game));//get distance

        turnsTilHit=(distance/34)+1;//convert to 2 board increments (time in flight=boards/2)
    }
      public void setWAA(WeaponAttackAction waa) {
        this.waa=waa;
    }
    public WeaponAttackAction getWAA() {
        return waa;
    }
}
