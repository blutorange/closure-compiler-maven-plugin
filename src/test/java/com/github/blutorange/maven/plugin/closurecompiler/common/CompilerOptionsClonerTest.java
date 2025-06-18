package com.github.blutorange.maven.plugin.closurecompiler.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.google.javascript.jscomp.CompilerOptions;
import org.junit.jupiter.api.Test;

class CompilerOptionsClonerTest {
    @Test
    void testCloneDefaultOptions() {
        var original = new CompilerOptions();
        var opts = CompilerOptionsCloner.cloneCompilerOptions(original);
        assertNotNull(opts);
        assertNotSame(opts, original);
    }
}
