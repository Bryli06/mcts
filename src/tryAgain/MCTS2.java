package tryAgain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import MCTS.Mcts;
import MCTS.TicTacToe;


//board stuff
//----------------------------------------------------------------------------------------------------------------//

class Position {
    int x;
    int y;

    public Position() {
    }

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

}

class Board {
    int[][] boardValues;
    int totalMoves;

    public static final int DEFAULT_BOARD_SIZE = 3;

    public static final int IN_PROGRESS = -1;
    public static final int DRAW = 0;
    public static final int P1 = 1;
    public static final int P2 = 2;

    public Board() {
        boardValues = new int[DEFAULT_BOARD_SIZE][DEFAULT_BOARD_SIZE];
    }

    public Board(int boardSize) {
        boardValues = new int[boardSize][boardSize];
    }

    public Board(int[][] boardValues) {
        this.boardValues = boardValues;
    }

    public Board(int[][] boardValues, int totalMoves) {
        this.boardValues = boardValues;
        this.totalMoves = totalMoves;
    }

    public Board(Board board) {
        int boardLength = board.getBoardValues().length;
        this.boardValues = new int[boardLength][boardLength];
        int[][] boardValues = board.getBoardValues();
        int n = boardValues.length;
        for (int i = 0; i < n; i++) {
            int m = boardValues[i].length;
            for (int j = 0; j < m; j++) {
                this.boardValues[i][j] = boardValues[i][j];
            }
        }
    }

    public void performMove(int player, Position p) {
        this.totalMoves++;
        boardValues[p.getX()][p.getY()] = player;
    }

    public int[][] getBoardValues() {
        return boardValues;
    }

    public void setBoardValues(int[][] boardValues) {
        this.boardValues = boardValues;
    }

    public int checkStatus() {
        int boardSize = boardValues.length;
        int maxIndex = boardSize - 1;
        int[] diag1 = new int[boardSize];
        int[] diag2 = new int[boardSize];
        
        for (int i = 0; i < boardSize; i++) {
            int[] row = boardValues[i];
            int[] col = new int[boardSize];
            for (int j = 0; j < boardSize; j++) {
                col[j] = boardValues[j][i];
            }
            
            int checkRowForWin = checkForWin(row);
            if(checkRowForWin!=0)
                return checkRowForWin;
            
            int checkColForWin = checkForWin(col);
            if(checkColForWin!=0)
                return checkColForWin;
            
            diag1[i] = boardValues[i][i];
            diag2[i] = boardValues[maxIndex - i][i];
        }

        int checkDia1gForWin = checkForWin(diag1);
        if(checkDia1gForWin!=0)
            return checkDia1gForWin;
        
        int checkDiag2ForWin = checkForWin(diag2);
        if(checkDiag2ForWin!=0)
            return checkDiag2ForWin;
        
        if (possible_Moves().size() > 0)
            return IN_PROGRESS;
        else
            return DRAW;
    }

    private int checkForWin(int[] row) {
        boolean isEqual = true;
        int size = row.length;
        int previous = row[0];
        for (int i = 0; i < size; i++) {
            if (previous != row[i]) {
                isEqual = false;
                break;
            }
            previous = row[i];
        }
        if(isEqual)
            return previous;
        else
            return 0;
    }

    public void to_String() {
        int size = this.boardValues.length;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print(boardValues[i][j] + " ");
            }
            System.out.println();
        }
    }

    public List<Position> possible_Moves() {
        int size = this.boardValues.length;
        List<Position> emptyPositions = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (boardValues[i][j] == 0)
                    emptyPositions.add(new Position(i, j));
            }
        }
        return emptyPositions;
    }

    public void printStatus() {
        switch (this.checkStatus()) {
        case P1:
            System.out.println("Player 1 wins");
            break;
        case P2:
            System.out.println("Player 2 wins");
            break;
        case DRAW:
            System.out.println("Game Draw");
            break;
        case IN_PROGRESS:
            System.out.println("Game In Progress");
            break;
        }
    }
}

