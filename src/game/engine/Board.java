package game.engine;
import game.engine.Constants;
import game.engine.cards.Card;
import game.engine.cells.*;
import game.engine.monsters.Monster;
import java.util.ArrayList;
public class Board {

	private Cell[][] boardCells;
	private static ArrayList<Monster> stationedMonsters;
	private static ArrayList<Card> originalCards;
	private static ArrayList<Card> cards;
	
	
	public ArrayList<Monster> getStationedMonsters() {
		return stationedMonsters;
	}
	public void setStationedMonsters(ArrayList<Monster> stationedMonsters) {
		this.stationedMonsters = stationedMonsters;
	}
	public ArrayList<Card> getCards() {
		return cards;
	}
	public void setCards(ArrayList<Card> cards) {
		this.cards = cards;
	}
	public Cell[][] getBoardCells() {
		return boardCells;
	}
	public ArrayList<Card> getOriginalCards() {
		return originalCards;
	}
	public Board(ArrayList<Card> readCards) {
		super();
		this.boardCells = new Cell[Constants.BOARD_ROWS][Constants.BOARD_COLS];
		this.stationedMonsters = new ArrayList<>();
		this.originalCards = readCards;
		this.cards = new ArrayList<>();
	}
	
	
}
