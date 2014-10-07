package org.infinispan.protostream.impl.parser;

import org.infinispan.protostream.AnnotationParserException;
import org.infinispan.protostream.descriptors.AnnotationElement;

import java.text.MessageFormat;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
final class AnnotationLexer {

   AnnotationTokens token;
   long pos;
   long lastPos;
   String name;

   // a temporary buffer used for accumulating the current token
   private char[] sbuf = new char[128];
   private int sp;

   private final char[] buf;
   private final int buflen;
   private int bp = -1;

   private char ch;
   private int line = 1;
   private int col = 0;

   public AnnotationLexer(char[] input) {
      buf = input;
      buflen = input.length;
      scanNextChar();
      skipNoise();
   }

   public void skipNoise() {
      if (token == AnnotationTokens.AT || token == AnnotationTokens.EOF) {
         return;
      }

      sp = 0;
      do {
         switch (ch) {
            case ' ':
            case '\t':
               scanNextChar();
               continue;
            case '\f':
               col = 0;
               scanNextChar();
               continue;
            case '\r':
               line++;
               col = 0;
               scanNextChar();
               if (ch == '\n') {
                  col = 0;
                  scanNextChar();
               }
               continue;
            case '\n':
               line++;
               col = 0;
               scanNextChar();
               continue;
            case 0:
            case '@':
               nextToken();
               return;
            default:
               scanNextChar();
         }
      } while (bp != buflen);

      token = AnnotationTokens.EOF;
   }

   private void scanNextChar() {
      bp++;
      if (bp == buflen) {
         ch = 0;
      } else {
         ch = buf[bp];
         col++;
      }
   }

   private void putChar(char c) {
      if (sp == sbuf.length) {
         char newSbuf[] = new char[sbuf.length * 2];
         System.arraycopy(sbuf, 0, newSbuf, 0, sbuf.length);
         sbuf = newSbuf;
      }
      sbuf[sp++] = c;
   }

   private void scanLiteralChar() {
      if (ch == '\\') {
         scanNextChar();
         switch (ch) {
            case 'b':
               putChar('\b');
               scanNextChar();
               break;
            case 't':
               putChar('\t');
               scanNextChar();
               break;
            case 'n':
               putChar('\n');
               scanNextChar();
               break;
            case 'f':
               putChar('\f');
               scanNextChar();
               break;
            case 'r':
               putChar('\r');
               scanNextChar();
               break;
            case '\'':
               putChar('\'');
               scanNextChar();
               break;
            case '"':
               putChar('"');
               scanNextChar();
               break;
            case '\\':
               putChar('\\');
               scanNextChar();
               break;
            default:
               throw lexerError(AnnotationElement.makePosition(line, col), "illegal escape character");
         }
      } else if (bp != buflen) {
         putChar(ch);
         scanNextChar();
      }
   }

   private void scanDecimal() {
      while (Character.digit(ch, 10) >= 0) {
         putChar(ch);
         scanNextChar();
      }
      if (ch == 'e' || ch == 'E') {
         putChar(ch);
         scanNextChar();
         if (ch == '+' || ch == '-') {
            putChar(ch);
            scanNextChar();
         }
         if ('0' <= ch && ch <= '9') {
            do {
               putChar(ch);
               scanNextChar();
            } while ('0' <= ch && ch <= '9');
         } else {
            throw lexerError("malformed floating point literal");
         }
      }
      if (ch == 'f' || ch == 'F') {
         scanNextChar();
         token = AnnotationTokens.FLOAT_LITERAL;
      } else {
         if (ch == 'd' || ch == 'D') {
            scanNextChar();
         }
         token = AnnotationTokens.DOUBLE_LITERAL;
      }
   }

   private void scanNumber() {
      while (Character.digit(ch, 10) >= 0) {
         putChar(ch);
         scanNextChar();
      }
      if (ch == '.') {
         putChar(ch);
         scanNextChar();
         scanDecimal();
      } else if (ch == 'e' || ch == 'E' || ch == 'f' || ch == 'F' || ch == 'd' || ch == 'D') {
         scanDecimal();
      } else if (ch == 'l' || ch == 'L') {
         scanNextChar();
         token = AnnotationTokens.LONG_LITERAL;
      } else {
         token = AnnotationTokens.INT_LITERAL;
      }
   }

