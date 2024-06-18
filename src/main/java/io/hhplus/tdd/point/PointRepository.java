package io.hhplus.tdd.point;

public interface PointRepository {

    UserPoint selectById(long id);

    UserPoint save(long id, long amount);
}
