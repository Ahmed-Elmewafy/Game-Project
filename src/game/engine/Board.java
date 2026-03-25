package game.engine;
import game.engine.cards.Card;
import game.engine.cells.*;
import game.engine.monsters.Monster;
import java.util.ArrayList;
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
	
	
}
