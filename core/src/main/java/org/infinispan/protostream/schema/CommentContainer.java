package org.infinispan.protostream.schema;

/**
 * @since 5.0
 */
public interface CommentContainer<T> {
   T addComment(String comment);
}
