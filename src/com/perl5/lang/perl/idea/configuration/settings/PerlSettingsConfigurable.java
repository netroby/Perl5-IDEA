/*
 * Copyright 2015 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perl5.lang.perl.idea.configuration.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.util.FileContentUtil;
import com.intellij.util.PlatformUtils;
import com.intellij.util.ui.FormBuilder;
import com.perl5.lang.perl.idea.actions.PerlFormatWithPerlTidyAction;
import com.perl5.lang.perl.idea.annotators.PerlCriticAnnotator;
import com.perl5.lang.perl.idea.project.PerlMicroIdeSettingsLoader;
import com.perl5.lang.perl.idea.sdk.PerlSdkType;
import com.perl5.lang.perl.util.PerlRunUtil;
import com.perl5.lang.perl.xsubs.PerlXSubsState;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by hurricup on 30.08.2015.
 */
public class PerlSettingsConfigurable implements Configurable
{
	Project myProject;
	Perl5Settings mySettings;

	TextFieldWithBrowseButton perlCriticPathInputField;
	RawCommandLineEditor perlCriticArgsInputField;

	TextFieldWithBrowseButton perlTidyPathInputField;
	RawCommandLineEditor perlTidyArgsInputField;

	TextFieldWithBrowseButton perlPathInputField;

	JTextField deparseArgumentsTextField;

	JPanel regeneratePanel;
	JButton regenerateButton;

	JCheckBox simpleMainCheckbox;
	JCheckBox autoInjectionCheckbox;
	JCheckBox perlCriticCheckBox;
	JCheckBox perlTryCatchCheckBox;
	JCheckBox perlAnnotatorCheckBox;
	JCheckBox allowInjectionWithInterpolation;

	CollectionListModel<String> selfNamesModel;
	JBList selfNamesList;


	public PerlSettingsConfigurable(Project myProject)
	{
		this.myProject = myProject;
		mySettings = Perl5Settings.getInstance(myProject);
	}

	@Nls
	@Override
	public String getDisplayName()
	{
		return "Perl5 settings";
	}

	@Nullable
	@Override
	public String getHelpTopic()
	{
		return null;
	}

	@Nullable
	@Override
	public JComponent createComponent()
	{
		FormBuilder builder = FormBuilder.createFormBuilder();
		builder.getPanel().setLayout(new VerticalFlowLayout());

		if (!PlatformUtils.isIntelliJ())
		{
			createMicroIdeComponents(builder);
		}

		simpleMainCheckbox = new JCheckBox("Use simple main:: subs resolution (many scripts with same named subs in main:: namespace)");
		builder.addComponent(simpleMainCheckbox);

		autoInjectionCheckbox = new JCheckBox("Automatically inject other languages in here-docs by marker text");
		builder.addComponent(autoInjectionCheckbox);

		allowInjectionWithInterpolation = new JCheckBox("Allow injections in QQ here-docs with interpolated entities");
		builder.addComponent(allowInjectionWithInterpolation);

		perlTryCatchCheckBox = new JCheckBox("Enable TryCatch syntax extension");
		builder.addComponent(perlTryCatchCheckBox);

		perlAnnotatorCheckBox = new JCheckBox("Enable perl -cw annotations [NYI]");
//		builder.addComponent(perlAnnotatorCheckBox);

		perlCriticCheckBox = new JCheckBox("Enable Perl::Critic annotations (should be installed)");
		builder.addComponent(perlCriticCheckBox);

		perlCriticPathInputField = new TextFieldWithBrowseButton();
		perlCriticPathInputField.setEditable(false);
		FileChooserDescriptor perlCriticDescriptor = new FileChooserDescriptor(true, false, false, false, false, false)
		{
			@Override
			public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles)
			{
				if (!super.isFileVisible(file, showHiddenFiles))
					return false;

				return file.isDirectory() || StringUtil.equals(file.getName(), PerlCriticAnnotator.PERL_CRITIC_NAME);
			}
		};

