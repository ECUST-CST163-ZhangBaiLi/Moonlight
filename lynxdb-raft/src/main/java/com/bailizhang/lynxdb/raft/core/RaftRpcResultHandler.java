package com.bailizhang.lynxdb.raft.core;

import com.bailizhang.lynxdb.raft.client.RaftClient;
import com.bailizhang.lynxdb.raft.request.RequestVote;
import com.bailizhang.lynxdb.raft.request.RequestVoteArgs;
import com.bailizhang.lynxdb.raft.spi.RaftConfiguration;
import com.bailizhang.lynxdb.raft.spi.RaftSpiService;
import com.bailizhang.lynxdb.raft.spi.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SelectionKey;

import static com.bailizhang.lynxdb.ldtp.annotations.LdtpCode.TRUE;

public class RaftRpcResultHandler {
    private static final Logger logger = LoggerFactory.getLogger(RaftRpcResultHandler.class);

    private final RaftClient client;
    private final RaftState raftState;
    private final StateMachine stateMachine;
    private final RaftConfiguration raftConfig;
    private final RaftLog raftLog;

    public RaftRpcResultHandler() {
        client = RaftClient.client();
        raftState = RaftStateHolder.raftState();
        stateMachine = RaftSpiService.stateMachine();
        raftConfig = RaftSpiService.raftConfig();
        raftLog = RaftLog.raftLog();
    }

    public void handlePreVoteResult(
            SelectionKey selectionKey,
            int term,
            byte voteGranted
    ) {
        logger.info("Handle preVote result, term: {}, voteGranted:{}.", term, voteGranted == TRUE);

        if(voteGranted != TRUE) {
            return;
        }

        int currentTerm = raftState.currentTerm().incrementAndGet();
        stateMachine.currentTerm(currentTerm);

        RequestVoteArgs args = new RequestVoteArgs(
                currentTerm,
                raftConfig.currentNode(),
                raftLog.maxIndex(),
                raftLog.maxTerm()
        );

        RequestVote requestVote = new RequestVote(args);
        client.broadcast(requestVote);
    }

    public void handleRequestVoteResult(
            SelectionKey selectionKey,
            int term,
            byte voteGranted
    ) {
        boolean isVoteGranted = voteGranted == TRUE;

        logger.info("Handle requestVote result, term: {}, voteGranted:{}.", term, isVoteGranted);

        var votedNodes = raftState.votedNodes();
        votedNodes.add(selectionKey);

        int votedCount = votedNodes.size();
        int clusterSize = stateMachine.clusterMembers().size();

        if(votedCount > ((clusterSize >> 1) + 1)) {
            raftState.role().compareAndSet(RaftRole.CANDIDATE, RaftRole.LEADER);
            RaftTimeWheel.timeWheel().heartbeat();
        }
    }

    public void handleAppendEntriesResult(
            SelectionKey selectionKey,
            int term,
            byte voteGranted
    ) {

    }

    public void handleInstallSnapshotResult(SelectionKey selectionKey, int term) {
    }
}