package org.infinispan.protostream.processor.tests;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
@ProtoSchema(syntax = ProtoSyntax.PROTO3)
public interface ProbablyTheSimplestInitializerInTheWorld extends SerializationContextInitializer {
}
