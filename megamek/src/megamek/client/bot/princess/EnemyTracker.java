package megamek.client.bot.princess;

import megamek.common.Coords;
import megamek.common.Entity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EnemyTracker {
    private final Map<Integer, EnemyProfile> enemyMap = new HashMap<>();

    private final Princess owner;

    public EnemyTracker(Princess owner) {
        this.owner = owner;
    }

    private Princess getOwner() {
        return owner;
    }

    public void updateEnemyPositions() {
        getOwner().getEnemyEntities().forEach(e -> {
            enemyMap.put(e.getId(), new EnemyProfile(
                e, e.getPosition(), (e.getArmorRemainingPercent() + e.getInternalRemainingPercent()) / 2.0, e.getMaxWeaponRange(),
                FireControl.getMaxDamageAtRange(e, e.getMaxWeaponRange(), false, false),
                FireControl.getMaxDamageAtRange(e, e.getMaxWeaponRange() / 2, false, false),
                FireControl.getMaxDamageAtRange(e, 1, false, false)));
        });
    }

    public List<Entity> getPriorityTargets(Coords swarmCenter) {
        return enemyMap.values().stream()
            .sorted(Comparator.comparingDouble((EnemyProfile ep) ->
                ep.getThreatScore() * (1.0 / ep.lastPosition().distance(swarmCenter)))
            )
            .limit(3)
            .map(EnemyProfile::entity)
            .toList();
    }

    private record EnemyProfile(Entity entity, Coords lastPosition, double percentHP, double maxRange, double maxDamage, double maxDamageHalfRange, double maxDamagePointBlank) {
        public double getThreatScore() {
            return ((maxDamage + maxDamageHalfRange + maxDamagePointBlank) / 3.0) * percentHP;
        }
    }

}
