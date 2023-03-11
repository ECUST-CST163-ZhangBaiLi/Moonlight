package com.bailizhang.lynxdb.raft.state;

import com.bailizhang.lynxdb.core.utils.BufferUtils;
import com.bailizhang.lynxdb.raft.common.RaftConfiguration;
import com.bailizhang.lynxdb.raft.common.RaftRole;
import com.bailizhang.lynxdb.raft.common.StateMachine;

import java.util.Optional;
import java.util.ServiceLoader;

public class BaseRaftState {
    private static final String CURRENT_TERM = "current_term";

    protected static final StateMachine stateMachine;
    protected static final RaftConfiguration raftConfiguration;

    static {
        ServiceLoader<StateMachine> stateMachines = ServiceLoader.load(StateMachine.class);
        Optional<StateMachine> smOptional = stateMachines.findFirst();

        if(smOptional.isEmpty()) {
            throw new RuntimeException("Can not find StateMachine.");
        }

        stateMachine = smOptional.get();

        ServiceLoader<RaftConfiguration> raftConfigurations = ServiceLoader.load(RaftConfiguration.class);
        Optional<RaftConfiguration> rcOptional = raftConfigurations.findFirst();

        if(rcOptional.isEmpty()) {
            throw new RuntimeException("Can not find RaftConfiguration.");
        }

        raftConfiguration = rcOptional.get();
    }

    protected volatile RaftRole raftRole = RaftRole.FOLLOWER;
    protected volatile int currentTerm;

    public void currentTerm(int term) {
        stateMachine.metaSet(CURRENT_TERM, BufferUtils.toBytes(term));
        currentTerm = term;
    }

    public synchronized void transformToCandidate() {
        raftRole(RaftRole.CANDIDATE);
        currentTerm(currentTerm ++);
    }

    public synchronized void transformToLeader() {
        raftRole(RaftRole.LEADER);
    }

    public synchronized void transformToFollower() {
        raftRole(RaftRole.FOLLOWER);
    }

    public boolean isLeader() {
        return raftRole == RaftRole.LEADER;
    }

    public boolean isCandidate() {
        return raftRole == RaftRole.CANDIDATE;
    }

    public boolean isFollower() {
        return raftRole == RaftRole.FOLLOWER;
    }

    public void raftRole(RaftRole role) {
        RaftRole oldRaftRole = raftRole;
        raftRole = role;
    }

    public String logDir() {
        return raftConfiguration.logDir();
    }
}
