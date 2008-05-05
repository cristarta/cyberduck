package ch.cyberduck.core.dav;

/*
 *  Copyright (c) 2008 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import com.apple.cocoa.foundation.NSBundle;
import com.apple.cocoa.foundation.NSDictionary;

import ch.cyberduck.core.*;
import ch.cyberduck.core.io.BandwidthThrottle;

import org.apache.log4j.Logger;
import org.apache.webdav.lib.WebdavResource;
import org.apache.webdav.lib.methods.DepthSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;

/**
 * @version $Id: $
 */
public class DAVPath extends Path {
    private static Logger log = Logger.getLogger(DAVPath.class);

    static {
        PathFactory.addFactory(Protocol.WEBDAV, new Factory());
    }

    private static class Factory extends PathFactory {
        protected Path create(Session session, String path, int type) {
            return new DAVPath((DAVSession) session, path, type);
        }

        protected Path create(Session session, String parent, String name, int type) {
            return new DAVPath((DAVSession) session, parent, name, type);
        }

        protected Path create(Session session, String path, Local file) {
            return new DAVPath((DAVSession) session, path, file);
        }

        protected Path create(Session session, NSDictionary dict) {
            return new DAVPath((DAVSession) session, dict);
        }
    }

    private final DAVSession session;

    protected DAVPath(DAVSession s, String parent, String name, int type) {
        super(parent, name, type);
        this.session = s;
    }

    protected DAVPath(DAVSession s, String path, int type) {
        super(path, type);
        this.session = s;
    }

    protected DAVPath(DAVSession s, String parent, Local file) {
        super(parent, file);
        this.session = s;
    }

    protected DAVPath(DAVSession s, NSDictionary dict) {
        super(dict);
        this.session = s;
    }

    public Session getSession() {
        return this.session;
    }

    public void readSize() {
            try {
                session.check();
                session.message(MessageFormat.format(NSBundle.localizedString("Getting size of {0}", "Status", ""),
                        new Object[]{this.getName()}));

                session.DAV.setPath(this.attributes.isDirectory() ?
                        this.getAbsolute() + Path.DELIMITER : this.getAbsolute());

                session.DAV.setProperties(WebdavResource.BASIC, DepthSupport.DEPTH_1);
                attributes.setSize(session.DAV.getGetContentLength());
            }
            catch(IOException e) {
                log.warn("Cannot read size:" + e);
            }
    }

    public void readTimestamp() {
        try {
            session.check();
            session.message(MessageFormat.format(NSBundle.localizedString("Getting timestamp of {0}", "Status", ""),
                    new Object[]{this.getName()}));

            session.DAV.setPath(this.attributes.isDirectory() ?
                    this.getAbsolute() + Path.DELIMITER : this.getAbsolute());

            session.DAV.setProperties(WebdavResource.BASIC, DepthSupport.DEPTH_1);
            attributes.setModificationDate(session.DAV.getGetLastModified());
        }
        catch(IOException e) {
            this.error("Cannot read file attributes", e);
        }
    }


    public void readPermission() {
        ;
    }

    public void delete() {
        log.debug("delete:" + this.toString());
        try {
            session.check();
            session.message(MessageFormat.format(NSBundle.localizedString("Deleting {0}", "Status", ""),
                    new Object[]{this.getName()}));

            session.DAV.deleteMethod(this.getAbsolute());
        }
        catch(IOException e) {
            if(this.attributes.isFile()) {
                this.error("Cannot delete file", e);
            }
            if(this.attributes.isDirectory()) {
                this.error("Cannot delete folder", e);
            }
        }
    }

    public AttributedList list(final ListParseListener listener) {
        AttributedList childs = new AttributedList() {
            public boolean add(Object object) {
                boolean result = super.add(object);
                listener.parsed(this);
                return result;
            }
        };
        try {
            session.check();
            session.message(MessageFormat.format(NSBundle.localizedString("Listing directory {0}", "Status", ""),
                    new Object[]{this.getName()}));

            session.setWorkdir(this);
            session.DAV.setContentType("text/xml");
            WebdavResource[] resources = session.DAV.listWebdavResources();

            for(int i = 0; i < resources.length; i++) {
                Path p = new DAVPath(session, resources[i].getPath(),
                        resources[i].getResourceType().isCollection() ? Path.DIRECTORY_TYPE : Path.FILE_TYPE);
                p.setParent(this);

                p.attributes.setOwner(resources[i].getOwner());
                p.attributes.setModificationDate(resources[i].getGetLastModified());
                p.attributes.setCreationDate(resources[i].getCreationDate());
                p.attributes.setSize(resources[i].getGetContentLength());

                childs.add(p);
            }
        }
        catch(IOException e) {
            childs.attributes().setReadable(false);
            this.error("Listing directory failed", e);
        }
        return childs;
    }


