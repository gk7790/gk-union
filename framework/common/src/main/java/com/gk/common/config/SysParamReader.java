package com.gk.common.config;

/**
 * Read-only boundary for consuming system parameters from optional modules.
 */
public interface SysParamReader {

    String getValue(String paramCode);
}
