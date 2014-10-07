package org.infinispan.protostream.impl.parser;

import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.DescriptorParser;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.impl.parser.mappers.ProtofileMapper;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parser for proto files based on the Protoparser.
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
   public Map<String, FileDescriptor> parseAndResolve(FileDescriptorSource fileDescriptorSource) throws DescriptorParserException {
      // parse the input
      Map<String, FileDescriptor> fileDescriptorMap = parse(fileDescriptorSource);

      // resolve imports and types
      Map<String, GenericDescriptor> types = new HashMap<>();
      for (FileDescriptor fileDescriptor : fileDescriptorMap.values()) {
         fileDescriptor.resolveDependencies(null, fileDescriptorMap, types);
         types.putAll(fileDescriptor.getTypes());
      }
      return fileDescriptorMap;
   }

   @Override
   public Map<String, FileDescriptor> parse(FileDescriptorSource fileDescriptorSource) throws DescriptorParserException {
      Map<String, char[]> input = fileDescriptorSource.getFileDescriptors();
      Map<String, FileDescriptor> fileDescriptorMap = new LinkedHashMap<>(input.size());
      for (Map.Entry<String, char[]> entry : input.entrySet()) {
         try {
            ProtoFile protoFile = ProtoSchemaParser.parse(entry.getKey(), new CharArrayReader(entry.getValue()));
            FileDescriptor fileDescriptor = PROTOFILE_MAPPER.map(protoFile);
            fileDescriptor.setConfiguration(configuration);
            fileDescriptorMap.put(entry.getKey(), fileDescriptor);
         } catch (IOException e) {
            throw new DescriptorParserException("Internal parsing error : " + e.getMessage());
         } catch (DescriptorParserException e) {
            throw e;
         } catch (RuntimeException e) {
            throw new DescriptorParserException(e);
         }
      }
      return fileDescriptorMap;
   }
}
