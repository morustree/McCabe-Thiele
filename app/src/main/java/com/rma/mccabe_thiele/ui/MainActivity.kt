package com.rma.mccabe_thiele.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.rma.mccabe_thiele.R
import com.rma.mccabe_thiele.data.PreferencesManager
import com.rma.mccabe_thiele.databinding.ActivityMainBinding
import com.rma.mccabe_thiele.helper.ToolbarEventViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val toolbarViewModel: ToolbarEventViewModel by viewModels()
    private var menuToolbar: Menu? = null

    private val prefsManager by lazy { PreferencesManager(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(prefsManager.lerTema())

        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top
            }
            insets
        }
        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        menuToolbar = menu
        toolbarListener()


        // recupera o estado do item de menu
        val themeItem = menu.findItem(R.id.action_theme)

        // verifica se o modo noturno está ativo
        val isNightMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        // define o ícone com base no estado atual
        if (isNightMode) {
            themeItem?.setIcon(R.drawable.ic_light_mode_24dp)
        } else {
            themeItem?.setIcon(R.drawable.ic_dark_mode_24dp)
        }


        return true
    }

    private fun toolbarListener() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                toolbarViewModel.botaoImportarAtivo.collectLatest { ativos ->
                    menuToolbar?.findItem(R.id.action_import)?.isEnabled = ativos
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                abrirConfiguracoes()
                true
            }
            R.id.action_import -> {
                toolbarViewModel.dispararCliqueImportarCsv()
                true
            }
            R.id.action_export -> {
                toolbarViewModel.dispararCliqueExportarCsv()
                true
            }
            R.id.action_theme -> {
                //modo noturno está ativo no sistema?
                val isNightModeActive = resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                if (isNightModeActive) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    prefsManager.salvarTema(AppCompatDelegate.MODE_NIGHT_NO)
                }else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    prefsManager.salvarTema(AppCompatDelegate.MODE_NIGHT_YES)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }


    private fun abrirConfiguracoes() {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        navController.navigate(R.id.SecondFragment)
    }


    private fun abrirComFade(intent: Intent) {
        val options = android.app.ActivityOptions.makeCustomAnimation(
            this, android.R.anim.fade_in, android.R.anim.fade_out
        )
        startActivity(intent, options.toBundle())
    }

}
