package io.hhplus.tdd.point;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
@ExtendWith(MockitoExtension.class)
public class PointControllerUnitTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PointService pointService;


    /**
     * Search Test
     * 1. 일반 포인트 조회
     * 2. long 형태가 아닌 id 요청 -> 실패
     * */
    @Test
    void 일반_포인트_조회 () throws Exception {
        //given
        long userId = 1;
        long amount = 1000;
        UserPoint expectedUserPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        when(pointService.search(userId)).thenReturn(expectedUserPoint);

        //when
        //then
        mvc.perform(get("/point/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(amount));

    }

    @Test
    void long_형태가_아닌_id_포인트_조회_요청 () throws Exception {
        //given
        String userId = "ABC";

        //when
        //then
        mvc.perform(get("/point/" + userId))
                .andExpect(status().is4xxClientError());
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

        PointHistory history1 = new PointHistory(1, userId, chargePoint, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory history2 = new PointHistory(2, userId, usePoint, TransactionType.USE, System.currentTimeMillis() +(60 * 1000));
        List<PointHistory> expectedHistories = new ArrayList<>();
        expectedHistories.add(history1);
        expectedHistories.add(history2);
        when(pointService.readHistories(userId)).thenReturn(expectedHistories);

        //when
        //then
        mvc.perform(get("/point/" + userId +"/histories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2))) // 리스트 크기 검증
                .andExpect(jsonPath("$[0].userId", Matchers.is(1))) // 첫 번째 userId 검증
                .andExpect(jsonPath("$[0].amount", Matchers.is(1000))) // 첫 번째 point 검증
                .andExpect(jsonPath("$[0].type", Matchers.is(TransactionType.CHARGE.name()))) // 첫 번째 type 검증
                .andExpect(jsonPath("$[1].userId", Matchers.is(1))) // 두 번째 userId 검증
                .andExpect(jsonPath("$[1].amount", Matchers.is(300))) // 두 번째 point 검증
                .andExpect(jsonPath("$[1].type", Matchers.equalTo(TransactionType.USE.name()))); // 두 번째 type 검증
    }

    @Test
    void 조회_내역_없을떄 () throws Exception{
        //given
        long userId = 1;
        List<PointHistory> expectedHistories = new ArrayList<>();
        when(pointService.readHistories(userId)).thenReturn(expectedHistories);

        //when
        //then
        mvc.perform(get("/point/" + userId +"/histories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0))); // 리스트 크기 검증
    }


    @Test
    void 시간_역순으로_표기 () throws Exception{
        //given
        long userId = 1;
        long chargePoint = 1000;
        long usePoint = 300;

        PointHistory history1 = new PointHistory(1, userId, chargePoint, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory history2 = new PointHistory(2, userId, usePoint, TransactionType.USE, System.currentTimeMillis() +(60 * 1000));
        List<PointHistory> expectedHistories = new ArrayList<>();
        expectedHistories.add(history2);
        expectedHistories.add(history1);
        when(pointService.readHistories(userId)).thenReturn(expectedHistories);

        //when
        //then
        MvcResult result = mvc.perform(get("/point/" + userId +"/histories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2))) // 리스트 크기 검증
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        DocumentContext jsonContext = JsonPath.parse(jsonResponse);
        long time1 = jsonContext.read("$[0].updateMillis", Long.class);
        long time2 = jsonContext.read("$[1].updateMillis", Long.class);

        boolean isCorrectOrder = true;
        if (time1 < time2) isCorrectOrder = false;

        assertTrue(isCorrectOrder);

    }


    /**
     * Charge Test
     * 1. 처음 충전
     * 2. 음수 충전
     * 3. 기존 id에 충전
     * 4. 동시에 여러건의 포인트 충전, 이용 요청
     * */

    @Test
    void 첫_포인트_충전_호출 () throws Exception {
        //given
        long userId = 1;
        long amount = 100;

        //여기타입이 안먹는 것 같은데 어떻게 해결하나요?
        String requestJson = "{\"amount\":" + amount + "}";
        when(pointService.charge(anyLong(),anyLong())).thenReturn(new UserPoint(userId, amount, System.currentTimeMillis()));

        //when
        //then
        mvc.perform(patch("/point/"+ userId + "/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(amount));
    }

    @Test
    void 음수_충전 () throws Exception {
        //given
        long userId = 1;
        long amount = -1000;

        //여기타입이 안먹는 것 같은데 어떻게 해결하나요?
        String requestJson = "{\"amount\":" + amount + "}";
        when(pointService.charge(anyLong(),anyLong())).thenThrow(new Exception("0이하의 수는 충전할 수 없습니다."));

        //when
        //then
        mvc.perform(patch("/point/"+ userId + "/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void 기존_id에_충전 () throws Exception {
        //given
        long userId = 1;
        long amount1 = 1000;
        long amount2 = 500;

        String requestJson = "{\"amount\":" + amount2 + "}";
        when(pointService.charge(anyLong(),anyLong())).thenReturn(new UserPoint(userId, amount1 + amount2, System.currentTimeMillis()));

        //when
        //then
        mvc.perform(patch("/point/"+ userId + "/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(amount1+amount2));

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
        String requestJson = "{\"amount\":" + usePoint + "}";
        when(pointService.use(anyLong(),anyLong())).thenReturn(new UserPoint(userId, amount-usePoint, System.currentTimeMillis()));

        //when
        //then
        mvc.perform(patch("/point/"+ userId + "/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(amount-usePoint));
    }

    //case 2 : 음수 사용
    @Test
    void 포인트_음수_사용 () throws Exception {
        //given
        long userId = 1;
        long usePoint = -300;
        String requestJson = "{\"amount\":" + usePoint + "}";
        when(pointService.charge(anyLong(),anyLong())).thenThrow(new Exception("0이하의 수는 사용할 수 없습니다."));

        //when
        //then
        mvc.perform(patch("/point/"+ userId + "/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is5xxServerError());

    }


    //case 3 : 잔여 포인트보다 많이 사용
    @Test
    void 잔여_포인트_보다_많이_사용 () throws Exception {
        //given
        long userId = 1;
        long usePoint = 300;
        String requestJson = "{\"amount\":" + usePoint + "}";
        when(pointService.charge(anyLong(),anyLong())).thenThrow(new Exception("잔여 포인트보다 많이 사용할 수 없습니다."));

        //when
        //then
        mvc.perform(patch("/point/"+ userId + "/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is5xxServerError());
    }
}
