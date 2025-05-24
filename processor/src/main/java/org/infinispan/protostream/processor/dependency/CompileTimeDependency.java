package org.infinispan.protostream.processor.dependency;

import org.infinispan.protostream.annotations.impl.types.XClass;

public class CompileTimeDependency {

   private final String fileName;
   private final XClass useToMakeTypeMetadata;

   public CompileTimeDependency(String fileName, XClass useToMakeTypeMetadata) {
      this.fileName = fileName;
      this.useToMakeTypeMetadata = useToMakeTypeMetadata;
   }

   public String getFileName() {
      return fileName;
   }

   public XClass getUseToMakeTypeMetadata() {
      return useToMakeTypeMetadata;
   }
}
