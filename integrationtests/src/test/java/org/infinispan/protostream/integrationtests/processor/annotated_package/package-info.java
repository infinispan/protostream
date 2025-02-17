/**
 * Annotate a package to test the result.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
@AutoProtoSchemaBuilder(className = "AnnotationOnPackageTestInitializer", basePackages = "org.infinispan.protostream", dependsOn = CommonTypes.class, syntax = ProtoSyntax.PROTO3)
package org.infinispan.protostream.integrationtests.processor.annotated_package;

import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.types.java.CommonTypes;
