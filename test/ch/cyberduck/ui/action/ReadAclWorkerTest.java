package ch.cyberduck.ui.action;

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

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Acl;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.AclPermission;

import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id$
 */
public class ReadAclWorkerTest extends AbstractTestCase {

    @Test
    public void testRun() throws Exception {
        final ReadAclWorker worker = new ReadAclWorker(new AclPermission() {
            @Override
            public Acl getPermission(final Path file) throws BackgroundException {
                return new Acl(new Acl.DomainUser("a"), new Acl.Role("r"));
            }

            @Override
            public void setPermission(final Path file, final Acl acl) throws BackgroundException {
                //
            }

            @Override
            public List<Acl.User> getAvailableAclUsers() {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Acl.Role> getAvailableAclRoles(final List<Path> files) {
                throw new UnsupportedOperationException();
            }
        }, Arrays.<Path>asList(new Path("/a", EnumSet.of(Path.Type.file)), new Path("/b", EnumSet.of(Path.Type.file)))) {
            @Override
            public void cleanup(final List<Acl.UserAndRole> result) {
                throw new UnsupportedOperationException();
            }
        };
        assertEquals(1, worker.run().size());
    }
}
