package com.example.equity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "equity.compression")
public class CompressionProperties {

    private boolean enabled = true;
    private int     thresholdBytes = 4096;
    private String  algorithm = "GZIP";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getThresholdBytes() { return thresholdBytes; }
    public void setThresholdBytes(int thresholdBytes) { this.thresholdBytes = thresholdBytes; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
}
