package org.infinispan.protostream.schema;

/**
 * @since 6.0
 */
class SchemaByString implements Schema {
   String fileName;
   String fileContent;

   SchemaByString(String fileName, String fileContent) {
       this.fileName = fileName;
       this.fileContent = fileContent;
   }

   @Override
   public String getName() {
      return fileName;
   }

   @Override
   public String toString() {
      return fileContent;
   }

   @Override
   public String getContent() {
      return fileContent;
   }
}
