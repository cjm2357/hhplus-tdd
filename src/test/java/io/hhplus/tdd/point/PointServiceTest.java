package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;


import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

//@WebMvcTest(PointService.class)
public class PointServiceTest {

    private PointService pointService;


    public PointServiceTest () {
        PointRepository pointRepository = new PointImplRepository();
        this.pointService = new PointService(pointRepository);
    }


    /**
     * Charge Test
     * 구현하기 쉬운 순서대로
     * 1. 첫 충전
     * 2. 0이하의 수 충전
     * 3. 기존 id에 충전
     * 4. 동시에 여러건의 포인트 충전, 이용 요청
     * */
    @Test
    void 첫_포인트_충전_호출 () throws Exception {
        //given
        long userId = 1;
        long amount = 100;

        //when
        UserPoint userPoint = pointService.charge(userId, amount);

        //then
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
        pointService.charge(userId, amount1);

        //when
        long amount2 = 200;
        UserPoint userPoint = pointService.charge(userId, amount2);

        //then
        assertThat(userPoint.point()).isEqualTo(amount1 + amount2);
    }
}
