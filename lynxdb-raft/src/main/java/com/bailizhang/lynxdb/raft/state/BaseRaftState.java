package com.bailizhang.lynxdb.raft.state;

import com.bailizhang.lynxdb.raft.common.RaftConfiguration;
import com.bailizhang.lynxdb.raft.common.StateMachine;

import java.util.Optional;
import java.util.ServiceLoader;

public class BaseRaftState {
    protected static final StateMachine stateMachine;
    protected static final RaftConfiguration raftConfiguration;

    static {
        ServiceLoader<StateMachine> stateMachines = ServiceLoader.load(StateMachine.class);
        Optional<StateMachine> smOptional = stateMachines.findFirst();

        if(smOptional.isEmpty()) {
            throw new RuntimeException("Can not find StateMachine.");
        }

        stateMachine = smOptional.get();
        raftConfiguration = RaftConfiguration.getInstance();
    }
}