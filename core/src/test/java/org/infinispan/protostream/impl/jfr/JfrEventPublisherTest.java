package org.infinispan.protostream.impl.jfr;

import static org.infinispan.protostream.domain.Account.Currency.BRL;
import static org.infinispan.protostream.domain.Account.Currency.USD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.jfr.consumer.RecordingStream;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.domain.Account;
import org.infinispan.protostream.test.AbstractProtoStreamTest;
import org.junit.Test;

public class JfrEventPublisherTest extends AbstractProtoStreamTest {

    @Test
    public void testAllocationEventPublished() throws Exception {
        Account account = createAccount();
        SerializationContext context = createContext();
        try (RecordingStream stream = new RecordingStream()) {
            stream.enable(BufferAllocateEvent.class);
            CompletableFuture<Integer> cf = new CompletableFuture<>();
            stream.onEvent(BufferAllocateEvent.NAME, e -> cf.complete(e.getValue("size")));
            stream.startAsync();

            byte[] bytes = ProtobufUtil.toWrappedByteArray(context, account);
            Account acc = ProtobufUtil.fromWrappedByteArray(context, bytes);
            assertEquals(acc, account);

            cf.get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testResizeEventPublished() throws Exception {
        Account account = createAccount();
        SerializationContext context = createContext();
        try (RecordingStream stream = new RecordingStream()) {
            stream.enable(BufferResizeEvent.class);
            CompletableFuture<Integer> cf = new CompletableFuture<>();
            stream.onEvent(BufferResizeEvent.NAME, e -> cf.complete(e.getValue("size")));
            stream.startAsync();

            account.setDescription(generateLargeDescription());

            byte[] bytes = ProtobufUtil.toWrappedByteArray(context, account);
            Account acc = ProtobufUtil.fromWrappedByteArray(context, bytes);
            assertEquals(acc, account);
            cf.get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testDifferentPrimitivesEvents() throws Exception {
        try (RecordingStream stream = new RecordingStream()) {
            stream.enable(BufferAllocateEvent.class);
            stream.enable(BufferResizeEvent.class);

            AtomicInteger index = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(5);
            List<Number> values = List.of(
                    Integer.BYTES * 2,
                    Long.BYTES * 2,
                    Float.BYTES * 2,
                    Double.BYTES * 2);

            stream.onEvent(BufferAllocateEvent.NAME, e -> {
                assertEquals(values.get(index.getAndIncrement()), e.getInt("size"));
                latch.countDown();
            });
            stream.onEvent(BufferResizeEvent.NAME, e -> {
                assertEquals(2 * Integer.BYTES, e.getInt("size"));
                latch.countDown();
            });
            stream.startAsync();

            JfrEventPublisher.intBufferAllocateEvent(2);
            JfrEventPublisher.longBufferAllocateEvent(2);
            JfrEventPublisher.floatBufferAllocateEvent(2);
            JfrEventPublisher.doubleBufferAllocateEvent(2);
            JfrEventPublisher.intBufferResizeEvent(2, 4);
            
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(4, index.get());
        }
    }

    private String generateLargeDescription() {
        return "B".repeat(4096);
    }


    private Account createAccount() {
        Account account = new Account();
        account.setId(1);
        account.setDescription("test account");
        Account.Limits limits = new Account.Limits();
        limits.setMaxDailyLimit(1.5);
        limits.setMaxTransactionLimit(3.5);
        limits.setPayees(new String[]{"Madoff", "Ponzi"});
        account.setLimits(limits);
        Account.Limits hardLimits = new Account.Limits();
        hardLimits.setMaxDailyLimit(5d);
        hardLimits.setMaxTransactionLimit(35d);
        account.setHardLimits(hardLimits);
        Date creationDate = Date.from(LocalDate.of(2017, 7, 20).atStartOfDay().toInstant(ZoneOffset.UTC));
        account.setCreationDate(creationDate);
        List<byte[]> blurb = new ArrayList<>();
        blurb.add(new byte[0]);
        blurb.add(new byte[]{123});
        blurb.add(new byte[]{1, 2, 3, 4});
        account.setBlurb(blurb);
        account.setCurrencies(new Account.Currency[]{USD, BRL});
        return account;
    }
}
