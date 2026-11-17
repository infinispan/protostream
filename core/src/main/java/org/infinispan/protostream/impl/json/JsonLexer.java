package org.infinispan.protostream.impl.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

/**
 * A strict, streaming JSON (RFC 8259) tokenizer.
 * <p>
 * The API mirrors the subset of Jackson's <code>JsonParser</code> that ProtoStream used: {@link #nextToken()} returns
 * the next {@link Token} (or <code>null</code> at the end of the input) and the accessors read the current token.
 * <p>
 * The grammar and the limits match Jackson's defaults: no comments, no single quotes, no unquoted names, no trailing
 * commas, no leading zeros or <code>+</code> signs, no <code>NaN</code>/<code>Infinity</code>, no unescaped control
 * characters in strings. Nesting is limited to {@value #MAX_DEPTH} levels, numbers to {@value #MAX_NUMBER_LENGTH}
 * digits, names to {@value #MAX_NAME_LENGTH} characters and strings to {@value #MAX_STRING_LENGTH} characters.
 * Unlike Jackson, only whitespace may follow the root value.
 * <p>
 * All syntax errors, overflows and invalid accessor calls throw {@link ParseException}.
 */
public final class JsonLexer {

   public enum Token {
      START_OBJECT,
      END_OBJECT,
      START_ARRAY,
      END_ARRAY,
      FIELD_NAME,
      VALUE_STRING,
      VALUE_NUMBER_INT,
      VALUE_NUMBER_FLOAT,
      VALUE_TRUE,
      VALUE_FALSE,
      VALUE_NULL
   }

   /**
    * Thrown when the input is not valid JSON, exceeds a limit, or a value cannot be read as the requested type.
    */
   public static final class ParseException extends IOException {
      ParseException(String message) {
         super(message);
      }
   }

   static final int MAX_DEPTH = 1000;
   static final int MAX_NUMBER_LENGTH = 1000;
   static final int MAX_NAME_LENGTH = 50_000;
   static final int MAX_STRING_LENGTH = 20_000_000;

   private static final int EOF = -1;

   private final Reader reader;
   private final char[] buffer = new char[8192];
   private int pos;
   private int limit;
   private int line = 1;
   private int column;
   // Position of the last character read, reported in errors
   private int errorLine = 1;
   private int errorColumn;

   // Per nesting level: whether it is an object, whether it already has an entry, and the last field name read
   private boolean[] isObject = new boolean[16];
   private boolean[] hasEntries = new boolean[16];
   private String[] names = new String[16];
   private int depth;
   private boolean expectingFieldValue;
   // Like Jackson, the value following a field name is read with the name, unless it is a string or a container
   private Token pendingToken;
   private String pendingText;
   private int pendingChar;
   private boolean rootRead;

   private Token token;
   private String text;

   public JsonLexer(Reader reader) {
      this.reader = reader;
   }

   /**
    * Advances to the next token.
    *
    * @return the next token, or <code>null</code> once the root value has been fully read
    */
   public Token nextToken() throws IOException {
      text = null;
      if (expectingFieldValue) {
         expectingFieldValue = false;
         if (pendingToken != null) {
            text = pendingText;
            token = pendingToken;
            pendingToken = null;
            pendingText = null;
            return token;
         }
         return token = readValue(pendingChar);
      }
      int c = skipWhitespace();
      if (depth == 0) {
         if (rootRead) {
            if (c != EOF) {
               throw error("Unexpected character " + describe(c) + " after the root value");
            }
            return token = null;
         }
         if (c == EOF) {
            return token = null;
         }
         rootRead = true;
         return token = readValue(c);
      }
      if (isObject[depth]) {
         if (c == '}') {
            depth--;
            return token = Token.END_OBJECT;
         }
         if (hasEntries[depth]) {
            if (c != ',') {
               throw error("Unexpected character " + describe(c) + ": expected ',' or '}'");
            }
            c = skipWhitespace();
         }
         if (c != '"') {
            throw error("Unexpected character " + describe(c) + ": expected a double-quoted field name");
         }
         String name = readString(MAX_NAME_LENGTH, "Name");
         c = skipWhitespace();
         if (c != ':') {
            throw error("Unexpected character " + describe(c) + ": expected ':' after the field name");
         }
         hasEntries[depth] = true;
         names[depth] = name;
         expectingFieldValue = true;
         c = skipWhitespace();
         if (c == '"' || c == '{' || c == '[') {
            pendingChar = c;
         } else {
            pendingToken = readValue(c);
            pendingText = text;
            text = null;
         }
         return token = Token.FIELD_NAME;
      }
      if (c == ']') {
         depth--;
         return token = Token.END_ARRAY;
      }
      if (hasEntries[depth]) {
         if (c != ',') {
            throw error("Unexpected character " + describe(c) + ": expected ',' or ']'");
         }
         c = skipWhitespace();
      }
      hasEntries[depth] = true;
      return token = readValue(c);
   }

