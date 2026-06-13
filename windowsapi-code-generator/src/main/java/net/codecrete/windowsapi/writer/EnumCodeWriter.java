//
// Windows API Generator for Java
// Copyright (c) 2025 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi.writer;

import net.codecrete.windowsapi.metadata.EnumType;
import net.codecrete.windowsapi.metadata.Member;

/**
 * Creates the Java code for enumerations.
 */
class EnumCodeWriter extends JavaCodeWriter<EnumType> {

    /**
     * Creates a new instance.
     *
     * @param generationContext the code generation context
     */
    EnumCodeWriter(GenerationContext generationContext) {
        super(generationContext);
    }

    /**
     * Creates a new file with the Java code for the specified enumeration.
     *
     * @param enumeration the enumeration
     */
    void writeEnum(EnumType enumeration) {
        var name = toJavaClassName(enumeration.name());
        withFile(enumeration.namespace(), enumeration, name, this::writeEnumValues);
    }

    private void writeEnumValues(JavaSourceFile<EnumType> file) {
        var writer = file.writer();
        var javaType = getPrimitiveJavaType(file.type().baseType());
        var fields = file.type().members();

        var prefix = file.className() + "_";
        var haveCommonPrefix = fields.stream().allMatch(field -> field.name().startsWith(prefix));

        writer.printf("package %s ;%n%n", file.packageName());

        writeEnumComment(file, haveCommonPrefix);

        writer.printf("""
                public class %1$s {
                """, file.className());


        for (Member field : fields) {
            var name = haveCommonPrefix ? field.name().substring(prefix.length()) : field.name();
            if (Character.isDigit(name.charAt(0)))
                name = "_" + name;
            var constantValue = getJavaIntegerConstant(javaType, field.value());
            writeComment(writer, "Enumeration value {@code %s}", field.name());
            writer.printf("""
                        public static final %s %s = %s;

                    """, javaType, name, constantValue);
        }

        writer.printf("""

                    private %s() {}
                }
                """, file.className());
    }

    void writeEnumComment(JavaSourceFile<EnumType> file, boolean haveCommonPrefix) {
        var writer = file.writer();
        writer.printf("""
                        /**
                         * {@code %1$s} %2$s
                        """,
                file.type().nativeName(),
                file.type().isEnumFlags() ? "enumeration flags" : "enumeration");

        if (haveCommonPrefix) {
            writer.printf("""
                     * <p>
                     * The enumeration member names do not include the prefix {@code %s} as it is the same as the enumeration name.
                     * </p>
                    """, file.type().nativeName());
        }

        writeDocumentationUrl(writer, file.type());

        writer.println(" */");
    }
}
