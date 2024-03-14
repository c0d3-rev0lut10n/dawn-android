/*
 * Copyright (c) 2024  Laurenz Werner
 *
 * This file is part of Dawn.
 *
 * Dawn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dawn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dawn.  If not, see <http://www.gnu.org/licenses/>.
 */

package dawn.android.messagereception

import java.security.SecureRandom

object SubscriptionUtil {
    fun createSubscriptions(ids: ArrayList<PollingId>, subscriptionCount: UShort): ArrayList<Subscription> {
        val subscriptions = ArrayList<Subscription>()
        val rng = SecureRandom()
        val subCount = subscriptionCount.toInt()
        if(subCount == 0) return subscriptions
        for(i in 0..subCount) {
            subscriptions.add(Subscription("", ArrayList(), 0U))
        }
        for(id in ids) {
            val subscriptionToUse = rng.nextInt(subCount)
            subscriptions[subscriptionToUse].associatedChats.add(id)
        }
        return subscriptions
    }
}