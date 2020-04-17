package de.wiomoc.tv.video

import android.util.SparseArray
import com.google.android.exoplayer2.extractor.ExtractorOutput
import com.google.android.exoplayer2.extractor.ts.SectionPayloadReader
import com.google.android.exoplayer2.extractor.ts.SectionReader
import com.google.android.exoplayer2.extractor.ts.TsExtractor
import com.google.android.exoplayer2.extractor.ts.TsPayloadReader
import com.google.android.exoplayer2.util.ParsableByteArray
import com.google.android.exoplayer2.util.TimestampAdjuster
import java.lang.Exception
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.HashSet
import kotlin.experimental.and

data class EPGEvent(
    val id: Short,
    val time: Date,
    val duration: Long,
    val name: String?,
    val shortDescription: String?,
    val description: String?
) {
    val isInPast: Boolean
        get() = Date().after(Date(time.time + duration * 1000))
    val isCurrently: Boolean
        get() = Date().after(Date(time.time)) && Date().before(Date(time.time + duration * 1000))
}

class /*Puny*/ EITReader(val listener: EPGEventListener) : SectionPayloadReader {

    interface EPGEventListener {
        fun onNewEvent(event: EPGEvent)
    }

    override fun init(
        timestampAdjuster: TimestampAdjuster?,
        extractorOutput: ExtractorOutput?,
        idGenerator: TsPayloadReader.TrackIdGenerator?
    ) {
    }

    val discoveredEvents = HashSet<Short>()

