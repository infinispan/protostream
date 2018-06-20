package org.infinispan.protostream;

/**
 * An evolvable message, with support for preserving unknown fields that result from schema evolution. This is an
 * optional interface to be implemented by message types. Implementing it is only required if support for preserving
 * unknown fields is desired.
 *
 * @author anistor@redhat.com
 * @since 1.0
 * @deprecated this mechanism was replaced by {@link UnknownFieldSetHandler} interface
 */
@Deprecated
public interface Message {

   UnknownFieldSet getUnknownFieldSet();

   void setUnknownFieldSet(UnknownFieldSet unknownFieldSet);
}
