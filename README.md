# Strategic Yut Online (2 vs 2) - 콘솔 네트워크 게임
네트워크로 4명이 접속해 2:2 팀전으로 즐기는 **전략 윷놀이**입니다.</br>
*더 지니어스 게임*에서 룰을 차용하였으며, 온라인으로 친구와 함께 즐길 수 있는 콘솔 게임입니다.</br>
각 플레이어가 동시에 윷의 앞/뒤를 선택해 결과를 만들어 윷놀이를 진행하며, 같은 팀끼리 **팀 채팅**으로 전략을 세웁니다.

---

## 핵심 규칙 요약
- **말/보드**: 팀당 말 2개, MVP 보드는 선형 20칸(0~20, 20 도착)
- **던지기(동시 선택)**: 4명이 동시에 앞/뒤 선택 -> 합산하여 결과 산출
  - 앞면 개수: 0=모(5), **1=백도(-1)**, 2=개(2), 3=걸(3), 4=윷(4)
- **추가 던지기**:
  - **윷/모**: 즉시 결과 토큰을 쌓고 한 번 더 선택(연속 던지기)
  - **잡기**: 이동 중 상대 말을 잡으면 즉시 결과 토큰을 쌓고 한 번 더 선택
- **업기**: 같은 팀 말은 한 칸에서 합쳐져 함께 이동(잡히면 함께 시작점으로)
- **승리 조건**: 팀의 **두 말 모두** 완주 시 승리

---

## 턴 진행(간단 흐름)
1. **CHOOSE**: 4명 동시 앞/뒤 선택(타임리밋, 미응답 랜덤)
2. **RESULT**: 결과 산출, 윷/모면 토큰 누적 후 다시 CHOOSE 반복
3. **ALLOCATE**: 팀은 보유 토큰들을 **원하는 순서/말**에 배치해 이동
   - 이동 중 **잡기** 발생 시 -> 즉시 토큰 +1 생성 -> 이번 턴에 계속 사용
4. 토큰을 모두 소진하면 턴 종료 -> 상대 팀 차례

---

## 팀 채팅
- `/t <msg>`: 팀 채팅(같은 팀 2명만 수신)

---

## 프로젝트 구조
```yaml
strategic-yut/
 ├ server/
 | ├ ServerMain.java
 | ├ net/ #소켓/라우터
 | ├ game/ #Board/Rules/Tokens/TurnManager
 | ├ chat/ #TeamChatService
 | └ util/ #JsonCodec, IdGen
 ├ client
 | ├ ClientMain.java
 | ├ net/ #Connection
 | └ ui/ #ConsoleUI, CommandParser
 ├ common/
 | └ dto/ #메시지 DTO/Enums
 ├ tests/ #단위 테스트
 └ README.md
```

---

## 빌드 & 실행
### 요구사항
- **Java 21**
- **Gradle 8** (Wrapper 사용 예정)

### 설치
```bash

git clone https://github.com/litrod1733/strategic-yut.git
cd strategic-yut
./gradlew build
```
(터미널 1) 서버 실행
```bash

# 기본 접속 localhost:8080
./gradlew runServer

# 포트 변경 예시
./gradlew runServer -Pport=9000
```
(터미널 2~5) 클라이언트 실행
```bash

# 기본 접속 localhost:8080
./gradlew runClient

# 호스트 포트 지정
./gradlew runClient --args="localhost 8080"
```
### 콘솔 명령(초안)
로비/팀
- `/team A` 또는 `/team B` 팀 선택
- `/ready`: 준비 완료

던지기(동시 선택)
- 서버가 `CHOOSE_REQUEST`를 보내면 `front` 또는 `back` 입력

이동/상태
- `/move <pieceID> <steps>`: 말 이동(예: `/move A1 5`)
- `/done`: 이번 턴 토큰 소진 완료
- `/status`: 현재 상태/보드/토큰 확인

채팅
- `/t <message>`: 팀 채팅

## 테스트 우선순위
1. **던지기 매핑**: 0/1/2/3/4 -> 모/백도/개/걸/윷
2. **윷·모 연속**: 토큰 누적 후 CHOOSE 반복
3. **잡기 보너스**: 이동 중 잡으면 즉시 토큰 +1 생성 및 사용 가능
4. **업기/완주**: 스택 이동·잡힘·완주 경계값
5. **CHOOSE 타임아웃**: 미응답 랜덤 처리

## 네트워크 프로토콜(요약)
- C -> S: `JOIN`, `TEAM_SELECT`, `READY`, `CHOOSE`, `MOVE`, `DONE`, `CHAT_T`, `STATUS`
- S -> C: `WELCOME`, `LOBBY_STATE`, `TEAMS_FORMED`, `TURN_START`, `CHOOSE_REQUEST`, `CHOOSE_RESULT`, `TOKENS_UPDATED`, `BOARD_UPDATE`, `CAPTURED`, `NEED_ALLOCATE`, `ERROR`, `GAME_END`

예시
```
// CHOOSE
C->S {"type": "CHOOSE", "face": "front"}
S->C {"type": "CHOOSE_RESULT", "fronts": 3,"outcome": "GUL", "steps": 3}

// 이동 & 잡기
C->S {"type": "MOVE","pieceId": "A1","steps": 5}
S->C {"type": "CAPTURED","by": "A1","victim": "B2"}
S->C {"type": "TOKENS_UPDATED","tokens": [2,4,3]} // 잡기 보상으로 즉시 +1
```
