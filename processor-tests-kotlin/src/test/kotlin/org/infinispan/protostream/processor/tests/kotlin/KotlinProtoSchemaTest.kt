package org.infinispan.protostream.processor.tests.kotlin

import org.infinispan.protostream.GeneratedSchema
import org.infinispan.protostream.ProtobufUtil
import org.infinispan.protostream.SerializationContextInitializer
import org.infinispan.protostream.annotations.ProtoSchema
import org.infinispan.protostream.annotations.ProtoSyntax
import org.infinispan.protostream.processor.tests.kotlin.testdomain.BooleanFieldMessage
import org.infinispan.protostream.processor.tests.kotlin.testdomain.CollectionMessage
import org.infinispan.protostream.processor.tests.kotlin.testdomain.GetterAnnotatedMessage
import org.infinispan.protostream.processor.tests.kotlin.testdomain.ImmutableMessage
import org.infinispan.protostream.processor.tests.kotlin.testdomain.KotlinEnum
import org.infinispan.protostream.processor.tests.kotlin.testdomain.MessageWithDefaults
import org.infinispan.protostream.processor.tests.kotlin.testdomain.MessageWithEnum
import org.infinispan.protostream.processor.tests.kotlin.testdomain.MutableMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@ProtoSchema(
    schemaFileName = "kotlin_test.proto",
    schemaPackageName = "kotlintest",
    includeClasses = [
        MutableMessage::class,
        ImmutableMessage::class,
        BooleanFieldMessage::class,
        KotlinEnum::class,
        MessageWithEnum::class,
        CollectionMessage::class,
        MessageWithDefaults::class,
        GetterAnnotatedMessage::class
    ],
    syntax = ProtoSyntax.PROTO3
)
interface KotlinTestSchema : GeneratedSchema

class KotlinProtoSchemaTest {

    private fun createContext(): org.infinispan.protostream.SerializationContext {
        val ctx = ProtobufUtil.newSerializationContext()
        val initializer: KotlinTestSchema = KotlinTestSchemaImpl()
        initializer.register(ctx)
        return ctx
    }

    @Test
    fun testSchemaGenerated() {
        val ctx = createContext()
        assertTrue(ctx.canMarshall(MutableMessage::class.java))
        assertTrue(ctx.canMarshall(ImmutableMessage::class.java))
        assertTrue(ctx.canMarshall(BooleanFieldMessage::class.java))
        assertTrue(ctx.canMarshall(KotlinEnum::class.java))
        assertTrue(ctx.canMarshall(MessageWithEnum::class.java))
        assertTrue(ctx.canMarshall(CollectionMessage::class.java))
        assertTrue(ctx.canMarshall(MessageWithDefaults::class.java))
        assertTrue(ctx.canMarshall(GetterAnnotatedMessage::class.java))
    }

    @Test
    fun testMutableMessageRoundTrip() {
        val ctx = createContext()
        val msg = MutableMessage().apply {
            name = "hello"
            value = 42
            description = "a test message"
        }
        val bytes = ProtobufUtil.toWrappedByteArray(ctx, msg)
        val restored = ProtobufUtil.fromWrappedByteArray<MutableMessage>(ctx, bytes)
        assertEquals(msg, restored)
    }

    @Test
    fun testImmutableMessageRoundTrip() {
        val ctx = createContext()
        val msg = ImmutableMessage("title", 7, "tagged")
        val bytes = ProtobufUtil.toWrappedByteArray(ctx, msg)
        val restored = ProtobufUtil.fromWrappedByteArray<ImmutableMessage>(ctx, bytes)
        assertEquals(msg, restored)
    }

    @Test
    fun testImmutableMessageNullField() {
        val ctx = createContext()
        // In proto3, null strings are deserialized as empty strings (proto3 has no null concept for scalars)
        val msg = ImmutableMessage("title", 3, null)
        val bytes = ProtobufUtil.toWrappedByteArray(ctx, msg)
        val restored = ProtobufUtil.fromWrappedByteArray<ImmutableMessage>(ctx, bytes)
        assertEquals("title", restored.title)
        assertEquals(3, restored.count)
        assertEquals("", restored.tag)
    }

    @Test
    fun testBooleanFieldsRoundTrip() {
        val ctx = createContext()
        val msg = BooleanFieldMessage().apply {
            enabled = true
            isActive = true
            visible = false
        }
        val bytes = ProtobufUtil.toWrappedByteArray(ctx, msg)
        val restored = ProtobufUtil.fromWrappedByteArray<BooleanFieldMessage>(ctx, bytes)
        assertEquals(msg, restored)
    }

    @Test
    fun testEnumRoundTrip() {
        val ctx = createContext()
        val msg = MessageWithEnum("test", KotlinEnum.SECOND)
        val bytes = ProtobufUtil.toWrappedByteArray(ctx, msg)
        val restored = ProtobufUtil.fromWrappedByteArray<MessageWithEnum>(ctx, bytes)
        assertEquals(msg, restored)
    }

    @Test
    fun testCollectionRoundTrip() {
        val ctx = createContext()
        val msg = CollectionMessage().apply {
            items = listOf("a", "b", "c")
            tags = listOf("x", "y")
        }
        val bytes = ProtobufUtil.toWrappedByteArray(ctx, msg)
        val restored = ProtobufUtil.fromWrappedByteArray<CollectionMessage>(ctx, bytes)
        assertEquals(msg, restored)
    }

    @Test
    fun testMessageWithDefaultsRoundTrip() {
        val ctx = createContext()
        // In proto3, default values for strings are empty string, not null
        val msg = MessageWithDefaults("item")
        val bytes = ProtobufUtil.toWrappedByteArray(ctx, msg)
        val restored = ProtobufUtil.fromWrappedByteArray<MessageWithDefaults>(ctx, bytes)
        assertEquals("item", restored.name)
        assertEquals(42, restored.quantity)
        assertEquals("", restored.note)
    }

    @Test
    fun testMessageWithDefaultsExplicitValues() {
        val ctx = createContext()
        val msg = MessageWithDefaults("item", 99, "some note")
        val bytes = ProtobufUtil.toWrappedByteArray(ctx, msg)
        val restored = ProtobufUtil.fromWrappedByteArray<MessageWithDefaults>(ctx, bytes)
        assertEquals(msg, restored)
    }

    @Test
    fun testGetterAnnotatedMessageRoundTrip() {
        val ctx = createContext()
        val msg = GetterAnnotatedMessage("alice", 100)
        val bytes = ProtobufUtil.toWrappedByteArray(ctx, msg)
        val restored = ProtobufUtil.fromWrappedByteArray<GetterAnnotatedMessage>(ctx, bytes)
        assertEquals(msg, restored)
    }

    @Test
    fun testProtoFileContent() {
        val initializer: KotlinTestSchema = KotlinTestSchemaImpl()
        val protoFile = initializer.protoFile
        assertNotNull(protoFile)
        assertTrue(protoFile.contains("message MutableMessage"))
        assertTrue(protoFile.contains("message ImmutableMessage"))
        assertTrue(protoFile.contains("message BooleanFieldMessage"))
        assertTrue(protoFile.contains("enum KotlinEnum"))
        assertTrue(protoFile.contains("message MessageWithEnum"))
        assertTrue(protoFile.contains("message CollectionMessage"))
        assertTrue(protoFile.contains("message MessageWithDefaults"))
        assertTrue(protoFile.contains("message GetterAnnotatedMessage"))
    }
}
