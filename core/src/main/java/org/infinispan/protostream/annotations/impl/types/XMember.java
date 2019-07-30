package org.infinispan.protostream.annotations.impl.types;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface XMember extends XElement {

   XClass getDeclaringClass();

   /**
    * Determine element type of array or Collection. For (non-constructor) members of type Collection or array only.
    * Other implementations can just return null.
    */
   default XClass determineRepeatedElementType() {
      return null;
   }
}
