package com.pixelgift.app.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProtocolChunkingTest {

    @Test
    fun chunkText_emptyString_returnsSingleEmptyChunk() {
        val chunks = ProtocolCodec.chunkText(target = ProtocolConstants.TEXT_TARGET_MESSAGE, text = "")
        assertEquals(1, chunks.size)
        assertEquals(0, chunks[0].target)
        assertEquals(0, chunks[0].idx)
        assertEquals(1, chunks[0].total)
        assertEquals("", chunks[0].text)
    }

    @Test
    fun chunkText_shortString_returnsSingleChunk() {
        val chunks = ProtocolCodec.chunkText(target = ProtocolConstants.TEXT_TARGET_BIRTHDAY_NAME, text = "ALICE")
        assertEquals(1, chunks.size)
        assertEquals(1, chunks[0].target)
        assertEquals(0, chunks[0].idx)
        assertEquals(1, chunks[0].total)
        assertEquals("ALICE", chunks[0].text)
    }

    @Test
    fun chunkText_multiChunkString_splitsInto14ByteChunks() {
        // 35 characters -> 14 + 14 + 7
        val message = "Happy 30th Birthday to my best pal!"
        assertEquals(35, message.length)

        val chunks = ProtocolCodec.chunkText(target = ProtocolConstants.TEXT_TARGET_BIRTHDAY_GREETING, text = message)
        assertEquals(3, chunks.size)

        // Chunk 0
        assertEquals(2, chunks[0].target)
        assertEquals(0, chunks[0].idx)
        assertEquals(3, chunks[0].total)
        assertEquals("Happy 30th Bir", chunks[0].text)
        assertEquals(14, chunks[0].text.length)

        // Chunk 1
        assertEquals(2, chunks[1].target)
        assertEquals(1, chunks[1].idx)
        assertEquals(3, chunks[1].total)
        assertEquals("thday to my be", chunks[1].text)
        assertEquals(14, chunks[1].text.length)

        // Chunk 2
        assertEquals(2, chunks[2].target)
        assertEquals(2, chunks[2].idx)
        assertEquals(3, chunks[2].total)
        assertEquals("st pal!", chunks[2].text)
        assertEquals(7, chunks[2].text.length)

        // Verify reassembly
        val reassembled = chunks.joinToString("") { it.text }
        assertEquals(message, reassembled)
    }

    @Test
    fun chunkText_exactMultipleOf14_chunksCorrectly() {
        val text = "1234567890123412345678901234" // 28 chars = 2 x 14
        val chunks = ProtocolCodec.chunkText(target = ProtocolConstants.TEXT_TARGET_MESSAGE, text = text)
        assertEquals(2, chunks.size)
        assertEquals("12345678901234", chunks[0].text)
        assertEquals("12345678901234", chunks[1].text)
        assertEquals(0, chunks[0].idx)
        assertEquals(1, chunks[1].idx)
        assertEquals(2, chunks[0].total)
        assertEquals(2, chunks[1].total)
    }

    @Test
    fun chunkText_nonAscii_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            ProtocolCodec.chunkText(target = 0, text = "Café 🎉")
        }
    }

    @Test
    fun chunkText_invalidTarget_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            ProtocolCodec.chunkText(target = 3, text = "Hello")
        }
    }

    @Test
    fun chunkDrawFrame_valid300Bytes_splitsInto22Chunks() {
        // 10x10 = 100 LEDs * 3 bytes (RGB) = 300 bytes
        val testFrame = ByteArray(ProtocolConstants.MATRIX_FRAME_BYTES) { i -> (i % 256).toByte() }

        val chunks = ProtocolCodec.chunkDrawFrame(testFrame)

        // 300 = 21 * 14 (294) + 1 * 6 = 22 chunks total
        assertEquals(22, chunks.size)

        var expectedOffset = 0
        val reassembled = ByteArray(ProtocolConstants.MATRIX_FRAME_BYTES)

        for (i in 0 until 22) {
            val chunk = chunks[i]
            assertEquals(expectedOffset, chunk.offset)
            if (i < 21) {
                assertEquals(14, chunk.data.size)
            } else {
                assertEquals(6, chunk.data.size)
            }
            System.arraycopy(chunk.data, 0, reassembled, chunk.offset, chunk.data.size)
            expectedOffset += chunk.data.size
        }

        assertEquals(ProtocolConstants.MATRIX_FRAME_BYTES, expectedOffset)
        org.junit.Assert.assertArrayEquals(testFrame, reassembled)
    }

    @Test
    fun chunkDrawFrame_invalidSize_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            ProtocolCodec.chunkDrawFrame(ByteArray(ProtocolConstants.MATRIX_FRAME_BYTES - 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ProtocolCodec.chunkDrawFrame(ByteArray(ProtocolConstants.MATRIX_FRAME_BYTES + 1))
        }
    }
}
