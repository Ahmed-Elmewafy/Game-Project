package game.engine.cards;
import game.engine.monsters.*;
import game.engine.interfaces.CanisterModifier;

public class EnergyStealCard extends Card implements CanisterModifier {
	private int energy;

	public EnergyStealCard(String name, String description, int rarity, int energy) {
		super(name, description, rarity, true);
		this.energy = energy;
	}
	
	public int getEnergy() {
		return energy;
	}
	
	public void performAction(Monster player, Monster opponent) {
		int actualStolen = 0; 

	    if (!opponent.isShielded()) { 
	        actualStolen = Math.min(this.getEnergy(), opponent.getEnergy()); 
	    }
	    this.modifyCanisterEnergy(opponent, -this.getEnergy()); 
	    this.modifyCanisterEnergy(player, actualStolen);
	}
	
	public void modifyCanisterEnergy(Monster monster, int canisterValue) {
		monster.alterEnergy(canisterValue);
	}
	
	
}
