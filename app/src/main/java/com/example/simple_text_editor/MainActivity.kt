@file:Suppress("DEPRECATION")

package com.example.simple_text_editor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {
    private val tag = "TagOpenTxt"
    private var uri2: Uri? = null
    lateinit var action: String
    private var isEditing = false
    private var filePath = ""
    lateinit var sPrefs:SharedPreferences
    lateinit var sPrefsEditor:SharedPreferences.Editor
    var encodingString = "UTF-8"

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        action = intent.action!!
        @Suppress("DEPRECATION")
        sPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        sPrefsEditor = sPrefs.edit()

        isStoragePermissionGranted()
        setToolbar()
        setUnEditable()
        getUriFromFile("")



        saveToSprefs(main_edit_text.text.toString())

        fab_undo.setOnClickListener {
            loadFromSprefs()
            Toast.makeText(this, "Изменения отменены", Toast.LENGTH_SHORT).show()
        }



        fab_edit_save.setOnClickListener {
            if (!isEditing){

                fab_edit_save.setColorFilter(R.color.teal_700)
                main_edit_text.inputType =
                        InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                isEditing = true
                buttonVisibility(true)
                Toast.makeText(this, "Режим изменения текста", Toast.LENGTH_SHORT).show()

            } else{
                main_edit_text.keyListener = null
                fab_edit_save.clearColorFilter()
                isEditing = false
                Toast.makeText(this, "Режим чтения", Toast.LENGTH_SHORT).show()
            }
        }

        fab_saving.setOnClickListener {
            if (filePath != ""){
                saveText(filePath)
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun buttonVisibility(b: Boolean) {
        if (b) {
            fab_saving.visibility = View.VISIBLE
            fab_undo.visibility = View.VISIBLE
        } else{
            fab_saving.visibility = View.GONE
            fab_undo.visibility = View.GONE
            fab_edit_save.clearColorFilter()
        }
    }

    private fun saveToSprefs(toString: String) {
        sPrefsEditor.putString("textContent", toString).apply()
    }

    private fun loadFromSprefs(){
        main_edit_text.setText(sPrefs.getString("textContent", "null"))
    }

    private fun setToolbar() {
        ib_title.setOnClickListener {
            action = Intent.ACTION_MAIN
            val documentIntent = Intent(Intent.ACTION_GET_CONTENT)
            documentIntent.type = "text/plain"
            startActivityForResult(documentIntent, 1)
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        }
    }

    private fun getUriFromFile(uriPath: String) {
        if (intent.data != null && Intent.ACTION_VIEW == action) {
            uri2 = intent.data
            if (isStoragePermissionGranted()) {
                setContentText(getRealPathFromURI(uri2)!!)

                setToolbarTitle(getRealPathFromURI(uri2)!!)
                filePath = getRealPathFromURI(uri2)!!
            }
        }
        if (uriPath != "" && Intent.ACTION_MAIN == action) {
            if (isStoragePermissionGranted()) {
                setContentText(uriPath)
                setToolbarTitle(uriPath)
                filePath = uriPath
            }
        } else {
            Log.d(tag, "intent was something else: $action")
            filePath = ""
        }
        buttonVisibility(false)
    }

    private fun setToolbarTitle(s: String) {
        val regex = """(.+)/(.+)\.(.+)""".toRegex()
        val matchResult = regex.matchEntire(s)
        val (directory, fileName, extension) = matchResult!!.destructured
        tv_title.text = "$fileName.$extension"
    }


    private fun setUnEditable() {
        main_edit_text.setTextIsSelectable(true)
        main_edit_text.keyListener = null
    }

    private fun saveText(s: String){
        File(s).writeText(main_edit_text.text.toString(), charset(encodingString))
        setUnEditable()
    }

    private fun setContentText(s: String) {
        main_edit_text.setText("")
        val buf =  File(s).readText().toByteArray()

        val fis = FileInputStream(s)
        val universalDetector = UniversalDetector(null)
        var nread: Int
        while (fis.read(buf).also { nread = it } > 0 && !universalDetector.isDone) {
            universalDetector.handleData(buf, 0, nread)
        }
        universalDetector.dataEnd()


        val encoding: String = universalDetector.detectedCharset
        println("Detected encoding = $encoding")
        universalDetector.reset()

        universalDetector.listener

        val mainText = File(s).readText(charset(encoding))
        encodingString = encoding


        main_edit_text.setText(mainText)
        saveToSprefs(mainText)

    }

    @Suppress("DEPRECATION")
    private fun getRealPathFromURI(contentUri: Uri?): String? {
        val project = arrayOf(MediaStore.Audio.Media.DATA)
        val cursor: Cursor = managedQuery(contentUri, project, null, null, null)
        val columnIndex: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        cursor.moveToFirst()
        Log.v("TAG", cursor.getString(columnIndex))
        return cursor.getString(columnIndex)
    }

    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("TAG", "Permission is granted")
                true
            } else {
                Log.v("TAG", "Permission is revoked")
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("TAG", "Permission is granted")
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("TAG", "Permission: " + permissions[0] + "was " + grantResults[0])
            //setContentText(getRealPathFromURI(uri2)!!)

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)


        if (resultCode != Activity.RESULT_OK)
            return

        if (requestCode == 1) {

            getUriFromFile(getRealPathFromURI(this, data?.data!!)!!)

        }
    }

    @Suppress("DEPRECATION")
    private fun getRealPathFromURI(context: Context, uri: Uri): String? {
        when {
            // DocumentProvider
            DocumentsContract.isDocumentUri(context, uri) -> {
                when {
                    // ExternalStorageProvider
                    isExternalStorageDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":").toTypedArray()
                        val type = split[0]
                        // This is for checking Main Memory
                        return if ("primary".equals(type, ignoreCase = true)) {
                            if (split.size > 1) {
                                Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                            } else {
                                Environment.getExternalStorageDirectory().toString() + "/"
                            }
                            // This is for checking SD Card
                        } else {
                            "storage" + "/" + docId.replace(":", "/")
                        }
                    }
                    isDownloadsDocument(uri) -> {
                        val fileName = getFilePath(context, uri)
                        if (fileName != null) {
                            return Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName
                        }
                        var id = DocumentsContract.getDocumentId(uri)
                        if (id.startsWith("raw:")) {
                            id = id.replaceFirst("raw:".toRegex(), "")
                            val file = File(id)
                            if (file.exists()) return id
                        }
                        val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
                        return getDataColumn(context, contentUri, null, null)
                    }
                    isMediaDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":").toTypedArray()
                        val type = split[0]
                        var contentUri: Uri? = null
                        when (type) {
                            "image" -> {
                                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            }
                            "video" -> {
                                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            }
                            "audio" -> {
                                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            }
                        }
                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])
                        return getDataColumn(context, contentUri, selection, selectionArgs)
                    }
                }
            }
            "content".equals(uri.scheme, ignoreCase = true) -> {
                // Return the remote address
                return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)
            }
            "file".equals(uri.scheme, ignoreCase = true) -> {
                return uri.path
            }
        }
        return null
    }

    private fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                              selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
                column
        )
        try {
            if (uri == null) return null
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs,
                    null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun getFilePath(context: Context, uri: Uri?): String? {
        var cursor: Cursor? = null
        val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME
        )
        try {
            if (uri == null) return null
            cursor = context.contentResolver.query(uri, projection, null, null,
                    null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

}

