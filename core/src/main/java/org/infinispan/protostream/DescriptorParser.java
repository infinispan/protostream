package org.infinispan.protostream;

import org.infinispan.protostream.descriptors.FileDescriptor;

import java.util.Map;

/**
 * Main parser interface.
 *
 * @author gustavonalle
 * @since 2.0
 */
public interface DescriptorParser {

   /**
    * Parses a set of protofiles and resolves imports and types.
    *
    * @param fileDescriptorSource the set of descriptors to parse
    * @return map of FileDescriptor objects keyed by with their names
    */
   Map<String, FileDescriptor> parseAndResolve(FileDescriptorSource fileDescriptorSource) throws DescriptorParserException;

   /**
    * Parses a set of protofiles but does not resolve dependencies.
    *
    * @param fileDescriptorSource the set of descriptors to parse
    * @return map of FileDescriptor objects keyed by with their names
    */
   Map<String, FileDescriptor> parse(FileDescriptorSource fileDescriptorSource) throws DescriptorParserException;
}
