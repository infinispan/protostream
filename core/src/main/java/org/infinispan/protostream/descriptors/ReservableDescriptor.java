package org.infinispan.protostream.descriptors;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.infinispan.protostream.impl.AnnotatedDescriptorImpl;
import org.infinispan.protostream.impl.Log;
import org.infinispan.protostream.impl.SparseBitSet;
import org.infinispan.protostream.schema.ReservedNumbers;

public abstract class ReservableDescriptor extends AnnotatedDescriptorImpl {
   protected final SparseBitSet reservedNumbers;
   protected final Set<String> reservedNames;

   protected ReservableDescriptor(String name, String fullName, String documentation, SparseBitSet reservedNumbers, Set<String> reservedNames) {
      super(name, fullName, documentation);
      this.reservedNumbers = reservedNumbers;
      this.reservedNames = Set.copyOf(reservedNames);
   }

   public ReservedNumbers getReservedNumbers() {
      return reservedNumbers;
   }

   public Collection<String> getReservedNames() {
      return reservedNames;
   }

   protected void checkReservation(ReservableDescriptor that, boolean strict, List<String> errors) {
      if (strict) {
         Set<String> oldNames = new HashSet<>(reservedNames);
         oldNames.removeAll(that.reservedNames);
         if (!oldNames.isEmpty()) {
            errors.add(Log.LOG.removedReservedNames(that.fullName, oldNames));
         }
         SparseBitSet oldNumbers = new SparseBitSet(reservedNumbers);
         oldNumbers.removeAll(that.reservedNumbers);
         if (!oldNumbers.isEmpty()) {
            errors.add(Log.LOG.removedReservedNumbers(that.fullName, oldNumbers));
         }
      }
   }
}
