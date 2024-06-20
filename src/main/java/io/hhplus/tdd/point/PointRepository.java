package io.hhplus.tdd.point;

public interface PointRepository {

    //4 layer?로 하기 위한 interface
    UserPoint findById(long id);

    UserPoint save(long id, long amount);
}
