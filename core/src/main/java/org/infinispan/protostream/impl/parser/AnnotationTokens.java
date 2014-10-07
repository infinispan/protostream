package org.infinispan.protostream.impl.parser;

import java.util.HashMap;
import java.util.Map;

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
   NULL("null"),
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

   private static final Map<String, AnnotationTokens> tokensByName = new HashMap<String, AnnotationTokens>();

   static {
      tokensByName.put("@", AT);
      tokensByName.put("(", LPAREN);
      tokensByName.put(")", RPAREN);
      tokensByName.put("{", LBRACE);
      tokensByName.put("}", RBRACE);
      tokensByName.put(",", COMMA);
      tokensByName.put(".", DOT);
      tokensByName.put("=", EQ);
      tokensByName.put("true", TRUE);
      tokensByName.put("false", FALSE);
      tokensByName.put("null", NULL);
   }

   static AnnotationTokens byName(String name) {
      return tokensByName.get(name);
   }
}
