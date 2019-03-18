/**
 * Abstract unified representation of Java language types, to isolate code generation from direct dependencies on the
 * actual type system implementation.
 * <p>
 * This package also offers a concrete implementation based on Java reflection with java.lang.Class and
 * java.lang.reflect to be used by annotation processors at runtime. Alternative implementations could use
 * javax.lang.model as a source of type metadata.
 * <p>
 * This package is somewhat similar in purpose with JEP 119, but instead of offering a javax.lang.model implementation
 * backed by Java reflection (which would be a lot of work) it just offers a minimal common API that unifies and
 * abstracts java.lang.reflect and javax.lang.model so annotation processing and code generation can easily work with
 * both.
 */
package org.infinispan.protostream.annotations.impl.types;
