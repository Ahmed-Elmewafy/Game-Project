package game.engine.cells;
import game.engine.Role;
import game.engine.interfaces.*;
import game.engine.monsters.*;
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
		super.onLand(landingMonster, opponentMonster);
		if (!isActivated())
		{
			if (getRole() == landingMonster.getRole()) {
				modifyCanisterEnergy(landingMonster, energy);
					
			}
		}
			
		
		
	}
}
