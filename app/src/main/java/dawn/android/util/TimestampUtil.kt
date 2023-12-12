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
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

object TimestampUtil {
    private var monday = "Mo."
    private var tuesday = "Tu."
    private var wednesday = "We."
    private var thursday = "Th."
    private var friday = "Fr."
    private var saturday = "Sa."
    private var sunday = "Su."
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
        return deriveHumanReadableTimestamp(currentDate, date)
    }

    private fun deriveHumanReadableTimestamp(dateNow: Instant, dateForTimestamp: Instant): String {
        val difference = Duration.between(dateForTimestamp, dateNow)
        if(difference.toDays() == 0L && (weekDayPattern.format(dateForTimestamp) == weekDayPattern.format(dateNow))) {
            // the timestamp is from the same day, return the time only
            return timeOnlyPattern.format(dateForTimestamp)
        }
        else if(difference.toDays() > 6L || (difference.toDays() == 6L && weekDayPattern.format(dateForTimestamp) == weekDayPattern.format(dateNow))) {
            // the timestamp refers to a date further away than a week or to the same day-of-week last week
            return dateOnlyPattern.format(dateForTimestamp)
        }
        else {
            // within a week and not from the same day
            return when(weekDayPattern.format(dateForTimestamp)) {
                "Mon" -> monday
                "Tue" -> tuesday
                "Wed" -> wednesday
                "Thu" -> thursday
                "Fri" -> friday
                "Sat" -> saturday
                "Sun" -> sunday
                else -> {
                    weekDayPattern.format(dateForTimestamp)
                }
            }
        }
    }
}