package rs.raf.app.model;

import lombok.Getter;
import lombok.Setter;
import rs.raf.app.model.actions.*;

import java.util.*;

@Getter
@Setter
public class Room extends Thread{

    //todo postavi room creation time kako bi se pratila koja je aktiva a koja je smece
    //todo smisli nacin da se sklone igraci ako kliknu back ili ako nisu aktivni (mozda na frontu)

    //room vars
    private GameResponse gameResponse;
    private boolean gameIsRunning = false;
    private String idKey;
    private String roomOwner;
    private int numOfPlayers;
    private ArrayList<String> players = new ArrayList<>(); // prvi koji udje je owner
    private int currentPlayersTurn = 0;
    private int playerWhoWon = -1;

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
    private boolean turnOver;
    Random random = new Random();
//    int playerWithActive7Card;
//    String cardDrawnWith7Card;


    //todo stavi da bude random ko pocinje prvi
    //todo stavi da onaj koji prvi igra dobije jednu vise kartu
    public void startGame() {
        for (int i = 0; i < players.size(); i++) {
            playerScore.put(i, 0);
            playerKings.put(i, 0);
        }
        dealCards();
        randomizeStartingPlayer();
        numOfPlayers = players.size();
        gameIsRunning = true;
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
        //todo dodaj da se nesto vrati, ili da se sacuva score na igracu ili nesto
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
    }

    //first player has 1 extra card
    private void randomizeStartingPlayer(){
        int firstPlayer = random.nextInt(players.size()) - 1;
        currentPlayersTurn = firstPlayer;
        playerHands.get(firstPlayer).add(deck.pop());
    }


