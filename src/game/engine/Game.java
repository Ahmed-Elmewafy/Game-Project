package game.engine;
import java.util.*; 
import game.engine.monsters.*; 
import game.engine.dataloader.*; 
import java.io.*;
public class Game {
private Board board;
private ArrayList<Monster> allMonsters= new ArrayList<>();
private Monster player;
private Monster opponent;
private Monster current;

public Game (Role playerRole) throws IOException
{
	board = new Board (DataLoader.readCards());
	allMonsters = DataLoader.readMonsters();
	player = selectRandomMonsterByRole(playerRole);
	Role oppositeRole = (playerRole == Role.SCARER)? Role.LAUGHER: Role.SCARER ;
	opponent = selectRandomMonsterByRole(oppositeRole); 
	current = player;
}


private Monster selectRandomMonsterByRole(Role role) {
	return allMonsters.get((int)(Math.random()*6)+1);
}
public Board getBoard() {
	return board;
}
public ArrayList <Monster> getAllMonsters() {
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
