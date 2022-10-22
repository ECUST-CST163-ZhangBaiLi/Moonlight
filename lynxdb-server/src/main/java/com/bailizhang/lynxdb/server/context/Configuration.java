package com.bailizhang.lynxdb.server.context;

import com.bailizhang.lynxdb.socket.client.ServerNode;
import lombok.ToString;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@ToString
public class Configuration {
    public static final String CLUSTER = "cluster";
    public static final String SINGLE = "single";

    private static final String DEFAULT_CONFIG_DIR = System.getProperty("user.dir") + "/config";
    private static final String DEFAULT_RAFT_LOG_DIR = System.getProperty("user.dir") + "/logs/raft";
    private static final String DEFAULT_FILENAME = "app.cfg";
    private static final String BASE_DIR = "[base]";

    private static final String SEPARATOR = "=";

    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String DATA_DIR = "data_dir";
    private static final String RUNNING_MODE = "running_mode";
    private static final String ELECTION_MODE = "election_mode";

    private final ServerNode currentNode;

    private String host;
    private int port;

    private String dataDir;
    private final String raftLogDir;

    private String runningMode;
    private String electionMode;

    private final Charset charset = StandardCharsets.UTF_8;

    private static class Holder {
        private static final Configuration instance;

        static {
            try {
                instance = new Configuration();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Configuration getInstance() {
        return Holder.instance;
    }

    private Configuration() throws IOException {
        this(DEFAULT_CONFIG_DIR, DEFAULT_FILENAME);
    }

    private Configuration(String dirname, String filename) throws IOException {
        Path path = Path.of(dirname, filename);
        File configFile = path.toFile();
        if(!configFile.exists()) {
            throw new RuntimeException("\"app.cfg\" file is not existed.");
        }

        BufferedReader reader = new BufferedReader(new FileReader(configFile));

        String line;
        while((line = reader.readLine()) != null) {
            String[] item = line.trim().split(SEPARATOR);
            if(item.length != 2) {
                String message = String.format("Value of \"%s\" can not contain \"=\".", item[0].trim());
                throw new RuntimeException(message);
            }

            String key = item[0].trim();
            String value = item[1].trim();

            switch (key.toLowerCase()) {
                case HOST -> host = value;
                case PORT -> port = Integer.parseInt(value);
                case DATA_DIR -> dataDir = value.startsWith(BASE_DIR)
                        ? value.replace(BASE_DIR, System.getProperty("user.dir"))
                        : value;
                case RUNNING_MODE -> runningMode = value;
                case ELECTION_MODE -> electionMode = value;
            }
        }

        raftLogDir = DEFAULT_RAFT_LOG_DIR;
        currentNode = new ServerNode(host, port);
    }

    public ServerNode currentNode() {
        return currentNode;
    }

    public String dataDir() {
        return dataDir;
    }

    public String electionMode() {
        return electionMode;
    }

    public Charset charset() {
        return charset;
    }

    public String raftLogDir() {
        return raftLogDir;
    }

    public String runningMode() {
        return runningMode;
    }
}
