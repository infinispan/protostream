/*
 * Copyright 2016 Daniel Skogquist Åborg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.infinispan.protostream.impl;

import java.util.Arrays;
import java.util.BitSet;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

import org.infinispan.protostream.schema.ReservedNumbers;

/**
 * A sparse bit set for storing occurrences of bits where a large amount of the stored bits are expected to be zero.
 * This implementation uses a simplistic indexing scheme which provides {@code log(n)} performance for accessing
 * individual bits. Memory usage is roughly proportional to {@link #bitCount}. The bit set can set any bit between
 * {@code 0} and {@link Long#MAX_VALUE}, inclusive. However, the maximum number of sparse 64-bit words in use by the
 * set bits in the bit set is limited by the int length limit of arrays.
 */
public class SparseBitSet implements ReservedNumbers {
   private long[] words;
   private long[] indices;
   private int size;

   private int modCount;

   /**
    * Construct a {@code SparseBitSet}.
    */
   public SparseBitSet() {
      this(10);
   }

   /**
    * Construct a {@code SparseBitSet} with the given values.
    */
   public SparseBitSet(long... values) {
      this(values.length);
      for (long value : values)
         set(value);
   }

   /**
    * Construct a {@code SparseBitSet} with the given initial capacity.
    */
   public SparseBitSet(int capacity) {
      this.words = new long[capacity];
      this.indices = new long[capacity];
   }

   public SparseBitSet(SparseBitSet other) {
      this.words = new long[other.words.length];
      this.indices = new long[other.indices.length];
      this.size = other.size;
      this.modCount = other.modCount;
      System.arraycopy(other.words, 0, this.words, 0, other.words.length);
      System.arraycopy(other.indices, 0, this.indices, 0, other.indices.length);
   }

   /**
    * Set the bit at index {@code i}.
    *
    * @return {@code true} if this bit set changed as a result of setting the bit, i.e. the bit was clear, {@code
    * false} otherwise.
    */
   public boolean set(long i) {
      if (i < 0)
         throw new IllegalArgumentException("i < 0: " + i);

      int wordIndex = findWord(i);
      if (wordIndex < 0) {
         insert(i, -(wordIndex + 1));
         modCount++;
         return true;
      }

      long bit = 1L << (i & 63);
      if ((words[wordIndex] & bit) != 0)
         return false;

      words[wordIndex] |= bit;
      modCount++;
      return true;
   }

   /**
    * Clear the bit at index {@code i}.
    *
    * @return {@code true} if this bit set changed as a result of setting the bit, i.e. the bit was set, {@code false}
    * otherwise.
    */
   public boolean clear(long i) {
      if (i < 0)
         throw new IllegalArgumentException("i < 0: " + i);

      int wordIndex = findWord(i);
      if (wordIndex < 0)
         return false;

      long bitIndex = i & 63;
      if ((words[wordIndex] & 1L << bitIndex) == 0)
         return false;

      removeBit(wordIndex, bitIndex);
      modCount++;
      return true;
   }

   /**
    * Set the bit at index {@code i} to the given {@code value}.
    *
    * @return {@code true} if this bit set changed as a result of changing the bit, false otherwise.
    */
   public boolean set(long i, boolean value) {
      return value ? set(i) : clear(i);
   }

   /**
    * Sets the bits from the specified fromIndex (inclusive) to the specified toIndex (exclusive) to true.
    * Params:
    *
    * @param fromIndex – index of the first bit to be set
    * @param toIndex   – index after the last bit to be set
    * @throws IndexOutOfBoundsException – if fromIndex is negative, or toIndex is negative, or fromIndex is larger than toIndex
    */
   public void set(long fromIndex, long toIndex) {
      for (long i = fromIndex; i < toIndex; i++) {
         set(i);
      }
   }

