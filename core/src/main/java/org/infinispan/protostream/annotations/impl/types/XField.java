package org.infinispan.protostream.annotations.impl.types;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface XField extends XMember {

   XClass getType();

   boolean isEnumConstant();

   XEnumConstant asEnumConstant();
}
