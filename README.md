# OpenSAML and commons-codec 1.23.0

Demonstrates that adding commons-codec 1.23.0 to the classpath breaks OpenSAML.

## Run the failing test

```
./mvnw test
```

The test fails with the following stack trace (the surefire and TestNG runner frames are included, so this is the complete output):

```
java.security.cert.CertificateException: net.shibboleth.shared.codec.DecodingException: Unable to base64 decode data: Strict decoding: Missing or incomplete final line separator.
	at org.opensaml.security.x509.X509Support.decodeCertificate(X509Support.java:405)
	at org.opensaml.xmlsec.keyinfo.KeyInfoSupport.getCertificate(KeyInfoSupport.java:233)
	at org.opensaml.xmlsec.keyinfo.KeyInfoSupport.getCertificates(KeyInfoSupport.java:206)
	at org.opensaml.xmlsec.keyinfo.KeyInfoSupport.getCertificates(KeyInfoSupport.java:181)
	at example.xmlsec.keyinfo.tests.KeyInfoSupportTest.getCertificatesWithCanonicalBase64(KeyInfoSupportTest.java:57)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
	at java.base/java.lang.reflect.Method.invoke(Method.java:565)
	at org.testng.internal.invokers.MethodInvocationHelper.invokeMethod(MethodInvocationHelper.java:139)
	at org.testng.internal.invokers.TestInvoker.invokeMethod(TestInvoker.java:677)
	at org.testng.internal.invokers.TestInvoker.invokeTestMethod(TestInvoker.java:221)
	at org.testng.internal.invokers.MethodRunner.runInSequence(MethodRunner.java:50)
	at org.testng.internal.invokers.TestInvoker$MethodInvocationAgent.invoke(TestInvoker.java:969)
	at org.testng.internal.invokers.TestInvoker.invokeTestMethods(TestInvoker.java:194)
	at org.testng.internal.invokers.TestMethodWorker.invokeTestMethods(TestMethodWorker.java:148)
	at org.testng.internal.invokers.TestMethodWorker.run(TestMethodWorker.java:128)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
	at org.testng.TestRunner.privateRun(TestRunner.java:829)
	at org.testng.TestRunner.run(TestRunner.java:602)
	at org.testng.SuiteRunner.runTest(SuiteRunner.java:437)
	at org.testng.SuiteRunner.runSequentially(SuiteRunner.java:431)
	at org.testng.SuiteRunner.privateRun(SuiteRunner.java:391)
	at org.testng.SuiteRunner.run(SuiteRunner.java:330)
	at org.testng.SuiteRunnerWorker.runSuite(SuiteRunnerWorker.java:52)
	at org.testng.SuiteRunnerWorker.run(SuiteRunnerWorker.java:95)
	at org.testng.TestNG.runSuitesSequentially(TestNG.java:1256)
	at org.testng.TestNG.runSuitesLocally(TestNG.java:1176)
	at org.testng.TestNG.runSuites(TestNG.java:1099)
	at org.testng.TestNG.run(TestNG.java:1067)
	at org.apache.maven.surefire.testng.TestNGExecutor.run(TestNGExecutor.java:155)
	at org.apache.maven.surefire.testng.TestNGDirectoryTestSuite.executeSingleClass(TestNGDirectoryTestSuite.java:102)
	at org.apache.maven.surefire.testng.TestNGDirectoryTestSuite.execute(TestNGDirectoryTestSuite.java:91)
	at org.apache.maven.surefire.testng.TestNGProvider.invoke(TestNGProvider.java:137)
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385)
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162)
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507)
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495)
Caused by: net.shibboleth.shared.codec.DecodingException: Unable to base64 decode data: Strict decoding: Missing or incomplete final line separator.
	at net.shibboleth.shared.codec.Base64Support.decode(Base64Support.java:107)
	at org.opensaml.security.x509.X509Support.decodeCertificate(X509Support.java:403)
	... 35 more
Caused by: java.lang.IllegalArgumentException: Strict decoding: Missing or incomplete final line separator.
	at org.apache.commons.codec.binary.BaseNCodec.validateCanonicalEnd(BaseNCodec.java:1066)
	at org.apache.commons.codec.binary.Base64.decode(Base64.java:1085)
	at org.apache.commons.codec.binary.BaseNCodec.decode(BaseNCodec.java:711)
	at org.apache.commons.codec.binary.BaseNCodec.decode(BaseNCodec.java:768)
	at net.shibboleth.shared.codec.Base64Support.decode(Base64Support.java:99)
	... 36 more
```

## What the test does