   /**
    * Get the bit at index {@code i}.
    *
    * @return {@code true} if the bit at index {@code i} is set, false otherwise.
    */
   @Override
   public boolean get(long i) {
      if (i < 0)
         throw new IllegalArgumentException("i < 0: " + i);

      int wordIndex = findWord(i);
      return wordIndex >= 0 && (words[wordIndex] & (1L << (i & 63))) != 0;
   }

   /**
    * @return the number of set bits in this {@code SparseBitSet}.
    */
   public long bitCount() {
      long bitCount = 0;
      for (int i = 0; i < size; i++)
         bitCount += Long.bitCount(words[i]);
      return bitCount;
   }

   /**
    * @return the size of this {@code SparseBitSet}, i.e. the number of set bits.
    * @see #bitCount()
    */
   @Override
   public int size() {
      long bitCount = bitCount();

      if (bitCount > Integer.MAX_VALUE)
         throw new IllegalStateException("size > Integer.MAX_VALUE: " + bitCount);

      return (int) bitCount;
   }

   /**
    * @return true if this {@code SparseBitSet} contains no set bits, false otherwise.
    */
   @Override
   public boolean isEmpty() {
      return size == 0;
   }

   @Override
   public int nextSetBit(int fromIndex) {
      if (fromIndex < 0) {
         throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
      }
      int wordIndex = findWord(fromIndex);
      if (wordIndex < 0) {
         wordIndex = -wordIndex - 1;
         fromIndex = adjustBase(fromIndex, wordIndex);
      }
      while (wordIndex < size) {
         if ((words[wordIndex] & (1L << (fromIndex & 63))) != 0) {
            return fromIndex;
         }
         fromIndex++;
         if ((fromIndex % 64) == 0) {
            wordIndex = findWord(fromIndex);
            if (wordIndex < 0) {
               wordIndex = -wordIndex - 1;
               fromIndex = adjustBase(fromIndex, wordIndex);
            }
         }
      }
      return -1;
   }

   private int adjustBase(int fromIndex, int wordIndex) {
      if (wordIndex < size) {
         fromIndex = (int) (indices[wordIndex] << 6);
      }
      return fromIndex;
   }

   @Override
   public int nextClearBit(int fromIndex) {
      if (fromIndex < 0) {
         throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
      }
      int wordIndex = findWord(fromIndex);
      if (wordIndex < 0) {
         wordIndex = -wordIndex - 1;
         fromIndex = adjustBase(fromIndex, wordIndex);
      }
      while (wordIndex < size) {
         if ((words[wordIndex] & (1L << (fromIndex & 63))) == 0) {
            return fromIndex;
         }
         fromIndex++;
         if ((fromIndex % 64) == 0) {
            wordIndex = findWord(fromIndex);
            if (wordIndex < 0) {
               wordIndex = -wordIndex - 1;
               fromIndex = adjustBase(fromIndex, wordIndex);
            }
         }
      }
      return -1;
   }

   /**
    * Reset this {@code SparseBitSet} to the empty state, i.e. with no bits set.
    */
   public void clear() {
      for (int i = 0; i < size; i++) {
         words[i] = 0;
         indices[i] = 0;
      }
      size = 0;
      modCount++;
   }

   public long firstLong() {
      if (isEmpty())
         throw new NoSuchElementException();

      long firstWord = words[0];

      int bitIndex = 0;
      while ((firstWord & (1L << bitIndex)) == 0)
         bitIndex++;

      return (indices[0] << 6) + bitIndex;
   }

   public long lastLong() {
      if (isEmpty())
         throw new NoSuchElementException();

      long lastWord = words[size - 1];

      int bitIndex = 63;
      while ((lastWord & (1L << bitIndex)) == 0)
         bitIndex--;

      return (indices[size - 1] << 6) + bitIndex;
   }

   @Override
   public PrimitiveIterator.OfLong iterator() {
      return new Iter(0, 0) {
         @Override
         protected boolean inRange() {
            return wordIndex < size;
         }

         @Override
         protected void step() {
            bitIndex++;
            if (bitIndex > 63 - startingBit) {
               bitIndex = startingBit;
               wordIndex++;
            }
         }
      };
   }

