package megamek.common.strategicBattleSystems;

import megamek.common.*;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import org.apache.logging.log4j.LogManager;

import java.util.Collections;

public class SBFGame extends AbstractGame implements PlanetaryConditionsUsing {

    private GameOptions options = new GameOptions(); //TODO: SBFGameOptions()
    private GamePhase phase = GamePhase.UNKNOWN; //TODO: SBFGamePhase - or possibly reuse Phase? very similar
    private Board board = new Board();
    private PlanetaryConditions planetaryConditions = new PlanetaryConditions();

    @Override
    public GameTurn getTurn() {
        return null;
    }

    @Override
    public GameOptions getOptions() {
        return options;
    }

    @Override
    public GamePhase getPhase() {
        return phase;
    }

    @Override
    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    @Override
    public boolean isForceVictory() { //TODO This should not be part of IGame! too specific
        return false;
    }

    @Override
    public void addPlayer(int id, Player player) { // Server / Client-side?
        super.addPlayer(id, player);
        player.setGame(this);
        setupTeams();

        if ((player.isBot()) && (!player.getSingleBlind())) {
            boolean sbb = getOptions().booleanOption(OptionsConstants.ADVANCED_SINGLE_BLIND_BOTS);
            boolean db = getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND);
            player.setSingleBlind(sbb && db);
        }
    }

    @Override
    public void removePlayer(int id) {

    }

    @Override
    public void setupTeams() {

    }

    @Override
    public int getNextEntityId() {
        return Collections.max(inGameObjects.keySet()) + 1;
    }

    @Override
    public void setBoard(Board board, int boardId) {
        this.board = board;
    }

    @Override
    public PlanetaryConditions getPlanetaryConditions() {
        return planetaryConditions;
    }

    @Override
    public void setPlanetaryConditions(@Nullable PlanetaryConditions conditions) {
        if (conditions == null) {
            LogManager.getLogger().error("Can't set the planetary conditions to null!");
        } else {
            planetaryConditions.alterConditions(conditions);
        }
    }

    public void addUnit(InGameObject unit) { // This is a server-side method!
        if (!isSupportedUnitType(unit)) {
            LogManager.getLogger().error("Tried to add unsupported object [" + unit + "] to the game!");
            return;
        }

        // Add this unit, ensuring that its id is unique
        int id = unit.getId();
        if (inGameObjects.containsKey(id)) {
            id = getNextEntityId();
            unit.setId(id);
        }
        inGameObjects.put(id, unit);
    }

    public void receiveUnit(InGameObject unit) { // This is a client-side method to set a unit sent by the server!
        inGameObjects.put(unit.getId(), unit);
    }

    private boolean isSupportedUnitType(InGameObject object) {
        return object instanceof SBFFormation || object instanceof AlphaStrikeElement || object instanceof SBFUnit;
    }
}
