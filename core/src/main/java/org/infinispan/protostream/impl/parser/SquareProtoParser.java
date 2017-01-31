package org.infinispan.protostream.impl.parser;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.infinispan.protostream.DescriptorParser;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.impl.parser.mappers.ProtofileMapper;

import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoParser;

/**
 * Parser for .proto files based on the Protoparser.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class SquareProtoParser implements DescriptorParser {

   private static final ProtofileMapper PROTOFILE_MAPPER = new ProtofileMapper();

   private final Configuration configuration;

   public SquareProtoParser(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public Map<String, FileDescriptor> parse(FileDescriptorSource fileDescriptorSource) throws DescriptorParserException {
      Map<String, char[]> input = fileDescriptorSource.getFileDescriptors();
      Map<String, FileDescriptor> fileDescriptorMap = new LinkedHashMap<>(input.size());
      for (Map.Entry<String, char[]> entry : input.entrySet()) {
         String fileName = entry.getKey();
         ProtoFile protoFile = null;
         try {
            protoFile = ProtoParser.parse(fileName, new CharArrayReader(entry.getValue()));
         } catch (IOException e) {
            reportParsingError(fileDescriptorSource, fileName, new DescriptorParserException("Internal parsing error : " + e.getMessage()));
         } catch (DescriptorParserException e) {
            reportParsingError(fileDescriptorSource, fileName, e);
         } catch (RuntimeException e) {
            reportParsingError(fileDescriptorSource, fileName, new DescriptorParserException(e));
         }
         FileDescriptor fileDescriptor = protoFile != null ? PROTOFILE_MAPPER.map(protoFile) : makeErrorStub(fileName);
         fileDescriptor.setConfiguration(configuration);
         fileDescriptorMap.put(fileName, fileDescriptor);
      }
      return fileDescriptorMap;
   }

   /**
    * Create an empty FileDescriptor that has just a name and an error status.
    */
   private FileDescriptor makeErrorStub(String fileName) {
      FileDescriptor stub = new FileDescriptor.Builder()
            .withName(fileName)
            .withPackageName(null)
            .withMessageTypes(Collections.emptyList())
            .withEnumTypes(Collections.emptyList())
            .withExtendDescriptors(Collections.emptyList())
            .withOptions(Collections.emptyList())
            .withDependencies(Collections.emptyList())
            .withPublicDependencies(Collections.emptyList())
            .build();
      stub.markError();
      return stub;
   }

   /**
    * Report the error to the callback if any, or just throw it otherwise.
    */
   private void reportParsingError(FileDescriptorSource fileDescriptorSource, String fileName, DescriptorParserException dpe) {
      if (fileDescriptorSource.getProgressCallback() == null) {
         throw dpe;
      }
      fileDescriptorSource.getProgressCallback().handleError(fileName, dpe);
   }
}
