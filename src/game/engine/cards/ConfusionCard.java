package game.engine.cards;
import game.engine.monsters.*;
import game.engine.Role;

public class ConfusionCard extends Card {
	private int duration;
	
	public ConfusionCard(String name, String description, int rarity, int duration) {
		super(name, description, rarity, false);
		this.duration = duration;
	}
	
	public int getDuration() {
		return duration;
	}

	public void performAction(Monster player, Monster opponent) {
		player.setConfusionTurns(this.getDuration());
        opponent.setConfusionTurns(this.getDuration());
        
        swapMonsterRole(player);
        swapMonsterRole(opponent);
	}
	
	private void swapMonsterRole(Monster target) {
        if (target.getRole() == Role.SCARER) 
            target.setRole(Role.LAUGHER);
        else 
            target.setRole(Role.SCARER);
  }
	
}
