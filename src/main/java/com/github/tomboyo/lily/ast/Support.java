package com.github.tomboyo.lily.ast;

public class Support {

  /**
   * Converts the given package and class names to a fully-qualified name (FQN). The class name is
   * converted to ClassCase.
   */
  public static String toFqn(String packageName, String className) {
    return String.join(".", packageName, toClassCase(className));
  }

  /** Converts the given name to ClassCase. For example, fooBar becomes FooBar. */
  public static String toClassCase(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }
}