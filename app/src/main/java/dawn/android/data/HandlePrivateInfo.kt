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

import android.util.Base64
import dawn.android.CurveKeys
import dawn.android.KyberKeys
import dawn.android.data.Result.Companion.ok
import dawn.android.data.Result.Companion.err

class HandlePrivateInfo (
    val initKeypairKyber: KyberKeys,
    val initKeypairCurve: CurveKeys,
    val initKeypairCurvePfs2: CurveKeys,
    val initKeypairKyberSalt: KyberKeys,
    val initKeypairCurveSalt: CurveKeys
) {
    override fun toString(): String {
        return initKeypairKyber.own_pubkey_kyber!! + "\n" +
                initKeypairKyber.own_seckey_kyber!! + "\n" +
                initKeypairCurve.own_pubkey_curve!! + "\n" +
                initKeypairCurve.own_seckey_curve!! + "\n" +
                initKeypairCurvePfs2.own_pubkey_curve!! + "\n" +
                initKeypairCurvePfs2.own_seckey_curve!! + "\n" +
                initKeypairKyberSalt.own_pubkey_kyber!! + "\n" +
                initKeypairKyberSalt.own_seckey_kyber!! + "\n" +
                initKeypairCurveSalt.own_pubkey_curve!! + "\n" +
                initKeypairCurveSalt.own_seckey_curve!!
    }

    companion object {
        fun fromString(input: String): Result<HandlePrivateInfo, String> {
            val substrings = input.split("\n", limit = 10)
            if(substrings.size < 10) return err("missing information")
            for(stringToTest in substrings) {
                try {
                    Base64.decode(stringToTest, Base64.NO_WRAP)
                }
                catch (e: Exception) {
                    return err("the following string isn't valid base64: $stringToTest\nerror: $e")
                }
            }
            val initKeypairKyber = KyberKeys("ok", substrings[0], substrings[1])
            val initKeypairCurve = CurveKeys("ok", substrings[2], substrings[3])
            val initKeypairCurvePfs2 = CurveKeys("ok", substrings[4], substrings[5])
            val initKeypairKyberSalt = KyberKeys("ok", substrings[6], substrings[7])
            val initKeypairCurveSalt = CurveKeys("ok", substrings[8], substrings[9])
            return ok(
                HandlePrivateInfo(
                    initKeypairKyber = initKeypairKyber,
                    initKeypairCurve = initKeypairCurve,
                    initKeypairCurvePfs2 = initKeypairCurvePfs2,
                    initKeypairKyberSalt = initKeypairKyberSalt,
                    initKeypairCurveSalt = initKeypairCurveSalt
                )
            )
        }
    }
}