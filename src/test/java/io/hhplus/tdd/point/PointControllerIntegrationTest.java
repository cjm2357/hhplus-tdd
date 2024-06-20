package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PointControllerIntegrationTest {


    PointController pointController;

    public PointControllerIntegrationTest() {
        UserPointTable userPointTable = new UserPointTable();
        PointRepository pointRepository = new PointImplRepository(userPointTable);
        PointHistoryTable pointHistoryTable = new PointHistoryTable();
        PointHistoryRepository pointHistoryRepository = new PointHistoryImplRepository(pointHistoryTable);
        PointService pointService = new PointService(pointRepository, pointHistoryRepository);
        this.pointController = new PointController(pointService);
    }

    /**
     * Search Test
     * 1. 일반 포인트 조회
     * */
    @Test
    void 일반_포인트_조회 () throws Exception {
        //given
        long userId = 1;
        long amount = 1000;
        pointController.charge(userId, 1000);

        //when
        UserPoint userPoint = pointController.point(userId);
        //then
        assertEquals(userId, userPoint.id());
        assertEquals(amount, userPoint.point());

    }

    /**
     * History Test
     * 1. 일반 내역 조회
     * 2. 조회 내역 없을 때
     * 3. 시간 역순서대로 표기
     * */

    @Test
    void 일반_내역_조회 () throws Exception{
        //given
        long userId = 1;
        long chargePoint = 1000;
        long usePoint = 300;
        pointController.charge(userId, chargePoint);

        //when
        List<PointHistory> histories = pointController.history(userId);

        //then
        PointHistory pointHistory = histories.get(0);
        assertEquals(userId, pointHistory.userId());
        assertEquals(chargePoint, pointHistory.amount());
        assertEquals(TransactionType.CHARGE, pointHistory.type());

    }

    @Test
    void 조회_내역_없을떄 () throws Exception{
        //given
        long userId = 1;

        //when
        List<PointHistory> histories = pointController.history(userId);

        //then
        assertTrue(histories.isEmpty());

    }

    @Test
    void 시간_역순으로_표기 () throws Exception{
        //given
        long userId = 1;
        long chargePoint = 1000;
        long usePoint = 300;
        pointController.charge(userId, chargePoint);
        pointController.use(userId, usePoint);

        //when
        List<PointHistory> histories = pointController.history(userId);

        //then
        PointHistory history1 = histories.get(0);
        PointHistory history2 = histories.get(1);

        boolean isCorrectOrder = true;
        if (history1.updateMillis() < history2.updateMillis()) isCorrectOrder = false;
        assertTrue(isCorrectOrder);

    }


    /**
     * Charge Test
     * 1. 처음 충전
     * 2. 음수 충전
     * 3. 기존 id에 충전
     * 4. 동시에 여러건의 포인트 충전, 이용 요청 못함
     * */

    @Test
    void 첫_포인트_충전_호출 () throws Exception {
        //given
        long userId = 1;
        long amount = 1000;

        //when
        pointController.charge(userId, amount);

        //then
        UserPoint userPoint = pointController.point(userId);
        assertEquals(userId, userPoint.id());
        assertEquals(amount, userPoint.point());

    }

    @Test
    void 음수_충전 () throws Exception {
        //given
        long userId = 1;
        long amount = -1000;

        //when
        Throwable exception = assertThrows(Exception.class, () -> {
            pointController.charge(userId, amount);
        });

        //then
        assertEquals("0이하의 수는 충전할 수 없습니다.", exception.getMessage());

    }


    @Test
    void 기존_id에_충전 () throws Exception {
        //given
        long userId = 1;
        long amount1 = 1000;
        long amount2 = 500;
        pointController.charge(userId, amount1);

        //when
        UserPoint userPoint = pointController.charge(userId, amount2);

        //then
        assertEquals(userId, userPoint.id());
        assertEquals(amount1 + amount2, userPoint.point());

    }


    /**
     * Use Test
     * 1. 일반 사용
     * 2. 음수 사용
     * 3. 잔여 포인트 보다 많이 사용
     * 4. 동시에 여러건의 포인트 충전, 이용 요청
     * */

    @Test
    void 포인트_일반_사용 () throws Exception {
        //given
        long userId = 1;
        long amount = 1000;
        long usePoint = 300;
        pointController.charge(userId, amount);

        //when
        pointController.use(userId, usePoint);
        UserPoint restPoint = pointController.point(userId);

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
        pointController.charge(userId, amount);

        //when
        Throwable exception = assertThrows(Exception.class, () -> {
            pointController.use(userId, usePoint);
        });

        //then
        assertEquals("0이하의 수는 사용할 수 없습니다.", exception.getMessage());
    }


    //case 3 : 잔여 포인트보다 많이 사용
    @Test
    void 잔여_포인트_보다_많이_사용 () throws Exception {
        //given
        long userId = 1;
        long usePoint = 300;

        //when
        Throwable exception = assertThrows(Exception.class, () -> {
            pointController.use(userId, usePoint);
        });

        //then
        assertEquals("잔여 포인트보다 많이 사용할 수 없습니다.", exception.getMessage());
    }

}