    public GameResponse playTurn(GameAction gameAction) {

        //todo wait za uskok 2 ce se uraditi na frontu

        switch (gameAction.getActionType()) {
            case NUMBER -> turnOver = playNumberCard(gameAction);
            case SCUTTLE -> turnOver = playScuttleCard(gameAction);
            case DISCARD_CARD -> turnOver = discardCardFromCurrentPlayersHand(gameAction.getCardPlayed()); //(for cards drawn by 7) we place a card we want to discard into played for convenience
            case COUNTER -> turnOver = counterCardPlayed(gameAction);
            case POWER -> {
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

        //todo mozda skloni ovaj boolean jer je malo usless
        if (!turnOver) System.err.println("KURAC SE DESIO, NADJI PROBLEM");

        gameResponse = new GameResponse(
                GameResponseType.REGULAR_GO_NEXT,
                currentPlayersTurn,
                graveyard,
                playerHands,
                playerTables,
                playerScore,
                -1
        );

        playerWhoWon = checkIfSomebodyWon();
        if (playerWhoWon != -1){
            gameResponse.setGameResponseType(GameResponseType.GAME_OVER_WON);
            gameResponse.setPlayerWhoWon(playerWhoWon);
        }
        swapTurnToNextPlayer();

        return gameResponse;
    }

    public GameResponse drawCard() {
        playerHands.get(currentPlayersTurn).add(deck.pop());

        gameResponse = new GameResponse(
                GameResponseType.REGULAR_GO_NEXT,
                currentPlayersTurn,
                graveyard,
                playerHands,
                playerTables,
                playerScore,
                -1
        );
        swapTurnToNextPlayer();
        return gameResponse;
    }

    /*
    used2s - all 2 cards that have been played and who played them <rank>_<suit>_<playerId> 2_S_3
    card played onto - card that has been countered
    onto player - who has been countered

    - also if a 2 is countered by another 2 it will be handled front side aka if 2Played var is filled it will just empty it (action passes), therefor countering a 2 with 2
     */
    private boolean counterCardPlayed(GameAction gameAction){
        String []split2Card;
        String whole2Card;
        //send the countered card to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getOntoCardPlayed());
        graveyard.add(gameAction.getOntoCardPlayed());

        //send all 2s used to graveyard
        for (String twoCard: gameAction.getUsed2s()) {
            split2Card = twoCard.split("_");
            whole2Card = split2Card[0] + "_" + split2Card[1];

            playerHands.get(Integer.parseInt(split2Card[2])).remove(whole2Card);//remove 2 from players hand
            graveyard.add(whole2Card);
        }

        return true;
    }

    private int checkIfSomebodyWon(){
        for (Map.Entry<Integer, Integer> entry : playerScore.entrySet()) {
            int playerId = entry.getKey();
            int numberOfKings = playerKings.get(playerId);
            int score = entry.getValue();

            switch (numberOfKings){
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

    private boolean playNumberCard(GameAction gameAction) {
        //we take the number of the card
        int playedCardPower = Integer.parseInt(gameAction.getCardPlayed().split("_")[0]);
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        playerTables.get(currentPlayersTurn).add(gameAction.getCardPlayed());
        playerScore.put(currentPlayersTurn, playerScore.get(currentPlayersTurn + playedCardPower));
        return true;
    }

    /*
    rank = number from card (9_C)
                            0  1    0 1  2   3  4    0 1  2   3 4  5   6  7
    cards you can scuttle = 10_S || J_S_<id>_10_S || J_S_<id>_J_H_<id>_10_S
    */
    private boolean playScuttleCard(GameAction gameAction) {
        String[] playedCardSplit = gameAction.getCardPlayed().split("_");
        String[] ontoPlayedCardSplit = gameAction.getOntoCardPlayed().split("_");

        int playedCardRank = Integer.parseInt(playedCardSplit[0]);
        int playedOntoCardRank = Integer.parseInt(ontoPlayedCardSplit[0]);
        String cardToScuttle = gameAction.getOntoCardPlayed();

        ArrayList<String> jacksToSendToGraveyard = new ArrayList<>();
        boolean cardWasJacked = false;

        //if Jack is on top of the card we are scuttling,
        //find all jacks on top and send to graveyard
        //then send regular point card to graveyard
        if (ontoPlayedCardSplit[0].equals("J")) {
            int jackCounter = 0;
            cardWasJacked = true;
            playedOntoCardRank = Integer.parseInt(playedCardSplit[playedCardSplit.length - 2]); //skipping jacks
            cardToScuttle = ontoPlayedCardSplit[ontoPlayedCardSplit.length - 2] + "_" + ontoPlayedCardSplit[ontoPlayedCardSplit.length - 1];//onto card it self

            String card;
            //collect all jacks to send to graveyard
            while (ontoPlayedCardSplit[jackCounter].equals("J")) { //J_S_<id>_10_S - we are splitting this
                card = ontoPlayedCardSplit[jackCounter] + "_" + ontoPlayedCardSplit[jackCounter + 1]; //ctr = J + _ + S
                jacksToSendToGraveyard.add(card);
                jackCounter = jackCounter + 3;
            }
        }

        //if card played is bigger scuttle
        if (playedCardRank > playedOntoCardRank) {
            playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());//remove card we are scuttling with
            playerTables.get(gameAction.getOntoPlayer()).remove(cardToScuttle);//remove scuttled card
            graveyard.add(gameAction.getCardPlayed());//add card we are scuttling with to graveyard
            graveyard.add(cardToScuttle);//add scuttled to graveyard
            if (cardWasJacked) graveyard.addAll(jacksToSendToGraveyard);//if was jacked send jacks to graveyard
            return true;
        }
        //if card numbs are same but played suit is bigger scuttle
        else if (playedCardRank == playedOntoCardRank) {
            if (cardSuitComparator(playedCardSplit[1], ontoPlayedCardSplit[1]) == CardComparison.BIGGER) {//check which suit is bigger
                playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
                playerTables.get(gameAction.getOntoPlayer()).remove(cardToScuttle);
                graveyard.add(gameAction.getCardPlayed());
                graveyard.add(cardToScuttle);
                if (cardWasJacked) graveyard.addAll(jacksToSendToGraveyard);
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
                if (cardSplit[0].equals("1") || cardSplit[0].equals("2") || cardSplit[0].equals("3") || cardSplit[0].equals("4") || cardSplit[0].equals("5")
                        || cardSplit[0].equals("6") || cardSplit[0].equals("7") || cardSplit[0].equals("8") || cardSplit[0].equals("9") || cardSplit[0].equals("10")) {
                    cardsOnTableList.remove(card);
                    graveyard.add(card);
                }
            }
        });

        //send 1 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        return true;
    }

    //todo code je isti kao power9 samo bez add (ako je bug kod jedne i kod druge je)

    private boolean play2power(GameAction gameAction) {
        String[] ontoPlayedCardSplit = gameAction.getOntoCardPlayed().split("_");

        //0 1  2   3 4     0 1  2   3 4  5   6 7  8   9  10...
        //J_S_<id>_10_C || J_S_<id>_J_C_<id>_J_H_<id>_10_C
        if (ontoPlayedCardSplit[0].equals("J")) {
            String topJackCard = ontoPlayedCardSplit[0] + "_" + ontoPlayedCardSplit[1];
            String cardToReturn = "empty_card";
            int playerToReturnCardTo;

            //if split[3] == J -> there is another Jack on top, return to that players table based on the next <id>
            //else if split[3] == num -> return the point card to the og players table
            if (ontoPlayedCardSplit[3].equals("J")) {//we have to build back the card with the Js still on it
                for (int i = 3; i < ontoPlayedCardSplit.length; i++) {
                    cardToReturn = ontoPlayedCardSplit[i];
                    if (i < ontoPlayedCardSplit.length - 1)
                        cardToReturn = cardToReturn + "_";
                }
                playerToReturnCardTo = Integer.parseInt(ontoPlayedCardSplit[5]);
            } else {
                cardToReturn = ontoPlayedCardSplit[3] + "_" + ontoPlayedCardSplit[4];
                playerToReturnCardTo = Integer.parseInt(ontoPlayedCardSplit[2]);
            }

            playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed()); //remove jacked card
            graveyard.add(topJackCard);//send jack to graveyard
            playerTables.get(playerToReturnCardTo).add(cardToReturn);
        }
        //Q_S...
        else if (ontoPlayedCardSplit[0].equals("Q") || ontoPlayedCardSplit[0].equals("P")) {
            playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());//remove 9ed card from table
            graveyard.add(gameAction.getOntoCardPlayed());//send 2ed card to graveyard
        }
        else if (ontoPlayedCardSplit[0].equals("K")) {
            playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());//remove 2ed card from table
            graveyard.add(gameAction.getOntoCardPlayed());//send 2ed card to graveyard
            playerKings.put(currentPlayersTurn, playerKings.get(currentPlayersTurn - 1));//remove one king to king tracker map
        }
        //1_S...
        else return false;

        //send 9 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        return true;
    }

