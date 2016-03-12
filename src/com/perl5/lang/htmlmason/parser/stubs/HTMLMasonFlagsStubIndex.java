/*
 * Copyright 2016 Alexandr Evstigneev
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

package com.perl5.lang.htmlmason.parser.stubs;

import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import com.perl5.lang.htmlmason.parser.psi.HTMLMasonFlagsStatement;
import org.jetbrains.annotations.NotNull;

/**
 * Created by hurricup on 10.03.2016.
 * Index used to handle parent-to-child hierarchy resolution
 */
public class HTMLMasonFlagsStubIndex extends StringStubIndexExtension<HTMLMasonFlagsStatement>
{
	public static final StubIndexKey<String, HTMLMasonFlagsStatement> KEY = StubIndexKey.createIndexKey("perl.html.mason.flags");
	public static final int VERSION = 1;

	@Override
	public int getVersion()
	{
		return VERSION + super.getVersion();
	}

	@NotNull
	@Override
	public StubIndexKey<String, HTMLMasonFlagsStatement> getKey()
	{
		return KEY;
	}
}