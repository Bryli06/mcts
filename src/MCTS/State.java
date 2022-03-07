package MCTS;

import java.util.ArrayList;

interface State {

    boolean isTerminal();

    short[] possibleMoves();

    int getPreviousAgent();

    double getRewardFor(int agent);

    State takeAction(short action);

    State copy();

    void applyAction(short action);

    int getWinner();

}