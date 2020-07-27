package org.infinispan.protostream.impl.parser;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.infinispan.protostream.DescriptorParser;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.impl.parser.mappers.ProtofileMapper;

import com.squareup.protoparser.OptionElement;
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
      Map<String, String> input = fileDescriptorSource.getFiles();
      Map<String, FileDescriptor> fileDescriptorMap = new LinkedHashMap<>(input.size());
      for (Map.Entry<String, String> entry : input.entrySet()) {
         String fileName = entry.getKey();
         try {
            ProtoFile protoFile = ProtoParser.parse(fileName, new StringReader(entry.getValue()));
            checkUniqueFileOptions(protoFile);
            FileDescriptor fileDescriptor = PROTOFILE_MAPPER.map(protoFile);
            fileDescriptor.setConfiguration(configuration);
            fileDescriptorMap.put(fileName, fileDescriptor);
         } catch (DescriptorParserException e) {
            reportParsingError(fileDescriptorSource, fileDescriptorMap, fileName, e);
         } catch (IOException | RuntimeException e) {
            reportParsingError(fileDescriptorSource, fileDescriptorMap, fileName, new DescriptorParserException(e));
         }
      }
      return fileDescriptorMap;
   }

   private void checkUniqueFileOptions(ProtoFile protoFile) {
      Set<String> optionNames = new HashSet<>(protoFile.options().size());
      for (OptionElement optionElement : protoFile.options()) {
         if (!optionNames.add(optionElement.name())) {
            throw new DescriptorParserException(protoFile.filePath() + ": Option \"" + optionElement.name() + "\" was already set.");
         }
      }
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
