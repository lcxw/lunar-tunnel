package com.lunar.cloud.tunnel.core.constant;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 */
@Data
@AllArgsConstructor
public class PortMapping {
    private int externalPort;
    private String internalHost;
    private int internalPort;
}
