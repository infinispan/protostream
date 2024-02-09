package org.infinispan.protostream.annotations.impl;

import org.infinispan.protostream.annotations.ProtoSyntax;

/**
 * Dumps proto schema to writer.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface HasProtoSchema {

   void generateProto(IndentWriter writer, ProtoSyntax syntax);
}
