package ch.cyberduck.core.sftp;

/*
 * Copyright (c) 2002-2013 David Kocher. All rights reserved.
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to feedback@cyberduck.ch
 */

import ch.cyberduck.core.Cache;
import ch.cyberduck.core.DefaultIOExceptionMappingService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.Permission;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Attributes;

import java.io.IOException;

import ch.ethz.ssh2.SFTPException;
import ch.ethz.ssh2.SFTPv3FileAttributes;

/**
 * @version $Id$
 */
public class SFTPAttributesFeature implements Attributes {

    private SFTPSession session;

    public SFTPAttributesFeature(SFTPSession session) {
        this.session = session;
    }

    @Override
    public PathAttributes find(final Path file) throws BackgroundException {
        try {
            return this.convert(session.sftp().stat(file.getAbsolute()));
        }
        catch(SFTPException e) {
            throw new SFTPExceptionMappingService().map(e);
        }
        catch(IOException e) {
            throw new DefaultIOExceptionMappingService().map(e);
        }
    }

    @Override
    public Attributes withCache(Cache cache) {
        return this;
    }

    public PathAttributes convert(final SFTPv3FileAttributes stat) {
        final PathAttributes attributes = new PathAttributes();
        if(null != stat.size) {
            if(stat.isRegularFile()) {
                attributes.setSize(stat.size);
            }
        }
        if(null != stat.permissions) {
            attributes.setPermission(new Permission(Integer.toString(stat.permissions, 8)));
        }
        if(null != stat.uid) {
            attributes.setOwner(stat.uid.toString());
        }
        if(null != stat.gid) {
            attributes.setGroup(stat.gid.toString());
        }
        if(null != stat.mtime) {
            attributes.setModificationDate(stat.mtime * 1000L);
        }
        if(null != stat.atime) {
            attributes.setAccessedDate(stat.atime * 1000L);
        }
        return attributes;
    }
}
