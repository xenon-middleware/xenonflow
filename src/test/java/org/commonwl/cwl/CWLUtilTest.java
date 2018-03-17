/**
 * Copyright 2013 Netherlands eScience Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonwl.cwl;

import static org.junit.Assert.assertEquals;

import org.commonwl.cwl.utils.CWLUtils;
import org.junit.Test;

public class CWLUtilTest {
	@Test
	public void relativePathTest() {
		assertEquals("wc-tool.cwl should be a local path", true, CWLUtils.isLocalPath("wc-tool.cwl"));
	}

	@Test
	public void absolutePathTest() {
		assertEquals("/home/bweel/wc-tool.cwl should be a local path", true,
				CWLUtils.isLocalPath("/home/bweel/wc-tool.cwl"));
	}

	@Test
	public void fileUrlTest() {
		assertEquals("file:///home/bweel/wc-tool.cwl should be a local path", true,
				CWLUtils.isLocalPath("file:///home/bweel/wc-tool.cwl"));
	}

	@Test
	public void urlTest() {
		assertEquals(
				"https://raw.githubusercontent.com/common-workflow-language/common-workflow-language/master/v1.0/v1.0/count-lines1-wf.cwl should be a remote path",
				false, CWLUtils.isLocalPath(
						"https://raw.githubusercontent.com/common-workflow-language/common-workflow-language/master/v1.0/v1.0/count-lines1-wf.cwl"));
	}
}
