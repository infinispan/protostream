package org.infinispan.protostream.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects 'reserved' statements in the generated schema of a message or enum type. The 'reserved' statement is added
 * inside the generated message or enum definition, right at the beginning. Mixing numbers, ranges and names in a single
 * annotation instance is allowed (for your convenience) but results in separate statements being generated for numbers
 * and names because protobuf does not allow mixing them.
 * <p>
 * All {@code ProtoReserved} annotations from the superclass and implemented/extended interfaces are inherited
 * (recursively up the hierarchy) and the numbers, ranges and names are merged. <em>No duplicates or overlaps must be
 * found</em> in the process or else an error will be generated. Regardless of how many annotations are found, after
 * merging at most two 'reserved' statements will be issued, one for numbers/ranges and one for names. Their contents
 * will be sorted ascendingly.
 * <p>
 * This annotation is not explicitly marked {@link java.lang.annotation.Inherited} but annotation processors will scan
 * for occurrences of this annotation in all superclasses and superinterfaces (recursively).
 *
 * @author anistor@redhat.com
 * @see <a href="https://developers.google.com/protocol-buffers/docs/proto#reserved">Protocol Buffers Language Guide -
 * Reserved Fields</a>
 * @see <a href="https://developers.google.com/protocol-buffers/docs/proto#enum_reserved">Protocol Buffers Language
 * Guide - Reserved Values</a>
 * @since 4.3
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ProtoReservedStatements.class)
public @interface ProtoReserved {

   /**
    * Numbers to reserve. Alias for {@link #value} (mutually exclusive).
    */
   int[] numbers() default {};

   /**
    * Alias for {@link #numbers} (mutually exclusive).
    */
   int[] value() default {};

   /**
    * Number ranges to be reserved.
    */
   Range[] ranges() default {};

   /**
    * Names to be reserved.
    */
   String[] names() default {};

   /**
    * A range of field numbers or enum constants. The upper bound can be left unspecified if 'max' is desired. The
    * acceptable values for {@link #from} and {@link #to} are different for fields vs. enum constants.
    */
   @interface Range {

      /**
       * Maximum allowed value for field numbers.
       */
      int MAX_FIELD = 536870911;

      /**
       * Maximum allowed value for enums.
       */
      int MAX_ENUM = Integer.MAX_VALUE;

      /**
       * The start of the range (inclusive). Minimum value is 1 for field numbers, but can be any 32-bit value for enums
       * (including negative values).
       */
      int from();

      /**
       * The end of the range (inclusive). Must be strictly greater than {@link #from}. The actual maximum is {@link #MAX_FIELD}
       * (2<sup>29</sup>-1 = 536870911) for field numbers, but it can be any 32-bit value for enums.
       */
      int to() default Integer.MAX_VALUE;
   }
}
