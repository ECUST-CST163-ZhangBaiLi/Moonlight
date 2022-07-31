package zbl.moonlight.storage.rocks.query.kv;

import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import zbl.moonlight.storage.core.ResultSet;
import zbl.moonlight.storage.rocks.query.Query;

import java.util.List;

public class KvBatchGetQuery extends Query<List<byte[]>, List<byte[]>> {
    public KvBatchGetQuery(List<byte[]> queryData, ResultSet<List<byte[]>> resultSet) {
        super(queryData, resultSet);
    }

    @Override
    public void doQuery(RocksDB db) throws RocksDBException {

    }
}