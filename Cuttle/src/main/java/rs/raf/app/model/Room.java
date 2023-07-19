package rs.raf.app.model;

import lombok.Getter;
import lombok.Setter;
import rs.raf.app.model.actions.CardComparison;
import rs.raf.app.model.actions.GameAction;

import java.util.*;

@Getter
@Setter
public class Room {

    //todo postavi room creation time kako bi se pratila koja je aktiva a koja je smece
    //todo smisli nacin da se sklone igraci ako kliknu back ili ako nisu aktivni (mozda na frontu)

    //room vars
    private boolean gameIsRunning = false;
    private String idKey;
    private String roomOwner;
    private ArrayList<String> players = new ArrayList<>(); // prvi koji udje je owner
    private int currentPlayersTurn;

    //decks
    private ArrayList<String> setUpDeck = new ArrayList<>();
    private Stack<String> deck = new Stack<>();
    private ArrayList<String> graveyard = new ArrayList<>();

    //player cards
    private Map<Integer, ArrayList<String>> playerHands = new HashMap<>(); //player number / hand
    private Map<Integer, ArrayList<String>> playerTables = new HashMap<>(); //cards that are in play
    private Map<Integer, Integer> playerScore = new HashMap<>(); //player number / hand

    //helper vars
    private boolean turnOver;
    Random random = new Random();



    public Room() {
        setUpDeck.addAll(Arrays.asList(
                "1_C", "2_C", "3_C", "4_C", "5_C", "6_C", "7_C", "8_C", "9_C", "10_C", "J_C", "Q_C", "K_C",//weakest suit
                "1_D", "2_D", "3_D", "4_D", "5_D", "6_D", "7_D", "8_D", "9_D", "10_D", "J_D", "Q_D", "K_D",
                "1_H", "2_H", "3_H", "4_H", "5_H", "6_H", "7_H", "8_H", "9_H", "10_H", "J_H", "Q_H", "K_H",
                "1_S", "2_S", "3_S", "4_S", "5_S", "6_S", "7_S", "8_S", "9_S", "10_S", "J_S", "Q_S", "K_S" //strongest suit
        ));
        Collections.shuffle(setUpDeck);
    }

    private void dealCards() {
        //1. we fill the stack with shuffled cards (stack because its easier to remove just the top card and not having to fuck with the list size)
        for (String card : setUpDeck) {
            deck.push(card);
        }

        //2. pop 6 cards into each players hand
        playerHands.forEach((playerId, cardsInHandList) -> {
            for (int i = 0; i < 6; i++) {
                cardsInHandList.add(deck.pop());
            }
        });
    }

    public void drawCard(){
        playerHands.get(currentPlayersTurn).add(deck.pop());
    }

    /*
    card send format = <number/rank> - <suit> (1-S / K-C)
     */

    //todo uradi return da znamo kada se potez zavrsio

    //todo dodaj na kraju check da li current player ima 21

    public void playTurn(GameAction gameAction) {

        switch (gameAction.getActionType()) {
            case NUMBER -> turnOver = playNumberCard(gameAction);
            case POWER -> turnOver = playScuttleCard(gameAction);
            case SCUTTLE -> {
                switch (gameAction.getCardPlayed().split("_")[0]) {
                    case "1" -> turnOver = play1power(gameAction);
                    case "2" -> turnOver = play2power(gameAction);
                    case "3" -> turnOver = play3power(gameAction);
                    case "4" -> turnOver = play4power(gameAction);
                    case "5" -> turnOver = play5power(gameAction);
                    case "6" -> turnOver = play6power(gameAction);
                    case "7" -> turnOver = play7power(gameAction);
                    case "8" -> turnOver = play8power(gameAction);
                    case "9" -> turnOver = play9power(gameAction);
                    case "J" -> turnOver = playJackPower(gameAction);
                    case "Q" -> turnOver = playQueenPower(gameAction);
                    case "K" -> turnOver = playKingPower(gameAction);
                }
            }
        }

        //todo NA KRAJU PROVERI DAL POSTOJI KRALJ NA TABLI I PROVERI DAL JE NEKO PRESAO 21
    }

