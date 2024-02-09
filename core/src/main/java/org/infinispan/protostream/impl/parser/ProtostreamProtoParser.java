package org.infinispan.protostream.impl.parser;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.impl.Log;

/**
 * Parser for .proto files based on the Protoparser.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class ProtostreamProtoParser {
   static ThreadLocal<StringBuilder> comments = ThreadLocal.withInitial(StringBuilder::new);

   private final Configuration configuration;

   public ProtostreamProtoParser(Configuration configuration) {
      this.configuration = configuration;
   }

   /**
    * Parses a set of .proto files but does not resolve type dependencies and does not detect semantic errors like
    * duplicate type definitions. If the {@link FileDescriptorSource} parameter does not include a progress callback
    * parsing will stop on first encountered error. If a callback exists all files will be processed; only one error per
    * file is reported and parsing will continue with the next file.
    *
    * @param fileDescriptorSource the set of descriptors to parse
    * @return a map of successfully parsed {@link FileDescriptor} objects keyed by with their names
    * @throws DescriptorParserException if parsing errors were encountered and no progress callback was specified in the
    *                                   {@link FileDescriptorSource}
    */
   public Map<String, FileDescriptor> parse(FileDescriptorSource fileDescriptorSource) throws DescriptorParserException {
      Map<String, String> input = fileDescriptorSource.getFiles();
      Map<String, FileDescriptor> fileDescriptorMap = new LinkedHashMap<>(input.size());
      for (Map.Entry<String, String> entry : input.entrySet()) {
         String fileName = entry.getKey();
         try {
            FileDescriptor fileDescriptor = ProtoParser.parse(fileName, new StringReader(entry.getValue()), configuration);
            fileDescriptor.setConfiguration(configuration);
            fileDescriptorMap.put(fileName, fileDescriptor);
         } catch (DescriptorParserException e) {
            reportParsingError(fileDescriptorSource, fileDescriptorMap, fileName, e);
         } catch (RuntimeException | TokenMgrError e) {
            reportParsingError(fileDescriptorSource, fileDescriptorMap, fileName, Log.LOG.parserException(fileName, e.getMessage()));
         } catch (ParseException e) {
            Token next = e.currentToken.next;
            String s = String.format("Syntax error in %s at %d:%d: unexpected label: %s", fileName, next.beginLine, next.endColumn, next.image);
            reportParsingError(fileDescriptorSource, fileDescriptorMap, fileName, new DescriptorParserException(s, e));
         }
      }
      return fileDescriptorMap;
   }

   /**
    * Report the error to the callback if any, or just throw it otherwise.
    */
   private void reportParsingError(FileDescriptorSource fileDescriptorSource, Map<String, FileDescriptor> fileDescriptorMap, String fileName, DescriptorParserException dpe) {
      if (fileDescriptorSource.getProgressCallback() == null) {
         // fail fast
         throw dpe;
      }

      // create an empty FileDescriptor that has just a name and a parsing error status
      FileDescriptor stub = new FileDescriptor.Builder()
            .withName(fileName)
            .withParsingException(dpe)
            .build();
      stub.setConfiguration(configuration);
      fileDescriptorMap.put(fileName, stub);

      fileDescriptorSource.getProgressCallback().handleError(fileName, dpe);
   }
}