    override fun consume(data: ParsableByteArray) {
        // TODO consume not every packet they are highly redundant
        val tableId = data.readUnsignedByte()
        if (tableId == 0x4E || (tableId in 0x50..0x50)) {
            val sectionLength = data.readShort() and 0x0FFF
            //Log.d("EIT", "Length $sectionLength")
            /*data.readShort() // service_id
            data.readUnsignedByte() // version_number, current_next_indicator
            data.readUnsignedByte() // section_number
            data.readUnsignedByte() // last_section_number
            data.readShort() // transport_stream_id
            data.readShort() // original_network_id
            data.readUnsignedByte() // section_last_section_number
            data.readUnsignedByte() // last_table_id*/
            data.skipBytes(11)
            //Log.d("EIT", Base64.encodeToString(data.data.sliceArray(data.position..data.data.size - 1), 0))

            var remainingSectionLength = sectionLength - 13
            while (remainingSectionLength > 4) {
                remainingSectionLength -= 12
                try {
                    val eventId = data.readShort()
                    if (discoveredEvents.contains(eventId)) {
                        // Skip event
                        data.skipBytes(8)
                        val descriptorsLength = data.readShort().toInt() and 0xFFF
                        data.skipBytes(descriptorsLength)
                        remainingSectionLength -= descriptorsLength
                        continue
                    }
                    val MJD = data.readUnsignedShort()
                    // Proudly stolen from https://github.com/Andy1978/parse_eit/blob/82869a201bc50c0350730546f81a3f70a43907ef/parse_eit.c#L88
                    val Ys = ((MJD - 15078.2) / 365.25).toInt();
                    val tmp = (Ys * 365.25).toInt()
                    val Ms = ((MJD - 14956.1 - tmp) / 30.6001).toInt()
                    val tmp1 = (Ys * 365.25).toInt()
                    val tmp2 = (Ms * 30.6001).toInt()
                    val startTimeDay = MJD - 14956 - tmp1 - tmp2
                    val K = if (Ms == 14 || Ms == 15) 1 else 0
                    val startTimeYear = Ys + K;
                    val startTimeMonth = Ms - 1 - K * 12;

                    val startTimeHour = data.readBcd()
                    val startTimeSecond = data.readBcd()
                    val startTimeMinute = data.readBcd()

                    // TODO Migrate to modern datetypes
                    val startTimeUTC =
                        Date.UTC(
                            startTimeYear,
                            startTimeMonth - 1,
                            startTimeDay,
                            startTimeHour,
                            startTimeSecond,
                            startTimeMinute
                        )
                    val startTime = Date(startTimeUTC)
                    val duration = data.readBcd() * 3600 +
                            data.readBcd() * 60 +
                            data.readBcd().toLong()

                    var descriptorsLength = data.readShort().toInt() and 0xFFF
                    remainingSectionLength -= descriptorsLength

                    var name: String? = null
                    var shortDescription: String? = null
                    var description: String? = ""

                    while (descriptorsLength > 0) {
                        val descriptorTag = data.readUnsignedByte()
                        val descriptorLength = data.readUnsignedByte()
                        descriptorsLength -= descriptorLength
                        descriptorsLength -= 2

                        when (descriptorTag) {
                            0x4d -> {
                                // val langCode = data.readEITString(3)
                                data.skipBytes(3)
                                val nameLength = data.readUnsignedByte()
                                name = data.readEITString(nameLength)

                                val shortDescriptionLength = data.readUnsignedByte()
                                shortDescription = data.readEITString(shortDescriptionLength)
                            }
                            0x4e -> { // extended event descriptor
                                data.readUnsignedByte()
                                // val langCode = data.readEITString(3)
                                data.skipBytes(3)
                                var itemsLengthRemaining = data.readUnsignedByte()
                                while (itemsLengthRemaining > 0) {
                                    val itemDescriptionLength = data.readUnsignedByte()
                                    //val itemDescription = data.readEITString(itemDescriptionLength)
                                    data.skipBytes(itemDescriptionLength)

                                    val itemLength = data.readUnsignedByte()
                                    //val item = data.readEITString(itemLength)
                                    data.skipBytes(itemLength)
                                    itemsLengthRemaining -= 2
                                    itemsLengthRemaining -= itemDescriptionLength
                                    itemsLengthRemaining -= itemLength
                                }

                                val textLength = data.readUnsignedByte()
                                val text = data.readEITString(textLength)

                                description += text
                            }
                            /* 0x50 -> { // component descriptor
                                 data.readUnsignedByte()
                                 data.readUnsignedByte()
                                 data.readUnsignedByte()

                                 val langCode = data.readString(3, Charset.forName("ISO-8859-1"))

                                 val eventText = data.readString(descriptorLength - 6, Charset.forName("ISO-8859-1"))

                                 Log.d("EIT", "COM langcode $langCode text: $eventText ")

                             }*/
                            else -> {
                                data.skipBytes(descriptorLength)
                            }
                        }
                    }

                    if (description!!.isEmpty()) description = null
                    val event = EPGEvent(
                        eventId,
                        startTime,
                        duration,
                        name,
                        shortDescription,
                        description
                    )
                    discoveredEvents.add(eventId)
                    listener.onNewEvent(event)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun ParsableByteArray.readBcd(): Int {
        val b = readUnsignedByte()
        return ((b ushr 4) and 0xF) * 10 + (b and 0xF)
    }

    private fun ParsableByteArray.readEITString(length: Int): String {
        val b = peekUnsignedByte()
        if (b < 0x20) {
            skipBytes(1)
            val charset = when (b) {
                0x01 -> "ISO-8859-5"
                0x02 -> "ISO-8859-6"
                0x03 -> "ISO-8859-7"
                0x04 -> "ISO-8859-8"
                0x05 -> "ISO-8859-9"
                0x06 -> "ISO-8859-10"
                0x07 -> "ISO-8859-11"
                0x09 -> "ISO-8859-13"
                0x0A -> "ISO-8859-14"
                0x0B -> "ISO-8859-15"
                0x11 -> "ISO-10646"
                0x13 -> "GB2313"
                0x15 -> "UTF-8"
                else -> return "-- unknown charset --"
            }
            return readString(length - 1, Charset.forName(charset))
        } else {
            return readString(length, Charset.forName("ISO-8859-1"))
        }
    }
}

fun TsExtractor.withEPGListener(listener: EITReader.EPGEventListener): TsExtractor {
    TsExtractor::class.java.getDeclaredField("tsPayloadReaders").let {
        it.isAccessible = true
        val tsPayloadReaders = it.get(this) as SparseArray<TsPayloadReader>
        tsPayloadReaders.put(0x12, SectionReader(EITReader(listener)))
    }
    return this
}
