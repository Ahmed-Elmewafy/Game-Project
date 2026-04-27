package game.engine.cards;
import game.engine.monsters.*;

public class SwapperCard extends Card {

	public SwapperCard(String name, String description, int rarity) {
		super(name, description, rarity, true);
	}
	
	public void performAction(Monster player, Monster opponent) {
		int temp;
		if(player.getPosition() < opponent.getPosition()) {
			temp = opponent.getPosition();
			opponent.setPosition(player.getPosition());
			player.setPosition(temp);
		}
	}
	
}
