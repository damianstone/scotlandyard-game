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

    @Nonnull
    @Override
    public Model build(GameSetup setup,
                       Player mrX,
                       ImmutableList<Player> detectives) {
        return new Model() { // anon class

            // returning the current game board
            @Nonnull
            @Override
            public Board getCurrentBoard() {
                return State;
            }

            @Override
            public void registerObserver(@Nonnull Observer observer) {
                if (observer == null) {
                    throw new NullPointerException("null observer");
                    // if the observer  is null throw an exception
                }
                if (observerList.contains(observer))
                    throw new IllegalArgumentException("the observer is already registered");
                observerList.add(observer);
                // if the observer is already registered throw another exception

            }

            @Override
            public void unregisterObserver(@Nonnull Observer observer) {
                if (observer == null)
                    throw new NullPointerException("Observer is null!");

                if (!observerList.contains(observer)) throw new IllegalArgumentException("Observer isn't registered!");
                observerList.remove(observer);
                // if the observer is not registered then throw exception
                // else add the observer to our list
            }

            @Nonnull
            @Override
            //all currently registered observers of the model
            public ImmutableSet<Observer> getObservers() {
                return ImmutableSet.copyOf(observerList);
            }

            @Override
            public void chooseMove(@Nonnull Move move) {
                State = State.advance(move);
                Observer.Event event;
                if (State.getWinner().isEmpty()) event = Observer.Event.MOVE_MADE;
                else event = Observer.Event.GAME_OVER;
                for (Observer observe : observerList) observe.onModelChanged(State, event);
            }

            List<Observer> observerList = new ArrayList<>();
            MyGameStateFactory factory = new MyGameStateFactory();
            Board.GameState State = factory.build(setup, mrX, detectives);

        };
    }
}