package rs.raf.app.model;

import lombok.Getter;
import lombok.Setter;
import rs.raf.app.model.actions.GameAction;
import rs.raf.app.model.actions.GameResponse;
import rs.raf.app.model.actions.enums.CardComparison;
import rs.raf.app.model.actions.enums.GameResponseType;

import java.util.*;

@Getter
@Setter
public class Room {


    //room vars
    private GameResponse gameResponse;
    private boolean gameIsRunning = false;
    private String idKey;
    private String roomOwner;
    private int numOfPlayers;
    private ArrayList<String> players = new ArrayList<>(); // prvi koji udje je owner
    private int startingPlayer = 0;
    private int currentPlayersTurn = 0;
    private int playerWhoWon = -1;
    private String playerWhoWonName = "";

    //decks
    private ArrayList<String> setUpDeck = new ArrayList<>();
    private Stack<String> deck = new Stack<>();
    private ArrayList<String> graveyard = new ArrayList<>();

    //player cards
    private Map<Integer, ArrayList<String>> playerHands = new HashMap<>(); //player number / hand
    private Map<Integer, ArrayList<String>> playerTables = new HashMap<>(); //cards that are in play
    private Map<Integer, Integer> playerScore = new HashMap<>(); //player number / hand
    private Map<Integer, Integer> playerKings = new HashMap<>(); //number of kinds a player has active

    //helper vars
    private Random random = new Random();
    private ArrayList<String> cardsToRemove = new ArrayList<>();
    private ArrayList<String> cardsForGraveyard = new ArrayList<>();
    private GameResponseType gameResponseType = GameResponseType.REGULAR_GO_NEXT;
    private Long roomLastUpdated = 0L;


    public GameResponse startGame() {
        for (int i = 0; i < players.size(); i++) {
            playerScore.put(i, 0);
            playerKings.put(i, 0);
            playerTables.put(i, new ArrayList<>());
            playerHands.put(i, new ArrayList<>());
        }

        System.out.println(playerHands);
        System.out.println(playerHands.size());

        dealCards();
        numOfPlayers = players.size();
        gameIsRunning = true;

        gameResponse = new GameResponse(
                GameResponseType.REGULAR_GO_NEXT,
                currentPlayersTurn,
                deck,
                graveyard,
                playerHands,
                playerTables,
                playerScore,
                ""
        );
        return gameResponse;
    }

    public GameResponse restartGame() {
        gameIsRunning = false;
        playerWhoWon = -1;
        playerWhoWonName = "";
        deck = new Stack<>();
        graveyard = new ArrayList<>();
        cardsToRemove = new ArrayList<>();
        cardsForGraveyard = new ArrayList<>();
        gameResponseType = GameResponseType.REGULAR_GO_NEXT;
        Collections.shuffle(setUpDeck);

        startingPlayer++;
        if (startingPlayer >= numOfPlayers) startingPlayer = 0;
        currentPlayersTurn = startingPlayer;

        return startGame();
    }

    /*
    card format = <rank>_<suit> (10_S / K_C)
    exception to
    - 8 power = P_<rank>_<suit> (P_10_S)
    - Jacked cards = J_<rank>_<last owner>_[repeat if more jacks]_<rank>_<suit> (J_S_3_10_S / J_S_3_J_H_2_10_S)
    */
    public Room() {
        setUpDeck.addAll(Arrays.asList(
                "1_C", "2_C", "3_C", "4_C", "5_C", "6_C", "7_C", "8_C", "9_C", "10_C", "J_C", "Q_C", "K_C",
                "1_D", "2_D", "3_D", "4_D", "5_D", "6_D", "7_D", "8_D", "9_D", "10_D", "J_D", "Q_D", "K_D",
                "1_H", "2_H", "3_H", "4_H", "5_H", "6_H", "7_H", "8_H", "9_H", "10_H", "J_H", "Q_H", "K_H",
                "1_S", "2_S", "3_S", "4_S", "5_S", "6_S", "7_S", "8_S", "9_S", "10_S", "J_S", "Q_S", "K_S"
        ));
        Collections.shuffle(setUpDeck);
    }

    public void stopGame() {
        gameIsRunning = false;
    }

