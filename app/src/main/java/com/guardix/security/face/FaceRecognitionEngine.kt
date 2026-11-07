package com.guardix.security.face

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class FaceRecognitionEngine(
    context: Context
) : Closeable {

    companion object {
        private const val MODEL_NAME = "mobilefacenet.tflite"
        private const val INPUT_SIZE = 112
        private const val EMBEDDING_SIZE = 192
    }

    private val interpreter: Interpreter

    init {
        val modelBuffer = context.assets.open(MODEL_NAME).use {
            val bytes = it.readBytes()
            ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
                rewind()
            }
        }

        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }

        interpreter = Interpreter(modelBuffer, options)
    }

    fun getFaceEmbedding(faceBitmap: Bitmap): FloatArray {

        val resizedBitmap = Bitmap.createScaledBitmap(
            faceBitmap,
            INPUT_SIZE,
            INPUT_SIZE,
            true
        )

        val inputBuffer = ByteBuffer.allocateDirect(
            1 * INPUT_SIZE * INPUT_SIZE * 3 * 4
        ).order(ByteOrder.nativeOrder())

        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {

                val pixel = resizedBitmap.getPixel(x, y)

                val red = ((pixel shr 16) and 0xFF)
                val green = ((pixel shr 8) and 0xFF)
                val blue = (pixel and 0xFF)

                inputBuffer.putFloat((red - 127.5f) / 128f)
                inputBuffer.putFloat((green - 127.5f) / 128f)
                inputBuffer.putFloat((blue - 127.5f) / 128f)
            }
        }

        inputBuffer.rewind()

        val output = Array(1) {
            FloatArray(EMBEDDING_SIZE)
        }

        interpreter.run(inputBuffer, output)

        return normalizeEmbedding(output[0])
    }

    private fun normalizeEmbedding(
        embedding: FloatArray
    ): FloatArray {

        val magnitude = sqrt(
            embedding.sumOf {
                (it * it).toDouble()
            }
        ).toFloat()

        if (magnitude == 0f) {
            throw IllegalStateException(
                "Invalid face embedding"
            )
        }

        return FloatArray(embedding.size) { index ->
            embedding[index] / magnitude
        }
    }

    fun compareFaces(
        first: FloatArray,
        second: FloatArray
    ): Float {

        require(first.size == EMBEDDING_SIZE)
        require(second.size == EMBEDDING_SIZE)

        return first.indices.sumOf { index ->
            (first[index] * second[index]).toDouble()
        }.toFloat()
    }

    override fun close() {
        interpreter.close()
    }
}