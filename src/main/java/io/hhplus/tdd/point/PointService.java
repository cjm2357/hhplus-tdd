package io.hhplus.tdd.point;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PointService {

    private PointRepository pointRepository;
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    public PointService(PointRepository pointRepository, PointHistoryRepository pointHistoryRepository) {

        this.pointRepository = pointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    //case 추가 순서
    public UserPoint charge(Long id, Long amount) throws Exception {
        //case 2 : 0이하의 수 충전으로 인한 코드 추가
        if (amount < 0) throw new Exception("0이하의 수는 충전할 수 없습니다.");
        
        //case 3 : 기존 충전 되어 있는 id에 대한 추가로직
        UserPoint prevUserPoint = pointRepository.findById(id);
        if (prevUserPoint != null) amount += prevUserPoint.point();


        //case 1: 첫 충전으로 인한 코드 추가
        UserPoint userPoint = pointRepository.save(id, amount);
        //통합테스트로 인해 history insert 추가
        pointHistoryRepository.insert(userPoint.id(), userPoint.point(), TransactionType.CHARGE, userPoint.updateMillis());


        return userPoint;
    }

    public UserPoint search(long id) {
        return pointRepository.findById(id);
    }

    public UserPoint use(long id, long amount) throws Exception{
        //use unit case 2 : 음수 사용
        if (amount < 0) throw new Exception("0이하의 수는 사용할 수 없습니다.");

        UserPoint curPoint = pointRepository.findById(id);
        //use unit case 3 :충전한적 없는데 포인트 사용

        if (curPoint == null) throw new Exception("충전된 포인트가 없습니다.");
        //use unit case 4 : 잔여 포인트보다 많이 사용
        if (curPoint.point() < amount) throw new Exception("잔여 포인트보다 많이 사용할 수 없습니다.");

        //use integration case 1 에서 추가
        UserPoint newUserPoint = pointRepository.save(id, curPoint.point() - amount);
        pointHistoryRepository.insert(newUserPoint.id(), newUserPoint.point(), TransactionType.USE, newUserPoint.updateMillis());
        //use unit case 1 : 일반 사용
        return newUserPoint;
    }

    public List<PointHistory> readHistories(long userId) throws Exception {
        //case 1
        List<PointHistory> pointHistories = null;

        pointHistories = pointHistoryRepository.findAllByUserId(userId);

        //case 2
        if (pointHistories == null) throw new Exception("포인트 내역이 없습니다.");

        //case 3
        pointHistories = pointHistories.stream()
                .sorted(Comparator.comparingLong(PointHistory::updateMillis).reversed())
                .collect(Collectors.toList());

        System.out.println("pointHistories = " + pointHistories);

        // case1
        return pointHistories;
    }
}
