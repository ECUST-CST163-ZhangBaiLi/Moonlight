package zbl.moonlight.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("zbl.moonlight")
public class MoonlightProperties {
    private String host;
    private int port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
