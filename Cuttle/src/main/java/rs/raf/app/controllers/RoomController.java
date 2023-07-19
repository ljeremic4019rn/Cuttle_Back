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
import rs.raf.app.model.actions.GameAction;
import rs.raf.app.model.actions.GameResponse;
import rs.raf.app.model.User;
import rs.raf.app.responses.ResponseDto;
import rs.raf.app.responses.RoomKeyResponse;
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
    public ResponseEntity<?> joinRoom(@PathVariable String roomKey){
        Optional<User> user = this.userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (user.isEmpty()) return ResponseEntity.status(404).body("You don't have a profile created");
        ResponseDto responseDto = roomService.joinRoom(roomKey, user.get().getUsername());
        return ResponseEntity.status(responseDto.getResponseCode()).body(responseDto.getResponse());
    }

    @PostMapping("/createRoom")
    public ResponseEntity<?> createRoom(){
        Optional<User> user = this.userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (user.isEmpty()) return ResponseEntity.status(404).body("You don't have a profile created");
        String newRoomKey = roomService.createRoom(user.get().getUsername());
        return ResponseEntity.ok(new RoomKeyResponse(newRoomKey));
    }

    @PostMapping("/startRoom/{roomKey}")
    public ResponseEntity<?> startRoom(@PathVariable String roomKey){
        Optional<User> user = this.userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (user.isEmpty()) return ResponseEntity.status(404).body("You don't have a profile created");
        ResponseDto responseDto = roomService.startGame(roomKey, user.get().getUsername());
        return ResponseEntity.status(responseDto.getResponseCode()).body(responseDto.getResponse());
    }

    @PostMapping("/stopRoom/{roomKey}")
    public ResponseEntity<?> stopRoom(@PathVariable String roomKey){
        Optional<User> user = this.userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (user.isEmpty()) return ResponseEntity.status(404).body("You don't have a profile created");
        ResponseDto responseDto = roomService.stopGame(roomKey, user.get().getUsername());
        return ResponseEntity.status(responseDto.getResponseCode()).body(responseDto.getResponse());
    }


    @MessageMapping("/playAction")
//    @SendTo("/cuttle/update")
    public String doAction(@Payload GameAction gameAction, StompHeaderAccessor stompHeaderAccessor){
        //do action
        System.out.println(stompHeaderAccessor.getUser().getName());

        String tmp = roomService.playCard(gameAction);
        //todo vrati pravilan response webu
        this.simpMessagingTemplate.convertAndSend("/cuttle/update/" + gameAction.getRoomKey(), new GameResponse("this is a response"));
        return "ok";
    }

}