package org.infinispan.protostream.impl.parser;

import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.DescriptorParser;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
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

   @Override
   public Map<String, FileDescriptor> parse(FileDescriptorSource fileDescriptorSource) throws IOException, DescriptorParserException {
      Map<String, ProtoFile> protoFileMap = parseInternal(fileDescriptorSource.getFileDescriptors());
      Map<String, FileDescriptor> fileDescriptorMap = new HashMap<>(protoFileMap.size());
      Map<String, FileDescriptor> types = new HashMap<>();
      for (String fileName : protoFileMap.keySet()) {
         ProtoFile protoFile = protoFileMap.get(fileName);
         FileDescriptor mapped = new ProtofileMapper(protoFileMap).map(protoFile);
         fileDescriptorMap.put(fileName, mapped);
         for (String typeName : mapped.getTypes().keySet()) {
            FileDescriptor fd = types.get(typeName);
            if (fd == null) {
               types.put(typeName, mapped);
            } else {
               throw new DescriptorParserException("Duplicate definition of " + typeName + " in " + mapped.getFullName() + " and " + fd.getFullName());
            }
         }
      }
      return fileDescriptorMap;
   }

   private Map<String, ProtoFile> parseInternal(Map<String, char[]> input) throws IOException, DescriptorParserException {
      Map<String, ProtoFile> fileMap = new LinkedHashMap<>();
      for (Map.Entry<String, char[]> entry : input.entrySet()) {
         ProtoFile protoFile = ProtoSchemaParser.parse(entry.getKey(), new CharArrayReader(entry.getValue()));
         fileMap.put(getFullName(protoFile), protoFile);
      }
      return fileMap;
   }

   private String getFullName(ProtoFile protoFile) {
      String fileName = protoFile.getFileName();
      String packageName = protoFile.getPackageName();
      return packageName != null ? packageName.replace('.', '/').concat("/").concat(fileName) : fileName;
   }
}
