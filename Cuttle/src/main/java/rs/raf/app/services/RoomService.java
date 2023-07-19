package rs.raf.app.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.app.model.Room;
import rs.raf.app.responses.ResponseDto;
import rs.raf.app.utils.RoomKeyGenerator;

import java.util.HashMap;
import java.util.Map;

@Service
public class RoomService {

    private Map<String, Room> activeRooms;

    @Autowired
    public RoomService(RoomKeyGenerator roomKeyGenerator) {
        this.activeRooms = new HashMap<>();
    }


    public String createRoom(String owner_username) {
        Room room = new Room();
        String roomKey = RoomKeyGenerator.generateKey();
        room.setIdKey(roomKey);
        room.getPlayers().add(owner_username);
        activeRooms.put(roomKey, room);
        return roomKey;
    }

    //todo player already in room
    public ResponseDto joinRoom(String roomKey, String username) {
        if (activeRooms.containsKey(roomKey)) {
            Room room = activeRooms.get(roomKey);
            if (room.isGameIsRunning()) return new ResponseDto("Game already in progress", 401);
            if (room.getPlayers().size() == 4) return new ResponseDto("Maximum room capacity reached", 401);
            room.getPlayers().add(username);
            return new ResponseDto("Successfully joined your room", 200);
        }
        else return new ResponseDto("Requested room doesn't exist", 404);
    }


    //todo samo vlasnik sme da startuje i zavrsi igru
    public ResponseDto startGame(String roomKey){
        if (activeRooms.containsKey(roomKey)) {
            Room room = activeRooms.get(roomKey);
            if (room.isGameIsRunning()) return new ResponseDto("Game already in progress", 401);
            room.startGame();
            return new ResponseDto("Game started", 200);
        }
        else return new ResponseDto("Requested room doesn't exist", 404);
    }

    //todo stop game treba da vrati replay i da izbrise sobu
    public ResponseDto stopGame(String roomKey){
        if (activeRooms.containsKey(roomKey)) {
            Room room = activeRooms.get(roomKey);
            if (!room.isGameIsRunning()) return new ResponseDto("Game is already finished", 401);
            room.stopGame();
            //todo delete room
            return new ResponseDto("Game stopped", 200);
        }
        else return new ResponseDto("Requested room doesn't exist", 404);
    }

}
