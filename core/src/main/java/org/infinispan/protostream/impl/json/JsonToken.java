package org.infinispan.protostream.impl.json;

enum JsonToken implements JsonTokenWriter {
   LEFT_BRACE('{'),
   RIGHT_BRACE('}'),
   STRING((char) Character.UNASSIGNED),
   COLON(':'),
   VALUE((char) Character.UNASSIGNED),
   COMMA(','),
   LEFT_BRACKET('['),
   RIGHT_BRACKET(']');

   private final char symbol;

   JsonToken(char symbol) {
      this.symbol = symbol;
   }

   @Override
   public void append(StringBuilder out) {
      if (symbol == Character.UNASSIGNED)
         throw new IllegalStateException(name() + " token should have extra content");

      out.append(symbol);
   }

   @Override
   public JsonToken token() {
      return this;
   }

   public boolean isEnd() {
      return this == RIGHT_BRACE || this == RIGHT_BRACKET;
   }

   public boolean isOpen() {
      return this == LEFT_BRACE || this == LEFT_BRACKET;
   }

   public static boolean followedByComma(JsonToken prev) {
      return prev != null && (prev == VALUE || prev.isEnd());
   }

   public static boolean isOpen(JsonToken token) {
      return token != null && token.isOpen();
   }
}
