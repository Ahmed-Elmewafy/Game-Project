package game.engine.cells;
import game.engine.interfaces.*;
public class ContaminationSock extends TransportCell implements CanisterModifier{
	
	public ContaminationSock(String name, int effect)  {
		super(name, (effect < 0)? effect: -effect);
	}

}
	