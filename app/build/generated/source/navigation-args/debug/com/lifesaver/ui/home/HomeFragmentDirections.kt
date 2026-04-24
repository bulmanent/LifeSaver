package com.lifesaver.ui.home

import android.os.Bundle
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.lifesaver.R
import kotlin.Int
import kotlin.String

public class HomeFragmentDirections private constructor() {
  private data class ActionHomeToDetail(
    public val groupId: String,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_home_to_detail

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("groupId", this.groupId)
        return result
      }
  }

  private data class ActionHomeToAddEditGroup(
    public val groupId: String = "",
  ) : NavDirections {
    public override val actionId: Int = R.id.action_home_to_add_edit_group

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("groupId", this.groupId)
        return result
      }
  }

  public companion object {
    public fun actionHomeToDetail(groupId: String): NavDirections = ActionHomeToDetail(groupId)

    public fun actionHomeToAddEditGroup(groupId: String = ""): NavDirections =
        ActionHomeToAddEditGroup(groupId)

    public fun actionHomeToSettings(): NavDirections =
        ActionOnlyNavDirections(R.id.action_home_to_settings)
  }
}
