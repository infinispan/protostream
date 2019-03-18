package org.infinispan.protostream.annotations.impl.types;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface XEnumConstant extends XElement {

   int getOrdinal();

   XClass getDeclaringClass();
}
