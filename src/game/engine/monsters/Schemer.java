package game.engine.monsters;

import game.engine.Role;
import game.engine.Constants;
import game.engine.*;
import java.util.ArrayList;
public class Schemer extends Monster {
	
	public Schemer(String name, String description, Role role, int energy) {
		super(name, description, role, energy);
	}
	
	private int stealEnergyFrom(Monster target) {
		int stealAmount = Math.min(Constants.SCHEMER_STEAL, target.getEnergy());
		target.setEnergy(target.getEnergy() - stealAmount);
		return stealAmount;
	}
	public void executePowerupEffect(Monster opponentMonster) {
        ArrayList <Monster> allMonsters = Board.getStationedMonsters();
		int totalStolen = 0;
    	totalStolen += stealEnergyFrom(opponentMonster);
    	for (Monster monster : allMonsters )
    			totalStolen += stealEnergyFrom(monster);
        if (totalStolen > 0) {
        this.alterEnergy(totalStolen); 
        }
    }
	public void setEnergy(int energy) {
        int change = energy - this.getEnergy();
        if (change != 0) {
            super.setEnergy(this.getEnergy() + change + 10);
        }
        }
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
