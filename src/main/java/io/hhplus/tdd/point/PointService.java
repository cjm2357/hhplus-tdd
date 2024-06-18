package io.hhplus.tdd.point;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PointService {

    private PointRepository pointRepository;

    @Autowired
    public PointService(PointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    public UserPoint charge(Long id, Long amount) throws Exception {
        //case 2 : 0이하의 수 충전으로 인한 코드 추가
        if (amount < 0) throw new Exception("0이하의 수는 충전할 수 없습니다.");
        
        //case 3 : 기존 충전 되어 있는 id에 대한 추가로직
        UserPoint prevUserPoint = pointRepository.selectById(id);
        amount += prevUserPoint.point();


        //case 1: 첫 충전으로 인한 코드 추가
        return pointRepository.save(id, amount);
    }
}
