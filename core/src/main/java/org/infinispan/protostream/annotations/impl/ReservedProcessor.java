package org.infinispan.protostream.annotations.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.infinispan.protostream.annotations.ProtoReserved;
import org.infinispan.protostream.annotations.ProtoSchemaBuilderException;
import org.infinispan.protostream.annotations.impl.types.XClass;

/**
 * Scans a class (and its superclass/superinterfaces) for ProtoReserved annotations and generates 'reserved' statements
 * accordingly.
 *
 * @author anistor@redhat.com
 * @since 4.3
 */
final class ReservedProcessor {

   private static final class ReservedInterval implements Comparable<ReservedInterval> {

      /**
       * The XClass that made the reservation.
       */
      final XClass where;

      /**
       * Lower bound (inclusive). from == to if it is a single reserved number (not a real range).
       */
      private final int from;

      /**
       * Upper bound (inclusive). When undefined it receives the default value Integer.MAX_VALUE, which translates into
       * ProtoReserved.Range.MAX_FIELD for fields and is left as is for enums.
       */
      private final int to;

      /**
       * Constructor for numbers.
       */
      ReservedInterval(XClass where, int number) {
         this.where = where;
         this.from = this.to = number;

         if (!where.isEnum()) {
            if (number < 1) {
               throw new ProtoSchemaBuilderException("Found invalid @ProtoReserved number " + number + " in "
                     + where.getCanonicalName() + ". Must be greater than 0.");
            }
            if (number > ProtoReserved.Range.MAX_FIELD) {
               throw new ProtoSchemaBuilderException("Found invalid @ProtoReserved number " + number + " in "
                     + where.getCanonicalName() + ". Must be lower than " + ProtoReserved.Range.MAX_FIELD + ".");
            }
         }
      }

      /**
       * Constructor for ranges.
       */
      ReservedInterval(XClass where, int from, int to) {
         this.where = where;
         this.from = from;
         this.to = to;

         if (from >= to) {
            throw new ProtoSchemaBuilderException("Found invalid @ProtoReserved range \"" + rangeAsString() + "\" in "
                  + where.getCanonicalName() + ". 'to' must be greater than 'from'.");
         }
         if (!where.isEnum()) {
            if (from < 1) {
               throw new ProtoSchemaBuilderException("Found invalid @ProtoReserved range \"" + this + "\" in "
                     + where.getCanonicalName() + ". 'from' must be greater than 0.");
            }
            if (to != Integer.MAX_VALUE && to > ProtoReserved.Range.MAX_FIELD) {
               throw new ProtoSchemaBuilderException("Found invalid @ProtoReserved range \"" + this + "\" in "
                     + where.getCanonicalName() + ". 'to' must be lower than " + ProtoReserved.Range.MAX_FIELD + ".");
            }
         }
      }

      ReservedInterval findOverlap(Set<ReservedInterval> numbers) {
         for (ReservedInterval i : numbers) {
            if (i.contains(from) || i.contains(to)) {
               return i;
            }
         }
         return null;
      }

      private boolean contains(int n) {
         return n >= from && n <= to;
      }

      @Override
      public int compareTo(ReservedInterval i) {
         return from - i.from;
      }

      @Override
      public String toString() {
         return from == to ? String.valueOf(from) : rangeAsString();
      }

      private String rangeAsString() {
         boolean toMax = to == Integer.MAX_VALUE || !where.isEnum() && to == ProtoReserved.Range.MAX_FIELD;
         return from + " to " + (toMax ? "max" : String.valueOf(to));
      }

      /**
       * Joins two non-overlapping intervals if possible. The given internval must come after the current.
       */
      ReservedInterval join(ReservedInterval i) {
         if (to == i.from - 1) {
            return new ReservedInterval(where, from, i.to);
         }
         return null;
      }
   }

   /**
    * Reserved intervals sorted by {@link ReservedInterval#from}. Intervals are non-overlapping.
    */
   private SortedSet<ReservedInterval> reservedNumbers;

   /**
    * The key is name, the value is the XClass where the reservation occurred.
    */
   private Map<String, XClass> reservedNames;

   XClass checkReserved(int number) {
      for (ReservedInterval i : reservedNumbers) {
         if (i.contains(number)) {
            return i.where;
         }
      }
      return null;
   }

