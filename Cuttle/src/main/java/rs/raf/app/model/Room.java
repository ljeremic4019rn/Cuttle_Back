package rs.raf.app.model;

import lombok.Getter;
import lombok.Setter;
import rs.raf.app.model.actions.GameAction;

import java.util.*;

@Getter
@Setter
public class Room {

    //todo postavi room creation time kako bi se pratila koja je aktiva a koja je smece
    //todo smisli nacin da se sklone igraci ako kliknu back ili ako nisu aktivni (mozda na frontu)

    private boolean gameIsRunning = false;
    private String idKey;
    private String roomOwner;
    private ArrayList<String> players = new ArrayList<>(); // prvi koji udje je owner
    private int currentPlayersTurn;

    private ArrayList<String> setUpDeck = new ArrayList<>();
    private Stack<String> deck = new Stack<>();
    private ArrayList<String> graveyard = new ArrayList<>();
    private Map<Integer, ArrayList<String>> playerHands = new HashMap<>(); //player number / hand


    public Room() {
        setUpDeck.addAll(Arrays.asList(
                "1C", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "10C", "JC", "QC", "KC",//weakest suit
                "1D", "2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D", "10D", "JD", "QD", "KD",
                "1H", "2H", "3H", "4H", "5H", "6H", "7H", "8H", "9H", "10H", "JH", "QH", "KH",
                "1S", "2S", "3S", "4S", "5S", "6S", "7S", "8S", "9S", "10S", "JS", "QS", "KS" //strongest suit
        ));
        Collections.shuffle(setUpDeck);
    }

    private void dealCards() {
        //1. we fill the stack with shuffled cards (stack because its easier to remove just the top card and not having to fuck with the list size)
        for (String card : setUpDeck) {
            deck.push(card);
        }

        //2. pop 6 cards into each players hand
        playerHands.forEach((key, handList) -> {
            for (int i = 0; i < 6; i++) {
                handList.add(deck.pop());
            }
        });
    }

    /*
    number cards 1-10
        <number/rank> - <suit> - <action_type> - action type is NUMBER OR POWER (1-C-N / 1-H-P)

    image cards
        <number/rank> - <suit> (J-S / K-C)
     */
    public void playTurn(GameAction gameAction) {
        String[] cardPlayed = gameAction.getOnToCardPlayed().split("-");

        switch (cardPlayed[0]) {
            case "1" -> play1(gameAction);
            case "2" -> play2(gameAction);
        }

    }

    private void play1(GameAction gameAction) {

    }

    private void play2(GameAction gameAction) {

    }

    private void play3(GameAction gameAction) {

    }

    private void play4(GameAction gameAction) {

    }

    private void play5(GameAction gameAction) {

    }

    private void play6(GameAction gameAction) {

    }

    private void play7(GameAction gameAction) {

    }

    private void play8(GameAction gameAction) {

    }

    private void play9(GameAction gameAction) {

    }

    private void play10(GameAction gameAction) {

    }

    private void playJack(GameAction gameAction) {

    }

    private void playQueen(GameAction gameAction) {

    }

    private void playKing(GameAction gameAction) {

    }

    public void startGame() {
        currentPlayersTurn = 1; //todo mozda 0 ili username

        System.err.println(deck);

        dealCards();
        gameIsRunning = true;

        System.out.println("after");
        System.err.println(playerHands.size());
        System.err.println(deck);
        System.err.println(playerHands);

    }

    public void stopGame() {
        gameIsRunning = false;
    }

}