    public void mkdir(boolean recursive) {
        log.debug("mkdir:" + this.getName());
        try {
            if(recursive) {
                if(!this.getParent().exists()) {
                    this.getParent().mkdir(recursive);
                }
            }
            session.check();
            session.message(MessageFormat.format(NSBundle.localizedString("Make directory {0}", "Status", ""),
                    new Object[]{this.getName()}));

            session.DAV.setContentType("text/xml");
            session.DAV.mkcolMethod(this.getAbsolute());
        }
        catch(IOException e) {
            this.error("Cannot create folder", e);
        }

    }

    public void writePermissions(Permission perm, boolean recursive) {
//            log.debug("changePermissions:" + perm);
//            try {
//                session.check();
//                session.message(NSBundle.localizedString("Changing permission of {0} to {1}", "Status", "") + " " + perm.getOctalString() + " (" + this.getName() + ")");
//                session.DAV.aclMethod(this.getAbsolute(), new Ace[]{});
//            }
//            catch(IOException e) {
//                this.error("Cannot change permissions", e);
//            }
    }

    public void rename(String absolute) {
        log.debug("rename:" + absolute);
        try {
            session.check();
            session.message(MessageFormat.format(NSBundle.localizedString("Renaming {0} to {1}", "Status", ""),
                    new Object[]{this.getName(), absolute}));

            session.DAV.moveMethod(this.getAbsolute(), absolute);
            this.setPath(absolute);
        }
        catch(IOException e) {
            if(attributes.isFile()) {
                this.error("Cannot rename file", e);
            }
            if(attributes.isDirectory()) {
                this.error("Cannot rename folder", e);
            }
        }

    }

    public void download(BandwidthThrottle throttle, StreamListener listener, final boolean check) {
        if(attributes.isFile()) {
            OutputStream out = null;
            InputStream in = null;
            try {
                if(check) {
                    session.check();
                }
                session.message(NSBundle.localizedString("Downloading", "Status", "") + " " + this.getName());
                if(this.getStatus().isResume()) {
                    session.DAV.addRequestHeader("Range", "bytes=" + this.getStatus().getCurrent() + "-");
                }
                session.DAV.addRequestHeader("Accept-Encoding", "gzip");
                in = session.DAV.getMethodData(this.getAbsolute());
                if(null == in) {
                    throw new IOException("Unable opening data stream");
                }
                if(!session.DAV.isResume()) {
                    getStatus().setCurrent(0);
                }
                out = new Local.OutputStream(this.getLocal(), this.getStatus().isResume());

                this.download(in, out, throttle, listener);
            }
            catch(IOException e) {
                this.error("Download failed", e);
            }
            finally {
                try {
                    if(in != null) {
                        in.close();
                    }
                    if(out != null) {
                        out.close();
                    }
                }
                catch(IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
        if(attributes.isDirectory()) {
            this.getLocal().mkdir(true);
        }

    }

    public void upload(BandwidthThrottle throttle, final StreamListener listener, final Permission p, final boolean check) {
        try {
            if(check) {
                session.check();
            }
            if(attributes.isFile()) {
                session.message(NSBundle.localizedString("Uploading", "Status", "") + " " + this.getName());

                final InputStream in = new Local.InputStream(this.getLocal());
                try {
                    //Set the content-type to use for this resource, for PUTs
                    session.DAV.setContentType(this.getLocal().getMimeType());
                    if(this.getStatus().isResume()) {
                        session.DAV.addRequestHeader("Content-Range", "bytes "
                                + this.getStatus().getCurrent()
                                + "-" + (this.getLocal().attributes.getSize() - 1)
                                + "/" + this.getLocal().attributes.getSize()
                        );
                        long skipped = in.skip(getStatus().getCurrent());
                        log.info("Skipping " + skipped + " bytes");
                        if(skipped < getStatus().getCurrent()) {
                            throw new IOResumeException("Skipped " + skipped + " bytes instead of " + getStatus().getCurrent());
                        }
                    }
                    if(session.DAV.putMethod(this.getAbsolute(), new InputStream() {
                        long bytesTransferred = getStatus().getCurrent();

                        public int read() throws IOException {
                            return read(new byte[1]);
                        }

                        public int read(byte buffer[], int offset, int length)
                                throws IOException {
                            if(getStatus().isCanceled()) {
                                return -1;
                            }
                            int read = in.read(buffer, offset, length);
                            if(-1 == read) {
                                // End of file
                                getStatus().setComplete(true);
                            }
                            if(read > 0) {
                                listener.bytesSent(read);
                                bytesTransferred += read;
                                getStatus().setCurrent(bytesTransferred);
                            }
                            return read;
                        }
                    }, this.getLocal().attributes.getSize() - this.getStatus().getCurrent())) {
                        // Upload successful
                    }
                }
                finally {
                    try {
                        if(in != null) {
                            in.close();
                        }
                    }
                    catch(IOException e) {
                        log.error(e.getMessage());
                    }
                }
            }
            if(attributes.isDirectory()) {
                this.mkdir();
            }
        }
        catch(IOException e) {
            this.error("Upload failed", e);
        }
    }
}