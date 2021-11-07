package com.github.tomboyo.lily.ast;

import com.github.tomboyo.lily.ast.type.*;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class OasSchemaToAst {

  public static List<Type> oasComponentsAst(Components components) {
    var refs =
        components.getSchemas().entrySet().stream()
            .collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                    entry -> oasSchemaAst(entry.getKey(), entry.getValue())));
    return refs.values()
            .stream()
            .map(type -> interpolateRefs(type, refs))
            .collect(Collectors.toList());
  }

  private static Type interpolateRefs(Type type, Map<String, Type> refs) {
    return switch (type) {
      case AstClass astClass -> new AstClass(
              astClass.name(),
              astClass.fields().stream()
                      .map(field -> new Field(interpolateRefs(field.type(), refs), field.name()))
                      .collect(Collectors.toList()));
      case ClassRef ref -> requireNonNull(
              refs.get(ref.ref().replaceFirst("^#/components/schemas/", "")),
              "Missing type definition for ref: ref=" + ref.ref());
      case StandardType t -> t;
      case UnsupportedType t -> t;
    };
  }

  // See https://swagger.io/specification/#data-types
  // Note: no primitive java types. All fields from an API are inherently nullable, even if this
  // breaks the APIs contract, because the user may request a partial (fragment, filtered, etc)
  // response using query parameters or similar.
  // TODO: unexpected format should be warnings only -- and potential extension points to support
  // user-defined formats like "email," as per the spec linked above.
  private static Type oasSchemaAst(String schmaName, Schema schema) {
    var type = schema.getType();
    var format = schema.getFormat();
    var ref = schema.get$ref();

    if (type == null) {
      if (ref == null) {
        throw new IllegalArgumentException("Null type and ref");
      }

      // We have already generated this class, or will do so soon, as a result of iterating over the
      // OAS components. We will save a reference to this class so that we can render correctly
      // later, but avoid circular dependencies during generation.
      return new ClassRef(ref);
    }

    switch (type) {
      case "integer":
        if (format == null) return new StandardType("java.math.BigInteger");
        if (format.equalsIgnoreCase("int64")) return new StandardType("Long");
        if (format.equalsIgnoreCase("int32")) return new StandardType("Integer");
        throw new IllegalArgumentException("Unexpected integer format: " + format);
      case "number":
        if (format == null) return new StandardType("java.math.BigDecimal");
        if (format.equalsIgnoreCase("double")) return new StandardType("Double");
        if (format.equalsIgnoreCase("float")) return new StandardType("Float");
        throw new IllegalArgumentException("Unexpected number format: " + format);
      case "string":
        if (format == null || format.equalsIgnoreCase("password"))
          return new StandardType("String");
        // base64 or octets.
        if (format.equalsIgnoreCase("byte") || format.equalsIgnoreCase("binary"))
          return new StandardType("Byte[]");
        if (format.equalsIgnoreCase("date")) return new StandardType("java.time.LocalDate");
        if (format.equalsIgnoreCase("date-time"))
          return new StandardType("java.time.ZonedDateTime");
        throw new IllegalArgumentException("Unexpected string format: " + format);
      case "boolean":
        return new StandardType("Boolean");
      case "object":
        return new AstClass(
            schmaName,
            ((Map<String, Schema>) schema.getProperties())
                .entrySet().stream()
                    .map(entry -> oasObjectPropertyToField(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList()));
      case "array":
        return new StandardType(
            "java.util.ArrayList",
            List.of(oasSchemaAst(schmaName, ((ArraySchema) schema).getItems())));
      default:
        return new UnsupportedType("type=" + type + " format=" + format);
    }
  }

  private static Field oasObjectPropertyToField(String propertyName, Schema propertySchema) {
    return new Field(oasSchemaAst(propertyName, propertySchema), propertyName);
  }
}