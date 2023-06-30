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

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import dawn.android.data.Theme
import dawn.android.databinding.ActivityMainBinding
import dawn.android.util.ThemeLoader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mLibraryConnector: LibraryConnector
    private lateinit var mTheme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {

        mLibraryConnector = LibraryConnector

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // load theme
        val mThemeLoader = ThemeLoader(this)
        mTheme = mThemeLoader.loadDarkTheme()

        window.statusBarColor = mTheme.primaryBackgroundColor

        val actionBar = binding.appBarMain.toolbar
        val actionBarTextColor = mTheme.primaryTextColor
        val actionBarString = "Dawn"
        val actionBarText = SpannableString(actionBarString)
        actionBarText.setSpan(ForegroundColorSpan(actionBarTextColor), 0, actionBarString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        actionBar.title = actionBarText
        //actionBar.setBackgroundDrawable(ColorDrawable(actionBarTextColor))

        actionBar.setBackgroundColor(mTheme.primaryBackgroundColor)

        binding.navView.setNavigationItemSelectedListener { navigate(it) }
    }

    override fun onResume() {
        if(binding.drawerLayout.isDrawerOpen(binding.navView))
            binding.drawerLayout.closeDrawer(binding.navView)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(mTheme.navigationIcon)
        binding.navView.setCheckedItem(R.id.nav_home)
        super.onResume()
    }

    override fun onSupportNavigateUp(): Boolean {
        if(binding.drawerLayout.isDrawerOpen(binding.navView))
            binding.drawerLayout.closeDrawer(binding.navView)
        else {
            binding.drawerLayout.openDrawer(binding.navView)
        }
        return super.onSupportNavigateUp()
    }

    private fun navigate(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        return true
    }
}