package org.infinispan.protostream;

/**
 * Interface to designate that a given implementation is backed by a byte[] that can be used directly. Note that
 * the implementation must be sure that the entire off set and length are contained within the returned byte[].
 */
public interface ByteArrayBacked {
   /**
    * Offset into the {@link #bytes()} where the data begins
    */
   int offset();

   /**
    * The length of the bytes to be used from the {@link #bytes()}.
    */
   int length();

   /**
    * The underlying byte[] that can be used directly
    */
   byte[] bytes();
}
