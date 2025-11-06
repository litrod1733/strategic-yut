import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import server.game.*;
import server.game.Models.*;
import support.FixedRandomProvider;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

@DisplayName("Rules 테스트 - 잡기 및 보상 검증")
public class CaptureRewardTest {

	@Test
	@DisplayName("상대 말을 잡으면 즉시 토큰이 1개 추가된다")
	void captureGivesImmediateToken() {
		// 보상 fronts=4 => 윷(4칸) 고정
		var rules = new Rules(new Board(), new FixedRandomProvider(4));
		var tokens = new Tokens();

		var a = new Team("A");
		var b = new Team("B");
		var A1 = new Piece("A1", "A");
		A1.pos = 3;
		a.pieces.add(A1);
		var B1 = new Piece("B1", "B");
		B1.pos = 5;
		b.pieces.add(B1);

		var res = rules.movePiece(List.of(a, b), a, A1, 2, tokens);	// 3->5, 캡쳐
		assertTrue(res.captured(), "잡기 발생 여부");
		assertEquals("B1", res.victimId());
		assertEquals(5, A1.pos);
		assertEquals(0, B1.pos);
		assertEquals(1, tokens.size());
		assertEquals(4, tokens.asList().get(0), "보상 토큰은 윷(4칸)");
	}
}
