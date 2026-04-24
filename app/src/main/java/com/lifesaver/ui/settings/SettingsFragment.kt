package com.lifesaver.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lifesaver.BuildConfig
import com.lifesaver.LifeSaverApplication
import com.lifesaver.R
import com.lifesaver.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory((requireActivity().application as LifeSaverApplication).repository)
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { showImportStrategyDialog(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvAppVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        binding.btnExport.setOnClickListener { viewModel.exportData() }
        binding.btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "*/*"))
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is SettingsEvent.ExportReady -> shareJson(event.json)
                is SettingsEvent.ImportSuccess -> {
                    Toast.makeText(requireContext(),
                        getString(R.string.import_success, event.count), Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.Error -> {
                    Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun shareJson(json: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
            putExtra(Intent.EXTRA_SUBJECT, "LifeSaver Backup")
        }
        startActivity(Intent.createChooser(intent, getString(R.string.export_via)))
    }

    private fun showImportStrategyDialog(uri: Uri) {
        val options = arrayOf(
            getString(R.string.merge_strategy),
            getString(R.string.replace_strategy)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_strategy_title)
            .setItems(options) { _, which ->
                if (which == 1) {
                    confirmReplaceAll(uri)
                } else {
                    viewModel.importFromUri(requireContext(), uri, replaceAll = false)
                }
            }
            .show()
    }

    private fun confirmReplaceAll(uri: Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.replace_all_title)
            .setMessage(R.string.replace_all_message)
            .setPositiveButton(R.string.replace) { _, _ ->
                viewModel.importFromUri(requireContext(), uri, replaceAll = true)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