   private void scanIdentifier() {
      do {
         putChar(ch);
         ch = buf[++bp];
         col++;
      }
      while (bp < buflen - 1 && (ch == '_' || ch == '$' || ch >= '0' && ch <= '9'
                                       || ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z'
                                       || ch > 128 && Character.isJavaIdentifierPart(ch)));
      name = new String(sbuf, 0, sp);
      AnnotationTokens tok = AnnotationTokens.byName(name);
      token = tok == null ? AnnotationTokens.IDENTIFIER : tok;
   }

   public int mark() {
      return bp;
   }

   public String getText(int startIndex, int endIndex) {
      return new String(buf, startIndex, endIndex - startIndex);
   }

   public void nextToken() {
      lastPos = AnnotationElement.makePosition(line, col);
      sp = 0;
      while (true) {
         pos = AnnotationElement.makePosition(line, col);
         switch (ch) {
            case ',':
               scanNextChar();
               token = AnnotationTokens.COMMA;
               return;
            case '(':
               scanNextChar();
               token = AnnotationTokens.LPAREN;
               return;
            case ')':
               scanNextChar();
               token = AnnotationTokens.RPAREN;
               return;
            case '{':
               scanNextChar();
               token = AnnotationTokens.LBRACE;
               return;
            case '}':
               scanNextChar();
               token = AnnotationTokens.RBRACE;
               return;
            case '@':
               scanNextChar();
               token = AnnotationTokens.AT;
               return;
            case '=':
               scanNextChar();
               token = AnnotationTokens.EQ;
               return;
            case '.':
               scanNextChar();
               if ('0' <= ch && ch <= '9') {
                  putChar('.');
                  scanDecimal();
               } else {
                  token = AnnotationTokens.DOT;
               }
               return;
            case '_':
            case '$':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
               scanIdentifier();
               return;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
               scanNumber();
               return;
            case '\'':
               scanNextChar();
               if (ch == '\'') {
                  throw lexerError("empty character literal");
               } else {
                  if (ch == '\r' || ch == '\n') {
                     throw lexerError(pos, "illegal line end in character literal");
                  }
                  scanLiteralChar();
                  if (ch == '\'') {
                     scanNextChar();
                     token = AnnotationTokens.CHARACTER_LITERAL;
                  } else {
                     throw lexerError(pos, "unclosed character literal");
                  }
               }
               return;
            case '"':
               scanNextChar();
               while (ch != '"' && ch != '\r' && ch != '\n' && bp < buflen) {
                  scanLiteralChar();
               }
               if (ch == '"') {
                  token = AnnotationTokens.STRING_LITERAL;
                  scanNextChar();
               } else {
                  throw lexerError(pos, "unclosed string literal");
               }
               return;
            case ' ':
            case '\t':
               scanNextChar();
               continue;
            case '\f':
               col = 0;
               scanNextChar();
               continue;
            case '\r':
               line++;
               col = 0;
               scanNextChar();
               if (ch == '\n') {
                  col = 0;
                  scanNextChar();
               }
               continue;
            case '\n':
               line++;
               col = 0;
               scanNextChar();
               continue;
            default:
               if (ch == 0 && bp == buflen) {
                  token = AnnotationTokens.EOF;
               } else if (Character.isJavaIdentifierStart(ch)) {
                  scanIdentifier();
               } else {
                  throw lexerError("illegal character: {0}", String.valueOf((int) ch));
               }
               return;
         }
      }
   }

   private AnnotationParserException lexerError(long pos, String errorMsg, String... errorArgs) {
      return new AnnotationParserException("Error: " + AnnotationElement.positionToString(pos) + ": " + MessageFormat.format(errorMsg, errorArgs));
   }

   private AnnotationParserException lexerError(String errorMsg, String... errorArgs) {
      return lexerError(pos, errorMsg, errorArgs);
   }
}
