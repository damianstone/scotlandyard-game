## scotlandyard-game

#Overview

The game of Scotland Yard is a classic board game in which players take on the role of detectives, trying to solve a crime by catching the Mrx who is another player. The game is played on a map of London, with each player having their own detective piece. The game is turn-based, with each player taking a turn to move their detective around the map. 

This game was implemented using Java 17 and Maven. The game's features were implemented using Graph Algorithms, the Visitor pattern, the Observer pattern, and many features of Java such as Generics, anonymous classes, etc.

#Implementation

The implementation of this program was divided into 5 important phases: (1) develop the constructor of MyGameState class and develop the necessary getters, (2) figure out how players could move through the map and nodes (3) develop the advance method to update the state, (4) implement the getWinner method (5) apply the observer pattern.

#MyGameStateFactory Class

This class is a factory that implements the Factory<GameSatate> interface. This means that it has a factory method of some sort (build in this case) which returns a new instance of GameState. The GameState extends board and thus, will have to implement 8 methods; 7 inherited from board plus the advance method that requires. After that, we had to think about the state data MyGameState needs to hold and define some first attributes. This part was done easily with the help of the getters. Next, was writing a constructor for MyGameState. this constructor will be called by the build method of the outer class MyGameSateFactory, and this gives us a hint that it should make use of at least this information: 1. The game setup    2.the player mrX    3.the ImmutableList<player> detectives. In addition, the remaining players were provided, and finally the current log.

#Get Available Moves

For the base implementation of the moves that the players could make, we first had to create two functions that would return whether the player could make a single or doubles moves. Then these two functions would be called in the getAvailableMoves method to return a list of all the possible moves that each user can make.

#Advance method 

The purpose of this method was to update the game state given a move. For this, we apply the Visitor Pattern through an anonymous class to take advantage of the variables in the scope of myGameState class. Using the visitor pattern, we implement the logic to move each player on the map.

#Determine who is the winner 

The rules book provided by the university was helpful to implement the checks needed in getWinner method, some of the checks that we did in getWinner were: if mrX is in the same location as a detective (mrX loses), if it is mrX's turn to move and there are no available moves (mrX loses), and if mrX managed to fill in the log without a detective catching him (mrX wins). Two helper functions were used for this task; a function that checks if a detective has moves left or not and a function to get the corresponding player from its piece. One important thing is when no winner is determined yet; return an empty set. This line of code was implemented in getAvailableMoves

#Observer pattern

This is a factory again, producing via build (…) a game model which should hold a GameState and observer list and can be observed by the observers with regards to some events. In this task there are 5 different methods that should be completed, their aim is to return the current game board, registers an observer to the model, unregister an observer to the model, and the hardest method of them which is aimed to notify all the observer with move passed. In this method, getWinner was also used besides the advance method to inform the observers about the new state and events such as MOVE_MADE and GAME_OVER. 
![image](https://user-images.githubusercontent.com/63305840/182848783-46ba32e5-a7fb-4feb-a7b1-843dbd10e778.png)

