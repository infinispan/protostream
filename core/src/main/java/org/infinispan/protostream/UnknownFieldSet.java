package org.infinispan.protostream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import java.io.IOException;

/**
 * {@code UnknownFieldSet} is used to keep track of fields which were seen when parsing a protocol message but whose
 * field numbers are not recognized. This usually  occurs when new fields are added to a message type and then messages
 * containing those fields are read by old versions of software that was built before the new types were added.
 */
public interface UnknownFieldSet {

   /**
    * Checks if there are any tags in this set.
    */
   boolean isEmpty();

   /**
    * Parse an entire message from {@code input} and merge its fields into this set.
    */
   void mergeFrom(CodedInputStream input) throws IOException;

   /**
    * Parse a single field from {@code input} and merge it into this set.
    *
    * @param tag The field's tag number, which was already parsed.
    * @return {@code false} if the tag is an end group tag.
    */
   boolean mergeFieldFrom(int tag, CodedInputStream input) throws IOException;

   /**
    * Convenience method for merging a new field containing a single varint value. This is used in particular when an
    * unknown enum value is encountered.
    */
   void mergeVarintField(int tag, int value);

   /**
    * Serializes the set and writes it to {@code output}.
    */
   void writeTo(CodedOutputStream output) throws IOException;

   /**
    * Reads and removes a tag value from the set.
    *
    * @param tag the field tag (containing field number and wire type)
    * @param <A> The expected type of the tag value
    * @return the first seen value or null if the tag was not found
    */
   <A> A consumeTag(int tag);
}
