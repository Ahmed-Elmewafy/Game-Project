package game.engine;
import java.util.*; import game.engine.monsters.*; import game.engine.dataloader.*; import game.engine.cards.*;
public class Game {
private Board board;
private ArrayList<Monster> allMonsters= new ArrayList<>();
private Monster player;
private Monster opponent;
private Monster current;

public Game(Role playerRole) throws IOException{
	try (ArrayList<Card> BoardCards=DataLoader.readCards()){
	ArrayList<Card>CellCards=Board.getOriginalCards();
	CellCards=BoardCards;}
}

private Monster selectRandomMonsterByRole(Role role) {
	return allMonsters.get((int)(Math.random()*6)+1);
}
public Board getBoard() {
	return board;
}
public ArrayList getAllMonsters() {
	return allMonsters;
}
public Monster getPlayer() {
	return player;
}
public Monster getOpponent() {
	return opponent;
}
public Monster getCurrent() {
	return current;
}
}
