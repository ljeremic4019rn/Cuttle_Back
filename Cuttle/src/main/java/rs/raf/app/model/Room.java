package rs.raf.app.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
public class Room{

    private boolean gameIsRunning = false;
    private String idKey;
    private ArrayList<String> players = new ArrayList<>(); //first player is the room owner




    public String playTurn(String input){
        return "This was the input " + input;
    }


    public void startGame(){
        gameIsRunning = true;
    }

    public void stopGame(){
        gameIsRunning = false;
    }

}
