package org.infinispan.protostream.descriptors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
public abstract class AnnotationElement {

   /**
    * On what kind of descriptor can we place that annotation?
    */
   public enum AnnotationTarget {
      MESSAGE, ENUM, FIELD
   }

   /**
    * What type is the attribute?
    */
   public enum AttributeType {
      IDENTIFIER, STRING, CHARACTER, BOOLEAN, INT, LONG, FLOAT, DOUBLE, ANNOTATION
   }

   public static final long UNKNOWN_POSITION = 0;

   private static final long LINESHIFT = 32;

   private static final long COLUMNMASK = (1L << LINESHIFT) - 1;

   /**
    * Text position, encoded in the form of a {@code long}. The upper half is the line number, the lower half is the column.
    */
   public final long position;

   /**
    * Returns the line number of the given position.
    *
    * @param pos the position, encoded as a {@code long} as described in {@link #position}
    * @return the line number, or {@code -1} if the position is unknown
    */
   public static int line(long pos) {
      return (int) (pos >>> LINESHIFT);
   }

   /**
    * Returns the column number of the given position.
    *
    * @param pos the position, encoded as a {@code long} as described in {@link #position}
    * @return the column number, or {@code -1} if the position is unknown
    */
   public static int column(long pos) {
      return (int) (pos & COLUMNMASK);
   }

   /**
    * Returns the position encoded as a {@code long}.
    *
    * @param line   the line number
    * @param column the column number
    * @return the position, encoded as a {@code long}
    */
   public static long makePosition(int line, int column) {
      return ((long) line << LINESHIFT) + column;
   }

   /**
    * Returns a string representation of the given position. The string is of the form {@code line:column}.
    *
    * @param pos the position, encoded as a {@code long} as described in {@link #position}
    * @return a string representation of the given position, or {@code "unknown"} if the position is unknown
    */
   public static String positionToString(long pos) {
      return line(pos) + "," + column(pos);
   }

   protected AnnotationElement(long position) {
      this.position = position;
   }

   public abstract static class Value extends AnnotationElement {

      protected Value(long pos) {
         super(pos);
      }

      public abstract Object getValue();

      /**
       * All {@code Values} must override {@code toString()} in a sensible manner.
       */
      @Override
      public abstract String toString();
   }

   public static final class Annotation extends Value {

      /**
       * The name of the default attribute.
       */
      public static final String VALUE_DEFAULT_ATTRIBUTE = "value";

      private final String name;

      private final String packageName;

      private final Map<String, Attribute> attributes;

      public Annotation(long pos, String name, Map<String, Attribute> attributes) {
         super(pos);
         int dot = name.lastIndexOf('.');
         this.name = dot < 0 ? name : name.substring(dot + 1);
         this.packageName = dot < 0 ? null : name.substring(0, dot);
         this.attributes = attributes;
      }

      /**
       * Returns the name of the annotation.
       *
       * @return the name of the annotation.
       */
      public String getName() {
         return name;
      }

      /**
       * Returns the package name of the annotation, or {@code null} if the annotation is in the default package.
       *
       * @return the package name of the annotation, or {@code null} if the annotation is in the default package.
       */
      public String getPackageName() {
         return packageName;
      }

      /**
       * Returns the attributes of this annotation.
       *
       * @return a map of attribute names to {@link Attribute} instances. The map is unmodifiable.
       */
      public Map<String, Attribute> getAttributes() {
         return attributes;
      }

      /**
       * Returns the {@link Annotation} itself. This is useful for {@link Visitor} implementations.
       *
       * @return the {@link Annotation} itself. This is useful for {@link Visitor} implementations.
       */
      @Override
      public Annotation getValue() {
         return this;
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append('(');
         for (Attribute a : attributes.values()) {
            if (sb.length() > 1) {
               sb.append(", ");
            }
            sb.append(a);
         }
         sb.append(')');
         return "@" + name + sb;
      }

