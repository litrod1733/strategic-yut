package server.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import server.game.Models.*;
import support.FixedRandomProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TurnManagerFlowTest {

	@Test
	@DisplayName("윷/모가 나오면 토큰이 누적되고 여전히 CHOOSE 단계에 머무른다")
	void extraThrowsKeepChoosing() {
		// 팀/말 구성
		var A = new Team("A");
		A.pieces.add(new Piece("A1", "A"));
		A.pieces.add(new Piece("A2", "A"));
		var B = new Team("B");
		B.pieces.add(new Piece("B1", "B"));
		B.pieces.add(new Piece("B2", "B"));

		// Rules: 보상 난수는 고정(여기선 상관없지만 일관성 유지)
		var rules = new Rules(new Board(), new FixedRandomProvider(2)); // 보상 fronts=2 (개)

		var tm = new TurnManager(List.of(A, B), rules, List.of("A", "B"));
		tm.startTurn();
		assertEquals(TurnManager.Phase.CHOOSE, tm.getPhase());
		assertEquals("A", tm.getCurrentTeamId());

		// fronts=0 -> 모(5)
		tm.applyChoiceFronts(0);
		assertEquals(TurnManager.Phase.CHOOSE, tm.getPhase());
		assertEquals(List.of(5), tm.getTokens());

		// fronts=4 -> 윷(4)
		tm.applyChoiceFronts(4);
		assertEquals(TurnManager.Phase.CHOOSE, tm.getPhase());
		assertEquals(List.of(5, 4), tm.getTokens());

		// fronts=2 -> 개(2)
		tm.applyChoiceFronts(2);
		assertEquals(TurnManager.Phase.ALLOCATE, tm.getPhase());
		assertEquals(List.of(5, 4, 2), tm.getTokens());
	}
}