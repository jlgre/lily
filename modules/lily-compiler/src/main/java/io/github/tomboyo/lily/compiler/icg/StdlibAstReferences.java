package io.github.tomboyo.lily.compiler.icg;

import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import java.util.List;

public class StdlibAstReferences {

  public static AstReference astBigInteger() {
    return AstReference.ref(Fqn.of("java.math", "BigInteger"), List.of());
  }

  public static AstReference astLong() {
    return AstReference.ref(Fqn.of("java.lang", "Long"), List.of());
  }

  public static AstReference astInteger() {
    return AstReference.ref(Fqn.of("java.lang", "Integer"), List.of());
  }

  public static AstReference astBigDecimal() {
    return AstReference.ref(Fqn.of("java.math", "BigDecimal"), List.of());
  }

  public static AstReference astDouble() {
    return AstReference.ref(Fqn.of("java.lang", "Double"), List.of());
  }

  public static AstReference astFloat() {
    return AstReference.ref(Fqn.of("java.lang", "Float"), List.of());
  }

  public static AstReference astString() {
    return AstReference.ref(Fqn.of("java.lang", "String"), List.of());
  }

  public static AstReference astByteBuffer() {
    return AstReference.ref(Fqn.of("java.nio", "ByteBuffer"), List.of());
  }

  public static AstReference astLocalDate() {
    return AstReference.ref(Fqn.of("java.time", "LocalDate"), List.of());
  }

  public static AstReference astOffsetDateTime() {
    return AstReference.ref(Fqn.of("java.time", "OffsetDateTime"), List.of());
  }

  public static AstReference astBoolean() {
    return AstReference.ref(Fqn.of("java.lang", "Boolean"), List.of());
  }

  public static AstReference astListOf(AstReference t) {
    return AstReference.ref(Fqn.of("java.util", "List"), List.of(t));
  }
}
