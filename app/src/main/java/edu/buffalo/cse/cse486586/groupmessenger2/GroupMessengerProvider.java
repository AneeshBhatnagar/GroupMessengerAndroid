package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import static edu.buffalo.cse.cse486586.groupmessenger2.DatabaseHelper.COLUMN_KEY;
import static edu.buffalo.cse.cse486586.groupmessenger2.DatabaseHelper.COLUMN_VALUE;
import static edu.buffalo.cse.cse486586.groupmessenger2.DatabaseHelper.TABLE_NAME;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {
    SQLiteDatabase sqLiteDatabase;
    DatabaseHelper databaseHelper;
    private static final String TAG = ContentProvider.class.getSimpleName();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        try {
            //Try to insert the row into the database
            sqLiteDatabase.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG,values.toString());
        } catch (SQLException e) {
            Log.e(TAG, "SQL Insert Content Provider Error");
            e.printStackTrace();
        }
        Log.v("insert", values.toString());
        sqLiteDatabase.close();
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        databaseHelper = new DatabaseHelper(getContext());
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        sqLiteDatabase = databaseHelper.getReadableDatabase();
        String[] colsFetch = {COLUMN_KEY, COLUMN_VALUE};
        String searchClause = COLUMN_KEY + " = ?";
        String[] searchQuery = {selection};
        Cursor cursor = sqLiteDatabase.query(TABLE_NAME, colsFetch, searchClause, searchQuery, null, null, null);
        Log.v("query", selection);
        cursor.moveToFirst();
        Object[] values = {cursor.getString(0), cursor.getString(1)};
        MatrixCursor matrixCursor = new MatrixCursor(colsFetch);
        matrixCursor.addRow(values);
        cursor.close();
        sqLiteDatabase.close();
        return matrixCursor;
    }
}
