package rs.raf.app.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import rs.raf.app.model.User;
import rs.raf.app.model.actions.GameAction;
import rs.raf.app.model.actions.StartGameResponse;
import rs.raf.app.model.actions.VisualUpdate;
import rs.raf.app.model.actions.enums.ActionType;
import rs.raf.app.responses.ResponseDto;
import rs.raf.app.responses.RoomKeyResponse;
import rs.raf.app.responses.RoomUpdateResponse;
import rs.raf.app.responses.RoomUpdateType;
import rs.raf.app.services.RoomService;
import rs.raf.app.services.UserService;

import java.util.Optional;

@Controller
@RequestMapping("/room")
@CrossOrigin
public class RoomController {

    private SimpMessagingTemplate simpMessagingTemplate;
    private RoomService roomService;
    private UserService userService;


    @Autowired
    public RoomController(SimpMessagingTemplate simpMessagingTemplate, RoomService roomService, UserService userService) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.roomService = roomService;
        this.userService = userService;
    }

    @GetMapping("/joinRoom/{roomKey}")
    public ResponseEntity<?> joinRoom(@PathVariable String roomKey) {
        Optional<User> user = this.userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (user.isEmpty()) return ResponseEntity.status(404).body("You don't have a profile created");
        ResponseDto responseDto = roomService.joinRoom(roomKey, user.get().getUsername());
        if (responseDto.getResponseCode() == 200) {
            RoomUpdateResponse roomUpdateResponse = new RoomUpdateResponse(RoomUpdateType.JOIN);
            roomUpdateResponse.setCurrentPlayersInRoom(roomService.getRoomPlayers(roomKey));
            this.simpMessagingTemplate.convertAndSend("/cuttle/updateRoom/" + roomKey, roomUpdateResponse);
        }
        return ResponseEntity.status(responseDto.getResponseCode()).body(responseDto.getResponse());
    }


    @PostMapping("/createRoom")
    public ResponseEntity<?> createRoom() {
        Optional<User> user = this.userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (user.isEmpty()) return ResponseEntity.status(404).body("You don't have a profile created");
        String newRoomKey = roomService.createRoom(user.get().getUsername());
        return ResponseEntity.ok(new RoomKeyResponse(newRoomKey));
    }

    @PostMapping("/startRoom/{roomKey}")
    public ResponseEntity<?> startRoom(@PathVariable String roomKey) {
        Optional<User> user = this.userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (user.isEmpty()) return ResponseEntity.status(404).body("You don't have a profile created");
        StartGameResponse startGameResponse = roomService.startGame(roomKey, user.get().getUsername());

        if (startGameResponse.getGameResponse() == null) {
            return ResponseEntity.status(startGameResponse.getResponseDto().getResponseCode()).body(startGameResponse.getResponseDto().getResponse());
        } else {
            RoomUpdateResponse roomUpdateResponse = new RoomUpdateResponse(RoomUpdateType.START);
            roomUpdateResponse.setGameResponse(startGameResponse.getGameResponse());
            this.simpMessagingTemplate.convertAndSend("/cuttle/updateRoom/" + roomKey, roomUpdateResponse);
            return ResponseEntity.ok(roomUpdateResponse.getGameResponse());
        }
    }

    @PostMapping("/restartRoom/{roomKey}")
    public ResponseEntity<?> restartRoom(@PathVariable String roomKey) {
        Optional<User> user = this.userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (user.isEmpty()) return ResponseEntity.status(404).body("You don't have a profile created");
        StartGameResponse restartGameResponse = roomService.restartRoom(roomKey, user.get().getUsername());

        if (restartGameResponse.getGameResponse() == null) {
            return ResponseEntity.status(restartGameResponse.getResponseDto().getResponseCode()).body(restartGameResponse.getResponseDto().getResponse());
        } else {
            RoomUpdateResponse roomUpdateResponse = new RoomUpdateResponse(RoomUpdateType.RESTART);
            roomUpdateResponse.setGameResponse(restartGameResponse.getGameResponse());
            this.simpMessagingTemplate.convertAndSend("/cuttle/update/" + roomKey, roomUpdateResponse);
            return ResponseEntity.ok(restartGameResponse.getGameResponse());
        }
    }

    @PostMapping("/stopRoom/{roomKey}")
    public ResponseEntity<?> stopRoom(@PathVariable String roomKey) {
        Optional<User> user = this.userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (user.isEmpty()) return ResponseEntity.status(404).body("You don't have a profile created");
        ResponseDto responseDto = roomService.stopGame(roomKey, user.get().getUsername());
        return ResponseEntity.status(responseDto.getResponseCode()).body(responseDto.getResponse());
    }

    @MessageMapping("/visualUpdate")
    public ResponseEntity<?> visualRoomUpdate(@Payload VisualUpdate visualUpdate) {
        this.simpMessagingTemplate.convertAndSend("/cuttle/update/" + visualUpdate.getRoomKey(), visualUpdate);
        return ResponseEntity.ok("Socket updated");
    }

    @MessageMapping("/playAction")
    public ResponseEntity<?> doAction(@Payload GameAction gameAction, StompHeaderAccessor stompHeaderAccessor) {
        if (gameAction.getActionType() == ActionType.DRAW) {
            this.simpMessagingTemplate.convertAndSend("/cuttle/update/" + gameAction.getRoomKey(), roomService.drawCard(gameAction.getRoomKey()));
        } else {
            this.simpMessagingTemplate.convertAndSend("/cuttle/update/" + gameAction.getRoomKey(), roomService.playCard(gameAction));
        }
        return ResponseEntity.ok("Socket updated");
    }

}
