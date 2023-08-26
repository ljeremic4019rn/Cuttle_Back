package rs.raf.app.model.actions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import rs.raf.app.model.actions.enums.ActionType;

import java.util.List;

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
    /*
    this helper-list is used when a card is countered by a 2 (ActionType.COUNTER)
    a list must be used because it more than one 2 is thrown (2 countered by 2 countered by 2) we need to send all of them to the graveyard
    HOW ITS WRITTEN - <rank>_<suit>_<playerId> 2_S_3 (id is here because all players can activate card at the same time)
    OR
    when discarding cards with 4-power
    HOW ITS WRITTEN - <rank>_<suit> 4_S
     */
    private List<String> helperCardList;

    public GameAction() {
    }

    @Override
    public String toString() {
        return "GameAction{" +
                "roomKey='" + roomKey + '\'' +
                ", actionType=" + actionType +
                ", fromPlayer=" + fromPlayer +
                ", cardPlayed='" + cardPlayed + '\'' +
                ", ontoPlayer=" + ontoPlayer +
                ", ontoCardPlayed='" + ontoCardPlayed + '\'' +
                ", used2s=" + helperCardList +
                '}';
    }
}