   public PrimitiveIterator.OfLong descendingIterator() {
      return new Iter(size - 1, 63) {
         @Override
         protected boolean inRange() {
            return wordIndex >= 0;
         }

         @Override
         protected void step() {
            bitIndex--;
            if (bitIndex < 63 - startingBit) {
               bitIndex = startingBit;
               wordIndex--;
            }
         }
      };
   }

   /**
    * @return a string representation of this {@code BitSet} in the same format as {@link BitSet#toString()}, e.g. the
    * indices of all the set bits in ascending order surrounded by curly brackets {@code "{}"}.
    */
   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder(size * 10); // heuristic
      builder.append("{");

      boolean started = false;
      for (int i = 0; i < size; i++) {
         long word = words[i];
         long index = indices[i];
         for (long bitIndex = 0; bitIndex < 64; bitIndex++) {
            long bit = 1L << bitIndex;
            if ((word & bit) != 0) {
               if (started)
                  builder.append(", ");
               else
                  started = true;
               builder.append((index << 6) + bitIndex);
            }
         }
      }

      builder.append("}");
      return builder.toString();
   }

   private int findWord(long x) {
      return Arrays.binarySearch(indices, 0, size, x >> 6);
   }

   private void insert(long x, int insertionPoint) {
      if (words.length == size) {
         words = Arrays.copyOf(words, words.length + (words.length >> 1));
         indices = Arrays.copyOf(indices, indices.length + (words.length >> 1));
      }
      System.arraycopy(words, insertionPoint, words, insertionPoint + 1, size - insertionPoint);
      System.arraycopy(indices, insertionPoint, indices, insertionPoint + 1, size - insertionPoint);
      words[insertionPoint] = 1L << (x & 63);
      indices[insertionPoint] = x >> 6;
      size++;
   }

   private void removeBit(int wordIndex, long bitIndex) {
      words[wordIndex] &= ~(1L << bitIndex);
      if (words[wordIndex] == 0)
         removeWord(wordIndex);
   }

   private void removeWord(int index) {
      System.arraycopy(words, index + 1, words, index, size - index - 1);
      System.arraycopy(indices, index + 1, indices, index, size - index - 1);
      words[size - 1] = 0;
      indices[size - 1] = 0;
      size--;
   }

   public void removeAll(ReservedNumbers that) {
      for(long l : that) {
         set(l, false);
      }
   }

   private abstract class Iter implements PrimitiveIterator.OfLong {
      protected int wordIndex;
      protected int bitIndex;
      protected int lastWordIndex = -1;
      protected int lastBitIndex = -1;
      protected final int startingBit;

      protected int expectedModCount = modCount;

      protected Iter(int wordIndex, int bitIndex) {
         this.wordIndex = wordIndex;
         this.bitIndex = bitIndex;
         startingBit = bitIndex;
      }

      @Override
      public boolean hasNext() {
         // find next set bit
         while (inRange() && (words[wordIndex] & (1L << bitIndex)) == 0)
            step();

         return inRange();
      }

      @Override
      public long nextLong() {
         checkForCoModification();
         if (!hasNext())
            throw new NoSuchElementException();

         lastWordIndex = wordIndex;
         lastBitIndex = bitIndex;
         long next = (indices[wordIndex] << 6) + bitIndex;
         step();
         return next;
      }

      @Override
      public void remove() {
         checkForCoModification();
         if (lastWordIndex < 0)
            throw new IllegalStateException("Cannot remove before call to nextLong or after call to remove");

         long previousSize = size;
         removeBit(lastWordIndex, lastBitIndex);
         if (size < previousSize) {
            wordIndex = lastWordIndex;
            bitIndex = startingBit;
         }
         lastWordIndex = -1;
         lastBitIndex = -1;
         modCount++;
         expectedModCount++;
      }

      protected abstract boolean inRange();

      protected abstract void step();

      protected void checkForCoModification() {
         if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
      }
   }
}
