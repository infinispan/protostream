package org.infinispan.protostream.annotations.impl.types;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface XMember extends XElement {

   XClass getDeclaringClass();

   // for (non-constructor) members of type Collection or array only
   XClass determineRepeatedElementType();
}
