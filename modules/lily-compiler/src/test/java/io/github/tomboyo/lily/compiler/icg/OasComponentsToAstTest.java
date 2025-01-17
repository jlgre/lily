package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astBigDecimal;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astBigInteger;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astBoolean;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astByteBuffer;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astDouble;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astFloat;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astInteger;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astListOf;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astLocalDate;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astLong;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astOffsetDateTime;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstClassAlias;
import io.github.tomboyo.lily.compiler.ast.Field;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OasComponentsToAstTest {

  /** A list of scalar types and formats, and the java types they evaluate to. * */
  public static Stream<Arguments> scalarsSource() {
    return Stream.of(
        arguments("boolean", null, astBoolean()),
        arguments("boolean", "unsupported-format", astBoolean()),
        arguments("integer", null, astBigInteger()),
        arguments("integer", "unsupported-format", astBigInteger()),
        arguments("integer", "int32", astInteger()),
        arguments("integer", "int64", astLong()),
        arguments("number", null, astBigDecimal()),
        arguments("number", "unsupported-format", astBigDecimal()),
        arguments("number", "double", astDouble()),
        arguments("number", "float", astFloat()),
        arguments("string", null, astString()),
        arguments("string", "unsupportedFormat", astString()),
        arguments("string", "password", astString()),
        arguments("string", "byte", astByteBuffer()),
        arguments("string", "binary", astByteBuffer()),
        arguments("string", "date", astLocalDate()),
        arguments("string", "date-time", astOffsetDateTime()));
  }

  @Nested
  class ScalarComponents {
    @ParameterizedTest
    @MethodSource("io.github.tomboyo.lily.compiler.icg.OasComponentsToAstTest#scalarsSource")
    void evaluate(String oasType, String oasFormat, Fqn expectedRef) {
      var actual =
          OasComponentsToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyComponent"),
              new Schema().type(oasType).format(oasFormat));

      assertEquals(
          Set.of(
              new AstClassAlias(
                  Fqn.newBuilder().packageName("p").typeName("MyComponent").build(), expectedRef)),
          actual.collect(Collectors.toSet()));
    }
  }

  @Nested
  class Refs {
    @Test
    void evaluate() {
      var actual =
          OasComponentsToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyComponent"),
              new Schema().$ref("#/components/schemas/MyRef"));

      assertEquals(
          Set.of(
              new AstClassAlias(
                  Fqn.newBuilder().packageName("p").typeName("MyComponent").build(),
                  Fqn.newBuilder().packageName("p").typeName("MyRef").build())),
          actual.collect(Collectors.toSet()));
    }
  }

  @Nested
  class Arrays {
    @Test
    void evaluateWithScalarItem() {
      var actual =
          OasComponentsToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyComponent"),
              new ArraySchema().items(new Schema<>().type("boolean")));

      assertEquals(
          Set.of(
              new AstClassAlias(
                  Fqn.newBuilder().packageName("p").typeName("MyComponent").build(),
                  astListOf(astBoolean()))),
          actual.collect(Collectors.toSet()),
          "Array components evaluate to aliases of lists");
    }

    @Test
    void evaluateWithObjectItem() {
      var actual =
          OasComponentsToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyComponent"),
              new ArraySchema()
                  .items(new ObjectSchema().properties(Map.of("myField", new ObjectSchema()))));

      assertEquals(
          Set.of(
              new AstClassAlias(
                  Fqn.newBuilder().packageName("p").typeName("MyComponent").build(),
                  Fqn.newBuilder()
                      .packageName("java.util")
                      .typeName("List")
                      .typeParameters(
                          List.of(
                              Fqn.newBuilder()
                                  .packageName("p.mycomponent")
                                  .typeName("MyComponentItem")
                                  .build()))
                      .build()),
              AstClass.of(
                  Fqn.newBuilder().packageName("p.mycomponent").typeName("MyComponentItem").build(),
                  List.of(
                      new Field(
                          Fqn.newBuilder()
                              .packageName("p.mycomponent.mycomponentitem")
                              .typeName("MyField")
                              .build(),
                          SimpleName.of("myField"),
                          "myField"))),
              AstClass.of(
                  Fqn.newBuilder()
                      .packageName("p.mycomponent.mycomponentitem")
                      .typeName("MyField")
                      .build(),
                  List.of())),
          actual.collect(Collectors.toSet()),
          "Inline types within aliases are defined in packages subordinate to the class alias");
    }

    @Test
    void evaluateWithArrayOfObjectItem() {
      var actual =
          OasComponentsToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyComponent"),
              new ArraySchema().items(new ArraySchema().items(new ObjectSchema())));

      assertEquals(
          Set.of(
              AstClass.of(
                  Fqn.newBuilder().packageName("p.mycomponent").typeName("MyComponentItem").build(),
                  List.of()),
              new AstClassAlias(
                  Fqn.newBuilder().packageName("p").typeName("MyComponent").build(),
                  astListOf(
                      astListOf(
                          Fqn.newBuilder()
                              .packageName("p.mycomponent")
                              .typeName("MyComponentItem")
                              .build())))),
          actual.collect(Collectors.toSet()),
          "item schemas in nested arrays evaluate to AstClasses nested beneath the alias and not"
              + " deeper");
    }

    @Test
    void evaluateWithRefItem() {
      var actual =
          OasComponentsToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyComponent"),
              new ArraySchema().items(new Schema<>().$ref("#/components/schemas/MyRef")));

      assertEquals(
          Set.of(
              new AstClassAlias(
                  Fqn.newBuilder().packageName("p").typeName("MyComponent").build(),
                  astListOf(Fqn.newBuilder().packageName("p").typeName("MyRef").build()))),
          actual.collect(Collectors.toSet()),
          "arrays of refs evaluate to aliases of lists of the referent type");
    }
  }

  @Nested
  class Objects {
    @Test
    void evaluate() {
      assertEquals(
          OasSchemaToAst.evaluate(
                  PackageName.of("p"), SimpleName.of("MyComponent"), new ObjectSchema())
              .right()
              .collect(Collectors.toSet()),
          OasComponentsToAst.evaluate(
                  PackageName.of("p"), SimpleName.of("MyComponent"), new ObjectSchema())
              .collect(Collectors.toSet()),
          "Object components are evaluated the same as any other object schema (i.e. not aliased)");
    }
  }
}