   XClass checkReserved(String name) {
      return reservedNames.get(name);
   }

   void scan(XClass javaClass) {
      reservedNumbers = new TreeSet<>();
      reservedNames = new TreeMap<>();
      scanReserved(javaClass, new HashSet<>());
   }

   void generate(IndentWriter iw) {
      if (!reservedNumbers.isEmpty()) {
         iw.append("reserved ");
         boolean first = true;
         ReservedInterval r = null;
         for (ReservedInterval i : reservedNumbers) {
            if (r == null) {
               r = i;
            } else {
               ReservedInterval j = r.join(i);  // try to join ranges
               if (j == null) {
                  if (first) {
                     first = false;
                  } else {
                     iw.append(", ");
                  }
                  iw.append(r.toString());
                  r = i;
               } else {
                  r = j;
               }
            }
         }
         if (r != null) {
            if (!first) {
               iw.append(", ");
            }
            iw.append(r.toString());
         }
         iw.append(";\n");
      }

      if (!reservedNames.isEmpty()) {
         iw.append("reserved ");
         boolean first = true;
         for (String name : reservedNames.keySet()) {
            if (first) {
               first = false;
            } else {
               iw.append(", ");
            }
            iw.append('"').append(name).append('"');
         }
         iw.append(";\n");
      }
   }

   private void scanReserved(XClass clazz, Set<XClass> processedClasses) {
      if (!processedClasses.add(clazz)) {
         // avoid re-processing classes due to multiple interface inheritance
         return;
      }

      if (clazz.getSuperclass() != null) {
         scanReserved(clazz.getSuperclass(), processedClasses);
      }
      for (XClass i : clazz.getInterfaces()) {
         scanReserved(i, processedClasses);
      }

      for (ProtoReserved reserved : clazz.getAnnotationsByType(ProtoReserved.class)) {
         int[] numbers = reserved.numbers();
         if (numbers.length == 0) {
            numbers = reserved.value();
         } else if (reserved.value().length > 0) {
            throw new ProtoSchemaBuilderException("@ProtoReserved annotation must not specify both 'value' and 'numbers' : " + clazz.getCanonicalName());
         }
         for (int number : numbers) {
            ReservedInterval i = new ReservedInterval(clazz, number);
            ReservedInterval dup = i.findOverlap(reservedNumbers);
            if (dup != null) {
               if (dup.where.equals(clazz)) {
                  throw new ProtoSchemaBuilderException("Found duplicate @ProtoReserved number " + number + " in " + clazz.getCanonicalName());
               } else {
                  throw new ProtoSchemaBuilderException("@ProtoReserved number " + number + " in " + clazz.getCanonicalName()
                        + " conflicts with @ProtoReserved in " + dup.where.getCanonicalName());
               }
            }
            reservedNumbers.add(i);
         }

         for (ProtoReserved.Range range : reserved.ranges()) {
            ReservedInterval i = new ReservedInterval(clazz, range.from(), range.to());
            ReservedInterval dup = i.findOverlap(reservedNumbers);
            if (dup != null) {
               if (dup.where.equals(clazz)) {
                  throw new ProtoSchemaBuilderException("Found overlapping @ProtoReserved range \"" + i + "\" in " + clazz.getCanonicalName());
               } else {
                  throw new ProtoSchemaBuilderException("@ProtoReserved range \"" + i + "\" in " + clazz.getCanonicalName()
                        + " conflicts with @ProtoReserved in " + dup.where.getCanonicalName());
               }
            }
            reservedNumbers.add(i);
         }

         for (String name : reserved.names()) {
            if (name.isEmpty()) {
               throw new ProtoSchemaBuilderException("@ProtoReserved name cannot be empty: " + clazz.getCanonicalName());
            }
            XClass dup = reservedNames.put(name, clazz);
            if (dup != null) {
               if (dup.equals(clazz)) {
                  throw new ProtoSchemaBuilderException("Found duplicate @ProtoReserved name \"" + name + "\" in " + clazz.getCanonicalName());
               } else {
                  throw new ProtoSchemaBuilderException("@ProtoReserved name \"" + name + "\" in " + clazz.getCanonicalName()
                        + " conflicts with @ProtoReserved name in " + dup.getCanonicalName());
               }
            }
         }
      }
   }
}
