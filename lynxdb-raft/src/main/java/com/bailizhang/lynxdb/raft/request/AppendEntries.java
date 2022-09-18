package com.bailizhang.lynxdb.raft.request;

import com.bailizhang.lynxdb.socket.common.NioMessage;

import java.nio.channels.SelectionKey;

import static com.bailizhang.lynxdb.raft.request.RaftRequest.APPEND_ENTRIES;

public class AppendEntries extends NioMessage {
    public AppendEntries(SelectionKey selectionKey, AppendEntriesArgs args) {
        super(selectionKey);

        bytesList.appendRawByte(APPEND_ENTRIES);
        bytesList.append(args);
    }
}