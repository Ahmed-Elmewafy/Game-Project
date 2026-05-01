package game.engine.cells;
import game.engine.interfaces.*;
import game.engine.*;
import game.engine.monsters.*;
public class ContaminationSock extends TransportCell implements CanisterModifier{
	
	public ContaminationSock(String name, int effect)  {
		super(name, (effect < 0)? effect: -effect);
	}
		
	public void transport (Monster monster)
	{
		monster.setPosition(monster.getPosition()+getEffect());
	}
	public void modifyCanisterEnergy(Monster monster, int canisterValue)
	{
		 	monster.alterEnergy(canisterValue);
	}
	public void onLand (Monster landingMonster, Monster opponentMonster)
	{
		super.onLand(landingMonster, opponentMonster);
		this.modifyCanisterEnergy(landingMonster, -Constants.SLIP_PENALTY);
	} 
}
	