package org.infinispan.protostream.annotations.impl.types;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface XMethod extends XExecutable {

   XClass getReturnType();
}
