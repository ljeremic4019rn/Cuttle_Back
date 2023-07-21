package rs.raf.app.model.actions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GameAction {
    private String roomKey;
    private ActionType actionType;
    private int fromPlayer;
    private String cardPlayed;
    //this is userd only for power or scuttle plays
    private int ontoPlayer;
    private String ontoCardPlayed;

    public GameAction() {
    }

}
