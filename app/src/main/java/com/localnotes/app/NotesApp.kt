package com.localnotes.app

import android.app.Application
import com.localnotes.app.data.AppDatabase
import com.localnotes.app.data.NoteRepository
import com.localnotes.app.data.SessionStore

class NotesApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: NoteRepository by lazy { NoteRepository(database.noteDao()) }
    val session: SessionStore by lazy { SessionStore(this) }
}