   /**
    * @return the current token, or <code>null</code> before the first token and after the last one
    */
   public Token currentToken() {
      return token;
   }

   /**
    * @return the name of the current field: the name itself for {@link Token#FIELD_NAME}, the name of the field holding
    * the value for values and containers, or <code>null</code> outside of objects
    */
   public String currentName() {
      if (token == Token.START_OBJECT || token == Token.START_ARRAY) {
         return names[depth - 1];
      }
      return names[depth];
   }

   /**
    * @return the textual representation of the current token: the field name, the unescaped string, the number as
    * written in the input, the literal or the structural character; <code>null</code> if there is no current token
    */
   public String getText() {
      if (token == null) {
         return null;
      }
      return switch (token) {
         case START_OBJECT -> "{";
         case END_OBJECT -> "}";
         case START_ARRAY -> "[";
         case END_ARRAY -> "]";
         case FIELD_NAME -> names[depth];
         case VALUE_TRUE -> "true";
         case VALUE_FALSE -> "false";
         case VALUE_NULL -> "null";
         default -> text;
      };
   }

   public int getIntValue() throws IOException {
      requireToken(Token.VALUE_NUMBER_INT);
      try {
         return Integer.parseInt(text);
      } catch (NumberFormatException e) {
         throw error("Numeric value (" + text + ") out of range of int (" + Integer.MIN_VALUE + " - " + Integer.MAX_VALUE + ")");
      }
   }

   public long getLongValue() throws IOException {
      requireToken(Token.VALUE_NUMBER_INT);
      try {
         return Long.parseLong(text);
      } catch (NumberFormatException e) {
         throw error("Numeric value (" + text + ") out of range of long (" + Long.MIN_VALUE + " - " + Long.MAX_VALUE + ")");
      }
   }

   /**
    * Values from -128 to 255 are accepted, 128 to 255 wrapping to negative bytes as with a <code>(byte)</code> cast.
    */
   public byte getByteValue() throws IOException {
      requireToken(Token.VALUE_NUMBER_INT);
      int value;
      try {
         value = Integer.parseInt(text);
      } catch (NumberFormatException e) {
         value = Integer.MAX_VALUE;
      }
      if (value < Byte.MIN_VALUE || value > 255) {
         throw error("Numeric value (" + text + ") out of range of Java byte");
      }
      return (byte) value;
   }

   public float getFloatValue() throws IOException {
      requireNumber();
      float value = Float.parseFloat(text);
      // An integer token is converted from its integer value, so "-0" is 0.0
      return token == Token.VALUE_NUMBER_INT ? value + 0.0f : value;
   }

   public double getDoubleValue() throws IOException {
      requireNumber();
      double value = Double.parseDouble(text);
      return token == Token.VALUE_NUMBER_INT ? value + 0.0 : value;
   }

   public boolean getBooleanValue() throws IOException {
      if (token == Token.VALUE_TRUE) {
         return true;
      }
      if (token == Token.VALUE_FALSE) {
         return false;
      }
      throw error("Current token (" + token + ") not of boolean type");
   }

   /**
    * Decodes the current string as standard Base64. Padding is required; whitespace is allowed between groups of four
    * characters, and padded groups may be concatenated.
    */
   public byte[] getBinaryValue() throws IOException {
      requireToken(Token.VALUE_STRING);
      ByteArrayOutputStream out = new ByteArrayOutputStream(text.length() * 3 / 4);
      int ptr = 0;
      int len = text.length();
      while (true) {
         char ch;
         do {
            if (ptr >= len) {
               return out.toByteArray();
            }
            ch = text.charAt(ptr++);
         } while (ch <= ' ');
         int data = base64(ch, 0);
         data = (data << 6) | base64(base64Char(ptr++), 1);
         ch = base64Char(ptr++);
         if (ch == '=') {
            if (base64Char(ptr++) != '=') {
               throw invalidBase64(text.charAt(ptr - 1), 3, "expected padding character '='");
            }
            out.write(data >> 4);
            continue;
         }
         data = (data << 6) | base64(ch, 2);
         ch = base64Char(ptr++);
         if (ch == '=') {
            data >>= 2;
            out.write(data >> 8);
            out.write(data);
            continue;
         }
         data = (data << 6) | base64(ch, 3);
         out.write(data >> 16);
         out.write(data >> 8);
         out.write(data);
      }
   }

