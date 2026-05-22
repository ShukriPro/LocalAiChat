package com.shukri.ailocalchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class MainActivity : ComponentActivity() {

    private var engine: Engine? = null

    private val modelUrl =
        "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm"

    private val modelFileName =
        "gemma-3n-E2B-it-int4.litertlm"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LocalChatApp()
        }
    }

    @Composable
    fun LocalChatApp() {

        var input by remember { mutableStateOf("") }
        var output by remember {
            mutableStateOf(
                "Press Download Model first."
            )
        }

        var isBusy by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()

        val modelFile = File(filesDir, modelFileName)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Text(
                text = output,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                onClick = {

                    scope.launch {

                        isBusy = true

                        output =
                            "Downloading model...\n\nThis is 3.6GB.\nPlease wait."

                        try {

                            withContext(Dispatchers.IO) {

                                downloadFile(
                                    modelUrl,
                                    modelFile
                                )
                            }

                            output =
                                "Model downloaded successfully.\n\nNow ask something."

                        } catch (e: Exception) {

                            output =
                                "Download error:\n${e.message}"
                        }

                        isBusy = false
                    }
                }
            ) {

                Text(
                    if (modelFile.exists())
                        "Model Downloaded"
                    else
                        "Download Model"
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Ask something...")
                }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                modifier = Modifier.fillMaxWidth(),

                enabled =
                    !isBusy &&
                            input.isNotBlank() &&
                            modelFile.exists(),

                onClick = {

                    scope.launch {

                        isBusy = true

                        output = "Thinking..."

                        try {

                            val reply =
                                withContext(Dispatchers.IO) {

                                    if (engine == null) {

                                        engine = Engine(
                                            EngineConfig(
                                                modelPath =
                                                    modelFile.absolutePath,

                                                cacheDir =
                                                    cacheDir.absolutePath
                                            )
                                        )

                                        engine!!.initialize()
                                    }

                                    engine!!
                                        .createConversation()
                                        .use { conversation ->

                                            conversation
                                                .sendMessage(input)
                                                .toString()
                                        }
                                }

                            output = reply

                            input = ""

                        } catch (e: Exception) {

                            output =
                                "Chat error:\n${e.message}"
                        }

                        isBusy = false
                    }
                }
            ) {

                Text(
                    if (isBusy)
                        "Loading..."
                    else
                        "Send"
                )
            }
        }
    }

    private fun downloadFile(
        fileUrl: String,
        destination: File
    ) {

        destination.parentFile?.mkdirs()

        val connection =
            URL(fileUrl).openConnection()

        connection.setRequestProperty(
            "Authorization",
            //token this will need to hide on production
            "Bearer \${BuildConfig.HF_TOKEN}"
        )

        connection.setRequestProperty(
            "User-Agent",
            "AiLocalChat"
        )

        connection.connect()

        connection.getInputStream().use { input ->

            destination.outputStream().use { output ->

                input.copyTo(output)
            }
        }
    }

    override fun onDestroy() {

        engine?.close()
        engine = null

        super.onDestroy()
    }
}