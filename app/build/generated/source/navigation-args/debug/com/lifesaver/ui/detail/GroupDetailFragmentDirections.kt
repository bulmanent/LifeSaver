package com.lifesaver.ui.detail

import android.os.Bundle
import androidx.navigation.NavDirections
import com.lifesaver.R
import kotlin.Int
import kotlin.String

public class GroupDetailFragmentDirections private constructor() {
  private data class ActionDetailToView(
    public val groupId: String,
    public val pageIndex: Int = 0,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_detail_to_view

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("groupId", this.groupId)
        result.putInt("pageIndex", this.pageIndex)
        return result
      }
  }

  private data class ActionDetailToAddEditGroup(
    public val groupId: String = "",
  ) : NavDirections {
    public override val actionId: Int = R.id.action_detail_to_add_edit_group

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("groupId", this.groupId)
        return result
      }
  }

  public companion object {
    public fun actionDetailToView(groupId: String, pageIndex: Int = 0): NavDirections =
        ActionDetailToView(groupId, pageIndex)

    public fun actionDetailToAddEditGroup(groupId: String = ""): NavDirections =
        ActionDetailToAddEditGroup(groupId)
  }
}
