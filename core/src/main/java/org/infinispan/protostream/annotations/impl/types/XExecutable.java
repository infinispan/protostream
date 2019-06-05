package org.infinispan.protostream.annotations.impl.types;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface XExecutable extends XMember {

   int getParameterCount();

   String[] getParameterNames();

   XClass[] getParameterTypes();

   String toGenericString();
}
