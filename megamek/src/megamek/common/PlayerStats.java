package megamek.common;

import java.util.Objects;

/**
 * Handles all statistical calculations for a player.
 * Extracted from Player class to improve maintainability.
 */
public class PlayerStats {
    private final Player player;
    private final IGame game;

    public PlayerStats(Player player, IGame game) {
        this.player = Objects.requireNonNull(player);
        this.game = game;
    }

    /**
     * Returns the combined strength (Battle Value/PV) of all the player's usable assets. This includes only
     * units that should count according to {@link InGameObject#countForStrengthSum()}.
     *
     * @return The combined strength (BV/PV) of all the player's assets
     */
    public int getBV() {
        if (game == null) {
            return 0; // comportement par défaut pour les tests unitaires
        }

        return game.getInGameObjects().stream()
              .filter(player::isMyUnit)
              .filter(InGameObject::countForStrengthSum)
              .mapToInt(InGameObject::getStrength)
              .sum();
    }
}