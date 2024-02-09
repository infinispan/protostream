package org.infinispan.protostream.descriptors;

public interface OptionContainer<T> {
    T addOption(Option option);
}
