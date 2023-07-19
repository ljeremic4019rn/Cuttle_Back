package rs.raf.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.demo.model.Message;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class MessageController {

    private SimpMessagingTemplate simpMessagingTemplate;
    private SimpUserRegistry simpUserRegistry;

    @Autowired
    public MessageController(SimpMessagingTemplate simpMessagingTemplate, SimpUserRegistry simpUserRegistry) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.simpUserRegistry = simpUserRegistry;
    }

    @MessageMapping("/send-message")
    @SendTo("/cuttle/messages")
    public Message send(@Payload Message message, StompHeaderAccessor stompHeaderAccessor) throws Exception {
        System.out.println(stompHeaderAccessor.getUser().getName());
        message.setFrom(stompHeaderAccessor.getUser().getName());
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        System.out.println("[" + time + "] Message sent.");
        return message;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/test")
    public String test() {
        this.simpMessagingTemplate.convertAndSend("/cuttle/messages", new Message("Example name", "test"));
        return "ok";
    }
}
