package org.infinispan.protostream.annotations.impl;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
interface HasProtoSchema {

   void generateProto(IndentWriter iw);

   /**
    * Get the schema as String, mainly as an aid for debugging.
    */
   default String toProtoSchema() {
      IndentWriter iw = new IndentWriter();
      generateProto(iw);
      return iw.toString();
   }
}
