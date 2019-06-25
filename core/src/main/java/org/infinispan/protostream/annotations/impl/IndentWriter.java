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
   private static final int TAB_SIZE = 3;

   private int indent = 0;

   private boolean indentNeeded = false;

   public IndentWriter() {
   }

   /**
    * Increase indentation.
    */
   public IndentWriter inc() {
      indent++;
      return this;
   }

   /**
    * Decrease indentation.
    */
   public IndentWriter dec() {
      if (indent == 0) {
         throw new IllegalStateException();
      }
      indent--;
      return this;
   }

   @Override
   public void write(int c) {
      if (indentNeeded) {
         for (int i = indent * TAB_SIZE; i > 0; i--) {
            sb.append(' ');
         }
      }
      sb.append((char) c);
      indentNeeded = c == '\n';
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
