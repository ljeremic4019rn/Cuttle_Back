package rs.raf.app.model.actions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import rs.raf.app.model.actions.enums.ActionType;

@Getter
@Setter
@AllArgsConstructor
public class VisualUpdate {

    private boolean visualUpdate;
    private String roomKey;
    private ActionType actionType;
    private int fromPlayer;
    private String cardPlayed;
    private int ontoPlayer;
    private String ontoCardPlayed;
}
