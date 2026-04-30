package game.engine;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import game.engine.dataloader.DataLoader;
import game.engine.monsters.*;
import game.engine.exceptions.*;
import game.engine.*;
import game.engine.cells.*;

public class Game {
	private Board board;
	private ArrayList<Monster> allMonsters; 
	private Monster player;
	private Monster opponent;
	private Monster current;
	
	public Game(Role playerRole) throws IOException {
		this.board = new Board(DataLoader.readCards());
		
		this.allMonsters = DataLoader.readMonsters();
		
		this.player = selectRandomMonsterByRole(playerRole);
		this.opponent = selectRandomMonsterByRole(playerRole == Role.SCARER ? Role.LAUGHER : Role.SCARER);
		this.current = player;
		allMonsters.remove(player);
		allMonsters.remove(opponent);
		board.setStationedMonsters(allMonsters);
		board.initializeBoard(DataLoader.readCells());
	
	}
	
	public Board getBoard() {
		return board;
	}
	
	public ArrayList<Monster> getAllMonsters() {
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
	
	public void setCurrent(Monster current) {
		this.current = current;
	}
	
	private Monster selectRandomMonsterByRole(Role role) {
		Collections.shuffle(allMonsters);
	    return allMonsters.stream()
	    		.filter(m -> m.getRole() == role)
	    		.findFirst()
	    		.orElse(null);
	}
	
	private Monster getCurrentOpponent() {
		if (this.current == this.player)
			return this.opponent;;
			return this.player;
	}
	
	 private int rollDice() {
		 return (int)(Math.random() * 6) + 1;
	 }
	
	 private void switchTurn() {
		 if (this.current == this.player) {
		        this.current = this.opponent;
		    } else {
		        this.current = this.player;
		    }
	 }
	 
	 private boolean checkWinCondition(Monster monster) {
		 if (monster.getEnergy() >= Constants.WINNING_ENERGY && monster.getPosition() >= Constants.WINNING_POSITION )
			 return true;
		 	 return false;
	 }
	
	 public Monster getWinner() {
		 if (this.checkWinCondition(this.player))
			 return this.player;
		 if(this.checkWinCondition(this.opponent))
			 return this.opponent;
		 else 
			 return null;
	 }

	 public void usePowerup() throws OutOfEnergyException
		{
			if (current.getEnergy() >= Constants.POWERUP_COST)
			{
				current.setEnergy(current.getEnergy() - Constants.POWERUP_COST);
				current.executePowerupEffect(this.getCurrentOpponent());
			}
			else
				throw new OutOfEnergyException() ;
		}
		
		public void playTurn()
		{
			if (current.isFrozen())
				current.setFrozen(false);
			else
				current.move(rollDice());
				
			switchTurn();
		}
}