      public Value getDefaultAttributeValue() {
         return getAttributeValue(VALUE_DEFAULT_ATTRIBUTE);
      }

      public Value getAttributeValue(String attributeName) {
         Attribute attribute = attributes.get(attributeName);
         if (attribute == null) {
            throw new IllegalStateException("Attribute '" + attributeName + "' of annotation '" + name + "' is missing");
         }
         return attribute.value;
      }

      @Override
      public void acceptVisitor(Visitor visitor) {
         visitor.visit(this);
      }
   }

   public static final class Attribute extends AnnotationElement {

      private final String name;

      private final String packageName;

      private final Value value;

      public Attribute(long pos, String name, Value value) {
         super(pos);
         int dot = name.lastIndexOf('.');
         this.name = dot < 0 ? name : name.substring(dot + 1);
         this.packageName = dot < 0 ? null : name.substring(0, dot);
         this.value = value;
      }

      /**
       * Returns the name of the attribute.
       *
       * @return the name of the attribute.
       */
      public String getName() {
         return name;
      }

      public String getPackageName() {
         return packageName;
      }

      public Value getValue() {
         return value;
      }

      @Override
      public void acceptVisitor(Visitor visitor) {
         visitor.visit(this);
      }

      @Override
      public String toString() {
         return name + "=" + value;
      }
   }

   /**
    * An identifier is a bit like a string literal, but it does not have the quotation marks, and it cannot contain white
    * space.
    */
   public static final class Identifier extends Value {

      private final String identifier;

      public Identifier(long pos, String identifier) {
         super(pos);
         this.identifier = identifier;
      }

      public String getIdentifier() {
         return identifier;
      }

      @Override
      public String getValue() {
         return identifier;
      }

      @Override
      public void acceptVisitor(Visitor visitor) {
         visitor.visit(this);
      }

      @Override
      public String toString() {
         return identifier;
      }
   }

   public static final class Array extends Value {

      private final List<Value> values;

      public Array(long pos, List<Value> values) {
         super(pos);
         this.values = values;
      }

      public List<Value> getValues() {
         return values;
      }

      @Override
      public List<Object> getValue() {
         List<Object> valueList = new ArrayList<>(values.size());
         for (Value val : values) {
            valueList.add(val.getValue());
         }
         return valueList;
      }

      @Override
      public void acceptVisitor(Visitor visitor) {
         visitor.visit(this);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append('[');
         for (Value v : values) {
            if (sb.length() > 1) {
               sb.append(", ");
            }
            sb.append(v);
         }
         sb.append(']');
         return sb.toString();
      }
   }

   /**
    * A constant value of type: {@link String}, {@link Character}, {@link Boolean} or {@link Number}.
    */
   public static final class Literal extends Value {

      private final Object value;

      public Literal(long pos, Object value) {
         super(pos);
         if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
         }
         this.value = value;
      }

      @Override
      public Object getValue() {
         return value;
      }

      @Override
      public void acceptVisitor(Visitor visitor) {
         visitor.visit(this);
      }

      @Override
      public String toString() {
         if (value instanceof String) {
            return "\"" + value + "\"";
         }
         if (value instanceof Character) {
            return "'" + value + "'";
         }
         return value.toString();
      }
   }

   public void acceptVisitor(Visitor visitor) {
      visitor.visit(this);
   }

   public static class Visitor {

      public void visit(Annotation tree) {
         visit((AnnotationElement) tree);
      }

      public void visit(Attribute tree) {
         visit((AnnotationElement) tree);
      }

      public void visit(Array tree) {
         visit((AnnotationElement) tree);
      }

      public void visit(Identifier tree) {
         visit((AnnotationElement) tree);
      }

      public void visit(Literal tree) {
         visit((AnnotationElement) tree);
      }

      public void visit(AnnotationElement annotationElement) {
         throw new IllegalStateException("Unexpected annotation element: " + annotationElement);
      }
   }
}
