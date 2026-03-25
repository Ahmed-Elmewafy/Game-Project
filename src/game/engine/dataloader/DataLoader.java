package game.engine.dataloader;
import java.io.*;
import java.util.*;
import game.engine.cards.*;
import game.engine.cells.*;
import game.engine.monsters.*;
import game.engine.Role;
public class DataLoader {
	private static final String CARDS_FILE_NAME = "cards.csv";
	private static final String CELLS_FILE_NAME = "cells.csv";
	private static final String MONSTERS_FILE_NAME = "monsters.csv";
	
	public static ArrayList<Card> readCards() throws IOException
	{
		String line;
		ArrayList <Card> cards = new ArrayList<>();
		try (BufferedReader file = new BufferedReader(new FileReader(CARDS_FILE_NAME)))
		{
			while ((line = file.readLine()) != null) {
				String [] cardData = line.split(",");
				String cardType = cardData[0];
				if (cardType.equals("SWAPPER"))
					cards.add(new SwapperCard (cardData[1], cardData[2], Integer.parseInt(cardData[3])));
				else if (cardType.equals("SHIELD"))
					cards.add(new ShieldCard (cardData[1], cardData[2], Integer.parseInt(cardData[3])));
				else if (cardType.equals("ENERGYSTEAL"))
					cards.add(new EnergyStealCard (cardData[1], cardData[2], Integer.parseInt(cardData[3]), Integer.parseInt(cardData[4])));
				else if (cardType.equals("STARTOVER"))
					cards.add(new StartOverCard (cardData[1], cardData[2] ,Integer.parseInt(cardData[3]), Boolean.parseBoolean(cardData[4])));
				else if (cardType.equals("CONFUSION"))
					cards.add(new ConfusionCard (cardData[1], cardData[2] ,Integer.parseInt(cardData[3]), Integer.parseInt(cardData[4])));
			}
			return cards;
		}		
	}
		public static ArrayList<Cell> readCells() throws IOException {
			String line;
			ArrayList <Cell> cells = new ArrayList<>();
			try (BufferedReader file = new BufferedReader(new FileReader(CELLS_FILE_NAME))){
				while ((line = file.readLine()) != null) {
						String [] cellData = line.split(",");
						if (cellData.length == 3) {
							Role role = Role.valueOf(cellData[1]);
							cells.add(new DoorCell (cellData[0], role , Integer.parseInt(cellData[2])));
						}
						
						else
						{
							int effect = Integer.parseInt(cellData[1]);
							if (effect >= 0)
								cells.add(new ConveyorBelt (cellData[0], effect));
							else
								cells.add(new ContaminationSock (cellData[0], effect));
						}
				}
			return cells;
			}
		}
	
		public static ArrayList<Monster> readMonsters() throws IOException {
			String line;
			ArrayList <Monster> monsters = new ArrayList<>();
			try (BufferedReader file = new BufferedReader(new FileReader(MONSTERS_FILE_NAME))){
				while ((line = file.readLine()) != null) {
					String [] monsterData = line.split(",");
					String monsterType = monsterData[0];
					Role role = Role.valueOf(monsterData[3]);
					if (monsterType.equals("DYNAMO"))
						monsters.add(new Dynamo(monsterData[1], monsterData[2] ,role, Integer.parseInt(monsterData[4])));
					else if (monsterType.equals("DASHER"))
						monsters.add(new Dasher(monsterData[1], monsterData[2] ,role, Integer.parseInt(monsterData[4])));
					else if (monsterType.equals("MULTITASKER"))
						monsters.add(new MultiTasker(monsterData[1], monsterData[2] ,role, Integer.parseInt(monsterData[4])));
					else
						monsters.add(new Schemer(monsterData[1], monsterData[2] ,role, Integer.parseInt(monsterData[4])));
				}
			}	
			return monsters;
		}
	
}
	
	
	
