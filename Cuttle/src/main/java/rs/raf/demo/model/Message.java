package rs.raf.demo.model;

import lombok.Data;

@Data
public class Message {
    private String from;
    private String text;

    public Message() {
    }

    public Message(String from, String text) {
        this.from = from;
        this.text = text;
    }
}