   private char base64Char(int index) throws ParseException {
      if (index >= text.length()) {
         throw error("Unexpected end of Base64 content: padding is required");
      }
      return text.charAt(index);
   }

   private int base64(char ch, int index) throws ParseException {
      if (ch >= 'A' && ch <= 'Z') return ch - 'A';
      if (ch >= 'a' && ch <= 'z') return ch - 'a' + 26;
      if (ch >= '0' && ch <= '9') return ch - '0' + 52;
      if (ch == '+') return 62;
      if (ch == '/') return 63;
      throw invalidBase64(ch, index, null);
   }

   private ParseException invalidBase64(char ch, int index, String detail) {
      String message = "Illegal character " + describe(ch) + " in Base64 content at position " + index + " of a group";
      return error(detail == null ? message : message + ": " + detail);
   }

   private void requireToken(Token expected) throws ParseException {
      if (token != expected) {
         throw error("Current token (" + token + ") is not " + expected);
      }
   }

   private void requireNumber() throws ParseException {
      if (token != Token.VALUE_NUMBER_INT && token != Token.VALUE_NUMBER_FLOAT) {
         throw error("Current token (" + token + ") not numeric");
      }
   }

   private Token readValue(int c) throws IOException {
      switch (c) {
         case '{':
            push(true);
            return Token.START_OBJECT;
         case '[':
            push(false);
            return Token.START_ARRAY;
         case '"':
            text = readString(MAX_STRING_LENGTH, "String");
            return Token.VALUE_STRING;
         case 't':
            readLiteral("true");
            return Token.VALUE_TRUE;
         case 'f':
            readLiteral("false");
            return Token.VALUE_FALSE;
         case 'n':
            readLiteral("null");
            return Token.VALUE_NULL;
         default:
            if (c == '-' || (c >= '0' && c <= '9')) {
               return readNumber(c);
            }
            throw error("Unexpected character " + describe(c) + ": expected a value");
      }
   }

   private void push(boolean object) throws ParseException {
      if (depth == MAX_DEPTH) {
         throw error("Document nesting depth exceeds the maximum allowed (" + MAX_DEPTH + ")");
      }
      depth++;
      if (depth == isObject.length) {
         isObject = Arrays.copyOf(isObject, depth * 2);
         hasEntries = Arrays.copyOf(hasEntries, depth * 2);
         names = Arrays.copyOf(names, depth * 2);
      }
      isObject[depth] = object;
      hasEntries[depth] = false;
      names[depth] = null;
   }

   private void readLiteral(String literal) throws IOException {
      // The first character has already been consumed
      for (int i = 1; i < literal.length(); i++) {
         if (read() != literal.charAt(i)) {
            throw error("Unrecognized token: expected '" + literal + "'");
         }
      }
      requireDelimiter();
   }

   private Token readNumber(int c) throws IOException {
      StringBuilder sb = new StringBuilder();
      boolean integer = true;
      int digits = 0;
      if (c == '-') {
         sb.append('-');
         c = read();
      }
      if (c == '0') {
         sb.append('0');
         digits++;
         c = peek();
         if (c >= '0' && c <= '9') {
            read();
            throw error("Invalid numeric value: leading zeros are not allowed");
         }
      } else if (c >= '1' && c <= '9') {
         sb.append((char) c);
         digits = 1 + readDigits(sb, 1);
      } else {
         throw error("Invalid numeric value: expected a digit, got " + describe(c));
      }
      if (peek() == '.') {
         integer = false;
         sb.append((char) read());
         requireDigit(sb, "after the decimal point");
         digits += 1 + readDigits(sb, digits + 1);
      }
      c = peek();
      if (c == 'e' || c == 'E') {
         integer = false;
         sb.append((char) read());
         c = peek();
         if (c == '+' || c == '-') {
            sb.append((char) read());
         }
         requireDigit(sb, "in the exponent");
         digits += 1 + readDigits(sb, digits + 1);
      }
      requireDelimiter();
      text = sb.toString();
      return integer ? Token.VALUE_NUMBER_INT : Token.VALUE_NUMBER_FLOAT;
   }

