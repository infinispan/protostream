package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.ProtoFile;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.DescriptorParserException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.*;

/**
 * Mapper for high level protofile to FileDescriptor.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public class ProtofileMapper implements Mapper<ProtoFile, FileDescriptor> {

   private final Map<String, ProtoFile> protoFileMap;

   private final Set<String> processedFiles = new HashSet<>();

   public ProtofileMapper(Map<String, ProtoFile> protoFileMap) {
      this.protoFileMap = protoFileMap;
   }

   @Override
   public FileDescriptor map(ProtoFile protoFile) {
      List<MessageType> messageTypes = filter(protoFile.getTypes(), MessageType.class);
      List<EnumType> enumTypes = filter(protoFile.getTypes(), EnumType.class);

      List<FileDescriptor> dependencies = mapDependencies(protoFile.getFileName(), protoFile.getDependencies());
      List<FileDescriptor> publicDependencies = mapDependencies(protoFile.getFileName(), protoFile.getPublicDependencies());

      return new FileDescriptor.Builder()
              .withName(protoFile.getFileName())
              .withPackageName(protoFile.getPackageName())
              .withMessageTypes(MESSAGE_LIST_MAPPER.map(messageTypes))
              .withEnumTypes(ENUM_LIST_MAPPER.map(enumTypes))
              .withExtendDescriptors(EXTEND_LIST_MAPPER.map(protoFile.getExtendDeclarations()))
              .withOptions(OPTION_LIST_MAPPER.map(protoFile.getOptions()))
              .withDependencies(dependencies)
              .withPublicDependencies(publicDependencies)
              .build();
   }

   private List<FileDescriptor> mapDependencies(String fileName, List<String> dependencies) {
      List<FileDescriptor> fileDescriptors = new ArrayList<>(dependencies.size());
      for (String dependency : dependencies) {
         FileDescriptor fd = mapInternal(fileName, dependency);
         fileDescriptors.add(fd);
      }
      return fileDescriptors;
   }

   private FileDescriptor mapInternal(String fileName, String dependency) {
      ProtoFile pf = protoFileMap.get(dependency);
      if (pf == null) {
         throw new DescriptorParserException("Import '" + dependency + "' not found");
      }
      if (!processedFiles.add(dependency)) {
         throw new DescriptorParserException("Possible cyclic import detected at " + fileName + ", import " + dependency);
      }
      return map(pf);
   }
}
