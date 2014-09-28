package org.infinispan.protostream.impl.parser.mappers;

import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.ProtoFile;
import org.infinispan.protostream.descriptors.FileDescriptor;

import java.util.List;

import static org.infinispan.protostream.impl.parser.mappers.Mappers.*;

/**
 * Mapper for high level protofile to FileDescriptor.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public class ProtofileMapper implements Mapper<ProtoFile, FileDescriptor> {

   @Override
   public FileDescriptor map(ProtoFile protoFile) {
      List<MessageType> messageTypes = filter(protoFile.getTypes(), MessageType.class);
      List<EnumType> enumTypes = filter(protoFile.getTypes(), EnumType.class);
      return new FileDescriptor.Builder()
              .withName(protoFile.getFileName())
              .withPackageName(protoFile.getPackageName())
              .withMessageTypes(MESSAGE_LIST_MAPPER.map(messageTypes))
              .withEnumTypes(ENUM_LIST_MAPPER.map(enumTypes))
              .withExtendDescriptors(EXTEND_LIST_MAPPER.map(protoFile.getExtendDeclarations()))
              .withOptions(OPTION_LIST_MAPPER.map(protoFile.getOptions()))
              .withDependencies(protoFile.getDependencies())
              .withPublicDependencies(protoFile.getPublicDependencies())
              .build();
   }
}
