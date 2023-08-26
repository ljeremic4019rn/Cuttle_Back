package rs.raf.app.responses;

import lombok.Data;
import rs.raf.app.model.actions.GameResponse;

import java.util.ArrayList;

@Data
public class RoomUpdateResponse {
    private RoomUpdateType roomUpdateType;
    private ArrayList<String> currentPlayersInRoom;
    private GameResponse gameResponse;


    public RoomUpdateResponse() {
    }

    public RoomUpdateResponse(RoomUpdateType roomUpdateType) {
        this.roomUpdateType = roomUpdateType;
    }
}
