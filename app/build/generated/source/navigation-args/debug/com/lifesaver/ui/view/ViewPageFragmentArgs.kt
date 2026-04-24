package com.lifesaver.ui.view

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.lang.IllegalArgumentException
import kotlin.Int
import kotlin.String
import kotlin.jvm.JvmStatic

public data class ViewPageFragmentArgs(
  public val groupId: String,
  public val pageIndex: Int = 0,
) : NavArgs {
  public fun toBundle(): Bundle {
    val result = Bundle()
    result.putString("groupId", this.groupId)
    result.putInt("pageIndex", this.pageIndex)
    return result
  }

  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    result.set("groupId", this.groupId)
    result.set("pageIndex", this.pageIndex)
    return result
  }

  public companion object {
    @JvmStatic
    public fun fromBundle(bundle: Bundle): ViewPageFragmentArgs {
      bundle.setClassLoader(ViewPageFragmentArgs::class.java.classLoader)
      val __groupId : String?
      if (bundle.containsKey("groupId")) {
        __groupId = bundle.getString("groupId")
        if (__groupId == null) {
          throw IllegalArgumentException("Argument \"groupId\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"groupId\" is missing and does not have an android:defaultValue")
      }
      val __pageIndex : Int
      if (bundle.containsKey("pageIndex")) {
        __pageIndex = bundle.getInt("pageIndex")
      } else {
        __pageIndex = 0
      }
      return ViewPageFragmentArgs(__groupId, __pageIndex)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): ViewPageFragmentArgs {
      val __groupId : String?
      if (savedStateHandle.contains("groupId")) {
        __groupId = savedStateHandle["groupId"]
        if (__groupId == null) {
          throw IllegalArgumentException("Argument \"groupId\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"groupId\" is missing and does not have an android:defaultValue")
      }
      val __pageIndex : Int?
      if (savedStateHandle.contains("pageIndex")) {
        __pageIndex = savedStateHandle["pageIndex"]
        if (__pageIndex == null) {
          throw IllegalArgumentException("Argument \"pageIndex\" of type integer does not support null values")
        }
      } else {
        __pageIndex = 0
      }
      return ViewPageFragmentArgs(__groupId, __pageIndex)
    }
  }
}
