/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package example.xmlsec.keyinfo.tests;

import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.List;

import net.shibboleth.shared.xml.impl.BasicParserPool;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.keyinfo.KeyInfoSupport;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Reads a signing certificate from SAML metadata. The certificate in {@code idp-metadata.xml} is already in the
 * canonical form the strict decoder in {@code Base64Support} requires: 76 character lines, {@code \n} separators
 * and a final separator. It still cannot be read with commons-codec 1.23.0, because OpenSAML trims the text of every
 * XMLObject while unmarshalling, which removes the final separator before decoding.
 *
 * @author Rob Winch
 */
public class KeyInfoSupportTest {

    @BeforeClass
    public void initializeOpenSaml() throws Exception {
        InitializationService.initialize();
    }

    @Test
    public void getCertificatesWithCanonicalBase64() throws Exception {
        assertCertificateCanBeRead("/idp-metadata.xml");
    }

    /**
     * Same certificate wrapped at 64 characters per line, as in PEM files, and indented. This fails with
     * commons-codec 1.23.0 as well, and passes with 1.16.1.
     */
    @Test
    public void getCertificatesWithLineWrappedBase64() throws Exception {
        assertCertificateCanBeRead("/idp-metadata-line-wrapped.xml");
    }

    private static void assertCertificateCanBeRead(String resource) throws Exception {
        EntityDescriptor entity = unmarshalMetadata(resource);
        IDPSSODescriptor idp = entity.getIDPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");
        KeyDescriptor key = idp.getKeyDescriptors().get(0);
        Assert.assertEquals(key.getUse(), UsageType.SIGNING);
        KeyInfo keyInfo = key.getKeyInfo();

        List<X509Certificate> certificates = KeyInfoSupport.getCertificates(keyInfo);

        Assert.assertEquals(certificates.size(), 1);
        Assert.assertEquals(certificates.get(0).getSubjectX500Principal().getName(), "CN=idp.example.org");
    }

    private static EntityDescriptor unmarshalMetadata(String resource) throws Exception {
        BasicParserPool parserPool = new BasicParserPool();
        parserPool.initialize();
        try (InputStream in = KeyInfoSupportTest.class.getResourceAsStream(resource)) {
            return (EntityDescriptor) XMLObjectProviderRegistrySupport.getUnmarshallerFactory()
                .getUnmarshaller(EntityDescriptor.DEFAULT_ELEMENT_NAME)
                .unmarshall(parserPool.parse(in).getDocumentElement());
        }
    }

}
