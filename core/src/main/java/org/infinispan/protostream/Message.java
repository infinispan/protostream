package org.infinispan.protostream;

/**
 * An evolvable message with support for preserving unknown fields. Implementing this interface is only required if
 * support for unknown fields is desired.
 *
 * @author anistor@redhat.com
 */
public interface Message {

   UnknownFieldSet getUnknownFieldSet();

   void setUnknownFieldSet(UnknownFieldSet unknownFieldSet);
}
