package rs.raf.demo.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import rs.raf.demo.model.Message;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.PathParam;

@Component
public class WebSocketEventListener {

    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    public WebSocketEventListener(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        System.out.println("Received a new web socket connection. Session: " + headerAccessor.getSessionId());

        Message message = new Message("Application", "New session: " + headerAccessor.getSessionId());
        this.messagingTemplate.convertAndSend("/topic/messages", message);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        System.out.println("Disconnected session : " + headerAccessor.getSessionId());

        Message message = new Message("Application", "Disconnected session: " + headerAccessor.getSessionId());
        this.messagingTemplate.convertAndSend("/topic/messages", message);
    }
}
