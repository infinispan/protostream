package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.ExtendDeclaration;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.Option;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.Type;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.DescriptorParserException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.*;

/**
 * Mapper for high level protofile to FileDescriptor
 *
 * @author gustavonalle
 * @since 2.0
 */
public class ProtofileMapper implements Mapper<ProtoFile, FileDescriptor> {

   private final Map<String, ProtoFile> protoFileMap;
   private final Set<String> processedFile = new HashSet<>();

   public ProtofileMapper(Map<String, ProtoFile> protoFileMap) {
      this.protoFileMap = protoFileMap;
   }

   @Override
   public FileDescriptor map(ProtoFile protoFile) {   
      // List<String> publicDependencies = protoFile.getPublicDependencies();
      List<Option> options = protoFile.getOptions();
      List<Type> types = protoFile.getTypes();
      List<MessageType> messageTypes = filter(types, MessageType.class);
      List<EnumType> enumTypes = filter(types, EnumType.class);
      List<ExtendDeclaration> extendDeclarations = protoFile.getExtendDeclarations();
      List<String> dependencies = protoFile.getDependencies();
      List<FileDescriptor> protoFiles = new ArrayList<>(dependencies.size());

      for (String dependency : dependencies) {
         FileDescriptor fd = mapInternal(protoFile.getFileName(), dependency, processedFile);
         protoFiles.add(fd);
      }

      return new FileDescriptor.Builder()
              .withName(protoFile.getFileName())
              .withPackageName(protoFile.getPackageName())
              .withMessageTypes(MESSAGE_LIST_MAPPER.map(messageTypes))
              .withEnumTypes(ENUM_LIST_MAPPER.map(enumTypes))
              .withExtendDescriptors(EXTEND_LIST_MAPPER.map(extendDeclarations))
              .withOptions(OPTION_LIST_MAPPER.map(options))
              .withDependencies(protoFiles)
              .build();
   }

   private FileDescriptor mapInternal(String fileName, String dependency, Set<String> processedFile) {
      ProtoFile pf = protoFileMap.get(dependency);
      if (pf == null) {
         throw new DescriptorParserException("Import '" + dependency + "' not found");
      }
      if (processedFile.contains(dependency)) {
         throw new DescriptorParserException("Possible cyclic import detected at " + fileName + ", import " + dependency);
      }
      processedFile.add(dependency);
      return map(protoFileMap.get(dependency));
   }
}
