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
	    
	    Role playerRole = player.getRole();
	    Role opponentRole = opponent.getRole();
	    
	    player.setRole(opponentRole);
	    opponent.setRole(playerRole);
	}

	
}
