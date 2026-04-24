package com.lifesaver.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.lifesaver.data.dao.DocumentGroupDao;
import com.lifesaver.data.dao.DocumentGroupDao_Impl;
import com.lifesaver.data.dao.DocumentPageDao;
import com.lifesaver.data.dao.DocumentPageDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile DocumentGroupDao _documentGroupDao;

  private volatile DocumentPageDao _documentPageDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `document_groups` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `sequence` INTEGER NOT NULL, `tags` TEXT NOT NULL, `description` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `document_pages` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `sequence` INTEGER NOT NULL, `uri` TEXT NOT NULL, `caption` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`groupId`) REFERENCES `document_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_document_pages_groupId` ON `document_pages` (`groupId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '32a1482642d2d3cc5d17fc5ac72c30ac')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `document_groups`");
        db.execSQL("DROP TABLE IF EXISTS `document_pages`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsDocumentGroups = new HashMap<String, TableInfo.Column>(5);
        _columnsDocumentGroups.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocumentGroups.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocumentGroups.put("sequence", new TableInfo.Column("sequence", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocumentGroups.put("tags", new TableInfo.Column("tags", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocumentGroups.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDocumentGroups = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDocumentGroups = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDocumentGroups = new TableInfo("document_groups", _columnsDocumentGroups, _foreignKeysDocumentGroups, _indicesDocumentGroups);
        final TableInfo _existingDocumentGroups = TableInfo.read(db, "document_groups");
        if (!_infoDocumentGroups.equals(_existingDocumentGroups)) {
          return new RoomOpenHelper.ValidationResult(false, "document_groups(com.lifesaver.data.entity.DocumentGroupEntity).\n"
                  + " Expected:\n" + _infoDocumentGroups + "\n"
                  + " Found:\n" + _existingDocumentGroups);
        }
        final HashMap<String, TableInfo.Column> _columnsDocumentPages = new HashMap<String, TableInfo.Column>(5);
        _columnsDocumentPages.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocumentPages.put("groupId", new TableInfo.Column("groupId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocumentPages.put("sequence", new TableInfo.Column("sequence", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocumentPages.put("uri", new TableInfo.Column("uri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocumentPages.put("caption", new TableInfo.Column("caption", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDocumentPages = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysDocumentPages.add(new TableInfo.ForeignKey("document_groups", "CASCADE", "NO ACTION", Arrays.asList("groupId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesDocumentPages = new HashSet<TableInfo.Index>(1);
        _indicesDocumentPages.add(new TableInfo.Index("index_document_pages_groupId", false, Arrays.asList("groupId"), Arrays.asList("ASC")));
        final TableInfo _infoDocumentPages = new TableInfo("document_pages", _columnsDocumentPages, _foreignKeysDocumentPages, _indicesDocumentPages);
        final TableInfo _existingDocumentPages = TableInfo.read(db, "document_pages");
        if (!_infoDocumentPages.equals(_existingDocumentPages)) {
          return new RoomOpenHelper.ValidationResult(false, "document_pages(com.lifesaver.data.entity.DocumentPageEntity).\n"
                  + " Expected:\n" + _infoDocumentPages + "\n"
                  + " Found:\n" + _existingDocumentPages);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "32a1482642d2d3cc5d17fc5ac72c30ac", "5f83dcce3f669bada2d90c1daff26b5d");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "document_groups","document_pages");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `document_groups`");
      _db.execSQL("DELETE FROM `document_pages`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(DocumentGroupDao.class, DocumentGroupDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DocumentPageDao.class, DocumentPageDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public DocumentGroupDao documentGroupDao() {
    if (_documentGroupDao != null) {
      return _documentGroupDao;
    } else {
      synchronized(this) {
        if(_documentGroupDao == null) {
          _documentGroupDao = new DocumentGroupDao_Impl(this);
        }
        return _documentGroupDao;
      }
    }
  }

  @Override
  public DocumentPageDao documentPageDao() {
    if (_documentPageDao != null) {
      return _documentPageDao;
    } else {
      synchronized(this) {
        if(_documentPageDao == null) {
          _documentPageDao = new DocumentPageDao_Impl(this);
        }
        return _documentPageDao;
      }
    }
  }
}
