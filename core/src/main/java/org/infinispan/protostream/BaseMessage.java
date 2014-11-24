package org.infinispan.protostream;

/**
 * Base class for message implementations. This is provided just for convenience, you are not required to extend from
 * it. Any {@code Message} implementation will do.
 *
 * @author anistor@redhat.com
 * @since 1.0
 * @deprecated see {@link UnknownFieldSetHandler}
 */
@Deprecated
public abstract class BaseMessage implements Message {

   protected UnknownFieldSet unknownFieldSet;

   @Override
   public UnknownFieldSet getUnknownFieldSet() {
      return unknownFieldSet;
   }

   @Override
   public void setUnknownFieldSet(UnknownFieldSet unknownFieldSet) {
      this.unknownFieldSet = unknownFieldSet;
   }
}
