package rs.raf.app.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.app.model.Room;
import rs.raf.app.model.actions.GameAction;
import rs.raf.app.model.actions.GameResponse;
import rs.raf.app.model.actions.GameResponseType;
import rs.raf.app.responses.ResponseDto;
import rs.raf.app.utils.RoomKeyGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class RoomService {

    //todo namesti da igraci biraju player size kada pokrenu sobu
    private static final int NUMBER_OF_PLAYERS = 3;
    private final Map<String, Room> activeRooms = new HashMap<>();

    @Autowired
    public RoomService() {
    }

    //todo napravi da moze da se izadje iz sobe

    //todo napravi "scraper" koji ubija prazne sobe


    public String createRoom(String owner_username) {
        Room room = new Room();
        String roomKey = RoomKeyGenerator.generateKey();
        room.setIdKey(roomKey);
        room.setRoomOwner(owner_username);
        room.getPlayers().add(owner_username);
        room.getPlayerHands().put(1, new ArrayList<>());
        activeRooms.put(roomKey, room);
        return roomKey;
    }

    //todo proveri checkove
    public ResponseDto joinRoom(String roomKey, String username) {
        if (activeRooms.containsKey(roomKey)) {
            Room room = activeRooms.get(roomKey);
            if (room.isGameIsRunning()) return new ResponseDto("Game already in progress", 401);
            if (room.getPlayerHands().size() == NUMBER_OF_PLAYERS)
                return new ResponseDto("Maximum room capacity reached", 401);
            if (room.getPlayers().contains(username)) return new ResponseDto("Already in room", 200);

            room.getPlayerHands().put(room.getPlayerHands().size() + 1, new ArrayList<>());
            return new ResponseDto("Successfully joined your room", 200);
        } else return new ResponseDto("Requested room doesn't exist", 404);
    }


    public ResponseDto startGame(String roomKey, String commandIssuingUser) {
        if (activeRooms.containsKey(roomKey)) {
            Room room = activeRooms.get(roomKey);
            if (room.isGameIsRunning()) return new ResponseDto("Game already in progress", 401);
            if (!room.getRoomOwner().equals(commandIssuingUser))
                return new ResponseDto("You are not the room owner", 403);

            room.startGame();
            return new ResponseDto("Game started", 200);
        } else return new ResponseDto("Requested room doesn't exist", 404);
    }

    //todo smisli sta da se na kraju uradi
    public ResponseDto stopGame(String roomKey, String commandIssuingUser) {
        if (activeRooms.containsKey(roomKey)) {
            Room room = activeRooms.get(roomKey);
            if (!room.isGameIsRunning()) return new ResponseDto("Game is already finished", 401);
            if (!room.getRoomOwner().equals(commandIssuingUser))
                return new ResponseDto("You are not the room owner", 403);

            room.stopGame();
            //todo delete room
            return new ResponseDto("Game stopped", 200);
        } else return new ResponseDto("Requested room doesn't exist", 404);
    }

    //todo mozda da se stavi check da samo current player moze da igra, ali mozda je lakse na frontu to
    public GameResponse playCard(GameAction gameAction) {
        GameResponse gameResponse = null;
        if (activeRooms.containsKey(gameAction.getRoomKey())) {
            Room room = activeRooms.get(gameAction.getRoomKey());
            gameResponse = room.playTurn(gameAction);

            //todo dodaj pravilan return

        } else {
            System.err.println("NESTO JE MNOGO LOSE");
        }

        return gameResponse;
    }


    public GameResponse drawCard(String roomKey) {
        if (activeRooms.containsKey(roomKey)) {
            Room room = activeRooms.get(roomKey);
            return room.drawCard();
        } else {
            System.err.println("NESTO JE MNOGO LOSE");
            return null;
        }
    }

}
