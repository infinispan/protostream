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
 * Parser for proto files based on the Protoparser
 *
 * @author gustavonalle
 * @since 2.0
 */
public final class SquareProtoParser implements DescriptorParser {

   @Override
   public Map<String, FileDescriptor> parse(FileDescriptorSource fileDescriptorSource) throws IOException, DescriptorParserException {
      Map<String, ProtoFile> fileMap = parseInternal(fileDescriptorSource.getFileDescriptors());
      Map<String, FileDescriptor> fileDescriptorMap = new HashMap<>(fileMap.size());
      for (String fileName : fileMap.keySet()) {
         if (!fileDescriptorMap.containsKey(fileName)) {
            FileDescriptor mapped = new ProtofileMapper(fileMap).map(fileMap.get(fileName));
            fileDescriptorMap.put(fileName, mapped);
         }
      }
      return fileDescriptorMap;
   }

   private Map<String, ProtoFile> parseInternal(Map<String, char[]> input) throws IOException, DescriptorParserException {
      Map<String, ProtoFile> fileMap = new LinkedHashMap<>();
      for (Map.Entry<String, char[]> entry : input.entrySet()) {
         CharArrayReader reader = new CharArrayReader(entry.getValue());
         ProtoFile protoFile = ProtoSchemaParser.parse(entry.getKey(), reader);
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
