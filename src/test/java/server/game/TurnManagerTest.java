package server.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class TurnManagerTest {

	@Test
	@DisplayName("턴이 팀 순서대로 순환되어야 한다")
	void turnsRotateInOrder() {
		TurnManager tm = new TurnManager(List.of("A", "B", "C"));

		assertEquals("A", tm.getCurrentTeamId());
		tm.nextTurn();
		assertEquals("B", tm.getCurrentTeamId());
		tm.nextTurn();
		assertEquals("C", tm.getCurrentTeamId());
		tm.nextTurn();
		assertEquals("A", tm.getCurrentTeamId());
	}

	@Test
	@DisplayName("현재 턴의 팀만 자신의 턴임을 판별할 수 있다")
	void onlyCurrentTeamHasTurn() {
		TurnManager tm = new TurnManager(List.of("A", "B"));
		assertTrue(tm.isMyTurn("A"));
		assertFalse(tm.isMyTurn("B"));

		tm.nextTurn();
		assertFalse(tm.isMyTurn("A"));
		assertTrue(tm.isMyTurn("B"));
	}
}