//MCTS stuff
//----------------------------------------------------------------------------------------------------------------//



class MCTS {

    private static final int WIN_SCORE = 10;
    private long start,end;
    private int opponent;
    private final int threads;
    private final ExecutorService executor;
	private long timeAllowedMilliseconds=1000;

    public MCTS(ExecutorService executor, int threads) {
    	this.executor=executor;
    	this.threads=threads;
    }
    public Board findNextMove(Board board, int playerNo) {
        start = System.currentTimeMillis();
        end=start+timeAllowedMilliseconds;

        opponent = 3 - playerNo;
        Tree tree = new Tree();
        Node rootNode = tree.getRoot();
        rootNode.getState().setBoard(board);
        rootNode.getState().setPlayerNo(opponent);
        
        
        Collection<Callable<Void>> tasks = new ArrayList<>();
        
        for (int i = 0; i < threads; i++)
            tasks.add(() -> {
            	while (System.currentTimeMillis() < end) {
                    Node promisingNode = selectPromisingNode(rootNode);
                    if (promisingNode.getState().getBoard().checkStatus() == Board.IN_PROGRESS)
                        expandNode(promisingNode);

                    Node nodeToExplore = promisingNode;
                    if (promisingNode.getChildArray().size() > 0) {
                        nodeToExplore = promisingNode.getRandomChildNode();
                    }
                    int playoutResult = simulateRandomPlayout(nodeToExplore);
                    backPropogation(nodeToExplore, playoutResult);
                }
                return null;
            });
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        Node winnerNode = rootNode.getChildWithMaxScore();
        tree.setRoot(winnerNode);
        return winnerNode.getState().getBoard();
    }

    private Node selectPromisingNode(Node rootNode) {
        Node node = rootNode;
        while (node.getChildArray().size() != 0) {
            node = UCT.findBestNodeWithUCT(node);
        }
        return node;
    }

    private void expandNode(Node node) {
        List<State> possibleStates = node.getState().getAllPossibleStates();
        possibleStates.forEach(state -> {
            Node newNode = new Node(state);
            newNode.setParent(node);
            newNode.getState().setPlayerNo(node.getState().getOpponent());
            node.getChildArray().add(newNode);
        });
    }

    private void backPropogation(Node nodeToExplore, int playerNo) {
        Node tempNode = nodeToExplore;
        while (tempNode != null) {
            tempNode.getState().incrementVisit();
            if (tempNode.getState().getPlayerNo() == playerNo)
                tempNode.getState().addScore(WIN_SCORE);
            tempNode = tempNode.getParent();
        }
    }

    private int simulateRandomPlayout(Node node) {
        Node tempNode = new Node(node);
        State tempState = tempNode.getState();
        int boardStatus = tempState.getBoard().checkStatus();

        if (boardStatus == opponent) {
            tempNode.getParent().getState().setWinScore(Integer.MIN_VALUE);
            return boardStatus;
        }
        while (boardStatus == Board.IN_PROGRESS) {
            tempState.togglePlayer();
            tempState.randomPlay();
            boardStatus = tempState.getBoard().checkStatus();
        }

        return boardStatus;
    }

}




class UCT {

    public static double uctValue(int totalVisit, double nodeWinScore, int nodeVisit) {
        if (nodeVisit == 0) {
            return Integer.MAX_VALUE;
        }
        return (nodeWinScore / (double) nodeVisit) + 1.41 * Math.sqrt(Math.log(totalVisit) / (double) nodeVisit);
    }

    static Node findBestNodeWithUCT(Node node) {
        int parentVisit = node.getState().getVisitCount();
        return Collections.max(
          node.getChildArray(),
          Comparator.comparing(c -> uctValue(parentVisit, c.getState().getWinScore(), c.getState().getVisitCount())));
    }
}



class State {
    private Board board;
    private int playerNo;
    private int visitCount;
    private double winScore;

    public State() {
        board = new Board();
    }

    public State(State state) {
        this.board = new Board(state.getBoard());
        this.playerNo = state.getPlayerNo();
        this.visitCount = state.getVisitCount();
        this.winScore = state.getWinScore();
    }

