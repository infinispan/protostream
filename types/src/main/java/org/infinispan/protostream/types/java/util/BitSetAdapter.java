package org.infinispan.protostream.types.java.util;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

import java.util.BitSet;

@ProtoTypeId(1013) // see org.infinispan.commons.marshall.ProtoStreamTypeIds
@ProtoAdapter(BitSet.class)
public final class BitSetAdapter {

    @ProtoField(1)
    byte[] getBits(BitSet bitSet) {
        return bitSet.toByteArray();
    }

    @ProtoFactory
    BitSet create(byte[] bits) {
        return BitSet.valueOf(bits);
    }

}
