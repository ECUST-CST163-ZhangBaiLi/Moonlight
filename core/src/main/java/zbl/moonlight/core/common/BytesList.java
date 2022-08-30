package zbl.moonlight.core.common;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import static zbl.moonlight.core.utils.NumberUtils.BYTE_LENGTH;
import static zbl.moonlight.core.utils.NumberUtils.INT_LENGTH;

public class BytesList implements BytesConvertible{
    public static final byte RAW = (byte) 0x01;
    public static final byte VAR = (byte) 0x02;

    private final LinkedList<BytesNode<?>> bytesNodes = new LinkedList<>();

    public BytesList() {

    }

    public void appendRawByte(byte value) {
        append(RAW, value);
    }

    public void appendRawBytes(byte[] value) {
        append(RAW, value);
    }

    public void appendRawInt(int value) {
        append(RAW, value);
    }

    public void appendVarBytes(byte[] value) {
        append(VAR, value);
    }

    public <V> void append(byte type, V value) {
        bytesNodes.add(new BytesNode<>(type, value));
    }

    public void append(BytesList list) {
        bytesNodes.addAll(list.bytesNodes);
    }

    public void append(BytesListConvertible convertible) {
        BytesList list = convertible.toBytesList();
        append(list);
    }

    @Override
    public byte[] toBytes() {
        int length = INT_LENGTH;
        for(BytesNode<?> node : bytesNodes) {
            if(node.type == VAR) {
                length += INT_LENGTH;
            }

            if(node.value instanceof Integer) {
                length += INT_LENGTH;
            } else if(node.value instanceof Byte) {
                length += BYTE_LENGTH;
            } else if (node.value instanceof byte[] bytes) {
                length += bytes.length;
            } else {
                throw new RuntimeException("Undefined value type");
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        for(BytesNode<?> node : bytesNodes) {
            if(node.type == VAR) {
                if(node.value instanceof byte[] bytes) {
                    buffer.putInt(bytes.length);
                } else {
                    throw new RuntimeException("Undefined value type");
                }
            }

            if(node.value instanceof Integer i) {
                buffer.putInt(i);
            } else if(node.value instanceof Byte b) {
                buffer.put(b);
            } else if (node.value instanceof byte[] bytes) {
                buffer.put(bytes);
            } else {
                throw new RuntimeException("Undefined value type");
            }
        }

        return buffer.array();
    }

    private static class BytesNode<V> {
        private final byte type;
        private final V value;

        private BytesNode(byte t, V v) {
            type = t;
            value = v;
        }
    }
}