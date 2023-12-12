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

package dawn.android.util

import android.content.Context
import dawn.android.R
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimestampUtil {
    private var monday = "Mo."
    private var tuesday = "Tu."
    private var wednesday = "We."
    private var thursday = "Th."
    private var friday = "Fr."
    private var saturday = "Sa."
    private var sunday = "Su."
    private var zone = ZoneId.of("UTC")
    private val weekDayPattern = DateTimeFormatter.ofPattern("EEE")
    private val timeOnlyPattern = DateTimeFormatter.ofPattern("HH:mm")
    private val dateOnlyPattern = DateTimeFormatter.ofPattern("dd.MM.")

    fun init(context: Context) {
        monday = context.getString(R.string.monday_short)
        tuesday = context.getString(R.string.tuesday_short)
        wednesday = context.getString(R.string.wednesday_short)
        thursday = context.getString(R.string.thursday_short)
        friday = context.getString(R.string.friday_short)
        saturday = context.getString(R.string.saturday_short)
        sunday = context.getString(R.string.sunday_short)
    }

    fun Long.toTimestampForChatPreview(): String {
        val date = Instant.ofEpochSecond(this)
        val currentDate = Instant.now()
        return deriveHumanReadableTimestamp(currentDate, date, zone)
    }

    fun deriveHumanReadableTimestamp(dateNow: Instant, dateForTimestamp: Instant, zone: ZoneId): String {
        val difference = Duration.between(dateForTimestamp, dateNow)
        val dateNowLocal = dateNow.atZone(zone)
        val dateForTimestampLocal = dateForTimestamp.atZone(zone)
        if(difference.toDays() == 0L && (dateForTimestampLocal.dayOfWeek == dateNowLocal.dayOfWeek)) {
            // the timestamp is from the same day, return the time only
            return timeOnlyPattern.format(dateForTimestampLocal)
        }
        else if(difference.toDays() > 6L || (difference.toDays() == 6L && dateForTimestampLocal.dayOfWeek == dateNowLocal.dayOfWeek)) {
            // the timestamp refers to a date further away than a week or to the same day-of-week last week
            return dateOnlyPattern.format(dateForTimestampLocal)
        }
        else {
            // within a week and not from the same day
            return when(dateForTimestampLocal.dayOfWeek) {
                DayOfWeek.MONDAY -> monday
                DayOfWeek.TUESDAY -> tuesday
                DayOfWeek.WEDNESDAY -> wednesday
                DayOfWeek.THURSDAY -> thursday
                DayOfWeek.FRIDAY -> friday
                DayOfWeek.SATURDAY -> saturday
                DayOfWeek.SUNDAY -> sunday
                else -> {
                    weekDayPattern.format(dateForTimestampLocal)
                }
            }
        }
    }
}