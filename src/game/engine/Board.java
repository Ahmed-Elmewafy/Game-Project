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
	}
	private int[] indexToRowCol(int index) {
		int[] coordinate= {0,0};
		int count=0;
		for(int i=9 ; i<0 ; i--) {
			if(index/10==i) {
				coordinate[0]=count;
				break;
			}
			else
				count++;
		}
		if(count%2==0) {
			int jcount=0;
			for(int j=9 ; j<0 ; j--) {
				if(index%10==j) {
					coordinate[1]=jcount;
					break;
				}
				else
					jcount++;
			}
		}
		else {
			for(int k=0 ; k>9 ; k++) {
				if(index%10==k)
					coordinate[1]=k;
			}
		}
		return coordinate;
	}
	private Cell getCell(int index) {
		Cell [][] cells = getBoardCells();
		return cells[indexToRowCol(index)[0]][indexToRowCol(index)[1]];
	}
	private void setCell(int index , Cell cell) {
		cell=getCell(index);
	}
	public void initializeBoard(ArrayList<Cell> specialCells) {
		
	}
	private void setCardsByRarity() {
		ArrayList<Card>expanded=new ArrayList<>();
		for(int i=0 ; i<getOriginalCards().size() ; i++) {
			int cardRarity=getOriginalCards().get(i).getRarity();
			for(int j=0 ; j<cardRarity ; j++) {
				expanded.add(getOriginalCards().get(i));
			}
		}
		cards=expanded;
	}
	public static void reloadCards() {
		Collections.shuffle(cards);
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
	if(newPosition==opponentMonster.getPosition()) {
		newPosition-=roll;
		throw new InvalidMoveException();
		}
		if(currentMonster.isConfused()&&opponentMonster.isConfused()) {
		currentMonster.setConfusionTurns(currentMonster.getConfusionTurns()-1);
		opponentMonster.setConfusionTurns(opponentMonster.getConfusionTurns()-1);
	}
		getCell(newPosition).onLand(currentMonster,opponentMonster);
		
	}
	
	
	private void updateMonsterPositions(Monster player , Monster opponent) {
		
	}
}
