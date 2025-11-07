package server.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.game.Models.*;
import support.FixedRandomProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TurnManagerAllocateTest {

	@Test
	@DisplayName("ALLOCATE에서 토큰을 모두 소진하면 턴이 종료되고 다음 팀으로 넘어간다")
	void allocateConsumesTokensAndEndsTurn() {
		var A = new Team("A");
		var a1 = new Piece("A1", "A");
		A.pieces.add(a1);
		var B = new Team("B");
		var b1 = new Piece("B1", "B");
		B.pieces.add(b1);

		var rules = new Rules(new Board(), new FixedRandomProvider(3)); // 보상 난수: 걸(3) (여기선 캡쳐 안함)
		var tm = new TurnManager(List.of(A, B), rules, List.of("A", "B"));
		tm.startTurn();

		// CHOOSE: fronts=2 (개) -> ALLOCATE
		tm.applyChoiceFronts(2);
		assertEquals(TurnManager.Phase.ALLOCATE, tm.getPhase());
		assertEquals(List.of(2), tm.getTokens());

		// 토큰 2로 A1 이동 -> 토큰 소진 -> 턴 종료 -> 팀 B 차례 + CHOOSE 복귀
		var result = tm.allocateMove("A1", 2);
		assertFalse(result.captured());
		assertEquals(2, a1.pos);

		assertEquals("B", tm.getCurrentTeamId());
		assertEquals(TurnManager.Phase.CHOOSE, tm.getPhase());
		assertTrue(tm.getTokens().isEmpty());
	}

	@Test
	@DisplayName("이동 중 잡기 발생 시 보상 토큰이 즉시 추가되고 같은 턴에서 계속 ALLOCATE 가능")
	void captureAddsImmediateTokenAndStayInAllocate() {
		var A = new Team("A");
		var a1 = new Piece("A1", "A");
		a1.pos = 3;
		A.pieces.add(a1);
		var B = new Team("B");
		var b1 = new Piece("B1", "B");
		b1.pos = 5;
		B.pieces.add(b1);

		// 보상 fronts=4 -> 윷(4) 즉시 토큰 추가 예상
		var rules = new Rules(new Board(), new FixedRandomProvider(4));
		var tm = new TurnManager(List.of(A, B), rules, List.of("A", "B"));
		tm.startTurn();

		// CHOOSE: fronts=2 (개) -> ALLOCATE 진입, 토큰 [2]
		tm.applyChoiceFronts(2);
		assertEquals(TurnManager.Phase.ALLOCATE, tm.getPhase());
		assertEquals(List.of(2), tm.getTokens());

		// 3->5로 이동하며 B1을 잡음 -> 보상 토큰(4) 즉시 추가
		var result = tm.allocateMove("A1", 2);
		assertTrue(result.captured());
		assertEquals(5, a1.pos);
		assertEquals(0, b1.pos);

		// allocateMove가 토큰 1개를 소비했지만, 캡쳐 보상(4)이 즉시 추가되어
		// 아직 토큰이 남아 있으므로 같은 턴에서 ALLOCATE 유지 + 여전히 A의 차례
		assertEquals("A", tm.getCurrentTeamId());
		assertEquals(TurnManager.Phase.ALLOCATE, tm.getPhase());
		assertFalse(tm.getTokens().isEmpty());
	}
}
