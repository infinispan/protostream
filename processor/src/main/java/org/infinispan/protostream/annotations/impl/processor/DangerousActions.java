package org.infinispan.protostream.annotations.impl.processor;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;

import org.infinispan.protostream.impl.Log;

/**
 * As Mr. Guetta once said,
 * <quote><pre>
 * I don't know where the lights are taking us
 * But something in the night is dangerous (let's go)
 * </pre></quote>
 *
 * @author anistor@redhat.com
 * @since 4.3.5
 */
final class DangerousActions {

   private static final Log log = Log.LogFactory.getLog(DangerousActions.class);

   private DangerousActions() {
   }

   /**
    * The 'dangerous' function must never return a value. It must cause a {@link MirroredTypeException} to be thrown instead.
    */
   static <A extends Annotation, C> TypeMirror getTypeMirror(A annotation, Function<A, Class<? extends C>> pureDanger) {
      TypeMirror typeMirror;
      try {
         // this must guarantee to fail with a MirroredTypeException
         Class<? extends C> clazz = pureDanger.apply(annotation);
         log.errorf("The function unexpectedly returned: %s", clazz);
         throw new IllegalStateException("MirroredTypeException was expected but it wasn't thrown!");
      } catch (MirroredTypeException mte) {
         typeMirror = mte.getTypeMirror();
      }
      return typeMirror;
   }

   /**
    * The 'dangerous' function must never return a value. It must cause a {@link MirroredTypesException} to be thrown instead.
    */
   static <A extends Annotation, C> List<? extends TypeMirror> getTypeMirrors(A annotation, Function<A, Class<? extends C>[]> pureDanger) {
      List<? extends TypeMirror> typeMirrors;
      try {
         // this must guarantee to fail with a MirroredTypesException
         Class<? extends C>[] classes = pureDanger.apply(annotation);
         log.errorf("The function unexpectedly returned: %s", Arrays.toString(classes));
         throw new IllegalStateException("MirroredTypesException was expected but it wasn't thrown!");
      } catch (MirroredTypesException mte) {
         typeMirrors = mte.getTypeMirrors();
      }
      return typeMirrors != null ? typeMirrors : Collections.emptyList();
   }
}
