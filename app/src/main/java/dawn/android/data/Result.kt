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

class Result<T, E>(private val ok: T?, private val err: E?) {

    companion object {
        fun <T: Any, E: Any> ok(res: T): Result<T, E> {
            return Result(res, null)
        }

        fun <T: Any, E: Any> err(err: E): Result<T, E> {
            return Result(null, err)
        }
    }

    fun unwrap(): T {
        if(this.ok == null) throw Exception("Called unwrap() on an Err value: ${this.err}")
        return this.ok
    }

    fun unwrapErr(): E {
        if(this.err == null) throw Exception("Called unwrapErr() on an Ok value: ${this.ok}")
        return this.err
    }

    fun print(): String {
        return if(isErr()) "Err($err)"
        else "Ok($ok)"
    }

    fun isErr(): Boolean {
        return this.ok == null
    }

    fun isOk(): Boolean {
        return this.err == null
    }

}