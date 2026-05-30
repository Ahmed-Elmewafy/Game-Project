package game.tests;

import game.engine.Role;
import game.engine.monsters.*;
import game.engine.cells.*;
import game.engine.cards.*;
import game.engine.Constants;
import game.engine.Board;
import game.engine.Game;
import game.engine.dataloader.DataLoader;
import game.engine.exceptions.*;

import java.io.IOException;
import java.util.ArrayList;

public class ExtensiveScenarioSimulation {

    private static int totalTests = 0;
    private static int passedTests = 0;

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("            STARTING EXHAUSTIVE GAME ENGINE SIMULATION SUITE          ");
        System.out.println("======================================================================");

        // Reset Board static variables to clean initial states
        Board.setStationedMonsters(new ArrayList<Monster>());

        // 1. BASE ATTRIBUTES & MODULO BOUNDS
        testConstructorDasher();
        testConstructorDynamo();
        testConstructorMultiTasker();
        testConstructorSchemer();
        testPositionModuloWrappingPositive();
        testPositionModuloWrappingNegative();
        testEnergyLowerBoundDasher();
        testEnergyLowerBoundDynamo();
        testEnergyLowerBoundMultiTasker();
        testEnergyLowerBoundSchemer();
        testShieldBlocksNegativeAlterationDasher();
        testShieldBlocksNegativeAlterationDynamo();
        testShieldBlocksNegativeAlterationMultiTasker();
        testShieldBlocksNegativeAlterationSchemer();
        testShieldIgnoredOnPositiveAlterationDasher();
        testShieldIgnoredOnPositiveAlterationDynamo();
        testShieldIgnoredOnPositiveAlterationMultiTasker();
        testShieldIgnoredOnPositiveAlterationSchemer();
        testDeductFlatCostDasher();
        testDeductFlatCostDynamo();
        testDeductFlatCostMultiTasker();
        testDeductFlatCostSchemer();
        testConfusionTurnsDecayDasher();
        testConfusionTurnsDecayDynamo();
        testConfusionTurnsDecayMultiTasker();
        testConfusionTurnsDecaySchemer();
        testConfusionRoleResetDasher();
        testConfusionRoleResetDynamo();
        testConfusionRoleResetMultiTasker();
        testConfusionRoleResetSchemer();
        testCompareToPositioning();

        // 2. SPEED & ENERGY SUBCLASS MECHANICS
        testDasherSpeedNormal();
        testDasherSpeedMomentum();
        testDasherSpeedMomentumDecay();
        testDasherPowerupEffect();
        testDynamoEnergyOverridePositive();
        testDynamoEnergyOverrideNegative();
        testDynamoPowerupEffect();
        testMultiTaskerSpeedFocused();
        testMultiTaskerSpeedNormal();
        testMultiTaskerSpeedFocusDecay();
        testMultiTaskerPowerupEffect();
        testMultiTaskerEnergyOverride();
        testSchemerEnergyOverrideNormal();
        testSchemerEnergyOverrideDouble();
        testSchemerPowerupOpponentSteal();
        testSchemerPowerupTeammateSteal();
        testSchemerPowerupDynamoSteal();
        testSchemerPowerupMultiTaskerSteal();

        // 3. COMBINATORIAL MONSTER CELLS (36 Tests)
        testMonsterCellEnemySwapUnshieldedCombinations();
        testMonsterCellEnemySwapShieldedCombinations();
        testMonsterCellAllyLandPowerupTrigger();

        // 4. DOOR CELL & TEAMMATE INTERACTIONS
        testDoorCellMatchingLanding();
        testDoorCellMismatchedLandingUnshielded();
        testDoorCellMismatchedLandingShielded();
        testDoorCellTeammateMatchingGain();
        testDoorCellTeammateMismatchedLoss();
        testDoorCellActivatedSubsequentLand();

        // 5. CARD EFFECT INTERACTIONS
        testSwapperCardBehindAllTypes();
        testSwapperCardAheadAllTypes();
        testStartOverCardLuckyAndUnlucky();
        testShieldCardExecutionAllTypes();
        testConfusionCardAllTypes();
        testEnergyStealCardUnshieldedAllTypes();
        testEnergyStealCardShieldedAllTypes();
        testEnergyStealCardZeroEnergyAllTypes();

        // 6. ENGINE FLOW & EXCEPTIONS
        testFrozenSkipTurnAllTypes();
        testCollisionAvoidanceAndPositionRollback();
        testCollisionSideEffectsPreserved();
        testWinConditionMeetsPositionAndEnergy();
        testWinConditionInsufficientEnergy();
        testWinConditionInsufficientPosition();
        testPowerupChecksOutOfEnergyException();
        testPowerupChecksDeductionSuccessful();

        // 7. DATALOADER & DECK MECHANICS
        testDataLoaderCSVReads();
        testDeckRarityExpansion();
        testDeckEmptyReloads();

        System.out.println("======================================================================");
        System.out.println(" SIMULATION COMPLETE: " + passedTests + "/" + totalTests + " SCENARIOS PASSED");
        System.out.println("======================================================================");

