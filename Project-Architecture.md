# 项目架构

Moonlight 分为三个模块：

- 核心模块（core）
- 服务器模块（server）
- 客户端模块（client）

## 核心模块

核心模块包括一下部分：

- Executor 部分
- Socket 部分
- Raft 部分
- Utils 部分

注：各个部分尽量不要过于耦合，可以单独测试，

### Executor 部分

`Executable` 接口定义：

```java
public interface Executable<E> extends Runnable {
    void offer(E e);
}
```

`Executor` 类定义了向执行器队列中添加元素 `offer` ，移除元素 `poll` 和阻塞移除元素 `blockPoll` 的方法：

```java
public abstract class Executor<E> implements Executable<E> {
    private final ConcurrentLinkedQueue<E> queue = new ConcurrentLinkedQueue<>();

    @Override
    public final void offer(E e) {
        if(e != null) {
            queue.offer(e);
            synchronized (queue) {
                queue.notify();
            }
        }
    }

    protected final E blockPoll() {
        if(queue.isEmpty()) {
            synchronized (queue) {
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return queue.poll();
    }

    protected final E poll() {
        return queue.poll();
    }
}
```

### Socket 部分

#### SocketRequest 格式

| status | serial number | data length | data                  |
|--------|---------------|-------------|-----------------------|
| 1 byte | 8 bytes       | 4 bytes     | = (data length) bytes |

#### SocketResponse 格式

| serial number | data length | data                  |
|---------------|-------------|-----------------------|
| 8 bytes       | 4 bytes     | = (data length) bytes |


#### SocketServer 相关

把每个请求的数据读取成 `ReadableSocketRequest`， 然后交给请求处理器 `RequestHandler` 处理。

```java
@FunctionalInterface
public interface RequestHandler {
    void handle(ReadableSocketRequest request);
}
```

**如何创建 SocketServer 并启动**

```java
class Main {
    public static void main(String[] args) {
        SocketServer server = new SocketServer(new SocketServerConfig(port));
        RequestHandler handler = (request) -> {
            byte[] data = request.getData().array();
            server.offer(new WritableSocketResponse(request.selectionKey(),
                    res.getBytes(StandardCharsets.UTF_8)));
        };

        server.setHandler(handler);
        Executable.start(server);       
    }
}
```

#### SocketClient 部分

SocketClient 作为 Socket 客户端，支持连接多台 SocketServer 服务器，可以发送**广播请求**和**单播请求**。

### Raft 部分

#### Socket 请求和响应格式

**请求格式**

| request type | raft request body |
|--------------|-------------------|
| 1 byte       | any bytes         |

**响应格式**

| response status | raft response body |
|-----------------|--------------------|
| 1 byte          | any bytes          |

**Request type 有哪些：**

- RequestVote：Raft 协议定义的“请求投票”请求
- AppendEntries：Raft 协议定义的“尾部添加日志”请求
- ClientRequest：客户端请求

#### Raft RPC 请求

- RequestVote 请求
- AppendEntries 请求

**Raft RPC 请求**

##### RequestVote 请求

**请求格式**

| host length | host                | port    | term    | last log index | last log term |
|-------------|---------------------|---------|---------|----------------|---------------|
| 4 bytes     | (host length) bytes | 4 bytes | 4 bytes | 4 bytes        | 4 bytes       |

**响应格式**

因为 Socket 请求都是做的异步处理，所以在 Raft 协议定义的基础上，还需要返回客户端的 host 和 port。

| term    | host length | host                | port    |
|---------|-------------|---------------------|---------|
| 4 bytes | 4 bytes     | (host length) bytes | 4 bytes |

##### AppendEntries 请求

**请求格式**

| host length | host                | port    | term    | prev log index | prev log term | leader commit | type   | raftLogEntry length | raftLogEntry               | ... |
|-------------|---------------------|---------|---------|----------------|---------------|---------------|--------|--------------|---------------------|-----|
| 4 bytes     | (host length) bytes | 4 bytes | 4 bytes | 4 bytes        | 4 bytes       | 4 bytes       | 1 byte | 4 bytes      | (raftLogEntry length size) | ... |

*raftLogEntry 格式*

| term    | command   |
|---------|-----------|
| 4 bytes | any bytes |

**响应格式**

因为 Socket 请求都是做的异步处理，所以在 Raft 协议定义的基础上，还需要返回客户端的 host 和 port，如果请求成功的话，还需要返回 matchIndex。

*AppendEntries成功的响应格式*

| term    | host length | host                | port    | matchIndex |
|---------|-------------|---------------------|---------|------------|
| 4 bytes | 4 bytes     | (host length) bytes | 4 bytes | 4 bytes    |

*AppendEntries失败的响应格式*

| term    | host length | host                | port    |
|---------|-------------|---------------------|---------|
| 4 bytes | 4 bytes     | (host length) bytes | 4 bytes |

#### ClientRequest 请求

**请求格式**

| request type | command     |
|--------------|-------------|
| 1 byte       | (any) bytes |

Client request type:
- GET: （查询操作）不需要增加 Raft 日志，直接从状态机返回相应的数据
- SET: （修改数据操作）需要增加 Raft 日志，并将日志复制到其他 Raft 节点

**响应格式**

当前节点所处的状态：

*Leader*：处理客户端请求，处理客户端请求前需要确认自己是否还是 leader
*Candidate*：不处理客户端请求，直接响应 “leader 不存在”
*Follower*：将请求重定向给 leader

| response status | command result |
|-----------------|----------------|
| 1 byte          | (any) bytes    |

基于解耦的处理，Raft 模块不做 ClientRequest 请求的解析，将 ClientRequest 的解析操作交给 `RaftClientRequestHandler` 接口处理。

并且 RaftLog 也只对 ClientRequest 的内容做存储操作，并不做解析处理，解析操作留给具体的业务逻辑实现。


#### RaftServer

### Serializer 部分

## 服务器模块

**服务器模块**依赖**核心模块**。

- Mdtp 通信部分
- Configuration 部分
- StorageEngine 部分

### Mdtp 通信模块

- KV_SINGLE_SET
- KV_SINGLE_GET
- KV_SINGLE_DELETE
- kv_batch_set
- kv_batch_get
- kv_batch_delete
- kv_create_db
- kv_delete_db
- table_single_set
- table_single_get
- table_single_delete
- table_batch_set
- table_batch_get
- table_batch_delete
- create_table
- delete_table
- create_table_column
- delete_table_column

#### KV_SINGLE_SET 请求

**文本格式**

```shell
set key value
```

**请求格式**

| method | key length | key                | value length | value          |
|--------|------------|--------------------|--------------|----------------|
| 1 byte | 4 bytes    | (key length) bytes | 4 bytes      | (value) length |

**响应格式**

无响应体

#### KV_SINGLE_GET 请求

**文本格式**

```shell
get key
```

**请求格式**

| method | key length | key                | value length        | value |
|--------|------------|--------------------|---------------------|-------|
| 1 byte | 4 bytes    | (key length) bytes | 4 bytes (value = 0) | none  |

**响应格式**

*值存在*

| value       |
|-------------|
| (any) bytes |

*值不存在：无响应体*

#### KV_SINGLE_DELETE 请求

**文本格式**

```shell
delete key
```

**请求格式**

| method | key length | key                | value length        | value |
|--------|------------|--------------------|---------------------|-------|
| 1 byte | 4 bytes    | (key length) bytes | 4 bytes (value = 0) | none  |

**响应格式**

无响应体

## 客户端模块

**客户端模块**依赖**服务器模块**和**核心模块**。

- 命令行客户端的实现（暂时不做拆分）