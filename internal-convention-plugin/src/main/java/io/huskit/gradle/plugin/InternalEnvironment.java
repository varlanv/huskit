package io.huskit.gradle.plugin;

import lombok.RequiredArgsConstructor;

/**
 * Simple utility for running different configuration base on local/CI/functional-test environments
 */
@RequiredArgsConstructor
public class InternalEnvironment {

    boolean isCi;
    boolean isTest;

    public static String name() {
        return "__internal_environment__";
    }

    public boolean isCi() {
        return isCi;
    }

    public boolean isLocal() {
        return !isCi;
    }

    public boolean isTest() {
        return isTest;
    }
}
