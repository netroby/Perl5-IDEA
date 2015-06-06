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

package com.perl5.lang.perl.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.perl5.lang.perl.psi.PerlVariable;
import com.perl5.lang.perl.psi.PerlVariableDeclaration;
import com.perl5.lang.perl.psi.properties.PerlLexicalScope;
import com.perl5.lang.perl.psi.utils.PerlVariableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by hurricup on 27.05.2015.
 * Misc helper methods
 */
public class PerlUtil
{

	/**
	 * Searches for innermost source root for a file
	 * @param project project to search in
	 * @param file	containing file
	 * @return	innermost root
	 */
	public static VirtualFile findInnermostSourceRoot(Project project, VirtualFile file)
	{
		VirtualFile innerMostRoot = null;

		for (VirtualFile sourceRoot : ProjectRootManager.getInstance(project).getContentSourceRoots())
		{
			if (VfsUtil.isAncestor(sourceRoot, file, true))
			{
				if (innerMostRoot == null || VfsUtil.isAncestor(innerMostRoot, sourceRoot, true))
					innerMostRoot = sourceRoot;
			}
		}

		return innerMostRoot;
	}

	/**
	 * Searches for innermost source root for a file by it's absolute path
	 * @param project project to search in
	 * @param filePath	containing filename
	 * @return	innermost root
	 */
	public static VirtualFile findInnermostSourceRoot(Project project, String filePath)
	{
		VirtualFile innerMostRoot = null;
		File file = new File(filePath);

		for (VirtualFile sourceRoot : ProjectRootManager.getInstance(project).getContentSourceRoots())
		{
			File sourceRootFile = new File(sourceRoot.getPath());

			if (VfsUtil.isAncestor(sourceRootFile, file, true))
			{
				if (innerMostRoot == null || VfsUtil.isAncestor(innerMostRoot, sourceRoot, true))
					innerMostRoot = sourceRoot;
			}
		}

		return innerMostRoot;
	}

}
