package com.example.calculatorr

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class PlayerActivity : AppCompatActivity() {

    private lateinit var tvCurrentTrack: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var lvTracks: ListView

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false

    private val tracks = mutableListOf<Track>()

    private val PERMISSION_REQUEST = 100

    private val audioExtensions = listOf("mp3", "m4a", "ogg", "wav", "flac", "aac")

    private val updateSeekBar = object : Runnable {
        override fun run() {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying && !isUserSeeking) {
                    seekBar.progress = mp.currentPosition
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        tvCurrentTrack = findViewById(R.id.tvCurrentTrack)
        seekBar = findViewById(R.id.seekBar)
        lvTracks = findViewById(R.id.lvTracks)

        findViewById<Button>(R.id.btnLoadTracks).setOnClickListener {
            checkPermissionAndLoad()
        }

        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            mediaPlayer?.let { mp ->
                if (!mp.isPlaying) {
                    mp.start()
                    handler.post(updateSeekBar)
                }
            }
        }

        findViewById<Button>(R.id.btnPause).setOnClickListener {
            mediaPlayer?.let { mp -> if (mp.isPlaying) mp.pause() }
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    mp.seekTo(0)
                    seekBar.progress = 0
                }
            }
        }

        findViewById<Button>(R.id.btnRewind).setOnClickListener {
            mediaPlayer?.let { mp ->
                val newPos = (mp.currentPosition - 10_000).coerceAtLeast(0)
                mp.seekTo(newPos)
                seekBar.progress = newPos
            }
        }

        findViewById<Button>(R.id.btnForward).setOnClickListener {
            mediaPlayer?.let { mp ->
                val newPos = (mp.currentPosition + 10_000).coerceAtMost(mp.duration)
                mp.seekTo(newPos)
                seekBar.progress = newPos
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) { isUserSeeking = true }
            override fun onStopTrackingTouch(sb: SeekBar?) { isUserSeeking = false }
        })

        lvTracks.setOnItemClickListener { _, _, position, _ ->
            val t = tracks[position]
            if (t.isDirectory) {
                Toast.makeText(this, "Это папка: ${t.title}", Toast.LENGTH_SHORT).show()
            } else {
                playTrack(t)
            }
        }
    }


    private fun checkPermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission)
            == PackageManager.PERMISSION_GRANTED) {
            loadTracks()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadTracks()
            } else {
                Toast.makeText(this, "Разрешение не дано", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ============ ЗАГРУЗКА ТРЕКОВ ============

    private fun loadTracks() {
        tracks.clear()

        // 1) Встроенный трек из res/raw/music.mp3 — всегда доступен
        tracks.add(
            Track(
                title = "music.mp3 (встроенный)",
                artist = "res/raw",
                path = "android.resource://$packageName/${R.raw.music}",
                isDirectory = false
            )
        )

        // 2) Треки из общей папки Music на устройстве
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

        if (musicDir.exists() && musicDir.isDirectory) {
            walkDirectory(musicDir)
        } else {
            Toast.makeText(this, "Папка Music не найдена", Toast.LENGTH_SHORT).show()
        }

        Toast.makeText(this, "Найдено: ${tracks.size}", Toast.LENGTH_SHORT).show()

        // Адаптер для ListView
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            tracks.map {
                if (it.isDirectory) "📁 ${it.title}"
                else "🎵 ${it.title}"
            }
        )
        lvTracks.adapter = adapter
    }

    /**
     * Рекурсивный обход папки с проверкой isDirectory.
     */
    private fun walkDirectory(dir: File) {
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                // Папка
                tracks.add(
                    Track(
                        title = file.name,
                        artist = "Папка",
                        path = file.absolutePath,
                        isDirectory = true
                    )
                )
                walkDirectory(file)
            } else {
                // Файл — проверяем расширение
                val ext = file.extension.lowercase()
                if (ext in audioExtensions) {
                    tracks.add(
                        Track(
                            title = file.name,
                            artist = file.parentFile?.name ?: "Неизвестно",
                            path = file.absolutePath,
                            isDirectory = false
                        )
                    )
                }
            }
        }
    }

    // ============ ВОСПРОИЗВЕДЕНИЕ ============

    private fun playTrack(track: Track) {
        try {
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                if (track.path.startsWith("android.resource://")) {
                    // Встроенный трек из res/raw
                    setDataSource(this@PlayerActivity, Uri.parse(track.path))
                } else {
                    // Обычный файл из памяти
                    setDataSource(this@PlayerActivity, Uri.fromFile(File(track.path)))
                }
                prepare()
                start()
                setOnCompletionListener {
                    seekBar.progress = 0
                }
            }

            tvCurrentTrack.text = "${track.title} — ${track.artist}"
            seekBar.max = mediaPlayer?.duration ?: 0
            seekBar.progress = 0
            handler.post(updateSeekBar)

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ============ LIFECYCLE ============

    override fun onPause() {
        super.onPause()
        mediaPlayer?.let { if (it.isPlaying) it.pause() }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBar)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}