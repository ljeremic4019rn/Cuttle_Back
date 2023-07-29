package rs.raf.app.model.actions;

import lombok.Data;
import rs.raf.app.responses.ResponseDto;

@Data
public class StartGameResponse {
    ResponseDto responseDto;
    GameResponse gameResponse;

    public StartGameResponse(ResponseDto responseDto, GameResponse gameResponse) {
        this.responseDto = responseDto;
        this.gameResponse = gameResponse;
    }
}
