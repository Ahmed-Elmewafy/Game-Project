package game.engine.cells;
import game.engine.monsters.*;
import game.engine.*;
public class CardCell  extends Cell{
	
	public  CardCell(String name) {
		super(name);
	}
	
	public void onLand(Monster landingMonster, Monster opponentMonster)
	{
		super.onLand(landingMonster, opponentMonster);
		Board.drawCard();
		
	}
}
