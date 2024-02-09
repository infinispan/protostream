package org.infinispan.protostream.annotations.impl;

import java.io.PrintWriter;
import java.io.Writer;

/**
 * A Writer with indentation capabilities to support more readable code generation.
 *
 * @author anistor@redhat.com
 * @since 3.0
 */
public final class IndentWriter extends PrintWriter {
   /**
    * The 'equivalent' of one TAB character, because we do not use TABs.
    */
   private static final int TAB_SIZE = 3;
   private static final String LOTS_OF_SPACES = " ".repeat(200); // Should be more than enough

   private int indent = 0;

   private boolean indentNeeded = false;

   public IndentWriter(Writer out) {
      super(out);
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
         super.write(LOTS_OF_SPACES, 0, indent * TAB_SIZE);
      }
      super.write(c);
      indentNeeded = c == '\n';
   }

   @Override
   public void write(String s) {
      if (indentNeeded) {
         super.write(LOTS_OF_SPACES, 0, indent * TAB_SIZE);
      }
      if (s == null) {
         s = "null";
      }
      super.write(s, 0, s.length());
      indentNeeded = s.endsWith("\n");
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
   public void println() {
      super.println();
      indentNeeded = true;
   }

   @Override
   public PrintWriter printf(String format, Object... args) {
      return super.printf(format, args);
   }
}
