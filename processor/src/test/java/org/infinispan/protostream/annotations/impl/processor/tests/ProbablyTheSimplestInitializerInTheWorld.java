package org.infinispan.protostream.annotations.impl.processor.tests;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoSyntax;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
@AutoProtoSchemaBuilder(syntax = ProtoSyntax.PROTO3)
public interface ProbablyTheSimplestInitializerInTheWorld extends SerializationContextInitializer {
}
