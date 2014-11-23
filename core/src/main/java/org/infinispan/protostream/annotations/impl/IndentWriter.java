package org.infinispan.protostream.annotations.impl;

import java.io.StringWriter;

/**
 * A StringWriter with indentation capabilities to support more readable code generation.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
final class IndentWriter extends StringWriter {

   private static final String TAB = "   ";
   private int indent = 0;
   private boolean indentNeeded = false;

   public void inc() {
      indent++;
   }

   public void dec() {
      if (indent > 0) {
         indent--;
      }
   }

   @Override
   public final void write(int c) {
      if (indentNeeded) {
         indentNeeded = false;
         for (int i = 0; i < indent; i++) {
            super.write(TAB);
         }
      }
      super.write(c);
      if (c == '\n') {
         indentNeeded = true;
      }
   }

   @Override
   public final void write(char[] buf, int off, int len) {
      for (int i = off; i < off + len; i++) {
         write(buf[i]);
      }
   }

   @Override
   public final void write(String s) {
      if (s == null) {
         s = "null";
      }
      write(s, 0, s.length());
   }

   @Override
   public final void write(String s, int off, int len) {
      for (int i = off; i < off + len; i++) {
         write(s.charAt(i));
      }
   }
}
