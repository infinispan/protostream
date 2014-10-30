package org.infinispan.protostream.impl.parser;

import org.infinispan.protostream.AnnotationParserException;
import org.infinispan.protostream.descriptors.AnnotationElement;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class AnnotationParser {

   private final AnnotationLexer lexer;

   public AnnotationParser(String input) {
      this.lexer = new AnnotationLexer(input.toCharArray());
   }

   public Map<String, AnnotationElement.Annotation> parse() throws AnnotationParserException {
      Map<String, AnnotationElement.Annotation> annotations = new LinkedHashMap<>();
      while (lexer.token != AnnotationTokens.EOF) {
         AnnotationElement.Annotation annotation = parseAnnotation();
         if (annotations.containsKey(annotation.getName())) {
            throw syntaxError(annotation.position, "duplicate annotation definition \"{0}\"", annotation.getName());
         }
         annotations.put(annotation.getName(), annotation);
         lexer.skipNoise();
      }
      return annotations;
   }

   private void expect(AnnotationTokens token) {
      if (lexer.token == token) {
         lexer.nextToken();
      } else {
         long pos = AnnotationElement.line(lexer.pos) > AnnotationElement.line(lexer.lastPos) ? lexer.lastPos : lexer.pos;
         throw syntaxError(pos, "{0} expected", token.text);
      }
   }

   private String identifier() {
      if (lexer.token == AnnotationTokens.IDENTIFIER) {
         String name = lexer.name;
         lexer.nextToken();
         return name;
      } else {
         expect(AnnotationTokens.IDENTIFIER);
         return null;
      }
   }

   private String qualifiedIdentifier() {
      String qualIdent = identifier();
      while (lexer.token == AnnotationTokens.DOT) {
         lexer.nextToken();
         qualIdent = qualIdent + "." + identifier();
      }
      return qualIdent;
   }

   private AnnotationElement.Annotation parseAnnotation() {
      if (lexer.token == AnnotationTokens.AT) {
         long pos = lexer.pos;
         expect(AnnotationTokens.AT);
         String name = qualifiedIdentifier();
         Map<String, AnnotationElement.Attribute> attributes = parseAttributes();
         return new AnnotationElement.Annotation(pos, name, attributes);
      }
      throw syntaxError("annotation expected");
   }

   private Map<String, AnnotationElement.Attribute> parseAttributes() {
      LinkedHashMap<String, AnnotationElement.Attribute> members = new LinkedHashMap<>();
      if (lexer.token == AnnotationTokens.LPAREN) {
         int start = lexer.mark();
         expect(AnnotationTokens.LPAREN);
         switch (lexer.token) {
            case AT: {
               long pos = lexer.pos;
               AnnotationElement.Annotation annotation = parseAnnotation();
               expect(AnnotationTokens.RPAREN);
               AnnotationElement.Attribute attribute = new AnnotationElement.Attribute(pos, AnnotationElement.Annotation.DEFAULT_ATTRIBUTE, annotation);
               members.put(attribute.getName(), attribute);
               return members;
            }
            case IDENTIFIER: {
               long pos = lexer.pos;
               AnnotationElement.Identifier identifier = parseIdentifier();
               if (lexer.token == AnnotationTokens.EQ) {
                  start = lexer.mark();
                  expect(AnnotationTokens.EQ);
                  AnnotationElement.Value value = parseValue(start);
                  AnnotationElement.Attribute attribute = new AnnotationElement.Attribute(pos, identifier.getIdentifier(), value);
                  members.put(attribute.getName(), attribute);
                  break;
               } else {
                  expect(AnnotationTokens.RPAREN);
                  AnnotationElement.Attribute attribute = new AnnotationElement.Attribute(pos, AnnotationElement.Annotation.DEFAULT_ATTRIBUTE, identifier);
                  members.put(attribute.getName(), attribute);
                  return members;
               }
            }
            case NULL:
            case FALSE:
            case TRUE:
            case INT_LITERAL:
            case LONG_LITERAL:
            case FLOAT_LITERAL:
            case DOUBLE_LITERAL:
            case CHARACTER_LITERAL:
            case STRING_LITERAL: {
               long pos = lexer.pos;
               AnnotationElement.Value literal = parseValue(start);
               expect(AnnotationTokens.RPAREN);
               AnnotationElement.Attribute attribute = new AnnotationElement.Attribute(pos, AnnotationElement.Annotation.DEFAULT_ATTRIBUTE, literal);
               members.put(attribute.getName(), attribute);
               return members;
            }
         }

         if (lexer.token == AnnotationTokens.COMMA) {
            expect(AnnotationTokens.COMMA);
            while (lexer.token != AnnotationTokens.RPAREN && lexer.token != AnnotationTokens.EOF) {
               AnnotationElement.Attribute attribute = parseAttribute();
               if (members.containsKey(attribute.getName())) {
                  throw syntaxError(attribute.position, "duplicate annotation member definition \"{0}\"", attribute.getName());
               }
               members.put(attribute.getName(), attribute);
               if (lexer.token != AnnotationTokens.RPAREN && lexer.token != AnnotationTokens.EOF) {
                  expect(AnnotationTokens.COMMA);
               }
            }
         }

         expect(AnnotationTokens.RPAREN);
      }
      return members;
   }

   private AnnotationElement.Attribute parseAttribute() {
      long pos = lexer.pos;
      String name = identifier();
      int start = lexer.mark();
      expect(AnnotationTokens.EQ);
      AnnotationElement.Value value = parseValue(start);
      return new AnnotationElement.Attribute(pos, name, value);
   }

   private AnnotationElement.Value parseValue(int start) {
      long pos = lexer.pos;
      switch (lexer.token) {
         case AT:
            return parseAnnotation();

         case LBRACE:
            return parseArray();

         case IDENTIFIER:
            return parseIdentifier();

         case INT_LITERAL:
         case LONG_LITERAL:
         case FLOAT_LITERAL:
         case DOUBLE_LITERAL:
         case CHARACTER_LITERAL:
         case STRING_LITERAL:
         case TRUE:
         case FALSE:
         case NULL:
            AnnotationTokens tok = lexer.token;
            String text = lexer.getText(start, lexer.mark());
            Object value = null;
            try {
               switch (tok) {
                  case INT_LITERAL:
                     value = Integer.parseInt(text.trim());
                     break;
                  case LONG_LITERAL:
                     value = Long.parseLong(text.trim());
                     break;
                  case FLOAT_LITERAL:
                     value = Float.parseFloat(text.trim());
                     break;
                  case DOUBLE_LITERAL:
                     value = Double.parseDouble(text.trim());
                     break;
                  case CHARACTER_LITERAL:
                     value = text.charAt(1);
                     break;
                  case STRING_LITERAL: {
                     value = text.substring(text.indexOf("\"") + 1, text.length() - 1);
                     break;
                  }
                  case TRUE:
                     value = Boolean.TRUE;
                     break;
                  case FALSE:
                     value = Boolean.FALSE;
                     break;
                  case NULL:
                     value = null;
                     break;
               }
            } catch (NumberFormatException e) {
               throw syntaxError("invalid numeric value: {0}", e.getMessage());
            }
            AnnotationElement.Literal literal = new AnnotationElement.Literal(pos, value);
            lexer.nextToken();
            return literal;
      }
      throw syntaxError("literal expected");
   }

   private AnnotationElement.Identifier parseIdentifier() {
      long pos = lexer.pos;
      String qualIdent = qualifiedIdentifier();
      return new AnnotationElement.Identifier(pos, qualIdent);
   }

   private AnnotationElement.Array parseArray() {
      int start = lexer.mark();
      long pos = lexer.pos;
      expect(AnnotationTokens.LBRACE);
      List<AnnotationElement.Value> values = new LinkedList<>();
      while (lexer.token != AnnotationTokens.RBRACE && lexer.token != AnnotationTokens.EOF) {
         values.add(parseValue(start));
         start = lexer.mark();
         if (lexer.token != AnnotationTokens.RBRACE && lexer.token != AnnotationTokens.EOF) {
            expect(AnnotationTokens.COMMA);
         }
      }
      expect(AnnotationTokens.RBRACE);
      return new AnnotationElement.Array(pos, values);
   }

   private AnnotationParserException syntaxError(long pos, String errorMsg, String... errorArgs) {
      return new AnnotationParserException("Error: " + AnnotationElement.positionToString(pos)
                                                 + ": " + MessageFormat.format(errorMsg, errorArgs));
   }

   private AnnotationParserException syntaxError(String errorMsg, String... errorArgs) {
      return syntaxError(lexer.pos, errorMsg, errorArgs);
   }
}
