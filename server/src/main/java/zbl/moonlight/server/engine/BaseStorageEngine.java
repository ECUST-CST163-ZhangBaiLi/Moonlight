package zbl.moonlight.server.engine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zbl.moonlight.server.annotations.MdtpMethod;
import zbl.moonlight.server.context.Configuration;
import zbl.moonlight.storage.core.KvAdapter;
import zbl.moonlight.storage.core.TableAdapter;
import zbl.moonlight.storage.rocks.RocksKvAdapter;
import zbl.moonlight.storage.rocks.RocksTableAdapter;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

/**
 * BaseStorageEngine：
 *  元数据存储
 *  KV 存储
 *  TABLE 存储
 */
public abstract class BaseStorageEngine {
    private static final Logger logger = LogManager.getLogger("StorageEngine");

    private static final String KV_DIR = "kv";
    private static final String TABLE_DIR = "table";
    private static final String META_DIR = "meta_info";

    public static final String META_DB_NAME = "raft_meta";

    protected final KvAdapter metaDb;

    protected final HashMap<Byte, Method> methodMap = new HashMap<>();
    protected final HashMap<String, KvAdapter> kvDbMap = new HashMap<>();
    protected final HashMap<String, TableAdapter> tableMap = new HashMap<>();

    /* TODO: Cache 以后再实现 */

    public BaseStorageEngine(Class<? extends BaseStorageEngine> clazz) {
        String dataDir = Configuration.getInstance().dataDir();
        String metaDbDir = Path.of(dataDir, META_DIR).toString();

        metaDb = new RocksKvAdapter(META_DB_NAME, metaDbDir);

        initKvDb();
        initTable();
        initMethod(clazz);
    }

    public byte[] doQuery(byte[] command) {
        if(command == null) {
            throw new RuntimeException("Command is null.");
        }

        byte method = findMethod(command);

        Method doQueryMethod = methodMap.get(method);
        if(doQueryMethod == null) {
            throw new RuntimeException("Not Supported mdtp method.");
        }

        try {
            return (byte[]) doQueryMethod.invoke(this, command);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract byte findMethod(byte[] command);

    private void initKvDb() {
        String dataDir = Configuration.getInstance().dataDir();
        Path kvPath = Path.of(dataDir, KV_DIR);

        File kvDir = kvPath.toFile();

        if(kvDir.isDirectory()) {
            String[] kvDbNames = kvDir.list();

            if(kvDbNames == null) {
                return;
            }

            for (String kvDbName : kvDbNames) {
                File subFile = Path.of(kvDir.getPath(), kvDbName).toFile();
                if(subFile.isDirectory()) {
                    kvDbMap.put(kvDbName, new RocksKvAdapter(kvDbName, kvDir.getPath()));
                }
            }
        }
    }

    private void initTable() {
        String dataDir = Configuration.getInstance().dataDir();
        Path tablePath = Path.of(dataDir, TABLE_DIR);

        File tableDir = tablePath.toFile();


        if(tableDir.isDirectory()) {
            String[] tableDbNames = tableDir.list();

            if(tableDbNames == null) {
                return;
            }

            for (String tableDbName : tableDbNames) {
                File subFile = Path.of(tableDir.getPath(), tableDbName).toFile();
                if(subFile.isDirectory()) {
                    tableMap.put(tableDbName, new RocksTableAdapter(tableDbName, tableDir.getPath()));
                }
            }
        }
    }

    private void initMethod(Class<? extends BaseStorageEngine> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        Arrays.stream(methods).forEach(method -> {
            MdtpMethod mdtpMethod = method.getAnnotation(MdtpMethod.class);

            if(mdtpMethod == null) {
                return;
            }

            methodMap.put(mdtpMethod.value(), method);
        });
    }
}