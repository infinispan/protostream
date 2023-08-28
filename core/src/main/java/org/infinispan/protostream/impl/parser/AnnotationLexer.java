package org.infinispan.protostream.impl.parser;

import org.infinispan.protostream.AnnotationParserException;
import org.infinispan.protostream.descriptors.AnnotationElement;

/**
 * Splits an input array of characters into tokens. See {@link AnnotationTokens}.
 *
 * @author anistor@redhat.com
 * @since 2.0
 */
final class AnnotationLexer {

   /**
    * Last recognized token.
    */
   AnnotationTokens token;
   long pos;
   long lastPos;

   /**
    * The actual identifier, if last recognized token is {@link AnnotationTokens#IDENTIFIER}, or {@code null} otherwise.
    */
   String identifier;

   // a temporary buffer used for accumulating the current token
   private final StringBuilder sb = new StringBuilder(32);

   /**
    * The input buffer.
    */
   private final char[] input;
   private int inputPos = -1;

   /**
    * The current input char.
    */
   private char ch = '\0';
   private int line = 1;
   private int col = 0;

   /**
    * Indicates if all scanned characters on current line up to current char are whitespace.
    */
   private boolean leadingWhitespace = true;

   /**
    * Do we expect to encounter non-syntactically correct text which is probably human readable documentation
    * surrounding the annotations?
    */
   private final boolean expectDocNoise;

   AnnotationLexer(char[] input, boolean expectDocNoise) {
      this.input = input;
      this.expectDocNoise = expectDocNoise;
      scanNextChar();
      skipDocNoise();
   }

   /**
    * Skip characters until we stumble on an annotation.
    */
   public void skipDocNoise() {
      if (token == AnnotationTokens.AT || token == AnnotationTokens.EOF) {
         return;
      }

      sb.setLength(0);
      do {
         switch (ch) {
            case ' ':
            case '\t':
               scanNextChar();
               continue;
            case '\f':
               col = 0;
               leadingWhitespace = true;
               scanNextChar();
               continue;
            case '\r':
               line++;
               col = 0;
               leadingWhitespace = true;
               scanNextChar();
               if (ch == '\n') {
                  col = 0;
                  leadingWhitespace = true;
                  scanNextChar();
               }
               continue;
            case '\n':
               line++;
               col = 0;
               leadingWhitespace = true;
               scanNextChar();
               continue;
            case '@':
               if (!leadingWhitespace) {
                  throw new AnnotationParserException(String.format("Error: %d,%d: Annotations must start on an empty line", line, col));
               }
               // intentional fall-through
            case '\0':
               nextToken();
               return;
            default:
               if (expectDocNoise) {
                  scanNextChar();
               } else {
                  throw new AnnotationParserException(String.format("Error: %d,%d: Unexpected character: %c", line, col, ch));
               }
         }
      } while (inputPos != input.length);

      token = AnnotationTokens.EOF;
   }

   private void scanNextChar() {
      if (ch != '\0' && !Character.isWhitespace(ch)) {
         leadingWhitespace = false;
      }
      inputPos++;
      if (inputPos == input.length) {
         ch = '\0';
      } else {
         ch = input[inputPos];
         col++;
      }
   }

   private void scanLiteralChar() {
      if (ch == '\\') {
         scanNextChar();
         switch (ch) {
            case 'b':
               sb.append('\b');
               scanNextChar();
               break;
            case 't':
               sb.append('\t');
               scanNextChar();
               break;
            case 'n':
               sb.append('\n');
               scanNextChar();
               break;
            case 'f':
               sb.append('\f');
               scanNextChar();
               break;
            case 'r':
               sb.append('\r');
               scanNextChar();
               break;
            case 'u':
               scanUnicode();
               break;
            case '\'':
               sb.append('\'');
               scanNextChar();
               break;
            case '"':
               sb.append('"');
               scanNextChar();
               break;
            case '\\':
               sb.append('\\');
               scanNextChar();
               break;
            default:
               throw new AnnotationParserException(String.format("Error: %d,%d: illegal escape character: %c", line, col, ch));
         }
      } else if (inputPos != input.length) {
         sb.append(ch);
         scanNextChar();
      }
   }

