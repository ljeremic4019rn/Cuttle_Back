package rs.raf.app.responses;

import lombok.Data;

import java.util.ArrayList;

@Data
public class JoinRoomResponse {
    private String playerUsername;
    private Integer playerNumber;
    private ArrayList<String> currentPlayersInRoom;


    public JoinRoomResponse() {
    }

    public JoinRoomResponse(String playerUsername, Integer playerNumber, ArrayList<String> currentPlayersInRoom) {
        this.playerUsername = playerUsername;
        this.playerNumber = playerNumber;
        this.currentPlayersInRoom = currentPlayersInRoom;
    }
}
