package megamek.common.alphaStrike;

import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameEvent;
import megamek.common.options.GameOptions;

import java.util.List;

public class ASGame extends AbstractGame {

    private GameOptions options = new GameOptions(); //TODO: SBFGameOptions()
    private GamePhase phase = GamePhase.UNKNOWN; //TODO: SBFGamePhase - or possibly reuse Phase? very similar
    private Board board = new Board();

    @Override
    public GameTurn getTurn() {
        return null;
    }

    @Override
    public GameOptions getOptions() {
        return null;
    }

    @Override
    public GamePhase getPhase() {
        return null;
    }

    @Override
    public void setPhase(GamePhase phase) {

    }

    @Override
    public void fireGameEvent(GameEvent event) {

    }

    @Override
    public boolean isForceVictory() {
        return false;
    }

    @Override
    public void addPlayer(int id, Player player) {

    }

    @Override
    public void setPlayer(int id, Player player) {

    }

    @Override
    public void removePlayer(int id) {

    }

    @Override
    public void setupTeams() {

    }

    @Override
    public int getNextEntityId() {
        return 0;
    }

    @Override
    public void replaceUnits(List<InGameObject> units) {

    }

    @Override
    public void setBoard(Board board, int boardId) {

    }
}
