package org.infinispan.protostream.impl.parser;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.AnnotationParserException;
import org.infinispan.protostream.descriptors.AnnotationElement;

/**
 * Parses all annotations it encounters and fails on first syntactic error. Everything that looks like a syntactically
 * correct annotation will be parsed and returned. At this stage there is no validation of what annotation name is
 * acceptable or not and no validation of attribute value types, multiplicity, etc. These steps are not part of the
 * syntactic analysis and will be performed later.
 *
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class AnnotationParser {

   private final AnnotationLexer lexer;

   /**
    * Creates a parser for a given input text.
    *
    * @param input          the input text to parse
    * @param expectDocNoise indicates if human readable text is expected to be encountered before the annotations
    */
   public AnnotationParser(String input, boolean expectDocNoise) {
      this.lexer = new AnnotationLexer(input.toCharArray(), expectDocNoise);
   }

   /**
    * Parse the text and extract the annotations.
    *
    * @return the list of annotations; name uniqueness is not mandatory at this stage
    * @throws AnnotationParserException if syntax errors are encountered
    */
   public List<AnnotationElement.Annotation> parse() throws AnnotationParserException {
      List<AnnotationElement.Annotation> annotations = new LinkedList<>();
      while (lexer.token != AnnotationTokens.EOF) {
         AnnotationElement.Annotation annotation = parseAnnotation();
         annotations.add(annotation);
         lexer.skipDocNoise();
      }
      return annotations;
   }

   /**
    * Matches the expected token or fails throwing an AnnotationParserException.
    */
   private void expect(AnnotationTokens token) {
      if (lexer.token == token) {
         lexer.nextToken();
      } else {
         long pos = AnnotationElement.line(lexer.pos) > AnnotationElement.line(lexer.lastPos) ? lexer.lastPos : lexer.pos;
         throw new AnnotationParserException(String.format("Error: %s: %s expected", AnnotationElement.positionToString(pos), token.text));
      }
   }

   private String identifier() {
      if (lexer.token == AnnotationTokens.IDENTIFIER) {
         String identifier = lexer.identifier;
         lexer.nextToken();
         return identifier;
      } else {
         expect(AnnotationTokens.IDENTIFIER);
         return null;
      }
   }

   private String qualifiedIdentifier() {
      StringBuilder qualIdent = new StringBuilder(identifier());
      while (lexer.token == AnnotationTokens.DOT) {
         lexer.nextToken();
         qualIdent.append(".").append(identifier());
      }
      return qualIdent.toString();
   }

   private AnnotationElement.Annotation parseAnnotation() {
      if (lexer.token != AnnotationTokens.AT) {
         throw new AnnotationParserException(String.format("Error: %s: annotation expected", AnnotationElement.positionToString(lexer.pos)));
      }
      long pos = lexer.pos;
      expect(AnnotationTokens.AT);
      String name = qualifiedIdentifier();
      Map<String, AnnotationElement.Attribute> attributes = parseAttributes();
      return new AnnotationElement.Annotation(pos, name, attributes);
   }

   private Map<String, AnnotationElement.Attribute> parseAttributes() {
      LinkedHashMap<String, AnnotationElement.Attribute> members = new LinkedHashMap<>();
      if (lexer.token == AnnotationTokens.LPAREN) {
         int start = lexer.getBufferPos();
         expect(AnnotationTokens.LPAREN);
         switch (lexer.token) {
            case AT: {
               long pos = lexer.pos;
               AnnotationElement.Annotation annotation = parseAnnotation();
               expect(AnnotationTokens.RPAREN);
               AnnotationElement.Attribute attribute = new AnnotationElement.Attribute(pos, AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE, annotation);
               members.put(attribute.getName(), attribute);
               return members;
            }
            case IDENTIFIER: {
               long pos = lexer.pos;
               AnnotationElement.Identifier identifier = parseIdentifier();
               if (lexer.token == AnnotationTokens.EQ) {
                  start = lexer.getBufferPos();
                  expect(AnnotationTokens.EQ);
                  AnnotationElement.Value value = parseValue(start);
                  AnnotationElement.Attribute attribute = new AnnotationElement.Attribute(pos, identifier.getIdentifier(), value);
                  members.put(attribute.getName(), attribute);
                  break;
               } else {
                  expect(AnnotationTokens.RPAREN);
                  AnnotationElement.Attribute attribute = new AnnotationElement.Attribute(pos, AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE, identifier);
                  members.put(attribute.getName(), attribute);
                  return members;
               }
            }
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
               AnnotationElement.Attribute attribute = new AnnotationElement.Attribute(pos, AnnotationElement.Annotation.VALUE_DEFAULT_ATTRIBUTE, literal);
               members.put(attribute.getName(), attribute);
               return members;
            }
         }

         if (lexer.token == AnnotationTokens.COMMA) {
            expect(AnnotationTokens.COMMA);
            while (lexer.token != AnnotationTokens.RPAREN && lexer.token != AnnotationTokens.EOF) {
               AnnotationElement.Attribute attribute = parseAttribute();
               if (members.containsKey(attribute.getName())) {
                  throw new AnnotationParserException(String.format("Error: %s: duplicate annotation member definition \"%s\"", AnnotationElement.positionToString(attribute.position), attribute.getName()));
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
      int start = lexer.getBufferPos();
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
            AnnotationTokens tok = lexer.token;
            String text = lexer.getText(start, lexer.getBufferPos());
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
               }
            } catch (NumberFormatException e) {
               throw new AnnotationParserException(String.format("Error: %s: invalid numeric value: %s", AnnotationElement.positionToString(lexer.pos), e.getMessage()));
            }
            AnnotationElement.Literal literal = new AnnotationElement.Literal(pos, value);
            lexer.nextToken();
            return literal;
      }
      throw new AnnotationParserException(String.format("Error: %s: literal expected", AnnotationElement.positionToString(lexer.pos)));
   }

   private AnnotationElement.Identifier parseIdentifier() {
      long pos = lexer.pos;
      String qualIdent = qualifiedIdentifier();
      return new AnnotationElement.Identifier(pos, qualIdent);
   }

   private AnnotationElement.Array parseArray() {
      int start = lexer.getBufferPos();
      long pos = lexer.pos;
      expect(AnnotationTokens.LBRACE);
      List<AnnotationElement.Value> values = new LinkedList<>();
      while (lexer.token != AnnotationTokens.RBRACE && lexer.token != AnnotationTokens.EOF) {
         values.add(parseValue(start));
         start = lexer.getBufferPos();
         if (lexer.token != AnnotationTokens.RBRACE && lexer.token != AnnotationTokens.EOF) {
            expect(AnnotationTokens.COMMA);
         }
      }
      expect(AnnotationTokens.RBRACE);
      return new AnnotationElement.Array(pos, values);
   }
}
