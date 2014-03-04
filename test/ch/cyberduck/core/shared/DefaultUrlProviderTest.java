package ch.cyberduck.core.shared;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.DescriptiveUrl;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.dav.DAVProtocol;
import ch.cyberduck.core.sftp.SFTPProtocol;

import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id$
 */
public class DefaultUrlProviderTest extends AbstractTestCase {

    @Test
    public void testDav() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch", new Credentials(
                "u", "p"
        ));
        host.setDefaultPath("/my/documentroot");
        assertEquals("http://test.cyberduck.ch/my/documentroot/f",
                new DefaultUrlProvider(host).toUrl(new Path("/my/documentroot/f", EnumSet.of(Path.Type.directory))).find(DescriptiveUrl.Type.provider).getUrl());
        assertEquals("http://test.cyberduck.ch/f",
                new DefaultUrlProvider(host).toUrl(new Path("/my/documentroot/f", EnumSet.of(Path.Type.directory))).find(DescriptiveUrl.Type.http).getUrl());
    }

    @Test
    public void testWhitespace() throws Exception {
        final Host host = new Host(new DAVProtocol(), "test.cyberduck.ch ", new Credentials(
                "u", "p"
        ));
        assertEquals("http://test.cyberduck.ch/f",
                new DefaultUrlProvider(host).toUrl(new Path("/f", EnumSet.of(Path.Type.directory))).find(DescriptiveUrl.Type.http).getUrl());
    }

    @Test
    public void testSftp() throws Exception {
        final Host host = new Host(new SFTPProtocol(), "test.cyberduck.ch", new Credentials(
                "u", "p"
        ));
        host.setDefaultPath("/my/documentroot");
        assertEquals("sftp://test.cyberduck.ch/my/documentroot/f",
                new DefaultUrlProvider(host).toUrl(new Path("/my/documentroot/f", EnumSet.of(Path.Type.directory))).find(DescriptiveUrl.Type.provider).getUrl());
        assertEquals("http://test.cyberduck.ch/f",
                new DefaultUrlProvider(host).toUrl(new Path("/my/documentroot/f", EnumSet.of(Path.Type.directory))).find(DescriptiveUrl.Type.http).getUrl());
    }

    @Test
    public void testAbsoluteDocumentRoot() {
        Host host = new Host("localhost");
        host.setDefaultPath("/usr/home/dkocher/public_html");
        Path path = new Path(
                "/usr/home/dkocher/public_html/file", EnumSet.of(Path.Type.directory));
        assertEquals("http://localhost/file", new DefaultUrlProvider(host).toUrl(path).find(DescriptiveUrl.Type.http).getUrl());
        host.setWebURL("http://127.0.0.1/~dkocher");
        assertEquals("http://127.0.0.1/~dkocher/file", new DefaultUrlProvider(host).toUrl(path).find(DescriptiveUrl.Type.http).getUrl());
    }

    @Test
    public void testRelativeDocumentRoot() {
        Host host = new Host("localhost");
        host.setDefaultPath("public_html");
        Path path = new Path(
                "/usr/home/dkocher/public_html/file", EnumSet.of(Path.Type.directory));
        assertEquals("http://localhost/file", new DefaultUrlProvider(host).toUrl(path).find(DescriptiveUrl.Type.http).getUrl());
        host.setWebURL("http://127.0.0.1/~dkocher");
        assertEquals("http://127.0.0.1/~dkocher/file", new DefaultUrlProvider(host).toUrl(path).find(DescriptiveUrl.Type.http).getUrl());
    }

    @Test
    public void testDefaultPathRoot() {
        Host host = new Host("localhost");
        host.setDefaultPath("/");
        Path path = new Path(
                "/file", EnumSet.of(Path.Type.directory));
        assertEquals("http://localhost/file", new DefaultUrlProvider(host).toUrl(path).find(DescriptiveUrl.Type.http).getUrl());
        host.setWebURL("http://127.0.0.1/~dkocher");
        assertEquals("http://127.0.0.1/~dkocher/file", new DefaultUrlProvider(host).toUrl(path).find(DescriptiveUrl.Type.http).getUrl());
    }
}