   private void scanDecimal() {
      while (Character.digit(ch, 10) >= 0) {
         sb.append(ch);
         scanNextChar();
      }
      if (ch == 'e' || ch == 'E') {
         sb.append(ch);
         scanNextChar();
         if (ch == '+' || ch == '-') {
            sb.append(ch);
            scanNextChar();
         }
         if ('0' <= ch && ch <= '9') {
            do {
               sb.append(ch);
               scanNextChar();
            } while ('0' <= ch && ch <= '9');
         } else {
            throw new AnnotationParserException(String.format("Error: %s: malformed floating point literal", AnnotationElement.positionToString(pos)));
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
         sb.append(ch);
         scanNextChar();
      }
      if (ch == '.') {
         sb.append(ch);
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

   private void scanUnicode() {
      sb.append("\\u");
      for(int i = 0; i < 4; i++) {
         scanNextChar();
         if (ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'F' || ch >= 'a' && ch <= 'f') {
            sb.append(ch);
         } else {
            throw new AnnotationParserException(String.format("Error: %s: malformed unicode escape", AnnotationElement.positionToString(pos)));
         }
      }
   }

   private void scanIdentifier() {
      do {
         sb.append(ch);
         if (++inputPos == input.length) {
            ch = '\0';
            break;
         }
         ch = input[inputPos];
         col++;
      }
      while (ch == '_' || ch == '$' || ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z'
            || ch > 127 && Character.isJavaIdentifierPart(ch));
      String keywordOrIdentifier = sb.toString();
      AnnotationTokens tok = AnnotationTokens.byName(keywordOrIdentifier);
      if (tok == null) {
         token = AnnotationTokens.IDENTIFIER;
         identifier = keywordOrIdentifier;
      } else {
         token = tok;
         identifier = null;
      }
   }

   public int getBufferPos() {
      return inputPos;
   }

   public String getText(int startIndex, int endIndex) {
      return new String(input, startIndex, endIndex - startIndex);
   }

   public void nextToken() {
      lastPos = AnnotationElement.makePosition(line, col);
      sb.setLength(0);
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
                  sb.append('.');
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
                  throw new AnnotationParserException(String.format("Error: %s: empty character literal", AnnotationElement.positionToString(pos)));
               } else {
                  if (ch == '\r' || ch == '\n') {
                     throw new AnnotationParserException(String.format("Error: %s: illegal line end in character literal", AnnotationElement.positionToString(pos)));
                  }
                  scanLiteralChar();
                  if (ch == '\'') {
                     scanNextChar();
                     token = AnnotationTokens.CHARACTER_LITERAL;
                  } else {
                     throw new AnnotationParserException(String.format("Error: %s: unclosed character literal", AnnotationElement.positionToString(pos)));
                  }
               }
               return;
            case '"':
               scanNextChar();
               while (ch != '"' && ch != '\r' && ch != '\n' && inputPos < input.length) {
                  scanLiteralChar();
               }
               if (ch == '"') {
                  token = AnnotationTokens.STRING_LITERAL;
                  scanNextChar();
               } else {
                  throw new AnnotationParserException(String.format("Error: %s: unclosed string literal", AnnotationElement.positionToString(pos)));
               }
               return;
            case ' ':
            case '\t':
               scanNextChar();
               continue;
            case '\f':
               col = 0;
               leadingWhitespace = true;
               scanNextChar();
               continue;
            case '\r':
               line++;
               col = 0;
               leadingWhitespace = true;
               scanNextChar();
               if (ch == '\n') {
                  col = 0;
                  leadingWhitespace = true;
                  scanNextChar();
               }
               continue;
            case '\n':
               line++;
               col = 0;
               leadingWhitespace = true;
               scanNextChar();
               continue;
            default:
               if (ch == '\0' && inputPos == input.length) {
                  token = AnnotationTokens.EOF;
               } else if (Character.isJavaIdentifierStart(ch)) {
                  scanIdentifier();
               } else {
                  throw new AnnotationParserException(String.format("Error: %s: illegal character: %c", AnnotationElement.positionToString(pos), ch));
               }
               return;
         }
      }
   }
}
