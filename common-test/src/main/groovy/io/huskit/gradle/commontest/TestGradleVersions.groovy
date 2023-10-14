package io.huskit.gradle.commontest

import groovy.transform.CompileStatic

@CompileStatic
class TestGradleVersions {

    static List<String> get() {
        return [
                current(),
                "7.6.1",
//                "6.9.4", todo
        ]
    }

    static String current() {
        return "8.4"
    }
}
