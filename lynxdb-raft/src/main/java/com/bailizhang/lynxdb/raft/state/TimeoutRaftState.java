package com.bailizhang.lynxdb.raft.state;

import com.bailizhang.lynxdb.core.timeout.Timeout;
import com.bailizhang.lynxdb.raft.common.RaftConfiguration;
import com.bailizhang.lynxdb.raft.timeout.ElectionTask;
import com.bailizhang.lynxdb.raft.timeout.HeartbeatTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimeoutRaftState extends LogRaftState {
    private static final Logger logger = LogManager.getLogger("TimeoutRaftState");

    private static final String HEARTBEAT_TIMEOUT_NAME = "HeartBeat_Timeout";
    private static final String ELECTION_TIMEOUT_NAME = "Election_Timeout";

    private static final int HEARTBEAT_INTERVAL_MILLIS = 80;
    private static final int ELECTION_MIN_INTERVAL_MILLIS = 150;
    private static final int ELECTION_MAX_INTERVAL_MILLIS = 300;

    private final Timeout heartbeat;
    private final Timeout election;

    public TimeoutRaftState() {
        final int ELECTION_INTERVAL_MILLIS = ((int) (Math.random() *
                (ELECTION_MAX_INTERVAL_MILLIS - ELECTION_MIN_INTERVAL_MILLIS)))
                + ELECTION_MIN_INTERVAL_MILLIS;

        heartbeat = new Timeout(new HeartbeatTask(), HEARTBEAT_INTERVAL_MILLIS);
        election = new Timeout(new ElectionTask(), ELECTION_INTERVAL_MILLIS);
    }

    public void startTimeout() {
        String electionMode = raftConfiguration.electionMode();

        if(RaftConfiguration.FOLLOWER.equals(electionMode)) {

            logger.info("Election Mode is [{}], Do not start Timeout.", electionMode);

        } else if (RaftConfiguration.LEADER.equals(electionMode)
                || RaftConfiguration.CANDIDATE.equals(electionMode)) {

            logger.info("Election Mode is [{}].", electionMode);

            new Thread(heartbeat, HEARTBEAT_TIMEOUT_NAME).start();
            new Thread(election, ELECTION_TIMEOUT_NAME).start();
        }
    }

    public void resetElectionTimeout() {
        election.reset();
    }

    public void resetHeartbeatTimeout() {
        heartbeat.reset();
    }
}
