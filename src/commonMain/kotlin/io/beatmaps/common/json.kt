package io.beatmaps.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

val json = Json {
    serializersModule = SerializersModule {
        modlog()
        userlog()
        playlist()
    }
    prettyPrint = true
}

val jsonIgnoreUnknown = Json {
    ignoreUnknownKeys = true
}

sealed class OptionalProperty<out T> {
    object WrongType : OptionalProperty<Nothing>() {
        override fun validate(notPresent: Boolean, block: (Nothing) -> Boolean) = notPresent
        override fun orNull() = null
    }
    object NotPresent : OptionalProperty<Nothing>() {
        override fun validate(notPresent: Boolean, block: (Nothing) -> Boolean) = notPresent
        override fun orNull() = null
    }
    data class Present<T>(val value: T) : OptionalProperty<T>() {
        override fun validate(notPresent: Boolean, block: (T) -> Boolean) = block(value)
        override fun orNull(): T? = value
    }

    abstract fun validate(notPresent: Boolean = true, block: (T) -> Boolean): Boolean
    abstract fun orNull(): T?
}

open class OptionalPropertySerializer<T>(
    private val valueSerializer: KSerializer<T>
) : KSerializer<OptionalProperty<T>> {
    final override val descriptor: SerialDescriptor = valueSerializer.descriptor

    final override fun deserialize(decoder: Decoder): OptionalProperty<T> =
        try {
            OptionalProperty.Present(valueSerializer.deserialize(decoder))
        } catch (e: SerializationException) {
            OptionalProperty.WrongType
        }

    final override fun serialize(encoder: Encoder, value: OptionalProperty<T>) {
        when (value) {
            OptionalProperty.WrongType, OptionalProperty.NotPresent -> throw SerializationException(
                "Tried to serialize an optional property that had no value present. Is encodeDefaults false?"
            )
            is OptionalProperty.Present ->
                valueSerializer.serialize(encoder, value.value)
        }
    }
}
