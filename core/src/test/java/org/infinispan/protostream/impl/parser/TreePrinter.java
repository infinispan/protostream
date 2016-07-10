package org.infinispan.protostream.impl.parser;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

import org.infinispan.protostream.descriptors.AnnotationElement;

/**
 * Visits a syntax tree and pretty-prints it into a String.
 *
 * @author anistor@redhat.com
 * @since 2.0
 */
public class TreePrinter extends AnnotationElement.Visitor {

   private final StringWriter out = new StringWriter();
   private final int indentWidth = 3;
   private int leftMargin = 0;

   public TreePrinter() {
   }

   public String getOutput() {
      return out.toString();
   }

   public void printAnnotations(Map<String, AnnotationElement.Annotation> annotations) {
      for (AnnotationElement.Annotation annotation : annotations.values()) {
         printTree(annotation);
         out.write('\n');
      }
   }

   private void align() {
      for (int i = 0; i < leftMargin; i++) {
         out.write(' ');
      }
   }

   private void indent() {
      leftMargin += indentWidth;
   }

   private void undent() {
      leftMargin -= indentWidth;
   }

   private void printTree(AnnotationElement annotationElement) {
      if (annotationElement == null) {
         out.write("/*null tree*/");
      } else {
         annotationElement.acceptVisitor(this);
      }
   }

   @Override
   public void visit(AnnotationElement.Annotation annotation) {
      out.write('@');
      out.write(annotation.getName());
      out.write('(');
      out.write('\n');
      indent();
      for (Iterator<AnnotationElement.Attribute> it = annotation.getAttributes().values().iterator(); it.hasNext(); ) {
         align();
         printTree(it.next());
         if (it.hasNext()) {
            out.write(",");
         }
         out.write('\n');
      }
      undent();
      align();
      out.write(')');
   }

   @Override
   public void visit(AnnotationElement.Array array) {
      out.write('{');
      out.write('\n');
      indent();
      for (Iterator<AnnotationElement.Value> it = array.getValues().iterator(); it.hasNext(); ) {
         align();
         printTree(it.next());
         if (it.hasNext()) {
            out.write(',');
         }
         out.write('\n');
      }
      undent();
      align();
      out.write('}');
   }

   @Override
   public void visit(AnnotationElement.Identifier identifier) {
      out.write(identifier.getIdentifier());
   }

   @Override
   public void visit(AnnotationElement.Attribute attribute) {
      out.write(attribute.getName());
      out.write('=');
      printTree(attribute.getValue());
   }

   @Override
   public void visit(AnnotationElement.Literal literal) {
      out.write(String.valueOf(literal.getValue()));
   }
}
