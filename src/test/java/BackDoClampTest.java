import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.game.*;
import server.game.Models.*;
import support.FixedRandomProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Rules 테스트 - 백도 경계 검증")
public class BackDoClampTest {

	@Test
	@DisplayName("백도 이동 . 시작점 아래로 내려가지 않는다")
	void cannotGoBelowStart() {
		var rules = new Rules(new Board(), new FixedRandomProvider(2));
		var tokens = new Tokens();
		var a = new Team("A");
		var b = new Team("B");
		var A1 = new Piece("A1", "A");
		a.pieces.add(A1);
		var B1 = new Piece("B1", "B");
		B1.pos = 10;
		b.pieces.add(B1);

		rules.movePiece(List.of(a,b), a, A1, -1, tokens);
		assertEquals(0, A1.pos);
	}
}
