package dawn.android.util

import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import dawn.android.util.TimestampUtil.deriveHumanReadableTimestamp as deriveHRTS

/*	Copyright (c) 2023 Laurenz Werner

	This file is part of Dawn.

	Dawn is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Dawn is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Dawn.  If not, see <http://www.gnu.org/licenses/>.
*/
class TimestampUtilTest {

    @Test
    fun deriveHumanReadableTimestamp() {
        val zone = ZoneId.of("UTC")
        val currentDate = Instant.ofEpochSecond(1702383891L)
        val timestampDate1 = Instant.ofEpochSecond(1702376691L) // two hours ago
        val timestampDate2 = Instant.ofEpochSecond(1702319091L) // 18 hours ago, on the previous day
        val timestampDate3 = Instant.ofEpochSecond(1701951891L) // five days ago
        val timestampDate4 = Instant.ofEpochSecond(1701861891L) // six days and one hour ago, weekday does not match
        val timestampDate5 = Instant.ofEpochSecond(1701815091L) // six days and 13 hours ago, therefore on a matching day-of-week
        val timestampDate6 = Instant.ofEpochSecond(1701519891L) // 10 days ago

        assert(deriveHRTS(currentDate, timestampDate1, zone) == "10:24")
        assert(deriveHRTS(currentDate, timestampDate2, zone) == "Mo.")
        assert(deriveHRTS(currentDate, timestampDate3, zone) == "Th.")
        assert(deriveHRTS(currentDate, timestampDate4, zone) == "We.")
        assert(deriveHRTS(currentDate, timestampDate5, zone) == "05.12.")
        assert(deriveHRTS(currentDate, timestampDate6, zone) == "02.12.")
    }
}