		perlCriticPathInputField.addBrowseFolderListener(
				"Select file",
				"Choose a Perl::Critic executable",
				null, // project
				perlCriticDescriptor
		);
		builder.addLabeledComponent(new JLabel("Path to PerlCritic executable:"), perlCriticPathInputField);
		perlCriticArgsInputField = new RawCommandLineEditor();
		builder.addComponent(copyDialogCaption(LabeledComponent.create(perlCriticArgsInputField, "Perl::Critic command line arguments:"), "Perl::Critic command line arguments:"));

		perlTidyPathInputField = new TextFieldWithBrowseButton();
		perlTidyPathInputField.setEditable(false);
		FileChooserDescriptor perlTidyDescriptor = new FileChooserDescriptor(true, false, false, false, false, false)
		{
			@Override
			public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles)
			{
				if (!super.isFileVisible(file, showHiddenFiles))
					return false;

				return file.isDirectory() || StringUtil.equals(file.getName(), PerlFormatWithPerlTidyAction.PERL_TIDY_NAME);
			}
		};

		perlTidyPathInputField.addBrowseFolderListener(
				"Select file",
				"Choose a Perl::Tidy executable",
				null, // project
				perlTidyDescriptor
		);
		builder.addLabeledComponent(new JLabel("Path to PerlTidy executable:"), perlTidyPathInputField);
		perlTidyArgsInputField = new RawCommandLineEditor();
		builder.addComponent(
				copyDialogCaption(
						LabeledComponent.create(perlTidyArgsInputField, "Perl::Tidy command line arguments (-se -se arguments will be added automatically):"),
						"Perl::Tidy command line arguments:"
				));

		regeneratePanel = new JPanel(new BorderLayout());
		regenerateButton = new JButton("Re-generate XSubs declarations");
		regenerateButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				PerlXSubsState.getInstance(myProject).reparseXSubs();
			}
		});
		regeneratePanel.add(regenerateButton, BorderLayout.WEST);
		builder.addComponent(regeneratePanel);

		deparseArgumentsTextField = new JTextField();
		builder.addLabeledComponent("Comma-separated B::Deparse options for deparse action", deparseArgumentsTextField);

		selfNamesModel = new CollectionListModel<String>();
		selfNamesList = new JBList(selfNamesModel);
		builder.addLabeledComponent(new JLabel("Scalar names considered as an object self-reference (without a $):"), ToolbarDecorator
				.createDecorator(selfNamesList)
				.setAddAction(new AnActionButtonRunnable()
				{
					@Override
					public void run(AnActionButton anActionButton)
					{
						String variableName = Messages.showInputDialog(
								myProject,
								"Type variable name:",
								"New Self-Reference Variable Name",
								Messages.getQuestionIcon(),
								"",
								null);
						if (StringUtil.isNotEmpty(variableName))
						{
							while (variableName.startsWith("$"))
							{
								variableName = variableName.substring(1);
							}

							if (StringUtil.isNotEmpty(variableName) && !selfNamesModel.getItems().contains(variableName))
							{
								selfNamesModel.add(variableName);
							}
						}
					}
				}).createPanel());


		return builder.getPanel();
	}

	protected void createMicroIdeComponents(FormBuilder builder)
	{
		perlPathInputField = new TextFieldWithBrowseButton()
		{

			@Override
			public void addBrowseFolderListener(@Nullable String title, @Nullable String description, @Nullable Project project, FileChooserDescriptor fileChooserDescriptor, TextComponentAccessor<JTextField> accessor, boolean autoRemoveOnHide)
			{
				addBrowseFolderListener(project, new BrowseFolderActionListener<JTextField>(title, description, this, project, fileChooserDescriptor, accessor)
				{
					@Nullable
					@Override
					protected VirtualFile getInitialFile()
					{
						VirtualFile virtualFile = super.getInitialFile();
						if (virtualFile == null)
						{
							String directoryName = PerlRunUtil.getPathFromPerl();
							if (StringUtil.isNotEmpty(directoryName))
							{
								directoryName = FileUtil.toSystemIndependentName(directoryName);
								VirtualFile path = LocalFileSystem.getInstance().findFileByPath(expandPath(directoryName));
								while (path == null && directoryName.length() > 0)
								{
									int pos = directoryName.lastIndexOf('/');
									if (pos <= 0) break;
									directoryName = directoryName.substring(0, pos);
									path = LocalFileSystem.getInstance().findFileByPath(directoryName);
								}
								return path;
							}
						}
						return virtualFile;
					}
				}, autoRemoveOnHide);
			}
		};

		perlPathInputField.getTextField().setEditable(false);

		FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false)
		{
			@Override
			public void validateSelectedFiles(VirtualFile[] files) throws Exception
			{
				PerlSdkType sdkType = PerlSdkType.getInstance();
				if (!sdkType.isValidSdkHome(files[0].getCanonicalPath()))
					throw new ConfigurationException("Unable to locate perl5 executable");
			}
		};

		perlPathInputField.addBrowseFolderListener(
				"Perl5 Interpreter",
				"Choose a directory with perl5 executable:",
				null, // project
				descriptor
		);


		builder.addLabeledComponent("Perl5 interpreter path: ", perlPathInputField, 1);
	}

	@Override
	public boolean isModified()
	{
		return isMicroIdeModified() ||
				mySettings.SIMPLE_MAIN_RESOLUTION != simpleMainCheckbox.isSelected() ||
				mySettings.AUTOMATIC_HEREDOC_INJECTIONS != autoInjectionCheckbox.isSelected() ||
				mySettings.ALLOW_INJECTIONS_WITH_INTERPOLATION != allowInjectionWithInterpolation.isSelected() ||
				mySettings.PERL_ANNOTATOR_ENABLED != perlAnnotatorCheckBox.isSelected() ||
				mySettings.PERL_CRITIC_ENABLED != perlCriticCheckBox.isSelected() ||
				mySettings.PERL_TRY_CATCH_ENABLED != perlTryCatchCheckBox.isSelected() ||
				!StringUtil.equals(mySettings.PERL_DEPARSE_ARGUMENTS, deparseArgumentsTextField.getText()) ||
				!StringUtil.equals(mySettings.PERL_CRITIC_PATH, perlCriticPathInputField.getText()) ||
				!StringUtil.equals(mySettings.PERL_CRITIC_ARGS, perlCriticArgsInputField.getText()) ||
				!StringUtil.equals(mySettings.PERL_TIDY_PATH, perlTidyPathInputField.getText()) ||
				!StringUtil.equals(mySettings.PERL_TIDY_ARGS, perlTidyArgsInputField.getText()) ||
				!mySettings.selfNames.equals(selfNamesModel.getItems());
	}

	protected boolean isMicroIdeModified()
	{
		return !PlatformUtils.isIntelliJ() &&
				(
						!mySettings.perlPath.equals(perlPathInputField.getText())
				);
	}

	@Override
	public void apply() throws ConfigurationException
	{
		mySettings.SIMPLE_MAIN_RESOLUTION = simpleMainCheckbox.isSelected();
		mySettings.AUTOMATIC_HEREDOC_INJECTIONS = autoInjectionCheckbox.isSelected();
		mySettings.ALLOW_INJECTIONS_WITH_INTERPOLATION = allowInjectionWithInterpolation.isSelected();
		mySettings.PERL_ANNOTATOR_ENABLED = perlAnnotatorCheckBox.isSelected();
		mySettings.setDeparseOptions(deparseArgumentsTextField.getText());

		boolean needReparse = mySettings.PERL_TRY_CATCH_ENABLED != perlTryCatchCheckBox.isSelected();
		mySettings.PERL_TRY_CATCH_ENABLED = perlTryCatchCheckBox.isSelected();

		mySettings.PERL_CRITIC_ENABLED = perlCriticCheckBox.isSelected();
		mySettings.PERL_CRITIC_PATH = perlCriticPathInputField.getText();
		mySettings.PERL_CRITIC_ARGS = perlCriticArgsInputField.getText();

		mySettings.PERL_TIDY_PATH = perlTidyPathInputField.getText();
		mySettings.PERL_TIDY_ARGS = perlTidyArgsInputField.getText();

		mySettings.selfNames.clear();
		mySettings.selfNames.addAll(selfNamesModel.getItems());

		if (!PlatformUtils.isIntelliJ())
		{
			applyMicroIdeSettings();
		}
		mySettings.settingsUpdated();

		if (needReparse)
		{
			FileContentUtil.reparseOpenedFiles();
		}
	}

	public void applyMicroIdeSettings()
	{
		mySettings.perlPath = perlPathInputField.getText();

		ApplicationManager.getApplication().runWriteAction(
				new Runnable()
				{
					@Override
					public void run()
					{
						ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(ModuleManager.getInstance(myProject).getModules()[0]).getModifiableModel();
						PerlMicroIdeSettingsLoader.applyClassPaths(modifiableModel);
						modifiableModel.commit();
					}
				}
		);
	}

	@Override
	public void reset()
	{
		selfNamesModel.removeAll();
		selfNamesModel.add(mySettings.selfNames);

		simpleMainCheckbox.setSelected(mySettings.SIMPLE_MAIN_RESOLUTION);
		autoInjectionCheckbox.setSelected(mySettings.AUTOMATIC_HEREDOC_INJECTIONS);
		allowInjectionWithInterpolation.setSelected(mySettings.ALLOW_INJECTIONS_WITH_INTERPOLATION);
		perlAnnotatorCheckBox.setSelected(mySettings.PERL_ANNOTATOR_ENABLED);
		perlTryCatchCheckBox.setSelected(mySettings.PERL_TRY_CATCH_ENABLED);
		deparseArgumentsTextField.setText(mySettings.PERL_DEPARSE_ARGUMENTS);

		perlCriticCheckBox.setSelected(mySettings.PERL_CRITIC_ENABLED);
		perlCriticPathInputField.setText(mySettings.PERL_CRITIC_PATH);
		perlCriticArgsInputField.setText(mySettings.PERL_CRITIC_ARGS);

		perlTidyPathInputField.setText(mySettings.PERL_TIDY_PATH);
		perlTidyArgsInputField.setText(mySettings.PERL_TIDY_ARGS);

		if (!PlatformUtils.isIntelliJ())
		{
			resetMicroIdeSettings();
		}
	}

	protected void resetMicroIdeSettings()
	{
		perlPathInputField.setText(mySettings.perlPath);
	}

	@Override
	public void disposeUIResources()
	{
		perlPathInputField = null;
		simpleMainCheckbox = null;
		selfNamesModel = null;
		selfNamesList = null;
		autoInjectionCheckbox = null;
		allowInjectionWithInterpolation = null;
		perlAnnotatorCheckBox = null;
		perlTryCatchCheckBox = null;
		deparseArgumentsTextField = null;
		regeneratePanel = null;
		regenerateButton = null;

		perlCriticCheckBox = null;
		perlCriticPathInputField = null;
		perlCriticArgsInputField = null;

		perlTidyPathInputField = null;
		perlTidyArgsInputField = null;
	}

	protected LabeledComponent<RawCommandLineEditor> copyDialogCaption(final LabeledComponent<RawCommandLineEditor> component, String text)
	{
		final RawCommandLineEditor rawCommandLineEditor = component.getComponent();
		rawCommandLineEditor.setDialogCaption(text);
		component.getLabel().setLabelFor(rawCommandLineEditor.getTextField());
		return component;
	}

}
