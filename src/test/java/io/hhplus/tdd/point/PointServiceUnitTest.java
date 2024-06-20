package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class PointServiceUnitTest {

    @Mock
    PointRepository pointRepository;
    @Mock
    PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    PointService pointService;


    /**
     * Charge Unit Test
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
        when(pointRepository.save(userId, amount)).thenReturn(new UserPoint(userId, amount, System.currentTimeMillis()));
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
        // 충전된 포인트
        when(pointRepository.findById(userId)).thenReturn(new UserPoint(userId, amount1, System.currentTimeMillis()));
        // 충전 응답
        when(pointRepository.save(anyLong(), anyLong())).thenReturn(new UserPoint(userId, amount1 + amount2, System.currentTimeMillis()));

        //when
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
        when(pointRepository.findById(anyLong())).thenReturn(new UserPoint(userId, amount, System.currentTimeMillis()));

        //when
        UserPoint userPoint = pointService.search(userId);

        //then
        assertEquals(userId, userPoint.id());
        assertEquals(amount, userPoint.point());
    }

    @Test
    void 충전한적_없는_유저_조회 () throws Exception{
        //given
        long userId = 1;
        when(pointRepository.findById(anyLong())).thenReturn(new UserPoint(userId, 0, System.currentTimeMillis()));

        //when
        UserPoint userPoint = pointService.search(1);

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

        //when
        when(pointRepository.findById(anyLong())).thenReturn(new UserPoint(userId, amount, System.currentTimeMillis()));
        when(pointRepository.save(anyLong(), anyLong())).thenReturn(new UserPoint(userId, amount - usePoint, System.currentTimeMillis()));
        UserPoint restPoint = pointService.use(userId, usePoint);

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
        when(pointRepository.save(anyLong(), anyLong())).thenReturn(new UserPoint(userId, amount, System.currentTimeMillis()));
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
    void 충전학적_없는데_포인트_사용 () throws Exception {
        //given
        long userId = 1;
        long usePoint = 300;
        when(pointRepository.findById(anyLong())).thenReturn(null);

        //when
        Throwable exception = assertThrows(Exception.class, () -> {
            pointService.use(userId, usePoint);
        });

        //then
        assertEquals("충전된 포인트가 없습니다.", exception.getMessage());
    }

    //case 4 : 잔여 포인트보다 많이 사용
    @Test
    void 잔여_포인트_보다_많이_사용 () throws Exception {
        //given
        long userId = 1;
        long amount = 100;
        long usePoint = 300;
        when(pointRepository.findById(anyLong())).thenReturn(new UserPoint(userId, amount, System.currentTimeMillis()));

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
        List<PointHistory> expectedHistories = new ArrayList<>();
        PointHistory pointHistory = new PointHistory(1, userId, 100, TransactionType.CHARGE, System.currentTimeMillis());
        expectedHistories.add(pointHistory);
        when(pointHistoryRepository.findAllByUserId(anyLong())).thenReturn(expectedHistories);

        //when
        List<PointHistory> pointHistories = pointService.readHistories(userId);

        //then
        Boolean isSameContent = expectedHistories.equals(pointHistories);
        assertTrue(isSameContent);

    }


    // case 2 : 이력이 없을 때
    @Test
    void 이력이_없을_때() throws Exception{
        //given
        long userId = 1;
        when(pointHistoryRepository.findAllByUserId(anyLong())).thenReturn(null);

        //when
        Throwable exception = assertThrows(Exception.class, () -> {
            pointService.readHistories(userId);
        });

        //then
        assertEquals("포인트 내역이 없습니다.", exception.getMessage());
    }

    // case 3 : 시간 역순서대로 표기
    @Test
    void 시간_역순서대로_표기되는지 () throws Exception {

        //given
        long userId = 1;
        long amount = 1000;

        List<PointHistory> expectedHistories = new ArrayList<>();
        PointHistory chargeHistory = new PointHistory(1, userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory useHistory = new PointHistory(2, userId, 300, TransactionType.USE, System.currentTimeMillis() + (1000 * 60));
        expectedHistories.add(chargeHistory);
        expectedHistories.add(useHistory);

        //when
        when(pointHistoryRepository.findAllByUserId(anyLong())).thenReturn(expectedHistories);
        List<PointHistory> pointHistories = pointService.readHistories(userId);

        //then
        assertTrue(isAfterTime(pointHistories.get(0).updateMillis(), pointHistories.get(1).updateMillis()));
    }


    private boolean isAfterTime(long time1, long time2) {
        if (time1 < time2) return false;
        return true;
    }

}
