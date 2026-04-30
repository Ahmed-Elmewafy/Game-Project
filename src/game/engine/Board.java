package game.engine;
import game.engine.cards.*;
import game.engine.cells.*;
import game.engine.monsters.*;
import java.util.*;
import game.engine.exceptions.*;
public class Board {

	private Cell[][] boardCells;
	private static ArrayList<Monster> stationedMonsters;
	private static ArrayList<Card> originalCards;
	public static ArrayList<Card> cards;
	
	public static ArrayList<Monster> getStationedMonsters() {
		return stationedMonsters;
	}
	public static void setStationedMonsters(ArrayList<Monster> stationedMonstersP) {
		stationedMonsters = stationedMonstersP;
	}
	public static ArrayList<Card> getCards() {
		return cards;
	}
	public static void setCards(ArrayList<Card> cardsP) {
		cards = cardsP;
	}
	public Cell[][] getBoardCells() {
		return boardCells;
	}
	public static ArrayList<Card> getOriginalCards() {
		return originalCards;
	}
	public Board(ArrayList<Card> readCards) {
		super();
		this.boardCells = new Cell[Constants.BOARD_ROWS][Constants.BOARD_COLS];
		this.stationedMonsters = new ArrayList<>();
		this.originalCards = readCards;
		this.cards = new ArrayList<>();
		setCardsByRarity();
		reloadCards();
	}
	private int[] indexToRowCol(int index) {
	    int row = index / 10;
	    int col;
	    if (row % 2 == 0) {
	        col = index % 10;
	    } else {
	        col = 9 - (index % 10);
	    }
	    return new int[]{row, col};
	}
	private Cell getCell(int index) {
		int [] position=indexToRowCol(index);
		return boardCells[position[0]][position[1]];
	}
	private void setCell(int index , Cell cell) {
		int[] indx=indexToRowCol(index);
		boardCells[indx[0]][indx[1]]=cell;
	}
	public void initializeBoard(ArrayList<Cell> specialCells) {

	ArrayList<Cell> doorcells = new ArrayList<>();
	ArrayList<Cell> conveyorcells = new ArrayList<>();
	ArrayList<Cell> sockcells = new ArrayList<>();
	
	HashSet<Integer> monsterIdx = new HashSet<>();
	HashSet<Integer> cardIdx = new HashSet<>();
	HashSet<Integer> conveyorIdx = new HashSet<>();
	HashSet<Integer> sockIdx = new HashSet<>();
	
	for (int i : Constants.MONSTER_CELL_INDICES) monsterIdx.add(i);
	for (int i : Constants.CARD_CELL_INDICES) cardIdx.add(i);
	for (int i : Constants.CONVEYOR_CELL_INDICES) conveyorIdx.add(i);
	for (int i : Constants.SOCK_CELL_INDICES) sockIdx.add(i);
	
	for (Cell c : specialCells) {
	if (c instanceof DoorCell) {
	doorcells.add(c);
	
	} else if (c instanceof ConveyorBelt) {
	conveyorcells.add(c);
	} else if (c instanceof ContaminationSock) {
	sockcells.add(c);
	}
	}
	
	int monsterPointer = 0;
	int cardPointer = 0;
	
	for (int i = 0; i < Constants.BOARD_SIZE; i++) {
	
	if (monsterIdx.contains(i)) {
	if (monsterPointer < stationedMonsters.size()) {
	Monster m = stationedMonsters.get(monsterPointer++);
	setCell(i, new MonsterCell(m.getName(), m));
	         m.setPosition(i);
	}
	
	} else if (cardIdx.contains(i)) {
	
	if (cardPointer < cards.size()) {
	Card c = cards.get(cardPointer++);
	setCell(i, new CardCell(c.getName()));
	}
	else{
	setCell(i, new CardCell("Card"));
	}
	
	} else if (conveyorIdx.contains(i) && !conveyorcells.isEmpty()) {
	setCell(i, conveyorcells.remove(0));
	
	} else if (sockIdx.contains(i) && !sockcells.isEmpty()) {
	setCell(i, sockcells.remove(0));
	
	} else if (i % 2 == 0) {
	setCell(i, new Cell("Normal " + i));
	
	} else if (!doorcells.isEmpty()) {
		setCell(i, doorcells.remove(0));
		}
	}
}
	
	private static void setCardsByRarity() {
		ArrayList<Card>expanded=new ArrayList<>();
		for(int i=0 ; i<getOriginalCards().size() ; i++) {
			int cardRarity=getOriginalCards().get(i).getRarity();
			for(int j=0 ; j<cardRarity ; j++) {
				expanded.add(getOriginalCards().get(i));
			}
		}
		cards=expanded;
		originalCards=expanded;
	}
	public static void reloadCards() {
		ArrayList<Card>reloadedCards=originalCards;
		Collections.shuffle(reloadedCards);
		cards=reloadedCards;
	}
	public static Card drawCard() {
		if(cards.size()>0) {
			Card drawnCard=cards.get(0);
			cards.remove(0);
			return drawnCard;
			}
		
		else {
			reloadCards();
			return drawCard();
		}
	}
	public void moveMonster(Monster currentMonster, int roll, Monster opponentMonster) throws InvalidMoveException{
	int newPosition=currentMonster.getPosition()+roll;
	int originalPosition=currentMonster.getPosition();
	int initialConfusionP=currentMonster.getConfusionTurns();
	int initialConfusionO=opponentMonster.getConfusionTurns();
	currentMonster.setPosition(newPosition);
	getCell(newPosition).onLand(currentMonster,opponentMonster);
	if(newPosition==opponentMonster.getPosition()) {
		newPosition=originalPosition;
		currentMonster.setPosition(newPosition);
		throw new InvalidMoveException();
		}
	if(currentMonster.isConfused()&&initialConfusionP>0) {
		currentMonster.setConfusionTurns(currentMonster.getConfusionTurns()-1);
	}
		currentMonster.setPosition(newPosition);
		opponentMonster.setPosition(opponentMonster.getPosition());
		updateMonsterPositions(currentMonster,opponentMonster);
	}
	
	
	private void updateMonsterPositions(Monster player , Monster opponent) {
		for (int i = 0; i < Constants.BOARD_SIZE; i++) {
	        int[] pos = indexToRowCol(i);
	        boardCells[pos[0]][pos[1]].setMonster(null);
	    }
	    getCell(player.getPosition()).setMonster(player);
	    getCell(opponent.getPosition()).setMonster(opponent);
	}
}