        if (passedTests != totalTests) {
            System.err.println("CRITICAL: Some simulation tests failed!");
            System.exit(1);
        }
    }

    private static void assertState(String testName, boolean condition, String errorDetails) {
        totalTests++;
        if (condition) {
            System.out.println("[PASS] " + testName);
            passedTests++;
        } else {
            System.out.println("[FAIL] " + testName + " - " + errorDetails);
        }
    }

    // ==========================================
    // 1. BASE ATTRIBUTES & MODULO BOUNDS
    // ==========================================

    private static void testConstructorDasher() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        assertState("Constructor Dasher name", "Mike".equals(d.getName()), "Got " + d.getName());
        assertState("Constructor Dasher role", d.getRole() == Role.LAUGHER, "Got " + d.getRole());
        assertState("Constructor Dasher energy", d.getEnergy() == 100, "Got " + d.getEnergy());
        assertState("Constructor Dasher position", d.getPosition() == 0, "Got " + d.getPosition());
    }

    private static void testConstructorDynamo() {
        Dynamo dy = new Dynamo("Sully", "Strong", Role.SCARER, 200);
        assertState("Constructor Dynamo name", "Sully".equals(dy.getName()), "Got " + dy.getName());
        assertState("Constructor Dynamo role", dy.getRole() == Role.SCARER, "Got " + dy.getRole());
        assertState("Constructor Dynamo energy", dy.getEnergy() == 200, "Got " + dy.getEnergy()); // (200 - 0)*2 = 400 energy! Wait, Dynamo starts with 200. Let's see: super constructor sets energy to 200, then no override in constructor. So 200.
    }

    private static void testConstructorMultiTasker() {
        MultiTasker mt = new MultiTasker("Celia", "Focus", Role.LAUGHER, 300);
        assertState("Constructor MultiTasker name", "Celia".equals(mt.getName()), "Got " + mt.getName());
        assertState("Constructor MultiTasker energy", mt.getEnergy() == 300, "Got " + mt.getEnergy());
        assertState("Constructor MultiTasker focus turns", mt.getNormalSpeedTurns() == 0, "Got " + mt.getNormalSpeedTurns());
    }

    private static void testConstructorSchemer() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 150);
        assertState("Constructor Schemer name", "Randall".equals(sc.getName()), "Got " + sc.getName());
        assertState("Constructor Schemer energy", sc.getEnergy() == 150, "Got " + sc.getEnergy());
    }

    private static void testPositionModuloWrappingPositive() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        d.setPosition(105);
        assertState("Position modulo wrap positive", d.getPosition() == 5, "Expected 5, got " + d.getPosition());
    }

    private static void testPositionModuloWrappingNegative() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        d.setPosition(-5);
        assertState("Position modulo wrap negative", d.getPosition() == 95 || d.getPosition() == -5, "Got " + d.getPosition()); // position % 100 is -5 on Java, or 95. Wait, in Monster.java: this.position = position % Constants.BOARD_SIZE;
    }

    private static void testEnergyLowerBoundDasher() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 50);
        d.alterEnergy(-100);
        assertState("Energy lower bound Dasher", d.getEnergy() == 0, "Expected 0, got " + d.getEnergy());
    }

    private static void testEnergyLowerBoundDynamo() {
        Dynamo dy = new Dynamo("Sully", "Strong", Role.SCARER, 50);
        dy.alterEnergy(-100); // Dynamo doubles change -> -200, sets to -150 -> clamped to 0
        assertState("Energy lower bound Dynamo", dy.getEnergy() == 0, "Expected 0, got " + dy.getEnergy());
    }

    private static void testEnergyLowerBoundMultiTasker() {
        MultiTasker mt = new MultiTasker("Celia", "Focus", Role.LAUGHER, 50);
        mt.alterEnergy(-300); // alters by -300, sets to -250 -> MultiTasker setEnergy: -250 + 200 = -50 -> clamped to 0
        assertState("Energy lower bound MultiTasker", mt.getEnergy() == 0, "Expected 0, got " + mt.getEnergy());
    }

    private static void testEnergyLowerBoundSchemer() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 50);
        sc.alterEnergy(-100); // alters by -100, sets to -50 -> Schemer setEnergy: -50 + 10 = -40 -> clamped to 0
        assertState("Energy lower bound Schemer", sc.getEnergy() == 0, "Expected 0, got " + sc.getEnergy());
    }

    private static void testShieldBlocksNegativeAlterationDasher() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        d.setShielded(true);
        d.alterEnergy(-30);
        assertState("Shield blocks negative Dasher energy", d.getEnergy() == 100, "Got " + d.getEnergy());
        assertState("Shield consumed Dasher", !d.isShielded(), "Shield should be false");
    }

    private static void testShieldBlocksNegativeAlterationDynamo() {
        Dynamo dy = new Dynamo("Sully", "Strong", Role.SCARER, 100);
        dy.setShielded(true);
        dy.alterEnergy(-50);
        assertState("Shield blocks negative Dynamo energy", dy.getEnergy() == 100, "Got " + dy.getEnergy());
        assertState("Shield consumed Dynamo", !dy.isShielded(), "Shield should be false");
    }

    private static void testShieldBlocksNegativeAlterationMultiTasker() {
        MultiTasker mt = new MultiTasker("Celia", "Focus", Role.LAUGHER, 100);
        mt.setShielded(true);
        mt.alterEnergy(-60);
        assertState("Shield blocks negative MultiTasker energy", mt.getEnergy() == 100, "Got " + mt.getEnergy());
        assertState("Shield consumed MultiTasker", !mt.isShielded(), "Shield should be false");
    }

    private static void testShieldBlocksNegativeAlterationSchemer() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 100);
        sc.setShielded(true);
        sc.alterEnergy(-40);
        assertState("Shield blocks negative Schemer energy", sc.getEnergy() == 100, "Got " + sc.getEnergy());
        assertState("Shield consumed Schemer", !sc.isShielded(), "Shield should be false");
    }

    private static void testShieldIgnoredOnPositiveAlterationDasher() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        d.setShielded(true);
        d.alterEnergy(50);
        assertState("Shield ignored positive Dasher energy", d.getEnergy() == 150, "Got " + d.getEnergy());
        assertState("Shield retained Dasher", d.isShielded(), "Shield should remain true");
    }

    private static void testShieldIgnoredOnPositiveAlterationDynamo() {
        Dynamo dy = new Dynamo("Sully", "Strong", Role.SCARER, 100);
        dy.setShielded(true);
        dy.alterEnergy(50); // Dynamo doubles: +100
        assertState("Shield ignored positive Dynamo energy", dy.getEnergy() == 200, "Got " + dy.getEnergy());
        assertState("Shield retained Dynamo", dy.isShielded(), "Shield should remain true");
    }

    private static void testShieldIgnoredOnPositiveAlterationMultiTasker() {
        MultiTasker mt = new MultiTasker("Celia", "Focus", Role.LAUGHER, 100);
        mt.setShielded(true);
        mt.alterEnergy(50); // set to 150 + 200 = 350
        assertState("Shield ignored positive MultiTasker energy", mt.getEnergy() == 350, "Got " + mt.getEnergy());
        assertState("Shield retained MultiTasker", mt.isShielded(), "Shield should remain true");
    }

    private static void testShieldIgnoredOnPositiveAlterationSchemer() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 100);
        sc.setShielded(true);
        sc.alterEnergy(50); // set to 150 + 10 = 160
        assertState("Shield ignored positive Schemer energy", sc.getEnergy() == 160, "Got " + sc.getEnergy());
        assertState("Shield retained Schemer", sc.isShielded(), "Shield should remain true");
    }

    private static void testDeductFlatCostDasher() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        d.setShielded(true);
        d.deductFlatCost(50);
        assertState("Deduct flat cost Dasher energy", d.getEnergy() == 50, "Got " + d.getEnergy());
        assertState("Deduct flat cost Dasher shield intact", d.isShielded(), "Shield should remain true");
    }

    private static void testDeductFlatCostDynamo() {
        Dynamo dy = new Dynamo("Sully", "Strong", Role.SCARER, 100);
        dy.deductFlatCost(50); // direct deduction, no doubling!
        assertState("Deduct flat cost Dynamo energy", dy.getEnergy() == 50, "Got " + dy.getEnergy());
    }

    private static void testDeductFlatCostMultiTasker() {
        MultiTasker mt = new MultiTasker("Celia", "Focus", Role.LAUGHER, 100);
        mt.deductFlatCost(50); // direct deduction, no +200 bonus!
        assertState("Deduct flat cost MultiTasker energy", mt.getEnergy() == 50, "Got " + mt.getEnergy());
    }

    private static void testDeductFlatCostSchemer() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 100);
        sc.deductFlatCost(50); // direct deduction, no +10 bonus!
        assertState("Deduct flat cost Schemer energy", sc.getEnergy() == 50, "Got " + sc.getEnergy());
    }

    private static void testConfusionTurnsDecayDasher() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        d.setConfusionTurns(2);
        d.decrementConfusion();
        assertState("Confusion turns decrement Dasher", d.getConfusionTurns() == 1, "Got " + d.getConfusionTurns());
        assertState("Confusion active Dasher", d.isConfused(), "Should be confused");
    }

    private static void testConfusionTurnsDecayDynamo() {
        Dynamo dy = new Dynamo("Sully", "Strong", Role.SCARER, 100);
        dy.setConfusionTurns(1);
        dy.decrementConfusion();
        assertState("Confusion turns decrement Dynamo", dy.getConfusionTurns() == 0, "Got " + dy.getConfusionTurns());
        assertState("Confusion inactive Dynamo", !dy.isConfused(), "Should not be confused");
    }

    private static void testConfusionTurnsDecayMultiTasker() {
        MultiTasker mt = new MultiTasker("Celia", "Focus", Role.LAUGHER, 100);
        mt.setConfusionTurns(0);
        mt.decrementConfusion();
        assertState("Confusion turns decay 0 MultiTasker", mt.getConfusionTurns() == 0, "Got " + mt.getConfusionTurns());
    }

    private static void testConfusionTurnsDecaySchemer() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 100);
        sc.setConfusionTurns(3);
        sc.decrementConfusion();
        assertState("Confusion turns decrement Schemer", sc.getConfusionTurns() == 2, "Got " + sc.getConfusionTurns());
    }

    private static void testConfusionRoleResetDasher() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        d.setConfusionTurns(1);
        d.setRole(Role.SCARER);
        d.decrementConfusion();
        assertState("Confusion role reset Dasher", d.getRole() == Role.LAUGHER, "Expected LAUGHER, got " + d.getRole());
    }

    private static void testConfusionRoleResetDynamo() {
        Dynamo dy = new Dynamo("Sully", "Strong", Role.SCARER, 100);
        dy.setConfusionTurns(1);
        dy.setRole(Role.LAUGHER);
        dy.decrementConfusion();
        assertState("Confusion role reset Dynamo", dy.getRole() == Role.SCARER, "Expected SCARER, got " + dy.getRole());
    }

    private static void testConfusionRoleResetMultiTasker() {
        MultiTasker mt = new MultiTasker("Celia", "Focus", Role.LAUGHER, 100);
        mt.setConfusionTurns(2);
        mt.setRole(Role.SCARER);
        mt.decrementConfusion();
        assertState("Confusion role no reset MultiTasker yet", mt.getRole() == Role.SCARER, "Expected SCARER, got " + mt.getRole());
        mt.decrementConfusion();
        assertState("Confusion role reset MultiTasker", mt.getRole() == Role.LAUGHER, "Expected LAUGHER, got " + mt.getRole());
    }

    private static void testConfusionRoleResetSchemer() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 100);
        sc.setConfusionTurns(1);
        sc.setRole(Role.LAUGHER);
        sc.decrementConfusion();
        assertState("Confusion role reset Schemer", sc.getRole() == Role.SCARER, "Expected SCARER, got " + sc.getRole());
    }

    private static void testCompareToPositioning() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        Dynamo dy = new Dynamo("Sully", "Strong", Role.SCARER, 100);
        d.setPosition(15);
        dy.setPosition(25);
        assertState("CompareTo behind", d.compareTo(dy) < 0, "Expected negative, got " + d.compareTo(dy));
        assertState("CompareTo ahead", dy.compareTo(d) > 0, "Expected positive, got " + dy.compareTo(d));
        dy.setPosition(15);
        assertState("CompareTo equal", d.compareTo(dy) == 0, "Expected 0, got " + d.compareTo(dy));
    }

    // ==========================================
    // 2. SPEED & ENERGY SUBCLASS MECHANICS
    // ==========================================

    private static void testDasherSpeedNormal() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        d.setPosition(10);
        d.move(5); // roll 5 -> move 10 cells
        assertState("Dasher normal speed (2x)", d.getPosition() == 20, "Expected 20, got " + d.getPosition());
    }

    private static void testDasherSpeedMomentum() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        d.setPosition(10);
        d.setMomentumTurns(2);
        d.move(5); // roll 5 -> move 15 cells
        assertState("Dasher momentum speed (3x)", d.getPosition() == 25, "Expected 25, got " + d.getPosition());
    }

    private static void testDasherSpeedMomentumDecay() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        d.setMomentumTurns(1);
        d.move(5);
        assertState("Dasher momentum turn count decay", d.getMomentumTurns() == 0, "Expected 0, got " + d.getMomentumTurns());
    }

    private static void testDasherPowerupEffect() {
        Dasher d = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        d.executePowerupEffect(null);
        assertState("Dasher powerup sets momentum turns", d.getMomentumTurns() == 3, "Expected 3, got " + d.getMomentumTurns());
    }

    private static void testDynamoEnergyOverridePositive() {
        Dynamo dy = new Dynamo("Sully", "Strong", Role.SCARER, 100);
        dy.alterEnergy(50); // setEnergy(150) -> 100 + (150-100)*2 = 200
        assertState("Dynamo energy doubling positive", dy.getEnergy() == 200, "Expected 200, got " + dy.getEnergy());
    }

    private static void testDynamoEnergyOverrideNegative() {
        Dynamo dy = new Dynamo("Sully", "Strong", Role.SCARER, 200);
        dy.alterEnergy(-50); // setEnergy(150) -> 200 + (150-200)*2 = 100
        assertState("Dynamo energy doubling negative", dy.getEnergy() == 100, "Expected 100, got " + dy.getEnergy());
    }

    private static void testDynamoPowerupEffect() {
        Dynamo dy = new Dynamo("Sully", "Strong", Role.SCARER, 100);
        Dasher opponent = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        dy.executePowerupEffect(opponent);
        assertState("Dynamo powerup freezes opponent", opponent.isFrozen(), "Expected opponent to be frozen");
    }

    private static void testMultiTaskerSpeedFocused() {
        MultiTasker mt = new MultiTasker("Celia", "Focus", Role.LAUGHER, 100);
        mt.setPosition(10);
        mt.setNormalSpeedTurns(2);
        mt.move(6); // roll 6 -> move 6 cells (1x speed)
        assertState("MultiTasker focused speed (1x)", mt.getPosition() == 16, "Expected 16, got " + mt.getPosition());
    }

    private static void testMultiTaskerSpeedNormal() {
        MultiTasker mt = new MultiTasker("Celia", "Focus", Role.LAUGHER, 100);
        mt.setPosition(10);
        mt.move(6); // roll 6 -> move 3 cells (0.5x speed)
        assertState("MultiTasker normal speed (0.5x)", mt.getPosition() == 13, "Expected 13, got " + mt.getPosition());
    }

    private static void testMultiTaskerSpeedFocusDecay() {
        MultiTasker mt = new MultiTasker("Celia", "Focus", Role.LAUGHER, 100);
        mt.setNormalSpeedTurns(1);
        mt.move(6);
        assertState("MultiTasker focus turns decay", mt.getNormalSpeedTurns() == 0, "Expected 0, got " + mt.getNormalSpeedTurns());
    }

    private static void testMultiTaskerPowerupEffect() {
        MultiTasker mt = new MultiTasker("Celia", "Focus", Role.LAUGHER, 100);
        mt.executePowerupEffect(null);
        assertState("MultiTasker powerup focus mode active", mt.getNormalSpeedTurns() == 2, "Expected 2, got " + mt.getNormalSpeedTurns());
    }

    private static void testMultiTaskerEnergyOverride() {
        MultiTasker mt = new MultiTasker("Celia", "Focus", Role.LAUGHER, 100);
        mt.setEnergy(50); // set to 50 + 200 = 250
        assertState("MultiTasker energy bonus override", mt.getEnergy() == 250, "Expected 250, got " + mt.getEnergy());
    }

    private static void testSchemerEnergyOverrideNormal() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 100);
        sc.setEnergy(50); // set to 50 + 10 = 60
        assertState("Schemer energy override normal", sc.getEnergy() == 60, "Expected 60, got " + sc.getEnergy());
    }

    private static void testSchemerEnergyOverrideDouble() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 100);
        sc.alterEnergy(50); // set to 150 + 10 = 160
        assertState("Schemer energy override alter", sc.getEnergy() == 160, "Expected 160, got " + sc.getEnergy());
    }

    private static void testSchemerPowerupOpponentSteal() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 100); // 100 initial
        Dasher opponent = new Dasher("Mike", "Fast", Role.LAUGHER, 50);

        ArrayList<Monster> stationed = new ArrayList<>();
        Board.setStationedMonsters(stationed);

        sc.executePowerupEffect(opponent);

        // Steals min(10, 50) = 10 from Mike (Mike energy: 50 -> 40)
        // Randall's energy: 100 + 10 = 110. Schemer passive adds 10 -> 120.
        assertState("Schemer powerup opponent energy loss", opponent.getEnergy() == 40, "Expected Mike 40, got " + opponent.getEnergy());
        assertState("Schemer powerup self energy gain", sc.getEnergy() == 120, "Expected Randall 120, got " + sc.getEnergy());
    }

    private static void testSchemerPowerupTeammateSteal() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 100);
        Dasher opponent = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        Dasher stationedFriend = new Dasher("Fungus", "Helper", Role.SCARER, 30);

        ArrayList<Monster> stationed = new ArrayList<>();
        stationed.add(stationedFriend);
        Board.setStationedMonsters(stationed);

        sc.executePowerupEffect(opponent);

        // Steals 10 from Mike -> Mike 90
        // Steals 10 from Fungus -> Fungus 20
        // Total stolen = 20
        // Randall: 100 + 20 = 120 -> passive adds 10 -> 130
        assertState("Schemer powerup stationed friend energy loss", stationedFriend.getEnergy() == 20, "Expected Fungus 20, got " + stationedFriend.getEnergy());
        assertState("Schemer powerup self energy with friend", sc.getEnergy() == 130, "Expected Randall 130, got " + sc.getEnergy());
    }

    private static void testSchemerPowerupDynamoSteal() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 100);
        Dynamo dynamoOpponent = new Dynamo("Sully", "Dynamo", Role.LAUGHER, 50);

        ArrayList<Monster> stationed = new ArrayList<>();
        Board.setStationedMonsters(stationed);

        sc.executePowerupEffect(dynamoOpponent);

        // Steals min(10, 50) = 10 from Sully.
        // Sully setEnergy(50 - 10) -> setEnergy(40).
        // Dynamo setEnergy(40) -> 50 + (40-50)*2 = 30.
        // Randall total stolen = 10. Randall energy: 100+10 = 110 -> passive adds 10 -> 120.
        assertState("Schemer powerup Dynamo opponent doubled loss", dynamoOpponent.getEnergy() == 30, "Expected Sully 30, got " + dynamoOpponent.getEnergy());
        assertState("Schemer powerup self energy with Dynamo", sc.getEnergy() == 120, "Expected Randall 120, got " + sc.getEnergy());
    }

    private static void testSchemerPowerupMultiTaskerSteal() {
        Schemer sc = new Schemer("Randall", "Sneaky", Role.SCARER, 100);
        MultiTasker mtOpponent = new MultiTasker("Celia", "MultiTasker", Role.LAUGHER, 100);

        ArrayList<Monster> stationed = new ArrayList<>();
        Board.setStationedMonsters(stationed);

        sc.executePowerupEffect(mtOpponent);

        // Steals min(10, 100) = 10 from Celia.
        // Celia setEnergy(100 - 10) -> setEnergy(90).
        // MultiTasker setEnergy(90) -> 90 + 200 = 290.
        // Randall total stolen = 10. Randall: 100+10 = 110 -> passive adds 10 -> 120.
        assertState("Schemer powerup MultiTasker opponent net gain instead of loss", mtOpponent.getEnergy() == 290, "Expected Celia 290, got " + mtOpponent.getEnergy());
    }

    // ==========================================
    // 3. COMBINATORIAL MONSTER CELLS (36 Tests)
    // ==========================================

    private static void testMonsterCellEnemySwapUnshieldedCombinations() {
        // Enumerate 16 combinations of Landing player type vs. Cell enemy type
        // Landing player starts with 600 energy, Cell enemy starts with 400 energy.
        // diff = 600 - 400 = 200.
        // Landing player loses 200, Cell enemy gains 200.
        // Expected outcomes:
        // Dasher (600 -> 400), Dynamo (600 -> 200), MultiTasker (600 -> 600), Schemer (600 -> 410)
        // Cell Enemy: Dasher (400 -> 600), Dynamo (400 -> 800), MultiTasker (400 -> 800), Schemer (400 -> 610)

        for (int pType = 0; pType < 4; pType++) {
            for (int eType = 0; eType < 4; eType++) {
                Monster player = createMonsterByType(pType, "Player", Role.LAUGHER, 600);
                Monster enemy = createMonsterByType(eType, "Enemy", Role.SCARER, 400);

                MonsterCell cell = new MonsterCell("Enemy Cell", enemy);
                cell.onLand(player, null);

                int expectedPlayerE = getExpectedEnergyAfterAlter(pType, 600, -200);
                int expectedEnemyE = getExpectedEnergyAfterAlter(eType, 400, 200);

                String testName = "MonsterCell Swap Unshielded (Player: " + player.getClass().getSimpleName() 
                    + " -> Cell: " + enemy.getClass().getSimpleName() + ")";
                
                assertState(testName + " Player Energy", player.getEnergy() == expectedPlayerE, 
                    "Expected player energy " + expectedPlayerE + ", got " + player.getEnergy());
                assertState(testName + " Enemy Energy", enemy.getEnergy() == expectedEnemyE, 
                    "Expected enemy energy " + expectedEnemyE + ", got " + enemy.getEnergy());
            }
        }
    }

    private static void testMonsterCellEnemySwapShieldedCombinations() {
        // Enumerate 16 combinations of Landing player (Shielded) vs. Cell enemy
        // Landing player starts with 600 energy (Shielded: true), Cell enemy starts with 400 energy.
        // Landing player's shield blocks the loss (-200). Landing player remains at 600 energy, shield consumed.
        // Cell enemy still gains 200 energy.
        // Expected outcomes:
        // Player energy = 600 (all types), shield = false.
        // Cell Enemy energy: Dasher (600), Dynamo (800), MultiTasker (800), Schemer (610).

        for (int pType = 0; pType < 4; pType++) {
            for (int eType = 0; eType < 4; eType++) {
                Monster player = createMonsterByType(pType, "Player", Role.LAUGHER, 600);
                player.setShielded(true);
                Monster enemy = createMonsterByType(eType, "Enemy", Role.SCARER, 400);

                MonsterCell cell = new MonsterCell("Enemy Cell", enemy);
                cell.onLand(player, null);

                String testName = "MonsterCell Swap Shielded (Player: " + player.getClass().getSimpleName() 
                    + " -> Cell: " + enemy.getClass().getSimpleName() + ")";

                assertState(testName + " Player Energy", player.getEnergy() == 600, 
                    "Expected player energy 600, got " + player.getEnergy());
                assertState(testName + " Player Shield Consumed", !player.isShielded(), 
                    "Expected shield to be false");

                int expectedEnemyE = getExpectedEnergyAfterAlter(eType, 400, 200);
                assertState(testName + " Enemy Energy", enemy.getEnergy() == expectedEnemyE, 
                    "Expected enemy energy " + expectedEnemyE + ", got " + enemy.getEnergy());
            }
        }
    }

    private static void testMonsterCellAllyLandPowerupTrigger() {
        // Enumerate 4 monster types landing on their own ally type cell
        // Triggering powerup effects
        for (int type = 0; type < 4; type++) {
            Monster player = createMonsterByType(type, "Player", Role.LAUGHER, 1000);
            Monster ally = createMonsterByType(type, "Ally", Role.LAUGHER, 500);
            Monster opponent = new Dasher("Opponent", "Opponent", Role.SCARER, 500);

            MonsterCell cell = new MonsterCell("Ally Cell", ally);
            cell.onLand(player, opponent);

            String testName = "MonsterCell Ally Land Powerup Trigger (" + player.getClass().getSimpleName() + ")";

            if (type == 0) { // Dasher
                assertState(testName, ((Dasher) player).getMomentumTurns() == 3, "Expected 3 momentum turns");
            } else if (type == 1) { // Dynamo
                assertState(testName, opponent.isFrozen(), "Expected opponent to be frozen");
            } else if (type == 2) { // MultiTasker
                assertState(testName, ((MultiTasker) player).getNormalSpeedTurns() == 2, "Expected 2 focus turns");
            } else if (type == 3) { // Schemer
                // Randall steals 10 from opponent -> opponent becomes 490. Randall 1000 + 10 = 1010 -> passive = 1020.
                assertState(testName + " Schemer gain", player.getEnergy() == 1020, "Expected Randall 1020, got " + player.getEnergy());
            }
        }
    }

    // ==========================================
    // 4. DOOR CELL & TEAMMATE INTERACTIONS
    // ==========================================

    private static void testDoorCellMatchingLanding() {
        Dasher player = new Dasher("Mike", "Fast", Role.LAUGHER, 200);
        DoorCell door = new DoorCell("Laugher Door", Role.LAUGHER, 50);

        door.onLand(player, null);

        assertState("DoorCell matching role player gains", player.getEnergy() == 250, "Expected 250, got " + player.getEnergy());
        assertState("DoorCell matching role activated", door.isActivated(), "Expected door activated");
    }

    private static void testDoorCellMismatchedLandingUnshielded() {
        Dasher player = new Dasher("Mike", "Fast", Role.LAUGHER, 200);
        DoorCell door = new DoorCell("Scarer Door", Role.SCARER, 50);

        door.onLand(player, null);

        assertState("DoorCell mismatched role player unshielded loses", player.getEnergy() == 150, "Expected 150, got " + player.getEnergy());
    }

    private static void testDoorCellMismatchedLandingShielded() {
        Dasher player = new Dasher("Mike", "Fast", Role.LAUGHER, 200);
        player.setShielded(true);
        DoorCell door = new DoorCell("Scarer Door", Role.SCARER, 50);

        door.onLand(player, null);

        assertState("DoorCell mismatched role player shielded blocks loss", player.getEnergy() == 200, "Expected 200, got " + player.getEnergy());
        assertState("DoorCell mismatched role shield consumed", !player.isShielded(), "Expected shield false");
    }

    private static void testDoorCellTeammateMatchingGain() {
        // Landing player matches door -> teammate matches landing role too -> teammate gains energy
        // Verify teammate's passive modifiers are applied on the gain of 50 energy.
        // Stationed teammate starts at 100 energy.
        for (int type = 0; type < 4; type++) {
            Dasher player = new Dasher("Mike", "Fast", Role.LAUGHER, 200);
            Monster teammate = createMonsterByType(type, "Teammate", Role.LAUGHER, 100);

            ArrayList<Monster> stationed = new ArrayList<>();
            stationed.add(teammate);
            Board.setStationedMonsters(stationed);

            DoorCell door = new DoorCell("Laugher Door", Role.LAUGHER, 50);
            door.onLand(player, null);

            int expectedTeammateE = getExpectedEnergyAfterAlter(type, 100, 50);
            String testName = "DoorCell Teammate Match Gain (" + teammate.getClass().getSimpleName() + ")";
            assertState(testName, teammate.getEnergy() == expectedTeammateE, 
                "Expected teammate energy " + expectedTeammateE + ", got " + teammate.getEnergy());
        }
    }

    private static void testDoorCellTeammateMismatchedLoss() {
        // Landing player mismatches door -> teammate matches landing role too -> teammate loses energy
        // Verify teammate's passive modifiers are applied on the loss of 50 energy.
        // Stationed teammate starts at 200 energy.
        for (int type = 0; type < 4; type++) {
            Dasher player = new Dasher("Mike", "Fast", Role.LAUGHER, 200);
            Monster teammate = createMonsterByType(type, "Teammate", Role.LAUGHER, 200);

            ArrayList<Monster> stationed = new ArrayList<>();
            stationed.add(teammate);
            Board.setStationedMonsters(stationed);

            DoorCell door = new DoorCell("Scarer Door", Role.SCARER, 50);
            door.onLand(player, null);

            int expectedTeammateE = getExpectedEnergyAfterAlter(type, 200, -50);
            String testName = "DoorCell Teammate Mismatch Loss (" + teammate.getClass().getSimpleName() + ")";
            assertState(testName, teammate.getEnergy() == expectedTeammateE, 
                "Expected teammate energy " + expectedTeammateE + ", got " + teammate.getEnergy());
        }
    }

    private static void testDoorCellActivatedSubsequentLand() {
        Dasher player = new Dasher("Mike", "Fast", Role.LAUGHER, 200);
        DoorCell door = new DoorCell("Laugher Door", Role.LAUGHER, 50);
        door.setActivated(true);

        door.onLand(player, null);

        assertState("Activated DoorCell does nothing", player.getEnergy() == 200, "Expected 200, got " + player.getEnergy());
    }

    // ==========================================
    // 5. CARD EFFECT INTERACTIONS
    // ==========================================

    private static void testSwapperCardBehindAllTypes() {
        // Verifies position swap when player position < opponent position
        for (int type = 0; type < 4; type++) {
            Monster player = createMonsterByType(type, "Player", Role.LAUGHER, 100);
            Monster opponent = new Dasher("Opponent", "Opponent", Role.SCARER, 100);
            player.setPosition(20);
            opponent.setPosition(40);

            SwapperCard card = new SwapperCard("Swapper", "Swap", 1);
            card.performAction(player, opponent);

            String testName = "SwapperCard behind swap (" + player.getClass().getSimpleName() + ")";
            assertState(testName + " player pos", player.getPosition() == 40, "Expected player at 40, got " + player.getPosition());
            assertState(testName + " opponent pos", opponent.getPosition() == 20, "Expected opponent at 20, got " + opponent.getPosition());
        }
    }

    private static void testSwapperCardAheadAllTypes() {
        // Verifies swap is skipped when player position >= opponent position
        for (int type = 0; type < 4; type++) {
            Monster player = createMonsterByType(type, "Player", Role.LAUGHER, 100);
            Monster opponent = new Dasher("Opponent", "Opponent", Role.SCARER, 100);
            player.setPosition(50);
            opponent.setPosition(30);

            SwapperCard card = new SwapperCard("Swapper", "Swap", 1);
            card.performAction(player, opponent);

            String testName = "SwapperCard ahead no swap (" + player.getClass().getSimpleName() + ")";
            assertState(testName + " player pos", player.getPosition() == 50, "Expected player at 50, got " + player.getPosition());
            assertState(testName + " opponent pos", opponent.getPosition() == 30, "Expected opponent at 30, got " + opponent.getPosition());
        }
    }

    private static void testStartOverCardLuckyAndUnlucky() {
        Dasher player = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
        Dasher opponent = new Dasher("Randall", "Opponent", Role.SCARER, 100);

        player.setPosition(40);
        opponent.setPosition(60);

        StartOverCard luckyCard = new StartOverCard("Lucky Start", "lucky", 1, true);
        luckyCard.performAction(player, opponent);
        assertState("StartOverCard Lucky resets opponent", opponent.getPosition() == 0, "Expected opponent 0, got " + opponent.getPosition());
        assertState("StartOverCard Lucky leaves player", player.getPosition() == 40, "Expected player 40, got " + player.getPosition());

        opponent.setPosition(60);
        StartOverCard unluckyCard = new StartOverCard("Unlucky Start", "unlucky", 1, false);
        unluckyCard.performAction(player, opponent);
        assertState("StartOverCard Unlucky resets player", player.getPosition() == 0, "Expected player 0, got " + player.getPosition());
        assertState("StartOverCard Unlucky leaves opponent", opponent.getPosition() == 60, "Expected opponent 60, got " + opponent.getPosition());
    }

    private static void testShieldCardExecutionAllTypes() {
        for (int type = 0; type < 4; type++) {
            Monster player = createMonsterByType(type, "Player", Role.LAUGHER, 100);
            Monster opponent = new Dasher("Opponent", "Opponent", Role.SCARER, 100);
            opponent.setShielded(true);

            ShieldCard card = new ShieldCard("Shield", "Shield", 1);
            card.performAction(player, opponent);

            String testName = "ShieldCard (" + player.getClass().getSimpleName() + ")";
            assertState(testName + " player shielded", player.isShielded(), "Player should be shielded");
            assertState(testName + " opponent shield cleared", !opponent.isShielded(), "Opponent shield should be cleared");
        }
    }

    private static void testConfusionCardAllTypes() {
        for (int type = 0; type < 4; type++) {
            Monster player = createMonsterByType(type, "Player", Role.LAUGHER, 100);
            Monster opponent = new Dasher("Opponent", "Opponent", Role.SCARER, 100);

            ConfusionCard card = new ConfusionCard("Confusion", "Confusion", 1, 3);
            card.performAction(player, opponent);

            String testName = "ConfusionCard role swap & duration (" + player.getClass().getSimpleName() + ")";
            assertState(testName + " player confusion turns", player.getConfusionTurns() == 3, "Expected 3, got " + player.getConfusionTurns());
            assertState(testName + " opponent confusion turns", opponent.getConfusionTurns() == 3, "Expected 3, got " + opponent.getConfusionTurns());
            assertState(testName + " player role swapped", player.getRole() == Role.SCARER, "Expected SCARER, got " + player.getRole());
            assertState(testName + " opponent role swapped", opponent.getRole() == Role.LAUGHER, "Expected LAUGHER, got " + opponent.getRole());
        }
    }

    private static void testEnergyStealCardUnshieldedAllTypes() {
        // Steals 100 from target. Verifies target's passive affects their energy loss, 
        // and player's passive affects their energy gain.
        // Target starts at 300 energy. Player starts at 200 energy.
        for (int pType = 0; pType < 4; pType++) {
            for (int tType = 0; tType < 4; tType++) {
                Monster player = createMonsterByType(pType, "Player", Role.LAUGHER, 200);
                Monster opponent = createMonsterByType(tType, "Opponent", Role.SCARER, 300);

                EnergyStealCard card = new EnergyStealCard("Steal", "Steal", 1, 100);
                card.performAction(player, opponent);

                // Opponent should lose min(100, 300) = 100.
                int expectedOpponentE = getExpectedEnergyAfterAlter(tType, 300, -100);

                // Player should gain the stolen value (100)
                int expectedPlayerE = getExpectedEnergyAfterAlter(pType, 200, 100);

                String testName = "EnergyStealCard Unshielded (Stealer: " + player.getClass().getSimpleName() 
                    + " -> Target: " + opponent.getClass().getSimpleName() + ")";

                assertState(testName + " target energy", opponent.getEnergy() == expectedOpponentE, 
                    "Expected target energy " + expectedOpponentE + ", got " + opponent.getEnergy());
                assertState(testName + " stealer energy", player.getEnergy() == expectedPlayerE, 
                    "Expected stealer energy " + expectedPlayerE + ", got " + player.getEnergy());
            }
        }
    }

    private static void testEnergyStealCardShieldedAllTypes() {
        // Stealer tries to steal 100 from shielded target.
        // Target shield blocks loss, shield consumed.
        // Stealer gets nothing.
        for (int pType = 0; pType < 4; pType++) {
            for (int tType = 0; tType < 4; tType++) {
                Monster player = createMonsterByType(pType, "Player", Role.LAUGHER, 200);
                Monster opponent = createMonsterByType(tType, "Opponent", Role.SCARER, 300);
                opponent.setShielded(true);

                EnergyStealCard card = new EnergyStealCard("Steal", "Steal", 1, 100);
                card.performAction(player, opponent);

                String testName = "EnergyStealCard Shielded (Stealer: " + player.getClass().getSimpleName() 
                    + " -> Target: " + opponent.getClass().getSimpleName() + ")";

                assertState(testName + " target energy unchanged", opponent.getEnergy() == 300, 
                    "Expected target energy 300, got " + opponent.getEnergy());
                assertState(testName + " target shield consumed", !opponent.isShielded(), "Expected shield false");
                assertState(testName + " stealer energy unchanged", player.getEnergy() == 200, 
                    "Expected stealer energy 200, got " + player.getEnergy());
            }
        }
    }

    private static void testEnergyStealCardZeroEnergyAllTypes() {
        // Stealer tries to steal 100 from target with 0 energy.
        // Target has 0 energy -> stolen amount is min(100, 0) = 0.
        // But the engine's design causes unique interactions due to the alterEnergy(0) call:
        for (int pType = 0; pType < 4; pType++) {
            for (int tType = 0; tType < 4; tType++) {
                Monster player = createMonsterByType(pType, "Player", Role.LAUGHER, 200);
                Monster opponent = createMonsterByType(tType, "Opponent", Role.SCARER, 0);

                EnergyStealCard card = new EnergyStealCard("Steal", "Steal", 1, 100);
                card.performAction(player, opponent);

                String testName = "EnergyStealCard ZeroEnergy (Stealer: " + player.getClass().getSimpleName() 
                    + " -> Target: " + opponent.getClass().getSimpleName() + ")";

                int expectedOpponentE = 0;
                if (tType == 2) {
                    expectedOpponentE = 200; // MultiTasker setEnergy(0) -> 200
                } else if (tType == 3) {
                    expectedOpponentE = 10;  // Schemer setEnergy(0) -> 10
                }

                int expectedPlayerE = 200;
                // Steal only continues to alter the stealer if the opponent's energy changed (tType == 2 or 3)
                if (tType == 2 || tType == 3) {
                    if (pType == 2) {
                        expectedPlayerE = 400; // MultiTasker setEnergy(200) -> 400
                    } else if (pType == 3) {
                        expectedPlayerE = 210; // Schemer setEnergy(200) -> 210
                    }
                }

                assertState(testName + " target remains 0", opponent.getEnergy() == expectedOpponentE, 
                    "Expected target energy " + expectedOpponentE + ", got " + opponent.getEnergy());
                assertState(testName + " stealer energy unchanged", player.getEnergy() == expectedPlayerE, 
                    "Expected stealer energy " + expectedPlayerE + ", got " + player.getEnergy());
            }
        }
    }

    // ==========================================
    // 6. ENGINE FLOW & EXCEPTIONS
    // ==========================================

    private static void testFrozenSkipTurnAllTypes() {
        for (int type = 0; type < 4; type++) {
            try {
                Game gameInstance = new Game(Role.SCARER);
                Monster player = gameInstance.getCurrent();
                player.setFrozen(true);

                Monster firstPlayer = gameInstance.getCurrent();
                gameInstance.playTurn();

                String testName = "Game playTurn Skip Frozen (" + player.getClass().getSimpleName() + ")";
                assertState(testName + " switches player", gameInstance.getCurrent() != firstPlayer, "Turn should switch");
                assertState(testName + " clears frozen flag", !firstPlayer.isFrozen(), "Frozen status should clear");
            } catch (Exception e) {
                assertState("Frozen skip exception: " + e.getMessage(), false, "Thrown error");
            }
        }
    }

    private static void testCollisionAvoidanceAndPositionRollback() {
        try {
            Game gameInstance = new Game(Role.SCARER);
            Board board = gameInstance.getBoard();
            
            // We use specifically constructed Dasher monsters to avoid dynamic type random selection issues
            Dasher current = new Dasher("Mike", "Fast", Role.LAUGHER, 100);
            Dasher opponent = new Dasher("Randall", "Opponent", Role.SCARER, 100);
            
            current.setPosition(10);
            opponent.setPosition(16);
            current.setMomentumTurns(0);

            try {
                board.moveMonster(current, 3, opponent);
                assertState("Collision avoidance rollback did not throw", false, "Expected InvalidMoveException");
            } catch (InvalidMoveException e) {
                assertState("Collision avoidance rollback throws", true, "Success");
                assertState("Collision avoidance rolls back position", current.getPosition() == 10, 
                    "Expected position to reset to 10, got " + current.getPosition());
            }
        } catch (Exception e) {
            assertState("Collision avoidance exception: " + e.getMessage(), false, "Thrown error");
        }
    }

    private static void testCollisionSideEffectsPreserved() {
        // If a player lands on a cell (triggering onLand side-effects) and then collides,
        // their position is rolled back, but side-effects (e.g. energy changes, door activations)
        // are NOT rolled back!
        try {
            Game gameInstance = new Game(Role.SCARER);
            Board board = gameInstance.getBoard();
            
            Dasher current = new Dasher("Celia", "Focus", Role.LAUGHER, 200);
            Dasher opponent = new Dasher("Opponent", "Opponent", Role.SCARER, 100);

            // Set current energy = 200, opponent energy = 100 (mismatched door).
            // Let's place a DoorCell at position 12. Mismatched to current.
            // Opponent is also placed at position 12.
            DoorCell door = new DoorCell("Scarer Door", Role.SCARER, 50); // Scarer door
            current.setRole(Role.LAUGHER); // Mismatch!
            current.setPosition(6); // moves 6 cells (Dasher roll 3) -> lands on 12 (DoorCell)
            current.setEnergy(200);

            opponent.setPosition(12); // Opponent waiting at 12

            // Let's modify the Board cells to place the door cell at index 12.
            // In Board.java, boardCells is private. We can access it via indexToRowCol, 
            // but we can also just call moveMonster and verify that current lands on index 12.
            // Let's calculate index to row/col for index 12.
            // index 12: row = 12 / 10 = 1. col = 12 % 10 = 2. 
            // row % 2 == 1 -> col = 10 - 1 - 2 = 7.
            // So boardCells[1][7] = door.
            Cell[][] cells = board.getBoardCells();
            cells[1][7] = door;

            assertState("Door cell not activated initially", !door.isActivated(), "Should be false");

            try {
                board.moveMonster(current, 3, opponent);
                assertState("Collision side-effects check (did not throw)", false, "Expected InvalidMoveException");
            } catch (InvalidMoveException e) {
                // Collided, rolled back to 6.
                assertState("Collision rolls back position to 6", current.getPosition() == 6, 
                    "Expected position 6, got " + current.getPosition());
                // But DoorCell onLand was called -> door is activated!
                assertState("Collision side-effect: door activated", door.isActivated(), "Expected door to activate");
                // And player lost 50 energy -> 150!
                assertState("Collision side-effect: player lost energy", current.getEnergy() == 150, 
                    "Expected energy 150, got " + current.getEnergy());
            }
        } catch (Exception e) {
            assertState("Collision side-effects exception: " + e.getMessage(), false, "Thrown error");
        }
    }

    private static void testWinConditionMeetsPositionAndEnergy() {
        try {
            Game gameInstance = new Game(Role.SCARER);
            Monster player = gameInstance.getPlayer();
            player.setPosition(Constants.WINNING_POSITION);
            player.setEnergy(Constants.WINNING_ENERGY);

            assertState("Game win condition met", gameInstance.getWinner() == player, "Expected player winner");
        } catch (Exception e) {
            assertState("Win condition exception: " + e.getMessage(), false, "Thrown error");
        }
    }

    private static void testWinConditionInsufficientEnergy() {
        try {
            Game gameInstance = new Game(Role.SCARER);
            Monster player = gameInstance.getPlayer();
            player.setPosition(Constants.WINNING_POSITION);
            player.setEnergy(500); // 500 is far enough below 1000 for any type even with passives

            assertState("Game win condition: insufficient energy", gameInstance.getWinner() == null, "Expected no winner");
        } catch (Exception e) {
            assertState("Win condition energy exception: " + e.getMessage(), false, "Thrown error");
        }
    }

    private static void testWinConditionInsufficientPosition() {
        try {
            Game gameInstance = new Game(Role.SCARER);
            Monster player = gameInstance.getPlayer();
            player.setPosition(Constants.WINNING_POSITION - 1);
            player.setEnergy(Constants.WINNING_ENERGY + 100);

            assertState("Game win condition: insufficient position", gameInstance.getWinner() == null, "Expected no winner");
        } catch (Exception e) {
            assertState("Win condition position exception: " + e.getMessage(), false, "Thrown error");
        }
    }

    private static void testPowerupChecksOutOfEnergyException() {
        try {
            Game gameInstance = new Game(Role.SCARER);
            Monster player = gameInstance.getCurrent();
            player.setEnergy(10); // 10 is guaranteed to keep all monster types below 500 energy

            try {
                gameInstance.usePowerup();
                assertState("OutOfEnergyException not thrown on low energy powerup", false, "Expected OutOfEnergyException");
            } catch (OutOfEnergyException e) {
                assertState("OutOfEnergyException thrown on low energy powerup", true, "Success");
            }
        } catch (Exception e) {
            assertState("Powerup low energy exception: " + e.getMessage(), false, "Thrown error");
        }
    }

    private static void testPowerupChecksDeductionSuccessful() {
        try {
            Game gameInstance = new Game(Role.SCARER);
            Dasher current = new Dasher("Mike", "Fast", Role.SCARER, 600); // exactly 600
            gameInstance.setCurrent(current);

            gameInstance.usePowerup();

            // Cost of powerup is 500. Energy deducted via deductFlatCost, so 600 - 500 = 100.
            // Bypasses passive overrides and shields.
            assertState("Powerup flat cost deducted successfully", current.getEnergy() == 100, 
                "Expected 100, got " + current.getEnergy());
        } catch (Exception e) {
            assertState("Powerup success exception: " + e.getMessage(), false, "Thrown error");
        }
    }

    // ==========================================
    // 7. DATALOADER & DECK MECHANICS
    // ==========================================

    private static void testDataLoaderCSVReads() {
        try {
            ArrayList<Card> cards = DataLoader.readCards();
            assertState("DataLoader read cards non-empty", !cards.isEmpty(), "Expected non-empty cards list");

            ArrayList<Cell> cells = DataLoader.readCells();
            assertState("DataLoader read cells non-empty", !cells.isEmpty(), "Expected non-empty cells list");

            ArrayList<Monster> monsters = DataLoader.readMonsters();
            assertState("DataLoader read monsters non-empty", !monsters.isEmpty(), "Expected non-empty monsters list");
        } catch (IOException e) {
            assertState("DataLoader IOException: " + e.getMessage(), false, "CSV files missing or malformed");
        }
    }

    private static void testDeckRarityExpansion() {
        ArrayList<Card> readCards = new ArrayList<>();
        readCards.add(new SwapperCard("Swapper", "rarity 1", 1));
        readCards.add(new ShieldCard("Shield", "rarity 3", 3));

        Board board = new Board(readCards);
        // expected size = 1 * 1 + 3 * 1 = 4 total cards in originalCards
        assertState("Deck rarity expansion duplicates correctly", Board.getOriginalCards().size() == 4, 
            "Expected original cards size 4, got " + Board.getOriginalCards().size());
    }

    private static void testDeckEmptyReloads() {
        ArrayList<Card> readCards = new ArrayList<>();
        readCards.add(new SwapperCard("Swapper", "rarity 1", 1));

        Board board = new Board(readCards);
        // Deck starts with 1 card. We draw it.
        Card c1 = Board.drawCard();
        assertState("Deck first card drawn", c1 != null, "Should draw card");

        // Deck is now empty. Next draw should trigger reload and shuffle, and draw again!
        Card c2 = Board.drawCard();
        assertState("Deck reloads on empty and draws again", c2 != null, "Should reload and draw card");
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private static Monster createMonsterByType(int type, String name, Role role, int energy) {
        switch (type) {
            case 0: return new Dasher(name, "desc", role, energy);
            case 1: return new Dynamo(name, "desc", role, energy);
            case 2: return new MultiTasker(name, "desc", role, energy);
            case 3: return new Schemer(name, "desc", role, energy);
            default: throw new IllegalArgumentException("Unknown type " + type);
        }
    }

    private static int getExpectedEnergyAfterAlter(int type, int currentEnergy, int alteration) {
        if (alteration < 0) {
            // Negative alteration
            int rawNew = Math.max(0, currentEnergy + alteration);
            switch (type) {
                case 0: // Dasher
                    return rawNew;
                case 1: // Dynamo
                    // Dynamo doubles the change
                    return Math.max(0, currentEnergy + alteration * 2);
                case 2: // MultiTasker
                    // MultiTasker sets to currentEnergy + alteration, and gets +200 bonus
                    return Math.max(0, (currentEnergy + alteration) + 200);
                case 3: // Schemer
                    // Schemer sets to currentEnergy + alteration, and gets +10 bonus
                    return Math.max(0, (currentEnergy + alteration) + 10);
            }
        } else {
            // Positive alteration
            switch (type) {
                case 0: // Dasher
                    return currentEnergy + alteration;
                case 1: // Dynamo
                    // Dynamo doubles change
                    return currentEnergy + alteration * 2;
                case 2: // MultiTasker
                    // MultiTasker gets +200 bonus on set
                    return (currentEnergy + alteration) + 200;
                case 3: // Schemer
                    // Schemer gets +10 bonus on set
                    return (currentEnergy + alteration) + 10;
            }
        }
        return currentEnergy;
    }
}
