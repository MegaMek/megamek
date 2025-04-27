package megamek.server.totalwarfare;

import megamek.common.*;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.Atmosphere;
import megamek.common.weapons.DamageType;
import megamek.logging.MMLogger;
import megamek.server.IDamageManager;
import megamek.server.ServerHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class TWDamageManagerOriginal implements IDamageManager {
    private static final MMLogger logger = MMLogger.create(TWDamageManagerOriginal.class);
    private final TWGameManager owner;
    private Game game;

    public TWDamageManagerOriginal(TWGameManager owner, Game game) {
        this.owner = owner;
        this.game = game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Vector<Report> damageEntity(DamageInfo damageInfo){
        return damageEntity(
              damageInfo.te(),
              damageInfo.hit(),
              damageInfo.damage(),
              damageInfo.ammoExplosion(),
              damageInfo.damageType(),
              damageInfo.damageIS(),
              damageInfo.areaSatArty(),
              damageInfo.throughFront(),
              damageInfo.underWater(),
              damageInfo.nukeS2S()
        );
    }


}
