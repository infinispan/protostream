package org.infinispan.protostream.annotations.impl;

/**
 * Dumps proto schema to writer.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface HasProtoSchema {

   void generateProto(IndentWriter writer);

   /**
    * Get the schema as String, mainly as an aid for debugging.
    */
   default String toProtoSchema() {
      IndentWriter iw = new IndentWriter();
      generateProto(iw);
      return iw.toString();
   }
}