   private void requireDigit(StringBuilder sb, String where) throws IOException {
      int c = read();
      if (c < '0' || c > '9') {
         throw error("Invalid numeric value: expected a digit " + where + ", got " + describe(c));
      }
      sb.append((char) c);
   }

   /**
    * Reads a run of digits, failing if the number would exceed {@value #MAX_NUMBER_LENGTH} digits.
    *
    * @param digits the number of digits already read in the number
    * @return the number of digits read
    */
   private int readDigits(StringBuilder sb, int digits) throws IOException {
      int count = 0;
      for (int c = peek(); c >= '0' && c <= '9'; c = peek()) {
         sb.append((char) read());
         if (digits + ++count > MAX_NUMBER_LENGTH) {
            throw error("Number value length exceeds the maximum allowed (" + MAX_NUMBER_LENGTH + " digits)");
         }
      }
      return count;
   }

   /**
    * Literals and numbers must be followed by whitespace, a structural character or the end of the input; at the root,
    * by whitespace or the end of the input.
    */
   private void requireDelimiter() throws IOException {
      int c = peek();
      switch (c) {
         case EOF, ' ', '\t', '\n', '\r':
            return;
         // Not ':', which only follows field names, and those are strings
         case ',', ']', '}':
            if (depth > 0) {
               return;
            }
         default:
            read();
            throw error("Unexpected character " + describe(c) + " after a value");
      }
   }

   private String readString(int maxLength, String kind) throws IOException {
      StringBuilder sb = new StringBuilder();
      while (true) {
         // Fast path: copy runs of plain characters straight from the buffer
         int start = pos;
         while (pos < limit) {
            char ch = buffer[pos];
            if (ch == '"' || ch == '\\' || ch < ' ') {
               break;
            }
            pos++;
         }
         sb.append(buffer, start, pos - start);
         column += pos - start;
         checkLength(sb, maxLength, kind);
         int c = read();
         if (c == '"') {
            return sb.toString();
         }
         if (c == '\\') {
            sb.append(readEscape());
            checkLength(sb, maxLength, kind);
         } else if (c == EOF) {
            throw error("Unexpected end of input in a string");
         } else if (c < ' ') {
            throw error("Illegal unquoted character " + describe(c) + ": control characters must be escaped in strings");
         } else {
            // Only reached when the fast path ran out of buffer
            sb.append((char) c);
         }
      }
   }

   private void checkLength(StringBuilder sb, int maxLength, String kind) throws ParseException {
      if (sb.length() > maxLength) {
         throw error(kind + " length exceeds the maximum allowed (" + maxLength + ")");
      }
   }

   private char readEscape() throws IOException {
      int c = read();
      switch (c) {
         case '"': return '"';
         case '\\': return '\\';
         case '/': return '/';
         case 'b': return '\b';
         case 'f': return '\f';
         case 'n': return '\n';
         case 'r': return '\r';
         case 't': return '\t';
         case 'u': {
            int value = 0;
            for (int i = 0; i < 4; i++) {
               c = read();
               int digit = Character.digit(c, 16);
               if (c > 'f' || digit < 0) {
                  throw error("Unexpected character " + describe(c) + " in a \\u escape: expected a hex digit");
               }
               value = (value << 4) | digit;
            }
            return (char) value;
         }
         default:
            throw error("Unrecognized character escape " + describe(c));
      }
   }

   private int skipWhitespace() throws IOException {
      while (true) {
         int c = read();
         switch (c) {
            case ' ', '\t', '\n', '\r':
               break;
            default:
               return c;
         }
      }
   }

   private int peek() throws IOException {
      if (pos == limit && !fill()) {
         return EOF;
      }
      return buffer[pos];
   }

   private int read() throws IOException {
      if (pos == limit && !fill()) {
         return EOF;
      }
      char c = buffer[pos++];
      errorLine = line;
      errorColumn = column + 1;
      if (c == '\n') {
         line++;
         column = 0;
      } else {
         column++;
      }
      return c;
   }

   private boolean fill() throws IOException {
      int n;
      do {
         n = reader.read(buffer, 0, buffer.length);
      } while (n == 0);
      if (n < 0) {
         return false;
      }
      pos = 0;
      limit = n;
      return true;
   }

   private ParseException error(String message) {
      return new ParseException(message + " at line " + errorLine + ", column " + errorColumn);
   }

   private static String describe(int c) {
      if (c == EOF) {
         return "end of input";
      }
      if (c < ' ' || c > '~') {
         return String.format("(code %d / 0x%04X)", c, c);
      }
      return "'" + (char) c + "' (code " + c + ")";
   }
}
