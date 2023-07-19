package rs.raf.demo.responses;

import lombok.Data;

@Data
public class RoomKeyResponse {
    private String roomKey;

    public RoomKeyResponse(String roomKey) {
        this.roomKey = roomKey;
    }
}