    public State(Board board) {
        this.board = new Board(board);
    }

    Board getBoard() {
        return board;
    }

    void setBoard(Board board) {
        this.board = board;
    }

    int getPlayerNo() {
        return playerNo;
    }

    void setPlayerNo(int playerNo) {
        this.playerNo = playerNo;
    }

    int getOpponent() {
        return 3 - playerNo;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    double getWinScore() {
        return winScore;
    }

    void setWinScore(double winScore) {
        this.winScore = winScore;
    }

    public List<State> getAllPossibleStates() {
        List<State> possibleStates = new ArrayList<>();
        List<Position> availablePositions = this.board.possible_Moves();
        availablePositions.forEach(p -> {
            State newState = new State(this.board);
            newState.setPlayerNo(3 - this.playerNo);
            newState.getBoard().performMove(newState.getPlayerNo(), p);
            possibleStates.add(newState);
        });
        return possibleStates;
    }

    void incrementVisit() {
        this.visitCount++;
    }

    void addScore(double score) {
        if (this.winScore != Integer.MIN_VALUE)
            this.winScore += score;
    }

    void randomPlay() {
        List<Position> availablePositions = this.board.possible_Moves();
        int totalPossibilities = availablePositions.size();
        int selectRandom = (int) (Math.random() * totalPossibilities);
        this.board.performMove(this.playerNo, availablePositions.get(selectRandom));
    }

    void togglePlayer() {
        this.playerNo = 3 - this.playerNo;
    }
}
//tree stuff
//----------------------------------------------------------------------------------------------------------------//
class Node {
    State state;
    Node parent;
    List<Node> childArray;

    public Node() {
        this.state = new State();
        childArray = new ArrayList<>();
    }

    public Node(State state) {
        this.state = state;
        childArray = new ArrayList<>();
    }

    public Node(State state, Node parent, List<Node> childArray) {
        this.state = state;
        this.parent = parent;
        this.childArray = childArray;
    }

    public Node(Node node) {
        this.childArray = new ArrayList<>();
        this.state = new State(node.getState());
        if (node.getParent() != null)
            this.parent = node.getParent();
        List<Node> childArray = node.getChildArray();
        for (Node child : childArray) {
            this.childArray.add(new Node(child));
        }
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public List<Node> getChildArray() {
        return childArray;
    }

    public void setChildArray(List<Node> childArray) {
        this.childArray = childArray;
    }

    public Node getRandomChildNode() {
        int noOfPossibleMoves = this.childArray.size();
        int selectRandom = (int) (Math.random() * noOfPossibleMoves);
        return this.childArray.get(selectRandom);
    }

    public Node getChildWithMaxScore() {
        return Collections.max(this.childArray, Comparator.comparing(c -> {
            return c.getState().getVisitCount();
        }));
    }

}


class Tree {
    Node root;

    public Tree() {
        root = new Node();
    }

    public Tree(Node root) {
        this.root = root;
    }

    public Node getRoot() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
    }

    public void addChild(Node parent, Node child) {
        parent.getChildArray().add(child);
    }

}

//Main

//-----------------------------------------------------------------------------------------------------------//
public class MCTS2 {
	private static Board state;
	private static MCTS mcts;
	private static int threads=2;
	private static int time=1000000;
	public static void main(String[] args) {
		ExecutorService executor = threads > 1
	            ? Executors.newFixedThreadPool(threads)
	            : null;
		mcts = new MCTS(executor, threads );
		Scanner in = new Scanner(System.in);
		int row,col;
		state= new Board();
		state.to_String();
		while(state.IN_PROGRESS==state.checkStatus()) {
			System.out.println(state.toString());
			row=in.nextInt();
			col=in.nextInt();
			if(state.IN_PROGRESS!=state.checkStatus()) break;
			state.performMove(1, new Position(col,row));
			state=mcts.findNextMove(state, 2);
			state.to_String();
		}
		System.out.println(state.checkStatus());
	}
}