    //here the ontoPlayedCard is referred to the graveyard card and not a card on the table
    private boolean play3power(GameAction gameAction) {
        //return card from graveyard to us
        graveyard.remove(gameAction.getOntoCardPlayed());
        playerHands.get(currentPlayersTurn).add(gameAction.getOntoCardPlayed());

        //send 3 to graveyard
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
            //todo ako ne radi discard function samo vrati manuelno (ovo ispod)
            discardCardFromCurrentPlayersHand(playerHands.get(gameAction.getOntoPlayer()).get(0));
//            graveyard.add(playerHands.get(gameAction.getOntoPlayer()).get(0));
//            playerHands.get(gameAction.getOntoPlayer()).remove(0);
        }
        //remove 2 at random and add to graveyard
        else {
            playerHandSize = playerHands.get(gameAction.getOntoPlayer()).size();
            randomCardIndex = random.nextInt(playerHandSize);
            discardCardFromCurrentPlayersHand(playerHands.get(gameAction.getOntoPlayer()).get(randomCardIndex));
//            graveyard.add(playerHands.get(gameAction.getOntoPlayer()).get(randomCardIndex));
//            playerHands.get(gameAction.getOntoPlayer()).remove(randomCardIndex);

            playerHandSize = playerHands.get(gameAction.getOntoPlayer()).size();
            randomCardIndex = random.nextInt(playerHandSize);
            discardCardFromCurrentPlayersHand(playerHands.get(gameAction.getOntoPlayer()).get(randomCardIndex));
//            graveyard.add(playerHands.get(gameAction.getOntoPlayer()).get(randomCardIndex));
//            playerHands.get(gameAction.getOntoPlayer()).remove(randomCardIndex);
        }

