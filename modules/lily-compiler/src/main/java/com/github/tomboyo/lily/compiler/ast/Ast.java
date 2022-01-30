package com.github.tomboyo.lily.compiler.ast;

public sealed interface Ast
    permits AstClass, AstClassAlias, AstApi, AstOperationsClass, AstOperationsClassAlias {}
