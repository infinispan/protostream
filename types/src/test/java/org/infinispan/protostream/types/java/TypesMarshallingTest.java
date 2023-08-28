package org.infinispan.protostream.types.java;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TypesMarshallingTest {

    @Test
    public void testStringArrayList() throws IOException {
        doCollectionMarshallTest(stringCollection(ArrayList::new), useWrappedMessage());
    }

    @Test
    public void testBookArrayList() throws IOException {
        doCollectionMarshallTest(bookCollection(ArrayList::new), useWrappedMessage());
    }

    @Test
    public void testStringHashSet() throws IOException {
        doCollectionMarshallTest(stringCollection(HashSet::new), useWrappedMessage());
    }

    @Test
    public void testBookHashSet() throws IOException {
        doCollectionMarshallTest(bookCollection(HashSet::new), useWrappedMessage());
    }

    @Test
    public void testStringLinkedHashSet() throws IOException {
        doCollectionMarshallTest(stringCollection(LinkedHashSet::new), useWrappedMessage());
    }

    @Test
    public void testBookLinkedHashSet() throws IOException {
        doCollectionMarshallTest(bookCollection(LinkedHashSet::new), useWrappedMessage());
    }

    @Test
    public void testStringLinkedList() throws IOException {
        doCollectionMarshallTest(stringCollection(LinkedList::new), useWrappedMessage());
    }

    @Test
    public void testBookLinkedList() throws IOException {
        doCollectionMarshallTest(bookCollection(LinkedList::new), useWrappedMessage());
    }

    @Test
    public void testStringTreeSet() throws IOException {
        doCollectionMarshallTest(stringCollection(TreeSet::new), useWrappedMessage());
    }

    @Test
    public void testBookTreeSet() throws IOException {
        doCollectionMarshallTest(bookCollection(TreeSet::new), useWrappedMessage());
    }

    @Test
    public void testStringArray() throws IOException {
        doArrayMarshallTest(stringArray(), useWrappedMessage());
    }

    @Test
    public void testBookArray() throws IOException {
        doArrayMarshallTest(bookArray(), useWrappedMessage());
    }

    @Test
    public void testUUIDUsingByteArray() throws IOException {
        doUUIDMarshallTest(useByteArray());
    }

    @Test
    public void testUUIDUsingInputStream() throws IOException {
        doUUIDMarshallTest(useInputStream());
    }

    @Test
    public void testUUIDUsingWrappedMessage() throws IOException {
        doUUIDMarshallTest(useWrappedMessage());
    }

    @Test
    public void testBigDecimalUsingByteArray() throws IOException {
        doBigDecimalMarshallTest(useByteArray());
    }

    @Test
    public void testBigDecimalUsingInputStream() throws IOException {
        doBigDecimalMarshallTest(useInputStream());
    }

    @Test
    public void testBigDecimalUsingWrappedMessage() throws IOException {
        doBigDecimalMarshallTest(useWrappedMessage());
    }

    @Test
    public void testBigIntegerUsingByteArray() throws IOException {
        doBigIntegerMarshallTest(useByteArray());
    }

    @Test
    public void testBigIntegerUsingInputStream() throws IOException {
        doBigIntegerMarshallTest(useInputStream());
    }

    @Test
    public void testBigIntegerUsingWrappedMessage() throws IOException {
        doBigIntegerMarshallTest(useWrappedMessage());
    }

    private void doUUIDMarshallTest(EncoderMethod<UUID> encoderMethod) throws IOException {
        UUID uuid = UUID.randomUUID();
        UUID other = encoderMethod.encodeAndDecode(uuid, newContext());
        assertEquals(uuid, other);
    }

    private void doBigDecimalMarshallTest(EncoderMethod<BigDecimal> encoderMethod) throws IOException {
        BigDecimal expected = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(-1000, 1000));
        BigDecimal copy = encoderMethod.encodeAndDecode(expected, newContext());
        assertEquals(expected, copy);
    }

    private void doBigIntegerMarshallTest(EncoderMethod<BigInteger> encoderMethod) throws IOException {
        BigInteger expected = BigInteger.valueOf(ThreadLocalRandom.current().nextInt(-1000, 1000));
        BigInteger copy = encoderMethod.encodeAndDecode(expected, newContext());
        assertEquals(expected, copy);
    }

    private static Collection<String> stringCollection(Supplier<Collection<String>> supplier) {
        Collection<String> collection = supplier.get();
        collection.add("a");
        collection.add("b");
        collection.add("c");
        return collection;
    }

    private static Collection<Book> bookCollection(Supplier<Collection<Book>> supplier) {
        Collection<Book> collection = supplier.get();
        collection.add(new Book("Book1", "Description1", 2020));
        collection.add(new Book("Book2", "Description2", 2021));
        collection.add(new Book("Book3", "Description3", 2022));
        return collection;
    }

    private static String[] stringArray() {
        return new String[] {"a", "b", "c"};
    }

    private static Object[] bookArray() {
        // cannot use new Book[] because there is no marshaller for it.
        return new Object[] {
                new Book("Book1", "Description1", 2020),
                new Book("Book2", "Description2", 2021),
                new Book("Book3", "Description3", 2022)
        };
    }

    private static <E> void doCollectionMarshallTest(Collection<E> collection, EncoderMethod<Collection<E>> encoderMethod) throws IOException {
        Collection<E> copy = encoderMethod.encodeAndDecode(collection, newContext());
        assertEquals(collection.getClass(), copy.getClass());
        assertEquals(collection, copy);
    }

    private static <T> void doArrayMarshallTest(T[] array, EncoderMethod<T[]> encoderMethod) throws IOException {
        T[] copy = encoderMethod.encodeAndDecode(array, newContext());
        assertArrayEquals(array, copy);
    }

    public static SerializationContext newContext() {
        SerializationContext context = ProtobufUtil.newSerializationContext();
        register(new CommonTypesSchema(), context);
        register(new CommonContainerTypesSchema(), context);
        register(new BookSchemaImpl(), context);
        return context;
    }

    private static void register(GeneratedSchema schema, SerializationContext context) {
        schema.registerMarshallers(context);
        schema.registerSchema(context);
    }

    public static <T> EncoderMethod<T> useByteArray() {
        return (object, ctx) -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
            ProtobufUtil.writeTo(ctx, baos, object);
            //noinspection unchecked
            return ProtobufUtil.fromByteArray(ctx, baos.toByteArray(), (Class<T>) object.getClass());
        };
    }

    public static <T> EncoderMethod<T> useInputStream() {
        return (object, ctx) -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
            ProtobufUtil.writeTo(ctx, baos, object);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            //noinspection unchecked
            return ProtobufUtil.readFrom(ctx, is, (Class<T>) object.getClass());
        };
    }

    public static <T> EncoderMethod<T> useWrappedMessage() {
        return (object, ctx) -> {
            byte[] data = ProtobufUtil.toWrappedByteArray(ctx, object, 512);
            return ProtobufUtil.fromWrappedByteArray(ctx, data);
        };
    }

    @FunctionalInterface
    public interface EncoderMethod<T> {
        T encodeAndDecode(T object, ImmutableSerializationContext ctx) throws IOException;
    }

}