        //send 4 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        return true;
    }

    private boolean play5power(GameAction gameAction) {
        playerHands.get(currentPlayersTurn).add(deck.pop());
        playerHands.get(currentPlayersTurn).add(deck.pop());

        //send 5 to graveyard
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
                if (cardSplit[0].equals("Q") || cardSplit[0].equals("P")) {
                    cardsOnTableList.remove(card);
                    graveyard.add(card);
                }
                else if (cardSplit[0].equals("K")) {
                    cardsOnTableList.remove(card);
                    graveyard.add(card);
                    playerKings.put(currentPlayersTurn, playerKings.get(currentPlayersTurn - 1));//remove one king to king tracker map
                }
                else if (cardSplit[0].equals("J")) {
                    //todo vrati jacked kartu nazad svom og vlasniku
                }
            }
        });

        //send 6 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        return true;
    }

    //todo postavi discard funkciju koja ce da stavi kartu na groblje
    //todo stavi skip turn dugme da ne morada se ceka 60 sec ako ne moze da se odigra karta
    private boolean play7power(GameAction gameAction) {
        //send 7 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        //todo provera dal je ova karta bacena ce biti na FRONTU, ako se baci isprazni, ako se ne baci discarduj
        //draw card to play next
        playerHands.get(currentPlayersTurn).add(deck.pop());
        currentPlayersTurn-= 1;//todo proveri ovo

//        cardDrawnWith7Card = deck.pop(); ovo je vrv samo govno
//        playerWithActive7Card = currentPlayersTurn;
//        playerHands.get(currentPlayersTurn).add(cardDrawnWith7Card);

        return true;
    }

    //when 8 power is played on table will be P_<rank>_<suit> - P_8_C
    private boolean play8power(GameAction gameAction) {
        //remove 8 from hand
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        playerTables.get(currentPlayersTurn).add("P_" + gameAction.getCardPlayed());

        //todo stavi na front boolean "8 is in play" sto ce svima da pokaze ruke

        return true;
    }

    private boolean play9power(GameAction gameAction) {
        String[] ontoPlayedCardSplit = gameAction.getOntoCardPlayed().split("_");

        //0 1  2   3 4     0 1  2   3 4  5   6 7  8   9  10...
        //J_S_<id>_10_C || J_S_<id>_J_C_<id>_J_H_<id>_10_C
        switch (ontoPlayedCardSplit[0]) {
            case "J" -> {
                String topJackCard = ontoPlayedCardSplit[0] + "_" + ontoPlayedCardSplit[1];
                int playerToReturnJackTo = Integer.parseInt(ontoPlayedCardSplit[2]);
                String cardToReturn = "empty_card";
                int playerToReturnCardTo;

                //if split[3] == J -> there is another Jack on top, return to that players table based on the next <id>
                //else if split[3] == num -> return the point card to the og players table
                if (ontoPlayedCardSplit[3].equals("J")) {//we are building back the card just without the top Jack (start from 3 split)
                    for (int i = 3; i < ontoPlayedCardSplit.length; i++) {
                        cardToReturn = ontoPlayedCardSplit[i];
                        if (i < ontoPlayedCardSplit.length - 1)
                            cardToReturn = cardToReturn + "_";
                    }
                    playerToReturnCardTo = Integer.parseInt(ontoPlayedCardSplit[5]);
                }
                else {//no top jacks, just build card regularly
                    cardToReturn = ontoPlayedCardSplit[3] + "_" + ontoPlayedCardSplit[4];
                    playerToReturnCardTo = Integer.parseInt(ontoPlayedCardSplit[2]);
                }
                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed()); //remove jacked card


//            graveyard.add(topJackCard);//send jack to graveyard
                playerHands.get(playerToReturnJackTo).add(topJackCard);
                playerTables.get(playerToReturnCardTo).add(cardToReturn);
            }
            //Q_S...
            case "Q", "P" -> {
                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());//remove 9ed card from table
                playerHands.get(gameAction.getOntoPlayer()).add(gameAction.getOntoCardPlayed());//return 9ed card to hand
            }
            case "K" -> {
                playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());//remove 9ed card from table
                playerHands.get(gameAction.getOntoPlayer()).add(gameAction.getOntoCardPlayed());//return 9ed card to hand
                playerKings.put(currentPlayersTurn, playerKings.get(currentPlayersTurn - 1));//remove one king to king tracker map
            }
            //1_S...
            default -> {
                return false;
            }
        }

        //send 9 to graveyard
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        graveyard.add(gameAction.getCardPlayed());

        return true;
    }

    private boolean playJackPower(GameAction gameAction) {
        ArrayList<String> stealFromPlayerTable = playerTables.get(gameAction.getFromPlayer());

        //if queen on table, need to remove it first to play jack on point card
        if (stealFromPlayerTable.contains("Q_C") || stealFromPlayerTable.contains("Q_H") || stealFromPlayerTable.contains("Q_D") || stealFromPlayerTable.contains("Q_S")) {
            System.err.println("There is a queen on the table");
            return false;
        }
        //else steal the cards to me
        else {
            //edit the card, remove Jack from my hand, remove card from enemy table, add Jacked card to my table
            //final result = J_S_<stolen from player id>_10_S
            String cardWithJackOnTop = gameAction.getCardPlayed() + "_" + gameAction.getOntoPlayer() + "_" + gameAction.getOntoCardPlayed();
            playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
            playerTables.get(gameAction.getOntoPlayer()).remove(gameAction.getOntoCardPlayed());
            playerTables.get(currentPlayersTurn).add(cardWithJackOnTop);
        }
        return true;
    }

    private boolean playQueenPower(GameAction gameAction) {
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        playerTables.get(currentPlayersTurn).add(gameAction.getCardPlayed());
        return true;
    }

    private boolean playKingPower(GameAction gameAction) {
        playerHands.get(currentPlayersTurn).remove(gameAction.getCardPlayed());
        playerTables.get(currentPlayersTurn).add(gameAction.getCardPlayed());
        playerKings.put(currentPlayersTurn, playerKings.get(currentPlayersTurn + 1));//add one king to king tracker map
        return true;
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


    private boolean discardCardFromCurrentPlayersHand(String cardToDiscard){
        playerHands.get(currentPlayersTurn).remove(cardToDiscard);
        graveyard.add(cardToDiscard);
        return true;
    }

    private void swapTurnToNextPlayer(){
        currentPlayersTurn += 1;
        if (currentPlayersTurn == numOfPlayers) currentPlayersTurn = 0;
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
        deck.forEach(System.out::println);
    }

    private void printGraveyard() {
        graveyard.forEach(System.out::println);
    }


}
