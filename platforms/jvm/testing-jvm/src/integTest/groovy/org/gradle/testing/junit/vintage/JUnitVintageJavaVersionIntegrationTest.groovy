/*
 * Copyright 2025 the original author or authors.
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

package org.gradle.testing.junit.vintage

import org.gradle.integtests.fixtures.TargetCoverage
import org.gradle.testing.AbstractTestJavaVersionIntegrationTest

import static org.gradle.testing.fixture.JUnitCoverage.getJUNIT_VINTAGE

/**
 * Tests support for different JUnit Vintage versions across different Java versions.
 */
@TargetCoverage({ JUNIT_VINTAGE })
class JUnitVintageJavaVersionIntegrationTest extends AbstractTestJavaVersionIntegrationTest implements JUnitVintageMultiVersionTest {
}
