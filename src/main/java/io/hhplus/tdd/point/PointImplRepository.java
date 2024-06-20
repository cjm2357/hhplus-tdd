package io.hhplus.tdd.point;


import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Repository;

//interface 구현체
@Repository
public class PointImplRepository implements PointRepository {

    private UserPointTable userPointTable;
    public PointImplRepository(UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
    }

    @Override
    public UserPoint findById(long id) {
        return userPointTable.selectById(id);
    }

    @Override
    public UserPoint save(long id, long amount) {
        return userPointTable.insertOrUpdate(id,amount);
    }

}
