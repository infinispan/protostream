package org.infinispan.protostream;

import java.util.Map;

import org.infinispan.protostream.descriptors.FileDescriptor;

/**
 * Main parser interface.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public interface DescriptorParser {

   /**
    * Parses a set of .proto files but does not resolve type dependencies and does not detect semantic errors like
    * duplicate type definitions. If the {@link FileDescriptorSource} parameter does not include a progress callback
    * parsing will stop on first encountered error. If a callback exists all files will be processed; only one error per
    * file is reported and parsing will continue with the next file.
    *
    * @param fileDescriptorSource the set of descriptors to parse
    * @return a map of successfully parsed {@link FileDescriptor} objects keyed by with their names
    * @throws DescriptorParserException if parsing errors were encountered and no progress callback was specified in the
    *                                   {@link FileDescriptorSource}
    */
   Map<String, FileDescriptor> parse(FileDescriptorSource fileDescriptorSource) throws DescriptorParserException;
}
