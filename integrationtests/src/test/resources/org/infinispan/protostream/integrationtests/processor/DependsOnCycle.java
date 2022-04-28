package test_depends_on_cycle;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

// The processor must spot the cyclic dependency and report an error

@AutoProtoSchemaBuilder(dependsOn = DependsOnCycle.class)
interface SomeInitializer extends SerializationContextInitializer {
}

@AutoProtoSchemaBuilder(dependsOn = SomeInitializer.class)
public interface DependsOnCycle extends SerializationContextInitializer {
}
