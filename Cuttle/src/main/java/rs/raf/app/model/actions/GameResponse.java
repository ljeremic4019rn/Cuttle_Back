package rs.raf.app.model.actions;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class GameResponse {
    private GameResponseType gameResponseType;
    private int currentPlayersTurn;

    private ArrayList<String> graveyard = new ArrayList<>();
    private Map<Integer, ArrayList<String>> playerHands = new HashMap<>();
    private Map<Integer, ArrayList<String>> playerTables = new HashMap<>();
    private Map<Integer, Integer> playerScore = new HashMap<>();

    private Integer playerWhoWon;

    public GameResponse() {
    }

}
