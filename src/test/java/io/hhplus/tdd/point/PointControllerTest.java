package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
public class PointControllerTest {

    @Autowired
    MockMvc mvc;


    /**
     * Charge Test
     * 1. 기존 id에 충전
     * 2. 음수 충전
     * 3. 처음 충전
     * 4. 동시에 여러건의 포인트 충전, 이용 요청
     * */

    @Test
    void 첫_포인트_충전_호출 () throws Exception {
        //given
        long userId = 1;
        long amount = 100;

        String requestJson = "{\"amount\":" + amount + "}";

        //then
//        mvc.perform(patch("/point/"+ userId + "/charge")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestJson))
//                .andExpect(status().isOk())
//                .andExpect(model().attribute("amount", 0));

        mvc.perform(get("/point/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("amount", 0));
    }
}
