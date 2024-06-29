package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class PointServiceIntegrationTest {

    private PointService pointService;
    private PointRepository pointRepository;
    private PointHistoryRepository pointHistoryRepository;

    public PointServiceIntegrationTest() {
        UserPointTable userPointTable = new UserPointTable();
        this.pointRepository = new PointImplRepository(userPointTable);
        PointHistoryTable pointHistoryTable = new PointHistoryTable();
        this.pointHistoryRepository = new PointHistoryImplRepository(pointHistoryTable);
        this.pointService = new PointService(pointRepository, pointHistoryRepository);
    }


    /**
     * Charge Integration Test
     * 구현하기 쉬운 순서대로
     * 1. 첫 충전
     * 2. 0이하의 수 충전
     * 3. 기존 id에 충전
     * 4. 동시에 여러건의 포인트 충전, 이용 요청
     * */

    //우선 test 코드 case3 까지 구현
    @Test
    void 첫_포인트_충전_호출 () throws Exception {
        //given
        long userId = 1;
        long amount = 100;

        //when
        UserPoint userPoint = pointService.charge(userId, amount);

        //then
        assertThat(userPoint.id()).isEqualTo(userId);
        assertThat(userPoint.point()).isEqualTo(amount);
    }

    @Test
    void 영이하의수_충전 () throws Exception {
        //given
        long userId = 1;
        long amount = -100;

        //when
        Throwable exception = assertThrows(Exception.class, () -> {
            pointService.charge(userId, amount);
        });

        //then
        assertEquals("0이하의 수는 충전할 수 없습니다.", exception.getMessage());
    }

    @Test
    void 기존_충전되있는_ID에_추가_충전 () throws Exception {
        //given
        long userId = 1;
        long amount1 = 100;
        long amount2 = 200;
        // 첫 충전
        pointService.charge(userId, amount1);
        
        //when
        //두번째 충전
        UserPoint userPoint = pointService.charge(userId, amount2);

        //then
        assertThat(userPoint.id()).isEqualTo(userId);
        assertThat(userPoint.point()).isEqualTo(amount1 + amount2);
    }


    /**
     * Search Test
     * 1. 일반 조회
     * 2. 충전한적 없는 유저 조회 -> 0
     * */
    @Test
    void 일반_조회 () throws Exception{
        //given
        long userId = 1;
        long amount = 100;
        pointService.charge(userId, amount);

        //when
        UserPoint userPoint = pointService.search(1);

        //then
        assertEquals(userId, userPoint.id());
        assertEquals(amount, userPoint.point());
    }

    @Test
    void 충전한적_없는_유저_조회 () throws Exception{
        //given
        long userId = 1;

        //when
        UserPoint userPoint = pointService.search(userId);

        //then
        assertEquals(userId, userPoint.id());
        assertEquals(0, userPoint.point());
    }

    /**
     * Use Test
     * 1. 일반 사용
     * 2. 음수 사용
     * 3. 충전한적 없는데 포인트 사용
     * 4. 잔여 포인트 보다 많이 사용
     * 5. 동시에 여러건의 포인트 충전, 이용 요청
     * */

    //case 1 : 일반 사용
    @Test
    void 포인트_일반_사용 () throws Exception {
        //given
        long userId = 1;
        long amount = 1000;
        long usePoint = 300;
        pointService.charge(userId, amount);

        //when
        pointService.use(userId, usePoint);
        UserPoint restPoint = pointService.search(userId);

        //then
        assertEquals(userId, restPoint.id());
        assertEquals(amount - usePoint, restPoint.point());
    }

    //case 2 : 음수 사용
    @Test
    void 포인트_음수_사용 () throws Exception {
        //given
        long userId = 1;
        long amount = 1000;
        long usePoint = -300;
        pointService.charge(userId, amount);

        //when
        Throwable exception = assertThrows(Exception.class, () -> {
            pointService.use(userId, usePoint);
        });

        //then
        assertEquals("0이하의 수는 사용할 수 없습니다.", exception.getMessage());
    }

    //case 3 :충전한적 없는데 포인트 사용
    @Test
    void 충전한적_없는데_포인트_사용 () throws Exception {
        //given
        long userId = 1;
        long usePoint = 300;

        //when
        Throwable exception = assertThrows(Exception.class, () -> {
            pointService.use(userId, usePoint);
        });

        //then
        //통합 테스트에서는 PointRepository가 0을 리턴하여 변경, 에러를 잔여 포인트로 변경
        assertEquals("잔여 포인트보다 많이 사용할 수 없습니다.", exception.getMessage());
    }

    //case 4 : 잔여 포인트보다 많이 사용
    @Test
    void 잔여_포인트_보다_많이_사용 () throws Exception {
        //given
        long userId = 1;
        long usePoint = 300;

        //when
        Throwable exception = assertThrows(Exception.class, () -> {
            pointService.use(userId, usePoint);
        });

        //then
        assertEquals("잔여 포인트보다 많이 사용할 수 없습니다.", exception.getMessage());
    }


    /**
     * History Test
     * 1. 일반 내역 조회
     * 2. 조회 리스트가 없을 때
     * 3. 시간 역순서대로 표기
     * */

    // case 1 : 일반 내역 조회
    @Test
    void 일반_내역_조회() throws Exception{
        //given
        long userId = 1;
        long amount = 100;
        pointService.charge(userId, amount);

        //when
        List<PointHistory> pointHistories = pointService.readHistories(userId);

        //then
        assertNotNull(pointHistories);
        assertFalse(pointHistories.isEmpty());

        PointHistory pointHistory = pointHistories.get(0);
        assertEquals(userId, pointHistory.userId());
        assertEquals(amount, pointHistory.amount());
        assertEquals(TransactionType.CHARGE, pointHistory.type());

    }


    // case 2 : 이력이 없을 때
    @Test
    void 이력이_없을_때() throws Exception{
        //given
        long userId = 1;

        // when
        List<PointHistory> pointHistories = pointService.readHistories(userId);

        //then
        // 통합 테스트에서 PointHistoryRepository가 이력이 없을 때 빈 list를 리턴하여 수정
        assertTrue(pointHistories.isEmpty());
    }

    // case 3 : 시간 역순서대로 표기
    @Test
    void 시간_역순서대로_표기되는지 () throws Exception {

        //given
        long userId = 1;
        long amount = 1000;
        long usePoint = 300;

        pointService.charge(userId, amount);
        pointService.use(userId, usePoint);

        //when
        List<PointHistory> pointHistories = pointService.readHistories(userId);

        //then
        assertTrue(isAfterTime(pointHistories.get(0).updateMillis(), pointHistories.get(1).updateMillis()));
    }

    @Test
    void 동시성테스트() throws Exception{
        // given
        pointService.charge(1l, 100000l);
        // when

        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> {
                    try {
                        pointService.use(1l, 10000l);
                    } catch (Exception e) {

                    }
                }),
                CompletableFuture.runAsync(() -> {
                    try {
                        pointService.charge(1l, 4000l);
                    } catch (Exception e) {

                    }
                }),
                CompletableFuture.runAsync(() -> {
                    try {
                        pointService.use(1l, 100l);
                    } catch (Exception e) {

                    }
                })
        ).join(); // 제일 오래 끝나는거 끝날떄까지 기다려줌. = 내가 비동기/병렬로 실행한 함수가 전부 끝남을 보장.


        // then
        UserPoint userPoint = pointService.search(1);
        // 수식으로 검증해서 테스트 작성자의 오류도 줄인다.
        assertThat(userPoint.point()).isEqualTo(100000 - 10000 + 4000 - 100);
    }


    private boolean isAfterTime(long time1, long time2) {
        if (time1 < time2) return false;
        return true;
    }
}
