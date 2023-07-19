package rs.raf.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameAction {
    private String roomKey;
    private String from;
    private String cartPlayed;
    private String onToCardPlayed;

    public GameAction() {
    }

}
