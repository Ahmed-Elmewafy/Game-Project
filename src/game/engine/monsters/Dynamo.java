package game.engine.monsters;

import game.engine.Role;

public class Dynamo extends Monster {
	
	public Dynamo(String name, String description, Role role, int energy) {
		super(name, description, role, energy);
	}
	
	public void executePowerupEffect(Monster opponentMonster) {
            opponentMonster.setFrozen(true);
	}
	public void setEnergy(int energy) {
        int temp = energy - this.getEnergy();
        if (temp != 0) {
            super.setEnergy(this.getEnergy() + (temp * 2));
        }
        }
}
