package org.infinispan.protostream.annotations.impl.processor.types;

import javax.lang.model.element.Element;

/**
 * Some {@link org.infinispan.protostream.annotations.impl.types.XElement}s have an associated
 * javax.lang.model.element.Element, in which case they implement this interface in order to expose the Element.
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
