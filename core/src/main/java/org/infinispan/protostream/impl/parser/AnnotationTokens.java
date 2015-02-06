package org.infinispan.protostream.impl.parser;

/**
 * The tokens used by the annotation grammar.
 *
 * @author anistor@redhat.com
 * @since 2.0
 */
enum AnnotationTokens {
   AT("'@'"),
   LPAREN("'('"),
   RPAREN("')'"),
   LBRACE("'{'"),
   RBRACE("'}'"),
   COMMA("','"),
   DOT("'.'"),
   EQ("'='"),
   TRUE("true"),
   FALSE("false"),
   IDENTIFIER("<identifier>"),
   CHARACTER_LITERAL("<character>"),
   STRING_LITERAL("<string>"),
   INT_LITERAL("<integer>"),
   LONG_LITERAL("<long>"),
   FLOAT_LITERAL("<float>"),
   DOUBLE_LITERAL("<double>"),
   EOF("<end of input>");

   final String text;

   AnnotationTokens(String text) {
      this.text = text;
   }

   static AnnotationTokens byName(String name) {
      if (name.length() == 1) {
         switch (name.charAt(0)) {
            case '@':
               return AT;
            case '(':
               return LPAREN;
            case ')':
               return RPAREN;
            case '{':
               return LBRACE;
            case '}':
               return RBRACE;
            case ',':
               return COMMA;
            case '.':
               return DOT;
            case '=':
               return EQ;
            default:
               return null;
         }
      }
      if ("true".equals(name)) {
         return TRUE;
      }
      if ("false".equals(name)) {
         return FALSE;
      }
      return null;
   }
}
