package uk.ac.bris.cs.scotlandyard.model;

import java.util.ArrayList;
import java.util.List;


import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

    /**
     * @param setup the game setup
     * @param mrX MrX player
     * @param detectives detective players list
     * @return an instance of GameState
     * @throws  NullPointerException if any argument is null
     * @throws IllegalArgumentException if any of the arguments is not valid
     */
    @Nonnull
    @Override
    public Model build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {

        // anon class
        return new Model() {
            List<Model.Observer> observerList = new ArrayList<>();
            MyGameStateFactory factory = new MyGameStateFactory();

            Board.GameState State = factory.build(setup, mrX, detectives);

            /**
             * @return the current game board
             */
            @Nonnull
            @Override
            public Board getCurrentBoard() {
                return State;
            }

            /**
             * Register an observer to the model
             * @param observer
             * @throws  NullPointerException if observer is null
             * @throws IllegalArgumentException if the observer is already registered
             */
            @Override
            public void registerObserver(@Nonnull Observer observer) {
                if (observer == null) {
                    throw new NullPointerException("null observer");
                }
                if (observerList.contains(observer))
                    throw new IllegalArgumentException("the observer is already registered");

                observerList.add(observer);
            }

            /**
             * Register an observer to the model
             * @param observer
             * @throws  NullPointerException if observer is null
             * @throws IllegalArgumentException if the observer is already registered
             */
            @Override
            public void unregisterObserver(@Nonnull Observer observer) {
                if (observer == null)
                    throw new NullPointerException("Observer is null!");

                if (!observerList.contains(observer)) throw new IllegalArgumentException("Observer isn't registered!");
                observerList.remove(observer);
            }

            /**
             * @return the registered observers of the model
             */
            @Nonnull
            @Override
            public ImmutableSet<Observer> getObservers() {
                return ImmutableSet.copyOf(observerList);
            }

            /**
             * This method is called when a move has been chosen by the GUI
             * @param move
             */
            @Override
            public void chooseMove(@Nonnull Move move) {
                State = State.advance(move);
                Observer.Event event;
                if (State.getWinner().isEmpty()) event = Observer.Event.MOVE_MADE;
                else event = Observer.Event.GAME_OVER;
                for (Observer observe : observerList) observe.onModelChanged(State, event);
            }

        };
    }
}