package MCTS;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
	private static TicTacToe state;
	private static Mcts<TicTacToe> mcts;
	private static int threads=1;
	private static int time=1000000;
	public static void main(String[] args) {
		ExecutorService executor = threads > 1
	            ? Executors.newFixedThreadPool(threads)
	            : null;
		mcts = new Mcts<>(executor, threads, 0, 100000);
		int action=-1;
		state = TicTacToe.start(19, 5);
		Scanner in = new Scanner(System.in);
		int row,col;
//		while(!state.isTerminal()) {
//			System.out.println(state.toString());
//			row=in.nextInt();
//			col=in.nextInt();
//			action=row*19+col;
//			state=(TicTacToe) state.takeAction((short)action);
//			System.out.println(state.toString());
//			mcts.setRoot(action, state);
//            mcts.think();
//            state = (TicTacToe) mcts.takeAction();
//            action = mcts.getLastAction();
//            System.out.println(action/19+" "+action%19);
//		}
		TicTacToe startState = TicTacToe.start(13,5);
        SelfPlay<TicTacToe> play = new SelfPlay<>(
            startState,
            1 > 1
            ? Executors.newFixedThreadPool(threads)
            : null,
            1 > 1
            ? Executors.newFixedThreadPool(threads)
            : null,
            1,
            1,
            0,
            0,
            0,
            0);
        System.out.println(play.play());
	}

}