[`KeyInfoSupportTest`](src/test/java/example/xmlsec/keyinfo/tests/KeyInfoSupportTest.java) parses [`idp-metadata.xml`](src/test/resources/idp-metadata.xml) and calls [`KeyInfoSupport.getCertificates`](https://codeberg.org/Shibboleth/java-opensaml/src/tag/5.2.3/opensaml-xmlsec-api/src/main/java/org/opensaml/xmlsec/keyinfo/KeyInfoSupport.java#L171). The certificate in the metadata is already in the form the strict base64 decoder requires (76 character lines and a final newline).

## Where OpenSAML removes the required newline

OpenSAML trims the text of the `X509Certificate` element while parsing, which removes the final newline before it is decoded:

- [`AbstractXMLObjectUnmarshaller#L363`](https://codeberg.org/Shibboleth/java-opensaml/src/tag/5.2.3/opensaml-core-api/src/main/java/org/opensaml/core/xml/io/AbstractXMLObjectUnmarshaller.java#L363) trims the text node.
- [`XSBase64BinaryUnmarshaller#L35`](https://codeberg.org/Shibboleth/java-opensaml/src/tag/5.2.3/opensaml-core-impl/src/main/java/org/opensaml/core/xml/schema/impl/XSBase64BinaryUnmarshaller.java#L35) trims it again.
- [`AbstractXMLObject#L285`](https://codeberg.org/Shibboleth/java-opensaml/src/tag/5.2.3/opensaml-core-api/src/main/java/org/opensaml/core/xml/AbstractXMLObject.java#L285) trims it a third time in `setValue`.

The trimmed value is then decoded by [`Base64Support#L99`](https://codeberg.org/Shibboleth/java-shib-shared/src/tag/9.2.3/shib-support/src/main/java/net/shibboleth/shared/codec/Base64Support.java#L99), which uses a strict chunked decoder. commons-codec 1.23.0 [rejects the missing final newline](https://github.com/apache/commons-codec/blob/rel/commons-codec-1.23.0/src/main/java/org/apache/commons/codec/binary/BaseNCodec.java#L1066).

## Line-wrapped certificates also fail

`getCertificatesWithLineWrappedBase64` uses a certificate wrapped at 64 characters per line. It also fails with commons-codec 1.23.0.

`getCertificatesWithLineWrappedBase64` fails with:

```
java.security.cert.CertificateException: net.shibboleth.shared.codec.DecodingException: Unable to base64 decode data: Strict decoding: Data follows the final line separator.
	at org.opensaml.security.x509.X509Support.decodeCertificate(X509Support.java:405)
	at org.opensaml.xmlsec.keyinfo.KeyInfoSupport.getCertificate(KeyInfoSupport.java:233)
	at org.opensaml.xmlsec.keyinfo.KeyInfoSupport.getCertificates(KeyInfoSupport.java:206)
	at org.opensaml.xmlsec.keyinfo.KeyInfoSupport.getCertificates(KeyInfoSupport.java:181)
	at example.xmlsec.keyinfo.tests.KeyInfoSupportTest.assertCertificateCanBeRead(KeyInfoSupportTest.java:70)
	at example.xmlsec.keyinfo.tests.KeyInfoSupportTest.getCertificatesWithLineWrappedBase64(KeyInfoSupportTest.java:60)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
	at java.base/java.lang.reflect.Method.invoke(Method.java:565)
	at org.testng.internal.invokers.MethodInvocationHelper.invokeMethod(MethodInvocationHelper.java:139)
	at org.testng.internal.invokers.TestInvoker.invokeMethod(TestInvoker.java:677)
	at org.testng.internal.invokers.TestInvoker.invokeTestMethod(TestInvoker.java:221)
	at org.testng.internal.invokers.MethodRunner.runInSequence(MethodRunner.java:50)
	at org.testng.internal.invokers.TestInvoker$MethodInvocationAgent.invoke(TestInvoker.java:969)
	at org.testng.internal.invokers.TestInvoker.invokeTestMethods(TestInvoker.java:194)
	at org.testng.internal.invokers.TestMethodWorker.invokeTestMethods(TestMethodWorker.java:148)
	at org.testng.internal.invokers.TestMethodWorker.run(TestMethodWorker.java:128)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
	at org.testng.TestRunner.privateRun(TestRunner.java:829)
	at org.testng.TestRunner.run(TestRunner.java:602)
	at org.testng.SuiteRunner.runTest(SuiteRunner.java:437)
	at org.testng.SuiteRunner.runSequentially(SuiteRunner.java:431)
	at org.testng.SuiteRunner.privateRun(SuiteRunner.java:391)
	at org.testng.SuiteRunner.run(SuiteRunner.java:330)
	at org.testng.SuiteRunnerWorker.runSuite(SuiteRunnerWorker.java:52)
	at org.testng.SuiteRunnerWorker.run(SuiteRunnerWorker.java:95)
	at org.testng.TestNG.runSuitesSequentially(TestNG.java:1256)
	at org.testng.TestNG.runSuitesLocally(TestNG.java:1176)
	at org.testng.TestNG.runSuites(TestNG.java:1099)
	at org.testng.TestNG.run(TestNG.java:1067)
	at org.apache.maven.surefire.testng.TestNGExecutor.run(TestNGExecutor.java:155)
	at org.apache.maven.surefire.testng.TestNGDirectoryTestSuite.executeSingleClass(TestNGDirectoryTestSuite.java:102)
	at org.apache.maven.surefire.testng.TestNGDirectoryTestSuite.execute(TestNGDirectoryTestSuite.java:91)
	at org.apache.maven.surefire.testng.TestNGProvider.invoke(TestNGProvider.java:137)
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385)
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162)
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507)
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495)
Caused by: net.shibboleth.shared.codec.DecodingException: Unable to base64 decode data: Strict decoding: Data follows the final line separator.
	at net.shibboleth.shared.codec.Base64Support.decode(Base64Support.java:107)
	at org.opensaml.security.x509.X509Support.decodeCertificate(X509Support.java:403)
	... 36 more
Caused by: java.lang.IllegalArgumentException: Strict decoding: Data follows the final line separator.
	at org.apache.commons.codec.binary.BaseNCodec.validateCanonicalByte(BaseNCodec.java:1037)
	at org.apache.commons.codec.binary.Base64.decode(Base64.java:1092)
	at org.apache.commons.codec.binary.BaseNCodec.decode(BaseNCodec.java:710)
	at org.apache.commons.codec.binary.BaseNCodec.decode(BaseNCodec.java:768)
	at net.shibboleth.shared.codec.Base64Support.decode(Base64Support.java:99)
	... 37 more
```

## Run the test with the original commons-codec

```
./mvnw test -Pcodec-1.16.1
```

The `codec-1.16.1` profile uses commons-codec 1.16.1, the version OpenSAML 5.2.3 manages. The same tests pass.
