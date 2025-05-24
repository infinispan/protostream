package org.infinispan.protostream.processor.types;

import javax.lang.model.element.Element;

/**
 * Some {@link org.infinispan.protostream.annotations.impl.types.XElement} implementations have an associated {@link
 * Element}, in which case they implement this interface in order to expose the Element. This is mostly used to track
 * the originating element to facilitate incremental compilation.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
public interface HasModelElement {

   /**
    * Get the {@link Element}, never {@code null}.
    *
    * @return the {@link Element}, never {@code null}
    */
   Element getElement();
}
