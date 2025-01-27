package org.infinispan.protostream.impl;

/**
 * Interface that accepts an output to write in text form.
 *
 * @since 6.0
 */
public interface Appendable {

   /**
    * Writes the given output.
    *
    * @param out The output to write the tokens.
    */
   void append(StringBuilder out);

}
