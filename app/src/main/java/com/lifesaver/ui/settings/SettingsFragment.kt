package com.lifesaver.ui.settings

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.lifesaver.LifeSaverApplication
import com.lifesaver.R
import com.lifesaver.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val app by lazy {
        requireActivity().application as LifeSaverApplication
    }

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(app.repository)
    }

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            runCatching { app.authManager.handleSignInResult(result.data) }
                .onSuccess {
                    viewModel.reloadState()
                    Toast.makeText(requireContext(), getString(R.string.google_sign_in_success), Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    val message = if (result.resultCode == Activity.RESULT_CANCELED) {
                        app.authManager.describeSignInFailure(it)
                    } else {
                        app.authManager.describeSignInFailure(it)
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    viewModel.reloadState()
                }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSignIn.setOnClickListener {
            signInLauncher.launch(app.authManager.signInIntent())
        }

        binding.btnSignOut.setOnClickListener {
            app.authManager.signOut()
            viewModel.reloadState()
            Toast.makeText(requireContext(), getString(R.string.signed_out), Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveConfig.setOnClickListener {
            viewModel.saveConfig(
                sheetsId = binding.etSheetsId.text?.toString().orEmpty(),
                rootFolderId = binding.etRootFolderId.text?.toString().orEmpty()
            )
        }

        binding.btnSyncNow.setOnClickListener {
            viewModel.syncNow()
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.tvGoogleAccount.text = state.accountEmail ?: getString(R.string.not_signed_in)

            if (binding.etSheetsId.text?.toString() != state.sheetsId) {
                binding.etSheetsId.setText(state.sheetsId)
            }

            if (binding.etRootFolderId.text?.toString() != state.rootFolderId) {
                binding.etRootFolderId.setText(state.rootFolderId)
            }

            binding.btnSyncNow.isEnabled = state.canSync
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is SettingsEvent.Message ->
                    Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
