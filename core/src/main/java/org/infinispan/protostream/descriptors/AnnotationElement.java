package org.infinispan.protostream.descriptors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
public abstract class AnnotationElement {

   private static final long LINESHIFT = 32;

   private static final long COLUMNMASK = (1L << LINESHIFT) - 1;

   public final long position;

   public static int line(long pos) {
      return (int) (pos >>> LINESHIFT);
   }

   public static int column(long pos) {
      return (int) (pos & COLUMNMASK);
   }

   public static long makePosition(int line, int column) {
      return ((long) line << LINESHIFT) + column;
   }

   public static String positionToString(long pos) {
      return line(pos) + "," + column(pos);
   }

   protected AnnotationElement(long position) {
      this.position = position;
   }

   public static abstract class Value extends AnnotationElement {

      protected Value(long pos) {
         super(pos);
      }

      public abstract Object getValue();
   }

   public static final class Annotation extends Value {

      public static final String DEFAULT_ATTRIBUTE = "value";

      public final String name;

      public final Map<String, Attribute> attributes;

      public Annotation(long pos, String name, Map<String, Attribute> attributes) {
         super(pos);
         this.name = name;
         this.attributes = attributes;
      }

      @Override
      public Annotation getValue() {
         return this;
      }

      public Value getDefaultAttributeValue() {
         return getAttributeValue(DEFAULT_ATTRIBUTE);
      }

      public Value getAttributeValue(String attributeName) {
         Attribute attribute = attributes.get(attributeName);
         return attribute != null ? attribute.value : null;
      }

      public void acceptVisitor(Visitor visitor) {
         visitor.visit(this);
      }
   }

   public static final class Attribute extends AnnotationElement {

      public final String name;

      // this can be an Identifier, a Literal, an Array or an Annotation
      public final Value value;

      public Attribute(long pos, String name, Value value) {
         super(pos);
         this.name = name;
         this.value = value;
      }

      public void acceptVisitor(Visitor visitor) {
         visitor.visit(this);
      }
   }

   public static final class Identifier extends Value {

      public final String identifier;

      public Identifier(long pos, String identifier) {
         super(pos);
         this.identifier = identifier;
      }

      @Override
      public String getValue() {
         return identifier;
      }

      public void acceptVisitor(Visitor visitor) {
         visitor.visit(this);
      }
   }

   public static final class Array extends Value {

      public final List<Value> values;

      public Array(long pos, List<Value> values) {
         super(pos);
         this.values = values;
      }

      @Override
      public List<Object> getValue() {
         List<Object> valueList = new ArrayList<>(values.size());
         for (Value val : values) {
            valueList.add(val.getValue());
         }
         return valueList;
      }

      public void acceptVisitor(Visitor visitor) {
         visitor.visit(this);
      }
   }

   /**
    * A String, Character or a Number.
    */
   public static final class Literal extends Value {

      public final Object value;

      public Literal(long pos, Object value) {
         super(pos);
         this.value = value;
      }

      @Override
      public Object getValue() {
         return value;
      }

      public void acceptVisitor(Visitor visitor) {
         visitor.visit(this);
      }
   }

   public void acceptVisitor(Visitor visitor) {
      visitor.visit(this);
   }

   public static abstract class Visitor {

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
