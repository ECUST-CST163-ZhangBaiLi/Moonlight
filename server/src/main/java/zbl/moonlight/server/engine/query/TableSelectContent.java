package zbl.moonlight.server.engine.query;

import zbl.moonlight.core.common.BytesList;
import zbl.moonlight.core.common.BytesListConvertible;
import zbl.moonlight.core.common.G;
import zbl.moonlight.core.utils.BufferUtils;
import zbl.moonlight.server.annotations.MdtpMethod;
import zbl.moonlight.server.engine.QueryParams;
import zbl.moonlight.storage.core.Column;
import zbl.moonlight.storage.core.MultiTableKeys;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class TableSelectContent implements BytesListConvertible {
    private final String table;
    private final MultiTableKeys multiKeys;

    private final List<byte[]> keys = new ArrayList<>();
    private final HashSet<Column> columns = new HashSet<>();

    public TableSelectContent(QueryParams params) {
        ByteBuffer buffer = ByteBuffer.wrap(params.content());
        table = BufferUtils.getString(buffer);

        byte[] keysBytes = BufferUtils.getBytes(buffer);
        byte[] columnBytes = BufferUtils.getBytes(buffer);

        ByteBuffer keysBuffer = ByteBuffer.wrap(keysBytes);

        while(!BufferUtils.isOver(keysBuffer)) {
            keys.add(BufferUtils.getBytes(keysBuffer));
        }

        ByteBuffer columnBuffer = ByteBuffer.wrap(columnBytes);

        while(!BufferUtils.isOver(columnBuffer)) {
            byte[] bytes = BufferUtils.getBytes(columnBuffer);
            columns.add(new Column(bytes));
        }

        multiKeys = new MultiTableKeys(keys, columns);
    }

    public TableSelectContent(String table, MultiTableKeys multiKeys) {
        this.table = table;
        this.multiKeys = multiKeys;
    }

    public String table() {
        return table;
    }

    public MultiTableKeys multiKeys() {
        return multiKeys;
    }

    public List<byte[]> keys() {
        return keys;
    }

    public HashSet<Column> columns() {
        return columns;
    }

    @Override
    public BytesList toBytesList() {
        BytesList bytesList = new BytesList();

        bytesList.appendRawByte(MdtpMethod.TABLE_SELECT);
        bytesList.appendVarBytes(G.I.toBytes(table));

        multiKeys.left().forEach(bytesList::appendVarBytes);
        multiKeys.right().stream().map(Column::value).forEach(bytesList::appendVarBytes);

        return bytesList;
    }
}