    private boolean playNumberCard(GameAction gameAction) {
        //we take the number of the card
        int playedCardPower = Integer.parseInt(gameAction.getCardPlayed().split("_")[0]);
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        playerTables.get(currentPlayersTurn).add(gameAction.getCardPlayed());
        playerScore.put(currentPlayersTurn, playerScore.get(currentPlayersTurn + playedCardPower));
        return true;
    }

    private boolean playScuttleCard(GameAction gameAction) {
        String[] playedCardSplit = gameAction.getCardPlayed().split("_");
        String[] ontoPlayedCardSplit = gameAction.getOntoCardPlayed().split("_");
        int playedCardPower = Integer.parseInt(playedCardSplit[0]);
        int playedOntoCardPower = Integer.parseInt(ontoPlayedCardSplit[0]);

        //if card played i bigger do action
        if (playedCardPower > playedOntoCardPower) {
            playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
            playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());
            graveyard.add(gameAction.getCardPlayed());
            graveyard.add(gameAction.getOntoCardPlayed());
            return true;
        }
        //if card numbs are same but played suit is bigger do aciton
        else if (playedCardPower == playedOntoCardPower) {
            if (cardSuitComparator(playedCardSplit[1], ontoPlayedCardSplit[1]) == CardComparison.BIGGER) {
                playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());
                graveyard.add(gameAction.getCardPlayed());
                graveyard.add(gameAction.getOntoCardPlayed());
                return true;
            }
        }
        //dont do anything, bad turn (should not get here ever)
        return false;
    }

    //todo PROVER APSOLUTNO SVAKU POWER FUNKCIJU DA LI DOBRO RADI

    private boolean play1power(GameAction gameAction) {
        //go through all player tables
        playerTables.forEach((playerId, cardsOnTableList) -> {
            String[] cardSplit;
            //go through all cards and remove all number cards
            for (String card : cardsOnTableList) {
                cardSplit = card.split("_");
                //remove all number cards except 8 as a permanent effect card
                if (cardSplit[1].equals("1") || cardSplit[1].equals("2") || cardSplit[1].equals("3")  || cardSplit[1].equals("4") || cardSplit[1].equals("5")
                        || cardSplit[1].equals("6")|| cardSplit[1].equals("7")|| cardSplit[1].equals("8")|| cardSplit[1].equals("9") || cardSplit[1].equals("10")){
                    cardsOnTableList.remove(card);
                    graveyard.add(card);
                }
            }
        });

        //discard 1 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        return true;
    }

    //todo finish 2
    private boolean play2power(GameAction gameAction) {
        //proveri da li ima kraljica na terenu
        //posalji kartu na groblje ako je igrano na permanent effect kartu na terenu
        //ako je igrana kao counter neke karte (NAPRAVI ENUM MOZDA) samo je baci na groblje



        //discard 2 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        return true;
    }

    //here the ontoPlayedCard is referred to the graveyard card and not a card on the table
    private boolean play3power(GameAction gameAction) {
        //return card from graveyard to us
        graveyard.remove(gameAction.getOntoCardPlayed());
        playerHands.get(currentPlayersTurn).add(gameAction.getOntoCardPlayed());

        //discard 3 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());
        return true;
    }

    //todo trenutno je namesteno da discarduje 2 random karte iz ruke protivnika, ako mozes posle namesti da on bira 2 karte koje hoce
    private boolean play4power(GameAction gameAction) {
        int playerHandSize = playerHands.get(gameAction.getOntoPlayer()).size();
        int randomCardIndex;

        //if 0 nothing to discard
        if (playerHandSize == 0) return true;
        //if one remove that single one and add to graveyard
        else if (playerHandSize == 1) {
            graveyard.add(playerHands.get(gameAction.getOntoPlayer()).get(0));
            playerHands.get(gameAction.getOntoPlayer()).remove(0);
        }
        //remove 2 at random and add to graveyard
        else {
            playerHandSize = playerHands.get(gameAction.getOntoPlayer()).size();
            randomCardIndex = random.nextInt(playerHandSize);
            graveyard.add(playerHands.get(gameAction.getOntoPlayer()).get(randomCardIndex));
            playerHands.get(gameAction.getOntoPlayer()).remove(randomCardIndex);

            playerHandSize = playerHands.get(gameAction.getOntoPlayer()).size();
            randomCardIndex = random.nextInt(playerHandSize);
            graveyard.add(playerHands.get(gameAction.getOntoPlayer()).get(randomCardIndex));
            playerHands.get(gameAction.getOntoPlayer()).remove(randomCardIndex);
        }

        //discard 4 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        return true;
    }

    private boolean play5power(GameAction gameAction) {
        playerHands.get(currentPlayersTurn).add(deck.pop());
        playerHands.get(currentPlayersTurn).add(deck.pop());

        //discard 5 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        return true;
    }

    private boolean play6power(GameAction gameAction) {
        //go through all player tables
        playerTables.forEach((playerId, cardsOnTableList) -> {
            String[] cardSplit;
            //go through all cards and remove all permanent effect (image) cards
            for (String card : cardsOnTableList) {
                cardSplit = card.split("_");
                //if its an image card or 8 in power remove it
                if (cardSplit[1].equals("J") || cardSplit[1].equals("Q") || cardSplit[1].equals("K") || cardSplit[1].equals("P")){
                    cardsOnTableList.remove(card);
                    graveyard.add(card);
                }
            }
        });

        //discard 6 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        return true;
    }

    //todo finish 7
    private boolean play7power(GameAction gameAction) {

        //discard 7 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        return true;
    }

    //todo kada se igra 8 power na terenu ce se napisati P_8_<suit>
    private boolean play8power(GameAction gameAction) {
        //remove 8 from hand
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        playerTables.get(currentPlayersTurn).add(gameAction.getCardPlayed());

        //todo stavi na front boolean "8 is in play" sto ce svima da pokaze ruke

        return true;
    }

    //todo finish 9
    private boolean play9power(GameAction gameAction) {
        String[] ontoPlayedCardSplit = gameAction.getOntoCardPlayed().split("_");

        switch (ontoPlayedCardSplit[1]){
            case "J" -> {}
            case "Q" -> {}
            case "K" -> {}
            case "P" -> {}
        }

        //discard 9 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        return true;
    }

    private boolean playJackPower(GameAction gameAction) {
        //proveri prvo dal ima kraljiva u igri
        //stavice se Stolen_<og player id> ispred karte i prebaci ce se kod igraca koji je bacio
        return true;
    }

    private boolean playQueenPower(GameAction gameAction) {
        //samo ce da se stavi na teren

        return true;
    }

    private boolean playKingPower(GameAction gameAction) {
        //samo postavi na tablu
        return true;
    }

    public void startGame() {
        currentPlayersTurn = 1; //todo mozda 0 ili username
        for (int i = 0; i < players.size(); i++) {
            playerScore.put(i, 0);
        }
        dealCards();
        gameIsRunning = true;
    }

    public void stopGame() {
        gameIsRunning = false;
    }

    //C - weakest
    //D
    //H
    //S - strongest
    private CardComparison cardSuitComparator(String playedCardSuit, String ontoPlayedCardSuit) {//check if left (played) iz bigger
        switch (playedCardSuit) {
            case "C" -> {
                return CardComparison.NOT_BIGGER;
            }
            case "D" -> {
                if (ontoPlayedCardSuit.equals("C")) return CardComparison.BIGGER;
                if (ontoPlayedCardSuit.equals("D")) return CardComparison.NOT_BIGGER;
                if (ontoPlayedCardSuit.equals("H")) return CardComparison.NOT_BIGGER;
                if (ontoPlayedCardSuit.equals("S")) return CardComparison.NOT_BIGGER;
            }
            case "H" -> {
                if (ontoPlayedCardSuit.equals("C")) return CardComparison.BIGGER;
                if (ontoPlayedCardSuit.equals("D")) return CardComparison.BIGGER;
                if (ontoPlayedCardSuit.equals("H")) return CardComparison.NOT_BIGGER;
                if (ontoPlayedCardSuit.equals("S")) return CardComparison.NOT_BIGGER;
            }
            case "S" -> {
                if (ontoPlayedCardSuit.equals("C")) return CardComparison.BIGGER;
                if (ontoPlayedCardSuit.equals("D")) return CardComparison.BIGGER;
                if (ontoPlayedCardSuit.equals("H")) return CardComparison.BIGGER;
                if (ontoPlayedCardSuit.equals("S")) return CardComparison.NOT_BIGGER;
            }
        }
        return CardComparison.NOT_BIGGER;
    }

}
