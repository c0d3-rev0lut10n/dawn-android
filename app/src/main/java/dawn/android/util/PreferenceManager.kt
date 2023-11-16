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

import dawn.android.data.Ok
import dawn.android.data.Result
import dawn.android.data.Result.Companion.ok
import dawn.android.data.Result.Companion.err
import dawn.android.data.SimpleStringMap
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object PreferenceManager {
    private var cache: SimpleStringMap = SimpleStringMap(
        HashMap()
    )
    private lateinit var path: File

    fun new(preferencePath: File) {
        path = preferencePath
    }

    fun init(preferencePath: File) {
        path = preferencePath
        val preferenceFileContent = DataManager.readFile("preferences", path)?: throw Exception("@$this: could not read preferences")
        cache = Json.decodeFromString(String(preferenceFileContent, Charsets.UTF_8))
    }

    fun get(key: String): Result<String, String> {
        val valueFromCache = cache.content[key]
        if(valueFromCache != null) return ok(valueFromCache)
        return err("not found")
    }

    fun set(key: String, value: String) {
        cache.content[key] = value
    }

    fun drop(key: String): Result<Ok, String> {
        return if(cache.content.remove(key) == null) err("value did not exist")
        else ok(Ok)
    }

    fun dump(): HashMap<String, String> {
        return this.cache.content
    }

    fun write(): Result<Ok, String> {
        val fileContent = Json.encodeToString(cache).toByteArray(Charsets.UTF_8)
        val result = DataManager.writeFile("preferences", path, fileContent, true)
        return if(!result) err("@$this: could not write preferences")
        else ok(Ok)
    }
}