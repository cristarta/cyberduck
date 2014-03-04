package ch.cyberduck.core.s3;

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

import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Encryption;

import java.util.Arrays;
import java.util.List;

/**
 * @version $Id$
 */
public class S3EncryptionFeature implements Encryption {

    private S3Session session;

    public S3EncryptionFeature(final S3Session session) {
        this.session = session;
    }

    @Override
    public List<String> getAlgorithms() {
        return Arrays.asList("AES256");
    }

    @Override
    public String getEncryption(final Path file) throws BackgroundException {
        if(file.isFile()) {
            return new S3ObjectDetailService(session).getDetails(file).getServerSideEncryptionAlgorithm();
        }
        return null;
    }

    @Override
    public void setEncryption(final Path file, final String algorithm) throws BackgroundException {
        if(file.isFile()) {
            final S3CopyFeature copy = new S3CopyFeature(session);
            // Copy item in place to write new attributes
            copy.copy(file, file, file.attributes().getStorageClass(), algorithm,
                    new S3AccessControlListFeature(session).getPermission(file));
            file.attributes().setEncryption(algorithm);
        }
    }
}
