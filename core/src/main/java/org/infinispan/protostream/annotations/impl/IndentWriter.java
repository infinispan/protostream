package org.infinispan.protostream.annotations.impl;

import java.io.Writer;

/**
 * A Writer capable of appending Strings in a similar manner to StringWriter but with indentation capabilities to
 * support more readable code generation. No IOExceptions are ever thrown. Closing has no effect.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class IndentWriter extends Writer {

   private final StringBuilder sb = new StringBuilder(256);

   /**
    * The 'equivalent' of one TAB character, because we do not use TABs.
    */
   private static final String TAB = "   ";
   private int indent = 0;
   private boolean indentNeeded = false;

   public IndentWriter() {
   }

   /**
    * Increase indentation.
    */
   public void inc() {
      indent++;
   }

   /**
    * Decrease indentation.
    */
   public void dec() {
      if (indent > 0) {
         indent--;
      }
   }

   @Override
   public void write(int c) {
      if (indentNeeded) {
         indentNeeded = false;
         for (int i = 0; i < indent; i++) {
            sb.append(TAB);
         }
      }
      sb.append((char) c);
      if (c == '\n') {
         indentNeeded = true;
      }
   }

   @Override
   public void write(char[] buf, int off, int len) {
      for (int i = off; i < off + len; i++) {
         write(buf[i]);
      }
   }

   @Override
   public void write(String s) {
      if (s == null) {
         s = "null";
      }
      write(s, 0, s.length());
   }

   @Override
   public void write(String s, int off, int len) {
      for (int i = off; i < off + len; i++) {
         write(s.charAt(i));
      }
   }

   @Override
   public IndentWriter append(CharSequence cs) {
      write(cs == null ? "null" : cs.toString());
      return this;
   }

   @Override
   public IndentWriter append(CharSequence cs, int start, int end) {
      if (cs == null) {
         cs = "null";
      }
      write(cs.subSequence(start, end).toString());
      return this;
   }

   @Override
   public IndentWriter append(char c) {
      write(c);
      return this;
   }

   @Override
   public String toString() {
      return sb.toString();
   }

   @Override
   public void flush() {
   }

   @Override
   public void close() {
   }
}
