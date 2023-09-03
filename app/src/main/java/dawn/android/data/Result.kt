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

private class Result<T, E>(val ok: T?, val err: E?) {

    companion object {
        fun <T> ok(res: T): Result<T, Nothing> {
            return Result(res, null)
        }

        fun <E> err(err: E): Result<Nothing, E> {
            return Result(null, err)
        }
    }

    fun unwrap(): T {
        if(this.ok == null) throw Exception()
        return this.ok
    }

    fun isErr(): Boolean {
        return this.ok == null
    }

}