    private void dealCards() {
        //1. we fill the stack with shuffled cards (stack because it's easier to remove just the top card and not having to fuck with the list size)
        for (String card : setUpDeck) {
            deck.push(card);
        }

        //2. pop 6 cards into each players hand
        playerHands.forEach((playerId, cardsInHandList) -> {
            for (int i = 0; i < 6; i++) {
                cardsInHandList.add(deck.pop());
            }
        });
        playerHands.get(currentPlayersTurn).add(deck.pop());
    }


    public GameResponse playTurn(GameAction gameAction) {
        gameResponseType = GameResponseType.REGULAR_GO_NEXT;

        switch (gameAction.getActionType()) {
            case NUMBER -> playNumberCard(gameAction);
            case SCUTTLE -> playScuttleCard(gameAction);
            case SKIP -> {
            }
            //(for cards drawn by 7 and not played are discarded)  we place a card we want to discard into played for convenience
            case DISCARD_CARD -> discardSelectedCardFromPlayerHand(gameAction.getCardPlayed(), currentPlayersTurn);
            case COUNTER -> counterCardPlayed(gameAction);
            case POWER -> {
                if (!(gameAction.getCardPlayed().split("_")[0].equals("4")) && gameAction.getHelperCardList().size() != 0) {
                    clearHelperCardList(gameAction.getHelperCardList());
                }

                switch (gameAction.getCardPlayed().split("_")[0]) {
                    case "1" -> play1power(gameAction);
                    case "2" -> play2power(gameAction);
                    case "3" -> play3power(gameAction);
                    case "4" -> play4power(gameAction);
                    case "5" -> play5power(gameAction);
                    case "6" -> play6power(gameAction);
                    case "7" -> {
                        play7power(gameAction);
                        gameResponseType = GameResponseType.SEVEN;
                    }
                    case "8" -> play8power(gameAction);
                    case "9" -> play9power(gameAction);
                    case "J" -> playJackPower(gameAction);
                    case "Q" -> playQueenPower(gameAction);
                    case "K" -> playKingPower(gameAction);
                }
            }
        }

        playerWhoWon = checkIfSomebodyWon();
        if (playerWhoWon != -1) {
            gameResponseType = GameResponseType.GAME_OVER_WON;
            playerWhoWonName = players.get(playerWhoWon);
        }

        swapTurnToNextPlayer();
        gameResponse = new GameResponse(
                gameResponseType,
                currentPlayersTurn,
                deck,
                graveyard,
                playerHands,
                playerTables,
                playerScore,
                playerWhoWonName
        );

        return gameResponse;
    }

    public GameResponse drawCard() {
        playerHands.get(currentPlayersTurn).add(deck.pop());
        swapTurnToNextPlayer();
        gameResponse = new GameResponse(
                GameResponseType.REGULAR_GO_NEXT,
                currentPlayersTurn,
                deck,
                graveyard,
                playerHands,
                playerTables,
                playerScore,
                ""
        );
        return gameResponse;
    }

    /*
    used2s - all 2 cards that have been played and who played them <rank>_<suit>_<playerId> 2_S_3
    card played onto - card that has been countered
    onto player - who has been countered

    - also if a 2 is countered by another 2 it will be handled front side aka if 2Played var is filled it will just empty it (action passes), therefor countering a 2 with 2
     */
    private void counterCardPlayed(GameAction gameAction) {//this happens when the base card was countered and there is 1 or 3 counter cards to sent to graveyard
        String[] split2Card;
        String whole2Card;
        //send the countered card to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        //send all 2s used to graveyard
        for (String twoCard : gameAction.getHelperCardList()) {
            split2Card = twoCard.split("_");
            whole2Card = split2Card[0] + "_" + split2Card[1];

            playerHands.get(Integer.parseInt(split2Card[2])).remove(whole2Card);//remove 2 from players hand
            graveyard.add(whole2Card);
        }


    }

    //this happens (every turn) when card is double countered aka somebody countered a counter
    //so the card goes through but the counter cards need to be sent to graveyard
    private void clearHelperCardList(List<String> helperCardList) {//<rank>_<suit>_<playerId> 2_S_3
        String[] splitCard;
        String wholeCard;

        //send all 2s used to graveyard
        for (String card : helperCardList) {
            splitCard = card.split("_");
            wholeCard = splitCard[0] + "_" + splitCard[1];
            playerHands.get(Integer.parseInt(splitCard[2])).remove(wholeCard);//remove 2 from players hand
            graveyard.add(wholeCard);
        }
    }

