import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import server.game.Throwing;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("윷 던지기 결과 매핑 테스트")
public class ThrowingTest {

	@ParameterizedTest
	@CsvSource({
		"0,5",	// 모
		"1,-1",	// 백도
		"2,2",	// 개
		"3,3",	// 걸
		"4,4"		// 윷
	})
	@DisplayName("앞면 개수(0~4)에 따라 이동 칸 수가 올바르게 매핑된다")
	void mapFronts(int fronts, int expectedSteps) {
		assertEquals(expectedSteps, Throwing.mapFrontCount(fronts).steps);
	}
}
