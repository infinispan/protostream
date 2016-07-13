package org.infinispan.protostream.impl.parser;

import java.io.CharArrayReader;
import java.io.IOException;
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
         try {
            ProtoFile protoFile = ProtoParser.parse(fileName, new CharArrayReader(entry.getValue()));
            FileDescriptor fileDescriptor = PROTOFILE_MAPPER.map(protoFile);
            fileDescriptor.setConfiguration(configuration);
            fileDescriptorMap.put(fileName, fileDescriptor);
         } catch (IOException e) {
            reportParsingError(fileName, new DescriptorParserException("Internal parsing error : " + e.getMessage()), fileDescriptorSource);
         } catch (DescriptorParserException e) {
            reportParsingError(fileName, e, fileDescriptorSource);
         } catch (RuntimeException e) {
            reportParsingError(fileName, new DescriptorParserException(e), fileDescriptorSource);
         }
      }
      return fileDescriptorMap;
   }

   private void reportParsingError(String fileName, DescriptorParserException dpe, FileDescriptorSource fileDescriptorSource) {
      if (fileDescriptorSource.getProgressCallback() == null) {
         throw dpe;
      }
      fileDescriptorSource.getProgressCallback().handleError(fileName, dpe);
   }
}
