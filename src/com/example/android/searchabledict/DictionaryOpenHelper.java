package com.example.android.searchabledict;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

/**
 * This creates/opens the database.
 */
public class DictionaryOpenHelper extends SQLiteOpenHelper
{

	private static final String TAG = DictionaryOpenHelper.class
			.getCanonicalName();

	private final Context mHelperContext;
	private SQLiteDatabase mDatabase;

	private static final String DATABASE_NAME = "dictionary";
	private static final int DATABASE_VERSION = 2;

	public static final String FTS_VIRTUAL_TABLE = "FTSdictionary";

	// The columns we'll include in the dictionary table
	public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;
	public static final String KEY_DEFINITION = SearchManager.SUGGEST_COLUMN_TEXT_2;

	/*
	 * Note that FTS3 does not support column constraints and thus, you cannot
	 * declare a primary key. However, "rowid" is automatically used as a unique
	 * identifier, so when making requests, we will use "_id" as an alias for
	 * "rowid"
	 */
	private static final String FTS_TABLE_CREATE = "CREATE VIRTUAL TABLE "
			+ FTS_VIRTUAL_TABLE + " USING fts3 (" + KEY_WORD + ", "
			+ KEY_DEFINITION + ");";

	DictionaryOpenHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mHelperContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		mDatabase = db;
		mDatabase.execSQL(FTS_TABLE_CREATE);
		loadDictionary();
	}

	/**
	 * Starts a thread to load the database table with words
	 */
	private void loadDictionary()
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					loadWords();
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			}
		}).start();
	}

	private void loadWords() throws IOException
	{
		Log.d(TAG, "Loading words...");
		final Resources resources = mHelperContext.getResources();
		InputStream inputStream = resources.openRawResource(R.raw.definitions);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				inputStream));

		try
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] strings = TextUtils.split(line, "-");
				if (strings.length < 2)
					continue;
				long id = addWord(strings[0].trim(), strings[1].trim());
				if (id < 0)
				{
					Log.e(TAG, "unable to add word: " + strings[0].trim());
				}
			}
			addWord("你好", "双方见面打招呼");
		}
		finally
		{
			reader.close();
		}
		Log.d(TAG, "DONE loading words.");
	}

	/**
	 * Add a word to the dictionary.
	 * 
	 * @return rowId or -1 if failed
	 */
	public long addWord(String word, String definition)
	{
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_WORD, word);
		initialValues.put(KEY_DEFINITION, definition);

		return mDatabase.insert(FTS_VIRTUAL_TABLE, null, initialValues);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
		onCreate(db);
	}
}
