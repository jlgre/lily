package com.github.tomboyo.lily.compiler.cg;

import java.nio.file.Path;

public record Source(Path relativePath, String contents) {}