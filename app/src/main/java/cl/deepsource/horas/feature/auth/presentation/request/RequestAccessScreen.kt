package cl.deepsource.horas.feature.auth.presentation.request

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.deepsource.horas.R
import cl.deepsource.horas.core.ui.components.DsFormScaffold
import cl.deepsource.horas.core.ui.components.DsPrimaryButton
import cl.deepsource.horas.core.ui.components.DsTextField
import cl.deepsource.horas.core.ui.theme.DeepSourceTheme
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsError
import cl.deepsource.horas.core.ui.theme.DsInput
import cl.deepsource.horas.core.ui.theme.DsOutline
import cl.deepsource.horas.core.ui.theme.DsSuccess
import cl.deepsource.horas.core.ui.theme.DsTextMuted
import cl.deepsource.horas.core.ui.theme.DsTextPrimary
import cl.deepsource.horas.core.ui.theme.DsSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestAccessRoute(
    onBack: () -> Unit,
    viewModel: RequestAccessViewModel = viewModel(factory = RequestAccessViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }

    DsFormScaffold(
        title = stringResource(R.string.request_title),
        subtitle = stringResource(R.string.request_subtitle),
        onBack = onBack,
    ) {
        DsTextField(
            value = state.fullName,
            onValueChange = viewModel::onNameChange,
            label = stringResource(R.string.field_name),
            leadingIcon = Icons.Outlined.Person,
            error = state.nameError,
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        DsTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.field_email),
            placeholder = stringResource(R.string.field_email_placeholder),
            leadingIcon = Icons.Outlined.Email,
            error = state.emailError,
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        )

        Column {
            Text(stringResource(R.string.field_project).uppercase(), style = MaterialTheme.typography.labelSmall, color = DsTextMuted)
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = menuOpen, onExpandedChange = { menuOpen = it }) {
                OutlinedTextField(
                    value = state.project,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DsInput,
                        unfocusedContainerColor = DsInput,
                        focusedBorderColor = DsCyan,
                        unfocusedBorderColor = DsOutline,
                        focusedTextColor = DsTextPrimary,
                        unfocusedTextColor = DsTextPrimary,
                    ),
                )
                ExposedDropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = DsSurface,
                ) {
                    DeepSourceTheme {
                        AvailableProjects.forEach { project ->
                            DropdownMenuItem(
                                text = { Text(project, color = DsTextPrimary) },
                                onClick = {
                                    viewModel.onProjectChange(project)
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
            }
        }

        state.generalError?.let { Text(stringResource(it), color = DsError, style = MaterialTheme.typography.bodyMedium) }
        DsPrimaryButton(
            text = stringResource(R.string.request_submit),
            onClick = viewModel::onSubmit,
            loading = state.isLoading,
        )
        state.sentTo?.let {
            Text(stringResource(R.string.request_sent, it), color = DsSuccess, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