    private int checkIfSomebodyWon() {
        for (Map.Entry<Integer, Integer> entry : playerScore.entrySet()) {
            int playerId = entry.getKey();
            int numberOfKings = playerKings.get(playerId);

            int score = entry.getValue();

            switch (numberOfKings) {
                case 0 -> {
                    if (score >= 21) return playerId;
                }
                case 1 -> {
                    if (score >= 14) return playerId;
                }
                case 2 -> {
                    if (score >= 10) return playerId;
                }
                case 3 -> {
                    if (score >= 7) return playerId;
                }
                case 4 -> {
                    if (score >= 5) return playerId;
                }
            }
        }
        return -1;
    }

    private void playNumberCard(GameAction gameAction) {
        //we take the number of the card
        int playedCardPower = Integer.parseInt(gameAction.getCardPlayed().split("_")[0]);
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        playerTables.get(currentPlayersTurn).add(gameAction.getCardPlayed());
        playerScore.put(currentPlayersTurn, playerScore.get(currentPlayersTurn) + playedCardPower);
    }

    /*
    rank = number from card (9_C)
                            0  1    0 1  2   3  4    0 1  2   3 4  5   6  7
    cards you can scuttle = 10_S || J_S_<id>_10_S || J_S_<id>_J_H_<id>_10_S
    */
    private void playScuttleCard(GameAction gameAction) {
        String[] playedCardSplit = gameAction.getCardPlayed().split("_");
        String[] ontoPlayedCardSplit = gameAction.getOntoCardPlayed().split("_");

        int playedCardRank = Integer.parseInt(playedCardSplit[0]);
        int playedOntoCardRank;
        String scuttledCardForGraveyard;

        ArrayList<String> jacksToSendToGraveyard = new ArrayList<>();
        boolean cardWasJacked = false;

        //if Jack is on top of the card we are scuttling,
        //find all jacks on top and send to graveyard
        //then send regular point card to graveyard
        if (ontoPlayedCardSplit[0].equals("J")) {
            cardWasJacked = true;
            playedOntoCardRank = Integer.parseInt(ontoPlayedCardSplit[ontoPlayedCardSplit.length - 2]); //skipping jacks
            scuttledCardForGraveyard = playedOntoCardRank + "_" + ontoPlayedCardSplit[ontoPlayedCardSplit.length - 1];//onto card it self

            int positionCounter = 0;
            String card;
            //collect all jacks to send to graveyard
            while (ontoPlayedCardSplit[positionCounter].equals("J")) { //J_S_<id>_10_S - we are splitting this
                card = ontoPlayedCardSplit[positionCounter] + "_" + ontoPlayedCardSplit[positionCounter + 1]; //ctr = J + _ + S
                jacksToSendToGraveyard.add(card);
                positionCounter = positionCounter + 3;
            }
        } else {
            playedOntoCardRank = Integer.parseInt(ontoPlayedCardSplit[0]);
            scuttledCardForGraveyard = gameAction.getOntoCardPlayed();
        }

        //if card played is bigger scuttle
        if (playedCardRank > playedOntoCardRank) {
            playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());//remove card we are scuttling with
            playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());//remove scuttled card
            graveyard.add(gameAction.getCardPlayed());//add card we are scuttling with to graveyard
            graveyard.add(scuttledCardForGraveyard);//add scuttled to graveyard
            playerScore.put(gameAction.getOntoPlayer(), playerScore.get(gameAction.getOntoPlayer()) - playedOntoCardRank);//subtract score for card worth
            if (cardWasJacked) graveyard.addAll(jacksToSendToGraveyard);//if was jacked send jacks to graveyard
        }
        //if card numbs are same but played suit is bigger scuttle
        else if (playedCardRank == playedOntoCardRank) {
            if (cardSuitComparator(playedCardSplit[1], ontoPlayedCardSplit[1]) == CardComparison.BIGGER) {//check which suit is bigger
                playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());
                graveyard.add(gameAction.getCardPlayed());
                graveyard.add(scuttledCardForGraveyard);
                playerScore.put(gameAction.getOntoPlayer(), playerScore.get(gameAction.getOntoPlayer()) - playedOntoCardRank);//subtract score for card worth
                if (cardWasJacked) graveyard.addAll(jacksToSendToGraveyard);
            }
        }
    }

    private void play1power(GameAction gameAction) {
        //go through all player tables
        playerTables.forEach((playerId, cardsOnTableList) -> {
            cardsToRemove.clear();
            cardsForGraveyard.clear();
            String[] cardSplit;

            //go through all cards and remove all number cards
            for (String card : cardsOnTableList) {
                cardSplit = card.split("_");
                //remove all number cards except 8 as a permanent effect card (P_8_S)
                if (cardSplit[0].equals("1") || cardSplit[0].equals("2") || cardSplit[0].equals("3") || cardSplit[0].equals("4") || cardSplit[0].equals("5")
                        || cardSplit[0].equals("6") || cardSplit[0].equals("7") || cardSplit[0].equals("8") || cardSplit[0].equals("9") || cardSplit[0].equals("10")) {
                    cardsToRemove.add(card);
                    cardsForGraveyard.add(card);
//                    graveyard.add(card);
                } else if (cardSplit[0].equals("J")) {
                    int jackCounter = 0;
                    String jackCard;
                    String cardToScuttle = cardSplit[cardSplit.length - 2] + "_" + cardSplit[cardSplit.length - 1];//onto card it self
                    //collect all jacks to send to graveyard
                    while (cardSplit[jackCounter].equals("J")) { //J_S_<id>_10_S - we are splitting this
                        jackCard = cardSplit[jackCounter] + "_" + cardSplit[jackCounter + 1]; //ctr = J + _ + S
                        cardsForGraveyard.add(jackCard);//add all jacks to graveyard
                        jackCounter = jackCounter + 3;
                    }
                    cardsForGraveyard.add(cardToScuttle);
                    cardsToRemove.add(card);//remove the actual jacked card
                }
            }
            cardsOnTableList.removeAll(cardsToRemove); //removing all points from table
            graveyard.addAll(cardsForGraveyard);//adding all points to graveyard
        });

        playerScore.replaceAll((k, v) -> 0);//setting all scores to 0
        //send 1 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());
    }

    private void play2power(GameAction gameAction) {
        String[] ontoPlayedCardSplit = gameAction.getOntoCardPlayed().split("_");

        //0 1  2   3 4     0 1  2   3 4  5   6 7  8   9  10...
        //J_S_<id>_10_C || J_S_<id>_J_C_<id>_J_H_<id>_10_C
        switch (ontoPlayedCardSplit[0]) {
            case "J" -> {
                String topJackCard = ontoPlayedCardSplit[0] + "_" + ontoPlayedCardSplit[1];
                String cardToReturn = "";
                int playerToReturnCardTo = Integer.parseInt(ontoPlayedCardSplit[2]);

                //if split[3] == J -> there is another Jack on top, return to that players table based on the next <id>
                //we have to build back the card with the Js still on it
                //else if split[3] == num -> return the point card to the og players table
                if (ontoPlayedCardSplit[3].equals("J")) {
                    for (int i = 3; i < ontoPlayedCardSplit.length; i++) {
                        cardToReturn = cardToReturn + ontoPlayedCardSplit[i];
                        if (i < ontoPlayedCardSplit.length - 1)
                            cardToReturn = cardToReturn + "_";
                    }
                } else {
                    cardToReturn = ontoPlayedCardSplit[3] + "_" + ontoPlayedCardSplit[4];
                }

                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed()); //remove jacked card
                graveyard.add(topJackCard);//send jack to graveyard
                playerTables.get(playerToReturnCardTo).add(cardToReturn);

                //exchange points
                int pointsToExchange = Integer.parseInt(ontoPlayedCardSplit[ontoPlayedCardSplit.length - 2]);
                playerScore.put(playerToReturnCardTo, playerScore.get(playerToReturnCardTo) + pointsToExchange);
                playerScore.put(gameAction.getOntoPlayer(), playerScore.get(gameAction.getOntoPlayer()) - pointsToExchange);
            }
            //Q_S...
            case "Q" -> {
                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());//remove 9ed card from table
                graveyard.add(gameAction.getOntoCardPlayed());//send 2ed card to graveyard
            }
            case "P" -> {
                String card8withoutP = ontoPlayedCardSplit[1] + "_" + ontoPlayedCardSplit[2];
                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());//remove 9ed card from table
                graveyard.add(card8withoutP);//return 9ed card to hand
            }
            case "K" -> {
                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());//remove 2ed card from table
                graveyard.add(gameAction.getOntoCardPlayed());//send 2ed card to graveyard
                playerKings.put(gameAction.getOntoPlayer(), playerKings.get(gameAction.getOntoPlayer()) - 1);//remove one king on king tracker map
            }
            //1_S...
            default -> {
            }
        }

        //send 2 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());


    }

    //here the ontoPlayedCard is referred to the graveyard card and not a card on the table
    private void play3power(GameAction gameAction) {
        //return card from graveyard to us
        graveyard.remove(gameAction.getOntoCardPlayed());
        playerHands.get(currentPlayersTurn).add(gameAction.getOntoCardPlayed());

        //send 3 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());
    }

    private void play4power(GameAction gameAction) {
        int playerHandSize = playerHands.get(gameAction.getOntoPlayer()).size();

        //if one remove that single one and add to graveyard
        if (playerHandSize == 1) {
            discardRandomCardFromPlayerHand(playerHands.get(gameAction.getOntoPlayer()));
        }
        //remove 2 at random and add to graveyard
        else {
            //if two cards are selected by enemy player on front end, discard them
//            if (gameAction.getHelperCardList().size() == 2) {
//                discardSelectedCardFromPlayerHand(gameAction.getHelperCardList().get(0), gameAction.getOntoPlayer());
//                discardSelectedCardFromPlayerHand(gameAction.getHelperCardList().get(1), gameAction.getOntoPlayer());
//            } else {//if player didn't select two cards, discard 2 at random
                discardRandomCardFromPlayerHand(playerHands.get(gameAction.getOntoPlayer()));
                discardRandomCardFromPlayerHand(playerHands.get(gameAction.getOntoPlayer()));
//            }
        }

        //send 4 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());
    }

    private void play5power(GameAction gameAction) {
        playerHands.get(currentPlayersTurn).add(deck.pop());
        playerHands.get(currentPlayersTurn).add(deck.pop());

        //send 5 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());
    }

    private void play6power(GameAction gameAction) {
        Map<Integer, String> cardsToGiveBackToOgPlayers = new HashMap<>();

        //go through all player tables
        for (Map.Entry<Integer, ArrayList<String>> playerTable : playerTables.entrySet()) {
            cardsToRemove.clear();
            cardsForGraveyard.clear();
            String[] cardSplit;

            //go through all cards and remove all permanent effect (image) cards
            for (int i = 0; i < playerTable.getValue().size(); i++) {
                String card = playerTable.getValue().get(i);

                cardSplit = card.split("_");
                //if its an image card or 8 in power remove it
                switch (cardSplit[0]) {
                    case "Q" -> {
                        cardsToRemove.add(card);
                        cardsForGraveyard.add(card);
                    }
                    case "P" -> {
                        String card8withoutP = cardSplit[1] + "_" + cardSplit[2];
                        cardsToRemove.add(card);
                        cardsForGraveyard.add(card8withoutP);
                    }
                    case "K" -> {
                        cardsToRemove.add(card);
                        cardsForGraveyard.add(card);
                        playerKings.put(playerTable.getKey(), playerKings.get(playerTable.getKey()) - 1);//remove one king to king tracker map
                    }
                    //0 1  2   3 4     0 1  2   3 4  5   6 7  8   9  10...
                    //J_S_<id>_10_C || J_S_<id>_J_C_<id>_J_H_<id>_10_C
                    case "J" -> {
                        String jackCard;
                        int jackCounter = 0;
                        ArrayList<String> jacksToSendToGraveyard = new ArrayList<>();
                        //collect all jacks to send to graveyard
                        while (cardSplit[jackCounter].equals("J")) { //J_S_<id>_10_S - we are splitting this
                            jackCard = cardSplit[jackCounter] + "_" + cardSplit[jackCounter + 1]; //ctr = J + _ + S
                            jacksToSendToGraveyard.add(jackCard);
                            jackCounter = jackCounter + 3;
                        }
                        cardsForGraveyard.addAll(jacksToSendToGraveyard);

                        //return the point card to og owner
                        int ogOwnerId = Integer.parseInt(cardSplit[cardSplit.length - 3]);//we are taking the last <id> aka first owner
                        String cardToGiveBack = cardSplit[cardSplit.length - 2] + "_" + cardSplit[cardSplit.length - 1];
                        cardsToRemove.add(card);//remove jacked card from table

//                        playerTables.get(ogOwnerId).add(cardToGiveBack);//give back point card to og owner
                        //save which cards to give back to players in the end
                        cardsToGiveBackToOgPlayers.put(ogOwnerId, cardToGiveBack);

                        //exchange points
                        String[] cardToGiveBackSplit = cardToGiveBack.split("_");
                        int pointsToExchange = Integer.parseInt(cardToGiveBackSplit[0]);//get points to exchange
                        playerScore.put(ogOwnerId, playerScore.get(ogOwnerId) + pointsToExchange);
                        playerScore.put(playerTable.getKey(), playerScore.get(playerTable.getKey()) - pointsToExchange);
                    }
                }
            }
            graveyard.addAll(cardsForGraveyard);
            playerTable.getValue().removeAll(cardsToRemove);
        }

        //go through saved cards and put them back on the table
        for (Map.Entry<Integer, String> cardToGiveBack : cardsToGiveBackToOgPlayers.entrySet()) {
            playerTables.get(cardToGiveBack.getKey()).add(cardToGiveBack.getValue());
        }

        //send 6 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());
    }

    private void play7power(GameAction gameAction) {
        //send 7 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        //draw card to play next
        playerHands.get(currentPlayersTurn).add(deck.pop());
        currentPlayersTurn -= 1;
    }

    //when 8 power is played on table will be P_<rank>_<suit> - P_8_C
    private void play8power(GameAction gameAction) {
        //remove 8 from hand
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        playerTables.get(currentPlayersTurn).add("P_" + gameAction.getCardPlayed());
    }

    private void play9power(GameAction gameAction) {
        String[] ontoPlayedCardSplit = gameAction.getOntoCardPlayed().split("_");

        //0 1  2   3 4     0 1  2   3 4  5   6 7  8   9  10...
        //J_S_<id>_10_C || J_S_<id>_J_C_<id>_J_H_<id>_10_C
        switch (ontoPlayedCardSplit[0]) {
            case "J" -> {
                String topJackCard = ontoPlayedCardSplit[0] + "_" + ontoPlayedCardSplit[1];
                String cardToReturn = "";
                int playerToReturnCardTo = Integer.parseInt(ontoPlayedCardSplit[2]);

                //if split[3] == J -> there is another Jack on top, return to that players table based on the next <id>
                //we have to build back the card with the Js still on it
                //else if split[3] == num -> return the point card to the og players table
                if (ontoPlayedCardSplit[3].equals("J")) {
                    for (int i = 3; i < ontoPlayedCardSplit.length; i++) {
                        cardToReturn = cardToReturn + ontoPlayedCardSplit[i];
                        if (i < ontoPlayedCardSplit.length - 1)
                            cardToReturn = cardToReturn + "_";
                    }
                } else {
                    cardToReturn = ontoPlayedCardSplit[3] + "_" + ontoPlayedCardSplit[4];
                }

                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed()); //remove jacked card
                playerHands.get(gameAction.getOntoPlayer()).add(topJackCard);//giving jack back to onto player because top jack is the one whose table it is
                playerTables.get(playerToReturnCardTo).add(cardToReturn);
                //exchange points
                int pointsToExchange = Integer.parseInt(ontoPlayedCardSplit[ontoPlayedCardSplit.length - 2]);
                playerScore.put(playerToReturnCardTo, playerScore.get(playerToReturnCardTo) + pointsToExchange);
                playerScore.put(gameAction.getOntoPlayer(), playerScore.get(gameAction.getOntoPlayer()) - pointsToExchange);
            }
            //Q_S...
            case "Q" -> {
                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());//remove 9ed card from table
                playerHands.get(gameAction.getOntoPlayer()).add(gameAction.getOntoCardPlayed());//return 9ed card to hand
            }
            case "P" -> {
                String card8withoutP = ontoPlayedCardSplit[1] + "_" + ontoPlayedCardSplit[2];
                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());//remove 9ed card from table
                playerHands.get(gameAction.getOntoPlayer()).add(card8withoutP);//return 9ed card to hand
            }
            case "K" -> {
                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());//remove 9ed card from table
                playerHands.get(gameAction.getOntoPlayer()).add(gameAction.getOntoCardPlayed());//return 9ed card to hand
                playerKings.put(gameAction.getOntoPlayer(), playerKings.get(gameAction.getOntoPlayer()) - 1);//remove one king to king tracker map
            }
            //1_S...
            default -> {
            }
        }

        //send 9 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());
    }

    private void playJackPower(GameAction gameAction) {
        ArrayList<String> stealFromPlayerTable = playerTables.get(gameAction.getOntoPlayer());

        //if queen on table, need to remove it first to play jack on point card
        if (stealFromPlayerTable.contains("Q_C") || stealFromPlayerTable.contains("Q_H") || stealFromPlayerTable.contains("Q_D") || stealFromPlayerTable.contains("Q_S")) {
            System.err.println("There is a queen on the table");
        }
        //else steal the cards to me
        else {
            //edit the card, remove Jack from my hand, remove card from enemy table, add Jacked card to my table
            //final result = J_S_<stolen from player id>_10_S
            String[] playedCardSplit = gameAction.getOntoCardPlayed().split("_");
            String cardWithJackOnTop = gameAction.getCardPlayed() + "_" + gameAction.getOntoPlayer() + "_" + gameAction.getOntoCardPlayed();//build a jacked card
            playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());//remove jack from hand
            playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());//remove og card
            playerTables.get(currentPlayersTurn).add(cardWithJackOnTop);//replace with jacked card version
            //exchange points
            int pointsToExchange = Integer.parseInt(playedCardSplit[playedCardSplit.length - 2]);//get points to exchange
            playerScore.put(gameAction.getFromPlayer(), playerScore.get(gameAction.getFromPlayer()) + pointsToExchange);
            playerScore.put(gameAction.getOntoPlayer(), playerScore.get(gameAction.getOntoPlayer()) - pointsToExchange);
        }
    }

    private void playQueenPower(GameAction gameAction) {
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        playerTables.get(currentPlayersTurn).add(gameAction.getCardPlayed());
    }

    private void playKingPower(GameAction gameAction) {
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        playerTables.get(currentPlayersTurn).add(gameAction.getCardPlayed());
        playerKings.put(currentPlayersTurn, playerKings.get(currentPlayersTurn) + 1);//add one king to king tracker map
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


    private void discardRandomCardFromPlayerHand(ArrayList<String> handToDiscardCardFrom) {
        int playerHandSize = handToDiscardCardFrom.size();
        int randomCardIndex = random.nextInt(playerHandSize);
        graveyard.add(handToDiscardCardFrom.get(randomCardIndex));
        handToDiscardCardFrom.remove(randomCardIndex);
    }

    private void discardSelectedCardFromPlayerHand(String cardToDiscard, int playerId) {
        playerHands.get(playerId).remove(cardToDiscard);
        graveyard.add(cardToDiscard);
    }

    private void swapTurnToNextPlayer() {
        currentPlayersTurn += 1;
        if (currentPlayersTurn == numOfPlayers) currentPlayersTurn = 0;
    }

    public void printAll() {
        System.out.println("Current players turn = " + currentPlayersTurn);

        System.out.println("Deck:");
        printDeck();
        System.out.println("Graveyard:");
        printGraveyard();
        System.out.println("Score");
        printPlayScore();
        System.out.println("Kings:");
        printKings();
        System.out.println("Table:");
        printAllTables();
        System.out.println("Hands:");
        printAllHands();
    }

    private void printAllHands() {
        playerHands.forEach((key, value) -> {
            System.out.println("player " + key + " = {" + value + "}");
        });
    }

    private void printAllTables() {
        playerTables.forEach((key, value) -> {
            System.out.println("player " + key + " = {" + value + "}");
        });
    }

    private void printDeck() {
        deck.forEach(value -> {
            System.out.print(value + ", ");
        });
        System.out.print("\n");
    }

    private void printGraveyard() {
        graveyard.forEach(value -> {
            System.out.print(value + ", ");
        });
        System.out.print("\n");
    }

    private void printPlayScore() {
        System.out.println(playerScore.toString());
    }

    private void printKings() {
        playerKings.forEach((key, value) -> {
            System.out.println("player " + key + " = {" + value + "}");
        });
    }


}
