package rs.raf.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import rs.raf.demo.model.GameAction;
import rs.raf.demo.model.GameResponse;
import rs.raf.demo.model.User;
import rs.raf.demo.responses.ResponseDto;
import rs.raf.demo.responses.RoomKeyResponse;
import rs.raf.demo.services.RoomService;
import rs.raf.demo.services.UserService;

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
        if (!user.isPresent()) return ResponseEntity.status(404).body("You don't have a profile created");
        ResponseDto responseDto = roomService.joinRoom(roomKey, user.get().getUsername());
        return ResponseEntity.status(responseDto.getResponseCode()).body(responseDto.getResponse());
    }

    @PostMapping("/createRoom")
    public ResponseEntity<?> createRoom(){
        Optional<User> user = this.userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!user.isPresent()) return ResponseEntity.status(404).body("You don't have a profile created");
        String newRoomKey = roomService.createRoom(user.get().getUsername());
        return ResponseEntity.ok(new RoomKeyResponse(newRoomKey));
    }

    @PostMapping("/startRoom/{roomKey}")
    public ResponseEntity<?> startRoom(@PathVariable String roomKey){
        Optional<User> user = this.userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!user.isPresent()) return ResponseEntity.status(404).body("You don't have a profile created");
        ResponseDto responseDto = roomService.startGame(roomKey);
        return ResponseEntity.status(responseDto.getResponseCode()).body(responseDto.getResponse());
    }

    @PostMapping("/stopRoom/{roomKey}")
    public ResponseEntity<?> stopRoom(@PathVariable String roomKey){
        Optional<User> user = this.userService.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!user.isPresent()) return ResponseEntity.status(404).body("You don't have a profile created");
        ResponseDto responseDto = roomService.stopGame(roomKey);
        return ResponseEntity.status(responseDto.getResponseCode()).body(responseDto.getResponse());
    }


    @MessageMapping("/playAction/{roomKey}")
    @SendTo("/cuttle/update")
    public String doAction(@PathVariable String roomKey, @Payload GameAction gameAction, StompHeaderAccessor stompHeaderAccessor){
        //do action
        System.out.println(stompHeaderAccessor.getUser().getName());

        this.simpMessagingTemplate.convertAndSend("/cuttle/messages/" + roomKey, new GameResponse("this is a response"));
        return "ok";
    }

}
