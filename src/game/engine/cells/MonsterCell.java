package game.engine.cells;
import game.engine.monsters.*;
public class MonsterCell extends Cell {
	private Monster cellMonster;
	
	public MonsterCell(String name, Monster cellMonster){
		super(name);
		this.cellMonster = cellMonster;
	}

	public Monster getCellMonster() {
		return cellMonster;
	}

	public void onLand (Monster landingMonster, Monster opponentMonster)
	{
		if (getCellMonster().getRole() == landingMonster.getRole())
			landingMonster.executePowerUpEffect();
		else
			if (landingMonster.getEnergy() > getCellMonster().getEnergy())
			{
				if (landingMonster.isShielded())
				{
					int replacedEnergy;
					replacedEnergy = landingMonster.getEnergy();
					landingMonster.setEnergy(cellMonster.getEnergy());
					cellMonster.setEnergy(replacedEnergy);	
				}
				else 
					cellMonster.setEnergy(landingMonster.getEnergy());		
			}
				
	}

}
