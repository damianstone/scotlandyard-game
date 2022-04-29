package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

import java.util.*;

/**
 * cw-modelto-do: clean code
 * Stage 1: Complete this class
 */


public final class MyGameStateFactory implements Factory<GameState> {

    /**
     * Creates an instance of GameState given the parameters required for a ScotlandYard game
     *
     * @param setup the game setup
     * @param mrX MrX player
     * @param detectives detective players
     * @return an instance of GameState
     */
    @Nonnull
    @Override
    public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
        return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
    }

    private final class MyGameState implements GameState {

        // The game setup
        private GameSetup setup;

        // The players that still has moves
        private ImmutableSet<Piece> remaining;

        // The log entries of MrX travel log
        private ImmutableList<LogEntry> log;

        // MrX player
        private Player mrX;

        // List containing all the detectives
        private List<Player> detectives;

        // Set containing all the moves (singles and doubles)
        private ImmutableSet<Move> moves;

        // All the player involved in the game
        private ImmutableList<Player> everyone;

        // The winner of the game
        private ImmutableSet<Piece> winner;


        /**
         * MyGameState constructor
         *
         * @param setup the game setup
         * @param remaining the pieces that can still move in the current round
         * @param log MrX's travel log
         * @param mrX MrX player
         * @param detectives detective players
         * @throws IllegalArgumentException if any of the arguments is not valid
         */
        private MyGameState(
                final GameSetup setup,
                final ImmutableSet<Piece> remaining, // all the pieces that haven't moved yet
                final ImmutableList<LogEntry> log,
                final Player mrX,
                final List<Player> detectives) {


            // CHECKS
            if (mrX.piece().webColour() != "#000") throw new IllegalArgumentException("MrX not a black piece");
            if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
            if (!mrX.isMrX()) throw new IllegalArgumentException("there is no mrX!");
            if (detectives.isEmpty()) throw new IllegalArgumentException("Detectives is empty!");
            if (setup.graph.edges().isEmpty()) throw new IllegalArgumentException("Graph is empty!");

            // check properties of each detective
            for (Player d : detectives) {
                if (!d.isDetective()) throw new IllegalArgumentException("No detective!");
                if (d.has(ScotlandYard.Ticket.DOUBLE))
                    throw new IllegalArgumentException("detectives cant have double");
                if (d.has(ScotlandYard.Ticket.SECRET))
                    throw new IllegalArgumentException("detectives shouldn't have secret ticket");
            }
            // Check duplicated properties of detectives
            // O(n^2) notation can be improved using a hash table to get O(n)
            for (int i = 0; i < detectives.size(); i++) {
                for (int j = i + 1; j < detectives.size(); j++) {
                    // if the next detective doest exist so break
                    if (j > detectives.size()) break;
                    if (detectives.get(i).location() == detectives.get(j).location()) {
                        throw new IllegalArgumentException("Same location!");
                    }
                    if (detectives.get(i).piece() == detectives.get(j).piece()) {
                        throw new IllegalArgumentException("Duplicated game pieces!");
                    }
                }
            }


            // create a list with all the players (detectives + mrx)
            List<Player> everyone = new ArrayList<>();
            everyone.add(mrX);
            everyone.addAll(detectives);

            this.setup = setup;
            this.remaining = remaining;
            this.log = log;
            this.mrX = mrX;
            this.detectives = detectives;
            this.everyone = ImmutableList.copyOf(everyone);
            this.winner = getWinner();
            this.moves = getAvailableMoves();


        }

        //-------------------- Getters --------------------//

        /**
         * @return the current game setup
         */
        @Nonnull
        @Override
        public GameSetup getSetup() {
            return setup;
        }

        /**
         * @return all the player pieces
         */
        @Nonnull
        @Override
        public ImmutableSet<Piece> getPlayers() {
            List<Piece> allPieces = new ArrayList<>();
            allPieces.add(mrX.piece());
            for (Player i : detectives) {
                allPieces.add(i.piece());
            }
            return ImmutableSet.copyOf(allPieces);
        }

        /**
         * @param  detective piece
         * @return the location of a given detective, and empty if the detective is part of te game
         */
        @Nonnull
        @Override
        public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
            for (Player p : detectives) {
                if (p.piece().equals(detective)) return Optional.of(p.location());
            }
            return Optional.empty();
        }

        /**
         * @param  piece
         * @return the ticket board of a given player; empty if the player is not part of the game
         */
        @Nonnull
        @Override
        public Optional<TicketBoard> getPlayerTickets(Piece piece) {
            ImmutableSet<Piece> players = getPlayers();
            // check if there is the piece
            if (!players.contains(piece)) return Optional.empty();

            if (piece.isMrX()) {
                // check if it has tickets has()
                return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));
            }
            if (piece.isDetective()) {
                for (Player player : detectives) {
                    if (player.piece() == piece) {
                        return Optional.of(ticket -> player.tickets().getOrDefault(ticket, 0));
                    }
                }
            }
            return Optional.empty();
        }

        /**
         * @return the mrx travel log
         */
        @Nonnull
        @Override
        public ImmutableList<LogEntry> getMrXTravelLog() {
            return log;
        }


        /**
         * @return winner player
         */
        @Nonnull
        @Override
        public ImmutableSet<Piece> getWinner() {
            List<Piece> detectivePiecesList = new ArrayList<>();
            List<Integer> detectivesLocation = new ArrayList<>();

            for (Player d : detectives) {
                detectivePiecesList.add(d.piece());
                detectivesLocation.add(d.location());
            }

            for (Player d : detectives) {
                // Mrx lost
                if (d.location() == this.mrX.location()) {
                    return ImmutableSet.copyOf(detectivePiecesList);
                }
            }

            // if its mrX turn to move and there are no available moves then game over, and he loses
            if ((makeSingleMoves(setup, detectives, mrX, mrX.location()).isEmpty()
                    && makeDoubleMoves(setup, detectives, mrX, mrX.location(), log).isEmpty()
                    && remaining.contains(mrX.piece()))) {
                return ImmutableSet.copyOf(detectivePiecesList);
            }

            // No detective has more moves so mrx win
            if (noDetectivesHasMoves(detectives)) return ImmutableSet.of(mrX.piece());


            // mrx manage to fill the log and no detectives could catch him, so mrx win
            if (setup.moves.size() == getMrXTravelLog().size()
                    && remaining.contains(mrX.piece())) {
                return ImmutableSet.of(mrX.piece());
            }

            return ImmutableSet.of();
        }

        /**
         * @return a set of single and doubles moves from all the players
         */
        @Nonnull
        @Override
        public ImmutableSet<Move> getAvailableMoves() {

            // if there is a winner
            if (!getWinner().isEmpty()) {
                return ImmutableSet.of();
            }

            HashSet<SingleMove> singleMoves = new HashSet<>();
            HashSet<DoubleMove> doubleMoves = new HashSet<>();
            HashSet<Move> moves = new HashSet<>();

            for (Player player : everyone) {
                if (remaining.contains(player.piece())) {
                    singleMoves.addAll(makeSingleMoves(setup, detectives, player, player.location()));
                    doubleMoves.addAll(makeDoubleMoves(setup, detectives, player, player.location(), log));
                }
            }

            moves.addAll(singleMoves);
            moves.addAll(doubleMoves);
            return ImmutableSet.copyOf(moves);
        }

        /**
         * Compute the next game state given a move from {@link #getAvailableMoves()}
         *
         * @param move
         * @return the game state of which the given move has been made
         * @throws IllegalArgumentException if the move was not a move from {@link #getAvailableMoves()}
         */
        @Nonnull
        @Override
        public GameState advance(Move move) {
            if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

            List<LogEntry> newLog = new ArrayList<>(log);
            List<Player> newDetectives = new ArrayList<>();
            List<Piece> oldRemaining = new ArrayList<>(remaining);
            List<Piece> newRemaining = new ArrayList<>();
            Player newMrx;

            //anon -> we can access to the list of all pieces (data from the constructor)
            // Advance method use the visitor pattern to move the player through the table
            Visitor<Player> v = new Visitor<>() {

                @Override
                public Player visit(SingleMove move) {

                    // to know if the player is mrX or a detective
                    Player player = playerFromPiece(move.commencedBy());

                    // taking the tickets and move the player to the destination
                    Player newPlayer = player.use(move.ticket).at(move.destination);

                    if (!player.isMrX()) {
                        // updating mrX with new tickets
                        mrX = mrX.give(move.ticket); // give the ticket to mrx from detectives
                    } else {
                        // check if in this round mrx has to reveal his moves
                        if (setup.moves.get(log.size())) {
                            newLog.add(LogEntry.reveal(move.ticket, move.destination));
                        } else {
                            newLog.add(LogEntry.hidden(move.ticket));
                        }
                    }
                    return newPlayer;
                }

                @Override
                public Player visit(DoubleMove move) {
                    Player player = playerFromPiece(move.commencedBy());

                    // take the tickets and move to the new location
                    // use move.tickets() to get rid of the DOUBLE tickets and go to the destination2
                    // taxi, bus, and double tickets should decrement by 1 for example
                    Player newPlayer = player.use(move.tickets());
                    newPlayer = newPlayer.at(move.destination2);


                        // if moves[rounds] == true, so mrx should reveal his moves
                        if (setup.moves.get(log.size())) {
                            newLog.add(LogEntry.reveal(move.ticket1, move.destination1));
                        } else {
                            newLog.add(LogEntry.hidden(move.ticket1));
                        }

                    /*
                    Checks once again if we are in a reveal round because we need
                    to add two separate entries, one for each move within the double move
                    */
                        if (setup.moves.get(newLog.size())) {
                            newLog.add(LogEntry.reveal(move.ticket2, move.destination2));
                        } else {
                            newLog.add(LogEntry.hidden(move.ticket2));
                        }

                    return newPlayer;
                }
            };

            // mrx exam
            Player newPlayer = move.accept(v);


            // only update newDetectives if newPlayer is a detective
            for (Player p : detectives) {
                if (p.piece() == newPlayer.piece()) {
                    newDetectives.add(newPlayer);
                } else {
                    newDetectives.add(p);
                }
            }

            // exclude the piece moved for remaining
            for (Piece p : remaining) {
                if (p != newPlayer.piece()) {
                    newRemaining.add(p);
                }
            }


            // check if the newPlayer is mrx
            if (newPlayer.isMrX()) {
                newMrx = newPlayer;
            } else {
                newMrx = mrX;
            }


            // beginning of each round, only mrX is in remaining set
            if (move.commencedBy().isMrX()) {
                for (Player p : detectives) {
                    newRemaining.add(p.piece());
                }
            } else {
                for (Piece p : remaining) {
                    if (p != move.commencedBy()) newRemaining.add(p);
                }
            }

            for(Piece p : remaining) {
                Player player = playerFromPiece(p);
                if(p.isMrX()) {
                    /* The game is not over if MrX is cornered, but he can still escape
                       can scape using a double move, or secret */
                    if(makeSingleMoves(setup, newDetectives, player, player.location()).isEmpty()
                            && !makeDoubleMoves(setup, newDetectives, newMrx, newMrx.location(), ImmutableList.copyOf(newLog)).isEmpty() ) {
                        newRemaining.add(newMrx.piece());
                    }
                } else {
                    // check if a detective still has moves
                    if(makeSingleMoves(setup, newDetectives, player, player.location()).isEmpty()) {
                        newRemaining.add(newMrx.piece());
                    }
                }
            }

            if (newRemaining.isEmpty()) newRemaining.add(newMrx.piece());

            return new MyGameState(
                    setup,
                    ImmutableSet.copyOf(newRemaining),
                    ImmutableList.copyOf(newLog),
                    newMrx,
                    newDetectives
            );
        }


        //-------------------- Auxiliary Functions --------------------//

        /**
         * Function to check if the detective have moves left or no
         *
         * @param detectives
         * @return boolean
         */
        public boolean noDetectivesHasMoves(List<Player> detectives) {
            for (Player p : detectives) {
                if (!makeSingleMoves(setup, detectives, p, p.location()).isEmpty())
                    return false; // IF THERE IS AVAILABLE MOVES FOR DETECTIVE
            }
            return true;
        }


        /**
         * Function to get corresponding player from its piece
         *
         * @param piece
         * @return a player or null if the piece is not belong to the game
         */
        private Player playerFromPiece(Piece piece) {
            for (Player player : everyone) {
                if (player.piece().equals(piece)) return player;
            }
            return null;
        }


    }


    /**
     * @param setup the game setup
     * @param detectives the detective players
     * @param player the player
     * @param source the source of the player
     * @return all the available single moves for the player
     */
    private static Set<SingleMove> makeSingleMoves(
            GameSetup setup,
            List<Player> detectives,
            Player player,
            int source) {

        HashSet<SingleMove> singleMoves = new HashSet<>();
        Set<Integer> locations = new HashSet<>();

        // store only locations from detectives
        for (Player d : detectives) locations.add(d.location());

        // map every node
        for (int destination : setup.graph.adjacentNodes(source)) {

            // Don't add in the collection of moves if the node is occupied by another player
            if (locations.contains(destination)) continue;


            // transport represent the transportation type (bus, taxi, etc)
            // map the transportations needed for every node
            for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {

                // find out if the player has the required tickets to transport to the node
                if (player.has(t.requiredTicket())) {
                    SingleMove m = new SingleMove(
                            player.piece(),
                            source,
                            t.requiredTicket(),
                            destination);
                    singleMoves.add(m);
                }
            }

            // mrX can still make a single moves having a secret ticket
            if (player.has(Ticket.SECRET)) {
                SingleMove m = new SingleMove(
                        player.piece(),
                        source,
                        Ticket.SECRET,
                        destination);
                singleMoves.add(m);
            }

        }
        return singleMoves;
    }

    /**
     * @param setup the game setup
     * @param detectives the detective players
     * @param player the player
     * @param source the source of the player
     * @param log MrX's travel log
     * @return all the available double moves for the player
     */
    private static Set<DoubleMove> makeDoubleMoves(
            GameSetup setup,
            List<Player> detectives,
            Player player,
            int source,
            ImmutableList<LogEntry> log) {

        HashSet<DoubleMove> doubleMoves = new HashSet<>();
        Set<Integer> locations = new HashSet<>();

        // store only locations from detectives
        for (Player d : detectives) locations.add(d.location());

        // 4 nested loops to check destination1, destination2 and transport1, transport2
        if ((player.has(Ticket.DOUBLE)) && (setup.moves.size() - log.size() >= 2)) {
            for (int destination1 : setup.graph.adjacentNodes(source)) {
                if (locations.contains(destination1)) continue;
                for (Transport t1 : setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of())) {
                    if (player.has(t1.requiredTicket())) {
                        for (int destination2 : setup.graph.adjacentNodes(destination1)) {
                            if (locations.contains(destination2)) continue;
                            for (Transport t2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())) {

                                // check if the required tickets for first and second move are the same
                                if (t2.requiredTicket() == t1.requiredTicket()) {
                                    // player needs at least 2 tickets
                                    if (player.hasAtLeast(t2.requiredTicket(), 2)) {
                                        DoubleMove doubleMove = new DoubleMove(
                                                player.piece(),
                                                source,
                                                t2.requiredTicket(),
                                                destination1,
                                                t2.requiredTicket(),
                                                destination2);
                                        doubleMoves.add(doubleMove);
                                    }
                                } else if (player.has(t2.requiredTicket())) {
                                    DoubleMove doubleMove = new DoubleMove(
                                            player.piece(),
                                            source,
                                            t1.requiredTicket(),
                                            destination1,
                                            t2.requiredTicket(),
                                            destination2);
                                    doubleMoves.add(doubleMove);
                                }
                            }

                            /* in double moves the player (mrx) can go to the first destination using a secret ticket
                            or go to the second destination using the secret ticket, and finally, he can use 2 secrets
                            to go to the first and second destination */

                            // if mrX has t1.tickets + secret ticket
                            if (player.has(Ticket.SECRET)) {
                                DoubleMove doubleMove = new DoubleMove(
                                        player.piece(),
                                        source,
                                        t1.requiredTicket(),
                                        destination1,
                                        Ticket.SECRET,
                                        destination2);
                                doubleMoves.add(doubleMove);
                            }
                        }
                    }
                }
                if (player.has(Ticket.SECRET)) {
                    for (int destination2 : setup.graph.adjacentNodes(destination1)) {
                        if (locations.contains(destination2)) continue;
                        for (Transport t2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())) {

                            // if mrX has secret ticket + t2.tickets
                            if (player.has(t2.requiredTicket())) {
                                DoubleMove doubleMove = new DoubleMove(
                                        player.piece(),
                                        source,
                                        Ticket.SECRET,
                                        destination1,
                                        t2.requiredTicket(),
                                        destination2);
                                doubleMoves.add(doubleMove);
                            }
                        }

                        // if mrX has secret + secret
                        if (player.hasAtLeast(Ticket.SECRET, 2)) {
                            DoubleMove doubleMove = new DoubleMove(
                                    player.piece(),
                                    source,
                                    Ticket.SECRET,
                                    destination1,
                                    Ticket.SECRET,
                                    destination2);
                            doubleMoves.add(doubleMove);
                        }
                    }
                }
            }
        }
        return doubleMoves;
    }

}

