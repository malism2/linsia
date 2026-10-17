package com.malism.linsia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.defaultComponentContext
import com.malism.linsia.core.platform.AndroidAppContext
import com.malism.linsia.learnword.data.db.AndroidAppHolder
import com.malism.linsia.root.DefaultAppRootComponent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAppContext.init(applicationContext)
        AndroidAppHolder.init(applicationContext)

        val root = DefaultAppRootComponent(defaultComponentContext())
        setContent {
            App(root)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
