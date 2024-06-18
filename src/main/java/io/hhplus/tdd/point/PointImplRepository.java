package io.hhplus.tdd.point;


import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Repository;

@Repository
public class PointImplRepository implements PointRepository {

    private UserPointTable userPointTable = new UserPointTable();

    @Override
    public UserPoint selectById(long id) {
        return userPointTable.selectById(id);
    }

    @Override
    public UserPoint save(long id, long amount) {
        return userPointTable.insertOrUpdate(id,amount);
    }

}
