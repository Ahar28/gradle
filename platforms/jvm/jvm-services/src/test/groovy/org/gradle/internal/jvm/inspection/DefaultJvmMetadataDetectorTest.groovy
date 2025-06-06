/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.internal.jvm.inspection

import org.gradle.api.JavaVersion
import org.gradle.api.internal.file.TestFiles
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.toolchain.internal.InstallationLocation
import org.gradle.process.ExecResult
import org.gradle.process.internal.ClientExecHandleBuilder
import org.gradle.process.internal.ClientExecHandleBuilderFactory
import org.gradle.process.internal.ExecHandle
import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import org.spockframework.runtime.SpockAssertionError
import spock.lang.Specification

class DefaultJvmMetadataDetectorTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider temporaryFolder = new TestNameTestDirectoryProvider(getClass())

    TestFile tmpDir
    def setup() {
        tmpDir = temporaryFolder.createDir("tmp")
    }

    def "cleans up generated Probe class"() {
        given:
        def execHandleFactory = createExecHandleFactory(currentGradle())

        when:
        def detector = createDefaultJvmMetadataDetector(execHandleFactory)
        def javaHome = temporaryFolder.createDir("localGradle")
        def metadata = detector.getMetadata(testLocation(javaHome))

        then:
        metadata
        tmpDir.assertIsEmptyDir()
    }

    def "can detect metadata of #displayName"() {
        given:
        def execHandleFactory = createExecHandleFactory(systemProperties)

        when:
        def detector = createDefaultJvmMetadataDetector(execHandleFactory)
        def javaHome = temporaryFolder.createDir(jdk)
        if (!jre) {
            def binDir = javaHome.file("bin")
            if (binDir.mkdir()) {
                binDir.file(OperatingSystem.current().getExecutableName('javac')).touch()
                binDir.file(OperatingSystem.current().getExecutableName('javadoc')).touch()
                binDir.file(OperatingSystem.current().getExecutableName('jar')).touch()
            }
        }
        def metadata = detector.getMetadata(testLocation(javaHome))

        then:
        metadata.languageVersion == javaVersion
        displayName == metadata.displayName
        metadata.javaHome != null

        where:
        jdk              | systemProperties         | javaVersion             | displayName                              | jre
        'openJdk9'       | openJdkJvm('9')          | JavaVersion.VERSION_1_9 | 'OpenJDK JDK 9 (1.9.0-b08)'              | false
        'openJdk9'       | openJdkJvm('9')          | JavaVersion.VERSION_1_9 | 'OpenJDK JRE 9 (1.9.0-b08)'              | true
        'AdoptOpenJDK11' | adoptOpenJDK('11.0.3')   | JavaVersion.VERSION_11  | 'AdoptOpenJDK JDK 11 (11.0.3+7)'         | false
        'AdoptOpenJDK11' | adoptOpenJDK('11.0.3')   | JavaVersion.VERSION_11  | 'AdoptOpenJDK JRE 11 (11.0.3+7)'         | true
        'AdoptOpenJDKJ9' | adoptOpenJDKJ9('14.0.2') | JavaVersion.VERSION_14  | 'AdoptOpenJDK JDK 14 (14.0.2+7)'         | false
        'oracleJdk9'     | oracleJvm('9')           | JavaVersion.VERSION_1_9 | 'Oracle JDK 9 (1.9.0-b08)'               | false
        'oracleJre9'     | oracleJvm('9')           | JavaVersion.VERSION_1_9 | 'Oracle JRE 9 (1.9.0-b08)'               | true
        'ibmJre4'        | ibmJvm('4')              | JavaVersion.VERSION_1_4 | 'IBM JRE 4 (1.4.0-b08)'                  | true
        'ibmJdk9'        | ibmJvm('9')              | JavaVersion.VERSION_1_9 | 'IBM JDK 9 (1.9.0-b08)'                  | false
        'zuluJre6'       | zuluJvm('6')             | JavaVersion.VERSION_1_6 | 'Azul Zulu JRE 6 (1.6.0_66-b08)'         | true
        'zuluJdk8'       | zuluJvm('8')             | JavaVersion.VERSION_1_8 | 'Azul Zulu JDK 8 (1.8.0_66-b08)'         | false
        'hpuxJre6'       | hpuxJvm('6')             | JavaVersion.VERSION_1_6 | 'HP-UX JRE 6 (1.6.0_66-b08)'             | true
        'hpuxJdk7'       | hpuxJvm('7')             | JavaVersion.VERSION_1_7 | 'HP-UX JDK 7 (1.7.0_66-b08)'             | false
        'sapjdk13'       | sapJvm('13')             | JavaVersion.VERSION_13  | 'SAP SapMachine JDK 13 (13.0.2-b08)'     | false
        'sapjre13'       | sapJvm('13')             | JavaVersion.VERSION_13  | 'SAP SapMachine JRE 13 (13.0.2-b08)'     | true
        'correttojdk11'  | correttoJvm('11')        | JavaVersion.VERSION_11  | 'Amazon Corretto JDK 11 (11.0.8+10-LTS)' | false
        'correttojre11'  | correttoJvm('11')        | JavaVersion.VERSION_11  | 'Amazon Corretto JRE 11 (11.0.8+10-LTS)' | true
        'bellsoftjdk11'  | bellsoftJvm('15')        | JavaVersion.VERSION_15  | 'BellSoft Liberica JDK 15 (15+36)'       | false
        'bellsoftjre11'  | bellsoftJvm('15')        | JavaVersion.VERSION_15  | 'BellSoft Liberica JRE 15 (15+36)'       | true
        'graalvm'        | graalVm('15')            | JavaVersion.VERSION_15  | 'GraalVM Community JRE 15 (15-b08)'      | true
        'temurinjdk17'   | temurinJvm('17')         | JavaVersion.VERSION_17  | 'Eclipse Temurin JDK 17 (17-b08)'        | false
        'temurinjre8'    | temurin8Jvm('8')         | JavaVersion.VERSION_1_8 | 'Eclipse Temurin JRE 8 (8-b08)'          | true
        'semerujdk16'    | semeruJvm16()            | JavaVersion.VERSION_16  | 'IBM JDK 16 (16.0.2+7)'                  | false
        'semerujdk17'    | semeruJvm17()            | JavaVersion.VERSION_17  | 'IBM JRE 17 (17.0.5+8)'                  | true
        'whitespaces'    | whitespaces('11.0.3')    | JavaVersion.VERSION_11  | 'AdoptOpenJDK JRE 11 (11.0.3+7)'         | true
    }

    def "probes whether #jdk is a j9 virtual machine"() {
        given:
        def execHandleFactory = createExecHandleFactory(systemProperties)

        when:
        def detector = createDefaultJvmMetadataDetector(execHandleFactory)
        def javaHome = temporaryFolder.createDir(jdk)
        def metadata = detector.getMetadata(testLocation(javaHome))

        then:
        assert metadata.capabilities.contains(JavaInstallationCapability.J9_VIRTUAL_MACHINE) == isJ9

        where:
        jdk              | systemProperties         | isJ9
        'openJdk9'       | openJdkJvm('9')          | false
        'AdoptOpenJDK11' | adoptOpenJDK('11.0.3')   | false
        'AdoptOpenJDKJ9' | adoptOpenJDKJ9('14.0.2') | true
        'oracleJdk4'     | oracleJvm('4')           | false
        'ibmJdk4'        | ibmJvm('4')              | true
        'ibmJre4'        | ibmJvm('4')              | true
        'ibmJdk5'        | ibmJvm('5')              | true
        'ibmJdk6'        | ibmJvm('6')              | true
        'ibmJdk7'        | ibmJvm('7')              | true
        'ibmJdk8'        | ibmJvm('8')              | true
        'ibmJdk9'        | ibmJvm('9')              | true
        'semeru11'       | semeruJvm11()            | true
        'semeru16'       | semeruJvm16()            | true
        'temurin8'       | temurin8Jvm('8')         | false
        'temurin11'      | temurin11Jvm('11')       | false
        'temurin17'      | temurinJvm('17')         | false
    }

    def "detects invalid installation because #errorMessage"() {
        given:
        def execHandleFactory = createExecHandleFactory(systemProperties)

        when:
        def detector = createDefaultJvmMetadataDetector(execHandleFactory)
        def javaHome = temporaryFolder.createDir(jdk)
        if (!exists) {
            javaHome.deleteDir()
        }
        def metadata = detector.getMetadata(testLocation(javaHome))

        then:
        metadata.getErrorMessage().startsWith(errorMessage)
        assert metadata.getJavaHome() == javaHome.toPath()

        assertIsUnsupported({ metadata.languageVersion })
        assertIsUnsupported({ metadata.vendor })
        assertIsUnsupported({ metadata.javaVersion })
        assertIsUnsupported({ metadata.runtimeName })
        assertIsUnsupported({ metadata.runtimeVersion })
        assertIsUnsupported({ metadata.jvmName })
        assertIsUnsupported({ metadata.jvmVersion })
        assertIsUnsupported({ metadata.jvmVendor })
        assertIsUnsupported({ metadata.architecture })

        where:
        jdk                                   | systemProperties | exists | errorMessage
        'localGradle'                         | currentGradle()  | false  | "No such directory: "
        'binary that has invalid output'      | invalidOutput()  | true   | "Unexpected command output:"
        'binary that returns unknown version' | invalidVersion() | true   | "Cannot parse version number: bad luck"
    }

    private DefaultJvmMetadataDetector createDefaultJvmMetadataDetector(ClientExecHandleBuilderFactory execHandleFactory) {
        return new DefaultJvmMetadataDetector(
                execHandleFactory,
                TestFiles.tmpDirTemporaryFileProvider(tmpDir)
        )
    }

    private static Map<String, String> invalidOutput() {
        // emulates a binary that would not return the correct number of outputs
        [:]
    }

    private static Map<String, String> invalidVersion() {
        // emulates a valid binary that would not return the correct version number
        ['java.home': "java-home",
         'java.version': "bad luck",
         'java.vendor': "Oracle Corporation",
         'os.arch': "amd64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "25.40-b25",
         'java.vm.vendor': "Oracle Corporation",
         'java.runtime.name': "Java(TM) SE Runtime Environment",
         'java.runtime.version': "bad luck"
        ]
    }

    private InstallationLocation testLocation(File javaHome) {
        InstallationLocation.userDefined(javaHome, "test")
    }

    private static Map<String, String> currentGradle() {
        ['java.home',
         'java.version',
         'java.vendor',
         'java.runtime.name',
         'java.runtime.version',
         'java.vm.name',
         'java.vm.version',
         'java.vm.vendor',
         'os.arch',
        ].collectEntries { [it, System.getProperty(it)] }
    }

    private static Map<String, String> openJdkJvm(String version) {
        ['java.home': "java-home",
         'java.version': "1.${version}.0",
         'java.vendor': "Oracle Corporation",
         'os.arch': "amd64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "25.40-b25",
         'java.vm.vendor': "Oracle Corporation",
         'java.runtime.name': "Java(TM) SE Runtime Environment",
         'java.runtime.version': "1.${version}.0-b08"
        ]
    }

    private static Map<String, String> adoptOpenJDK(String version) {
        ['java.home': "java-home",
         'java.version': version,
         'java.vendor': "AdoptOpenJDK",
         'os.arch': "x86_64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "${version}+7",
         'java.vm.vendor': "AdoptOpenJDK",
         'java.runtime.name': "OpenJDK Runtime Environment",
         'java.runtime.version': "${version}+7"
        ]
    }

    private static Map<String, String> adoptOpenJDKJ9(String version) {
        ['java.home': "java-home",
         'java.version': version,
         'java.vendor': "AdoptOpenJDK",
         'os.arch': "x86_64",
         'java.vm.name': "Eclipse OpenJ9 VM",
         'java.vm.version': "openj9-0.21.0",
         'java.vm.vendor': "AdoptOpenJDK",
         'java.runtime.name': "OpenJDK Runtime Environment",
         'java.runtime.version': "${version}+7"
        ]
    }

    private static Map<String, String> whitespaces(String version) {
        ['java.home': "home-with-whitespaces\r",
         'java.version': version,
         'java.vendor': "AdoptOpenJDK\r",
         'os.arch': "x86_64\r",
         'java.vm.name': "OpenJDK 64-Bit Server VM\r",
         'java.vm.version': "${version}+7\r",
         'java.vm.vendor': "AdoptOpenJDK\r",
         'java.runtime.name': "OpenJDK Runtime Environment\r",
         'java.runtime.version': "${version}+7\r"
        ]
    }

    private static Map<String, String> oracleJvm(String version) {
        ['java.home': "java-home",
         'java.version': "1.${version}.0",
         'java.vendor': "Oracle Corporation",
         'os.arch': "amd64",
         'java.vm.name': "Java HotSpot(TM) 64-Bit Server VM",
         'java.vm.version': "25.40-b25",
         'java.vm.vendor': "Oracle Corporation",
         'java.runtime.name': "Java(TM) SE Runtime Environment",
         'java.runtime.version': "1.${version}.0-b08"
        ]
    }

    private static Map<String, String> ibmJvm(String version) {
        ['java.home': "java-home",
         'java.version': "1.${version}.0",
         'java.vendor': "IBM Corporation",
         'os.arch': "amd64",
         'java.vm.name': "IBM J9 VM",
         'java.vm.version': "2.4",
         'java.vm.vendor': "IBM Corporation",
         'java.runtime.name': "Java(TM) SE Runtime Environment",
         'java.runtime.version': "1.${version}.0-b08"
        ]
    }

    private static Map<String, String> zuluJvm(String version) {
        ['java.home': "java-home",
         'java.version': "1.${version}.0_66",
         'java.vendor': "Azul Systems, Inc.",
         'os.arch': "amd64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "25.66-b17",
         'java.vm.vendor': "Azul Systems, Inc.",
         'java.runtime.name': "OpenJDK Runtime Environment",
         'java.runtime.version': "1.${version}.0_66-b08"
        ]
    }

    private static Map<String, String> hpuxJvm(String version) {
        ['java.home': "java-home",
         'java.version': "1.${version}.0_66",
         'java.vendor': "Hewlett-Packard Co.",
         'os.arch': "ia64",
         'java.vm.name': "Java HotSpot(TM) 64-Bit Server VM",
         'java.vm.version': "25.66-b17",
         'java.vm.vendor': "Hewlett-Packard Co.",
         'java.runtime.name': "Java(TM) SE Runtime Environment",
         'java.runtime.version': "1.${version}.0_66-b08"
        ]
    }

    private static Map<String, String> sapJvm(String version) {
        ['java.home': "java-home",
         'java.version': "${version}.0.2",
         'java.vendor': "SAP SE",
         'os.arch': "x86_64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "13.0.2+8-sapmachine",
         'java.vm.vendor': "SAP SE",
         'java.runtime.name': "OpenJDK Runtime Environment",
         'java.runtime.version': "${version}.0.2-b08"
        ]
    }

    private static Map<String, String> correttoJvm(String version) {
        ['java.home': "java-home",
         'java.version': "${version}.0.8",
         'java.vendor': "Amazon.com Inc.",
         'os.arch': "x86_64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "11.0.8+10-LTS",
         'java.vm.vendor': "Amazon.com Inc.",
         'java.runtime.name': "OpenJDK Runtime Environment",
         'java.runtime.version': "${version}.0.8+10-LTS"
        ]
    }

    private static Map<String, String> bellsoftJvm(String version) {
        ['java.home': "java-home",
         'java.version': "${version}",
         'java.vendor': "BellSoft.",
         'os.arch': "x86_64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "${version}+36",
         'java.vm.vendor': "BellSoft.",
         'java.runtime.name': "OpenJDK Runtime Environment",
         'java.runtime.version': "${version}+36"
        ]
    }

    private static Map<String, String> graalVm(String version) {
        ['java.home': "java-home",
         'java.version': "${version}",
         'java.vendor': "GraalVM Community",
         'os.arch': "x86_64",
         'java.vm.name': "OpenJDK 64-Bit GraalVM CE 19.3.5",
         'java.vm.version': "25.282-b07-jvmci-19.3-b21",
         'java.vm.vendor': "GraalVM Community",
         'java.runtime.name': "OpenJDK Runtime Environment",
         'java.runtime.version': "${version}-b08"
        ]
    }

    // Temurin 8.x
    private static Map<String, String> temurin8Jvm(String version) {
        ['java.home': "java-home",
         'java.version': "${version}",
         'java.vendor': "Temurin",
         'os.arch': "x86_64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "25.282-b07-jvmci-19.3-b21",
         'java.vm.vendor': "Temurin",
         'java.runtime.name': "OpenJDK Runtime Environment",
         'java.runtime.version': "${version}-b08"
        ]
    }

    // Temurin 11.x and 16.x
    private static Map<String, String> temurin11Jvm(String version) {
        ['java.home': "java-home",
         'java.version': "${version}",
         'java.vendor': "Eclipse Foundation",
         'os.arch': "x86_64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "25.282-b07-jvmci-19.3-b21",
         'java.vm.vendor': "Eclipse Foundation",
         'java.runtime.name': "OpenJDK Runtime Environment",
         'java.runtime.version': "${version}-b08"
        ]
    }

    // Temurin 17.x and later
    private static Map<String, String> temurinJvm(String version) {
        ['java.home': "java-home",
         'java.version': "${version}",
         'java.vendor': "Eclipse Adoptium",
         'os.arch': "x86_64",
         'java.vm.name': "OpenJDK 64-Bit Server VM",
         'java.vm.version': "25.282-b07-jvmci-19.3-b21",
         'java.vm.vendor': "Eclipse Adoptium",
         'java.runtime.name': "OpenJDK Runtime Environment",
         'java.runtime.version': "${version}-b08"
        ]
    }

    private static Map<String, String> semeruJvm11() {
        ['java.home': "java-home",
         'java.version': "11.0.17",
         'java.vendor': "IBM Corporation",
         'os.arch': "x86_64",
         'java.vm.name': "Eclipse OpenJ9 VM",
         'java.vm.version': "openj9-0.35.0",
         'java.vm.vendor': "Eclipse OpenJ9",
         'java.runtime.name': "IBM Semeru Runtime Open Edition",
         'java.runtime.version': "11.0.17+8"
        ]
    }

    private static Map<String, String> semeruJvm16() {
        ['java.home': "java-home",
         'java.version': "16.0.2",
         'java.vendor': "International Business Machines Corporation",
         'os.arch': "x86_64",
         'java.vm.name': "Eclipse OpenJ9 VM",
         'java.vm.version': "openj9-0.27.0",
         'java.vm.vendor': "Eclipse OpenJ9",
         'java.runtime.name': "IBM Semeru Runtime Open Edition",
         'java.runtime.version': "16.0.2+7"
        ]
    }

    private static Map<String, String> semeruJvm17() {
        ['java.home': "java-home",
         'java.version': "17.0.5",
         'java.vendor': "IBM Corporation",
         'os.arch': "x86_64",
         'java.vm.name': "Eclipse OpenJ9 VM",
         'java.vm.version': "openj9-0.35.0",
         'java.vm.vendor': "Eclipse OpenJ9",
         'java.runtime.name': "IBM Semeru Runtime Open Edition",
         'java.runtime.version': "17.0.5+8"
        ]
    }

    def createExecHandleFactory(Map<String, String> actualProperties) {
        def probedSystemProperties = ProbedSystemProperty.values().findAll { it != ProbedSystemProperty.Z_ERROR }
        if (!actualProperties.isEmpty()) {
            assert actualProperties.keySet() == probedSystemProperties.collect { it.systemPropertyKey }.toSet()
        }

        def execHandleFactory = Mock(ClientExecHandleBuilderFactory)
        def exec = Mock(ClientExecHandleBuilder)
        execHandleFactory.newExecHandleBuilder() >> exec
        PrintStream output
        exec.setStandardOutput(_ as OutputStream) >> { OutputStream outputStream ->
            output = new PrintStream(outputStream)
            null
        }
        def handle = Mock(ExecHandle)
        handle.start() >> handle
        handle.waitForFinish() >> {
            // important to output in the order of the enum members as parsing uses enum ordinals
            probedSystemProperties.each {
                def actualValue = actualProperties[it.systemPropertyKey]
                // write conditionally to simulate wrong number of outputs
                if (actualValue != null) {
                    output.println(MetadataProbe.MARKER_PREFIX + actualValue)
                }
            }
            Mock(ExecResult)
        }
        exec.build() >> handle
        execHandleFactory
    }

    static void assertIsUnsupported(Closure<?> unsupportedOperation) {
        try {
            unsupportedOperation.run()
            throw new SpockAssertionError("Expected to throw UnsupportedOperationException")
        } catch (UnsupportedOperationException ignored) {
            // expected
        }
    }
}
