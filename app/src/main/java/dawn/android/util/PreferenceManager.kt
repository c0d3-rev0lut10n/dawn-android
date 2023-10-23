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

import dawn.android.data.Result
import dawn.android.data.Result.Companion.ok
import dawn.android.data.Result.Companion.err
import java.io.File

class PreferenceManager(
    private val preferencePath: File
) {
    private val cache: HashMap<String, String> = HashMap()

    init {
        val preferenceFileContent = DataManager.readFile("preferences", preferencePath)
        // TODO: parse preferences and cache them
    }

    fun get(key: String): Result<String, String> {
        val valueFromCache = cache[key]
        if(valueFromCache != null) return ok(valueFromCache)
        return err("not implemented")
    }

    fun set(key: String, value: String) {
        cache[key] = value
    }
}