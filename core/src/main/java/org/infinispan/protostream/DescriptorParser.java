package org.infinispan.protostream;

import org.infinispan.protostream.descriptors.FileDescriptor;

import java.io.IOException;
import java.util.Map;

/**
 * Main parser interface
 *
 * @author gustavonalle
 * @since 2.0
 */
public interface DescriptorParser {

   /**
    * Parses a set of protofiles
    *
    * @param fileDescriptorSource the set of descriptors to parse
    * @return map of FileDescriptor objects keyed by with their names
    */
   public Map<String, FileDescriptor> parse(FileDescriptorSource fileDescriptorSource) throws IOException, DescriptorParserException;
}
