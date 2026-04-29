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
		int[] coordinate= {0,0};
		int count=0;
		for(int i=9 ; i>=0 ; i--) {
			if(index/10==i) {
				coordinate[0]=count;
				break;
			}
			else
				count++;
		}
		if(count%2==0) {
			int jcount=0;
			for(int j=9 ; j>=0 ; j--) {
				if(index%10==j) {
					coordinate[1]=jcount;
					break;
				}
				else
					jcount++;
			}
		}
		else {
			for(int k=0 ; k<=9 ; k++) {
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
		for(int i=0 ; i<100 ; i++) {
			setCell(i, new Cell("Corridor"));
		}
		int doorIndex=0;
		for(int i=1 ; i<100 ; i+=2) {
			setCell(i, specialCells.get(doorIndex++));
		}
		for (int i = 0; i < Constants.CONVEYOR_CELL_INDICES.length; i++) {
	        setCell(Constants.CONVEYOR_CELL_INDICES[i], specialCells.get(50 + i));
	    }
		for (int i = 0; i < Constants.SOCK_CELL_INDICES.length; i++) {
	        setCell(Constants.SOCK_CELL_INDICES[i], specialCells.get(55 + i));
	    }
	    for (int idx : Constants.CARD_CELL_INDICES) {
	        setCell(idx, new CardCell("Card Station"));
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
	}
	public static void reloadCards() {
		ArrayList<Card>reloadedCards=cards;
		Collections.shuffle(reloadedCards);
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
	int initialConfusionP=currentMonster.getConfusionTurns();
	int initialConfusionO=opponentMonster.getConfusionTurns();
	if(newPosition==opponentMonster.getPosition()||newPosition>99) {
		newPosition-=roll;
		throw new InvalidMoveException();
		}
	if(currentMonster.isConfused()&&initialConfusionP>0) {
		currentMonster.setConfusionTurns(currentMonster.getConfusionTurns()-1);
	}
	if(opponentMonster.isConfused()&&initialConfusionO>0) {
		opponentMonster.setConfusionTurns(opponentMonster.getConfusionTurns()-1);
	}
		getCell(newPosition).onLand(currentMonster,opponentMonster);
		updateMonsterPositions(currentMonster,opponentMonster);
	}
	
	
	private void updateMonsterPositions(Monster player , Monster opponent) {
		for(int i=0 ; i<100 ; i++) {
			int[] position=indexToRowCol(i);
			boardCells[position[0]][position[1]].setMonster(null);
		}
		getCell(player.getPosition()).setMonster(player);
		getCell(opponent.getPosition()).setMonster(opponent);
		for(int i=0 ; i<Constants.MONSTER_CELL_INDICES.length; i++) {
			int idx = Constants.MONSTER_CELL_INDICES[i];
	        Monster stationed = stationedMonsters.get(i);
	        getCell(idx).setMonster(stationed);
		}
	}
}
