package game.engine.cells;
import game.engine.Role;
import game.engine.interfaces.*;
import game.engine.monsters.*;
import java.util.ArrayList;
import game.engine.*;
public class DoorCell extends Cell implements CanisterModifier {
	private Role role;
	private int energy;
	private boolean activated;

	public DoorCell(String name, Role role, int energy){
		super(name);
		this.role = role;
		this.energy = energy;
		this.activated = false;
	}

	public void modifyCanisterEnergy(Monster monster, int canisterValue)
	{
		monster.setEnergy(getEnergy() + canisterValue );
		
	}
	
	public boolean isActivated() {
		return activated;
	}

	public void setActivated(boolean activated) {
		this.activated = activated;
	}

	public Role getRole() {
		return role;
	}

	public int getEnergy() {
		return energy;
	}
	
	public void onLand (Monster landingMonster, Monster opponentMonster)
	{
		ArrayList<Boolean> shields = new ArrayList<>(); 
		super.onLand(landingMonster, opponentMonster);
		if (!isActivated())
		{
			boolean shield = false;
			ArrayList<Monster> stationedMonsters = Board.getStationedMonsters();
			if (getRole() == landingMonster.getRole()) {	
				for (Monster monster : stationedMonsters)
					if (monster.getRole() == landingMonster.getRole()) 
						modifyCanisterEnergy(monster, getEnergy());
				modifyCanisterEnergy(landingMonster, getEnergy());
				setActivated(true);		
			}
		
			else 
			{
				for (Monster monster : stationedMonsters)
					if (monster.getRole() == landingMonster.getRole()) 
					{ 
						shields.add(monster.isShielded());
						monster.alterEnergy(-energy);;
					}			
				
				shields.add(landingMonster.isShielded());
				landingMonster.alterEnergy(-energy);
				if(shields.contains(false))
					setActivated(true);		
			}
		
		}
	}
}