package game.engine.monsters;

import game.engine.Role;

public class Dasher extends Monster {
	private int momentumTurns;

	public Dasher(String name, String description, Role originalRole, int energy) {
		super(name, description, originalRole, energy);
		this.momentumTurns = 0;
	}

	public int getMomentumTurns() {
		return momentumTurns;
	}

	public void setMomentumTurns(int momentumTurns) {
		this.momentumTurns = momentumTurns;
	}
	





}
