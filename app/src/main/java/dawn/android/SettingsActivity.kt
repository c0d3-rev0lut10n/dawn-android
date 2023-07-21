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

package dawn.android

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import dawn.android.data.Theme
import dawn.android.databinding.ActivitySettingsBinding
import dawn.android.util.ThemeLoader

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var mTheme: Theme
    private lateinit var actionBarText: SpannableString

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val mThemeLoader = ThemeLoader(this)
        mTheme = mThemeLoader.loadDarkTheme()

        window.statusBarColor = mTheme.primaryBackgroundColor

        val actionBar = binding.toolbar
        val actionBarTextColor = mTheme.primaryTextColor
        val actionBarString = getString(R.string.settings)
        actionBarText = SpannableString(actionBarString)
        actionBarText.setSpan(ForegroundColorSpan(actionBarTextColor), 0, actionBarString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        actionBar.setBackgroundColor(mTheme.primaryBackgroundColor)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(mTheme.backButtonIcon)
    }

    override fun onResume() {
        binding.toolbar.title = actionBarText
        super.onResume()
    }
}