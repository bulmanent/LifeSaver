package com.lifesaver.ui.addedit

import android.os.Bundle
import androidx.navigation.NavDirections
import com.lifesaver.R
import kotlin.Int
import kotlin.String

public class AddEditGroupFragmentDirections private constructor() {
  private data class ActionAddEditGroupToDetail(
    public val groupId: String,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_addEditGroup_to_detail

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("groupId", this.groupId)
        return result
      }
  }

  public companion object {
    public fun actionAddEditGroupToDetail(groupId: String): NavDirections =
        ActionAddEditGroupToDetail(groupId)
  }
}
