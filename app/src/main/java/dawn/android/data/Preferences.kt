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

package dawn.android.data

object Preferences {
    // useful constants for other classes
    const val THEME_USE_SYSTEM = true
    const val THEME_MANUAL = false
    const val THEME_DARK = 2
    const val THEME_EXTRADARK = 3
    const val THEME_LIGHT = 4
    const val profileHandle = "profileHandle"
    const val profileName = "profileName"
    const val profileBio = "profileBio"
    const val server = "server"
    val sign = Sign
}

object Sign {
    const val ownPrivateKey = "seckeySig"
    const val ownPublicKey = "pubkeySig"
}